/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "slicer/instrumentation.h"

#include "slicer/dex_ir_builder.h"

#include <iomanip>
#include <sstream>

namespace slicer {

namespace {

struct BytecodeConvertingVisitor : public lir::Visitor {
  lir::Bytecode* out = nullptr;
  bool Visit(lir::Bytecode* bytecode) {
    out = bytecode;
    return true;
  }
};

void BoxValue(lir::Bytecode* bytecode,
              lir::CodeIr* code_ir,
              ir::Type* type,
              dex::u4 src_reg,
              dex::u4 dst_reg) {
  bool is_wide = false;
  const char* boxed_type_name = nullptr;
  switch (*(type->descriptor)->c_str()) {
    case 'Z':
      boxed_type_name = "Ljava/lang/Boolean;";
      break;
    case 'B':
      boxed_type_name = "Ljava/lang/Byte;";
      break;
    case 'C':
      boxed_type_name = "Ljava/lang/Character;";
      break;
    case 'S':
      boxed_type_name = "Ljava/lang/Short;";
      break;
    case 'I':
      boxed_type_name = "Ljava/lang/Integer;";
      break;
    case 'J':
      is_wide = true;
      boxed_type_name = "Ljava/lang/Long;";
      break;
    case 'F':
      boxed_type_name = "Ljava/lang/Float;";
      break;
    case 'D':
      is_wide = true;
      boxed_type_name = "Ljava/lang/Double;";
      break;
  }
  SLICER_CHECK_NE(boxed_type_name, nullptr);

  ir::Builder builder(code_ir->dex_ir);
  std::vector<ir::Type*> param_types;
  param_types.push_back(type);

  auto boxed_type = builder.GetType(boxed_type_name);
  auto ir_proto = builder.GetProto(boxed_type, builder.GetTypeList(param_types));

  auto ir_method_decl = builder.GetMethodDecl(
      builder.GetAsciiString("valueOf"), ir_proto, boxed_type);

  auto boxing_method = code_ir->Alloc<lir::Method>(ir_method_decl, ir_method_decl->orig_index);

  auto args = code_ir->Alloc<lir::VRegRange>(src_reg, 1 + is_wide);
  auto boxing_invoke = code_ir->Alloc<lir::Bytecode>();
  boxing_invoke->opcode = dex::OP_INVOKE_STATIC_RANGE;
  boxing_invoke->operands.push_back(args);
  boxing_invoke->operands.push_back(boxing_method);
  code_ir->instructions.InsertBefore(bytecode, boxing_invoke);

  auto move_result = code_ir->Alloc<lir::Bytecode>();
  move_result->opcode = dex::OP_MOVE_RESULT_OBJECT;
  move_result->operands.push_back(code_ir->Alloc<lir::VReg>(dst_reg));
  code_ir->instructions.InsertBefore(bytecode, move_result);
}

std::string MethodLabel(ir::EncodedMethod* ir_method) {
  auto signature_str = ir_method->decl->prototype->Signature();
  return ir_method->decl->parent->Decl() + "->" + ir_method->decl->name->c_str() + signature_str;
}

}  // namespace

bool EntryHook::Apply(lir::CodeIr* code_ir) {
  lir::Bytecode* bytecode = nullptr;
  // find the first bytecode in the method body to insert the hook before it
  for (auto instr : code_ir->instructions) {
    BytecodeConvertingVisitor visitor;
    instr->Accept(&visitor);
    bytecode = visitor.out;
    if (bytecode != nullptr) {
      break;
    }
  }
  if (bytecode == nullptr) {
    return false;
  }
  if (tweak_ == Tweak::ArrayParams) {
    return InjectArrayParamsHook(code_ir, bytecode);
  }

  ir::Builder builder(code_ir->dex_ir);
  const auto ir_method = code_ir->ir_method;

  // construct the hook method declaration
  std::vector<ir::Type*> param_types;
  if ((ir_method->access_flags & dex::kAccStatic) == 0) {
    ir::Type* this_argument_type;
    switch (tweak_) {
      case Tweak::ThisAsObject:
        this_argument_type = builder.GetType("Ljava/lang/Object;");
        break;
      default:
        this_argument_type = ir_method->decl->parent;
        break;
    }
    param_types.push_back(this_argument_type);
  }
  if (ir_method->decl->prototype->param_types != nullptr) {
    const auto& orig_param_types = ir_method->decl->prototype->param_types->types;
    param_types.insert(param_types.end(), orig_param_types.begin(), orig_param_types.end());
  }

  auto ir_proto = builder.GetProto(builder.GetType("V"),
                                   builder.GetTypeList(param_types));

  auto ir_method_decl = builder.GetMethodDecl(
      builder.GetAsciiString(hook_method_id_.method_name), ir_proto,
      builder.GetType(hook_method_id_.class_descriptor));

  auto hook_method = code_ir->Alloc<lir::Method>(ir_method_decl, ir_method_decl->orig_index);

  // argument registers
  auto regs = ir_method->code->registers;
  auto args_count = ir_method->code->ins_count;
  auto args = code_ir->Alloc<lir::VRegRange>(regs - args_count, args_count);

  // invoke hook bytecode
  auto hook_invoke = code_ir->Alloc<lir::Bytecode>();
  hook_invoke->opcode = dex::OP_INVOKE_STATIC_RANGE;
  hook_invoke->operands.push_back(args);
  hook_invoke->operands.push_back(hook_method);

  // insert the hook before the first bytecode in the method body
  code_ir->instructions.InsertBefore(bytecode, hook_invoke);
  return true;
}

void GenerateShiftParamsCode(lir::CodeIr* code_ir, lir::Instruction* position, dex::u4 shift) {
  const auto ir_method = code_ir->ir_method;

  // Since the goal is to relocate the registers when extra scratch registers are needed,
  // if there are no parameters this is a no-op.
  if (ir_method->code->ins_count == 0) {
    return;
  }

  // build a param list with the explicit "this" argument for non-static methods
  std::vector<ir::Type*> param_types;
  if ((ir_method->access_flags & dex::kAccStatic) == 0) {
    param_types.push_back(ir_method->decl->parent);
  }
  if (ir_method->decl->prototype->param_types != nullptr) {
    const auto& orig_param_types = ir_method->decl->prototype->param_types->types;
    param_types.insert(param_types.end(), orig_param_types.begin(), orig_param_types.end());
  }

  const dex::u4 regs = ir_method->code->registers;
  const dex::u4 ins_count = ir_method->code->ins_count;
  SLICER_CHECK_GE(regs, ins_count);

  // generate the args "relocation" instructions
  dex::u4 reg = regs - ins_count;
  for (const auto& type : param_types) {
    auto move = code_ir->Alloc<lir::Bytecode>();
    switch (type->GetCategory()) {
      case ir::Type::Category::Reference:
        move->opcode = dex::OP_MOVE_OBJECT_16;
        move->operands.push_back(code_ir->Alloc<lir::VReg>(reg - shift));
        move->operands.push_back(code_ir->Alloc<lir::VReg>(reg));
        reg += 1;
        break;
      case ir::Type::Category::Scalar:
        move->opcode = dex::OP_MOVE_16;
        move->operands.push_back(code_ir->Alloc<lir::VReg>(reg - shift));
        move->operands.push_back(code_ir->Alloc<lir::VReg>(reg));
        reg += 1;
        break;
      case ir::Type::Category::WideScalar:
        move->opcode = dex::OP_MOVE_WIDE_16;
        move->operands.push_back(code_ir->Alloc<lir::VRegPair>(reg - shift));
        move->operands.push_back(code_ir->Alloc<lir::VRegPair>(reg));
        reg += 2;
        break;
      case ir::Type::Category::Void:
        SLICER_FATAL("void parameter type");
    }
    code_ir->instructions.InsertBefore(position, move);
  }
}

bool EntryHook::InjectArrayParamsHook(lir::CodeIr* code_ir, lir::Bytecode* bytecode) {
  ir::Builder builder(code_ir->dex_ir);
  const auto ir_method = code_ir->ir_method;
  auto param_types_list = ir_method->decl->prototype->param_types;
  auto param_types = param_types_list != nullptr ? param_types_list->types : std::vector<ir::Type*>();
  bool is_static = (ir_method->access_flags & dex::kAccStatic) != 0;

  // number of registers that we need to operate
  dex::u2 regs_count = 3;
  auto non_param_regs = ir_method->code->registers - ir_method->code->ins_count;

  // do we have enough registers to operate?
  bool needsExtraRegs = non_param_regs < regs_count;
  if (needsExtraRegs) {
    // we don't have enough registers, so we allocate more, we will shift
    // params to their original registers later.
    code_ir->ir_method->code->registers += regs_count - non_param_regs;
  }

  // use three first registers:
  // all three are needed when we "aput" a string/boxed-value (1) into an array (2) at an index (3)

  // register that will store size of during allocation
  // later will be reused to store index when do "aput"
  dex::u4 array_size_reg = 0;
  // register that will store an array that will be passed
  // as a parameter in entry hook
  dex::u4 array_reg = 1;
  // stores result of boxing (if it's needed); also stores the method signature string
  dex::u4 value_reg = 2;
  // array size bytecode
  auto const_size_op = code_ir->Alloc<lir::Bytecode>();
  const_size_op->opcode = dex::OP_CONST;
  const_size_op->operands.push_back(code_ir->Alloc<lir::VReg>(array_size_reg));
  const_size_op->operands.push_back(code_ir->Alloc<lir::Const32>(
      2 + param_types.size())); // method signature + params + "this" object
  code_ir->instructions.InsertBefore(bytecode, const_size_op);

  // allocate array
  const auto obj_array_type = builder.GetType("[Ljava/lang/Object;");
  auto allocate_array_op = code_ir->Alloc<lir::Bytecode>();
  allocate_array_op->opcode = dex::OP_NEW_ARRAY;
  allocate_array_op->operands.push_back(code_ir->Alloc<lir::VReg>(array_reg));
  allocate_array_op->operands.push_back(code_ir->Alloc<lir::VReg>(array_size_reg));
  allocate_array_op->operands.push_back(
      code_ir->Alloc<lir::Type>(obj_array_type, obj_array_type->orig_index));
  code_ir->instructions.InsertBefore(bytecode, allocate_array_op);

  // fill the array with parameters passed into function

  std::vector<ir::Type*> types;
  types.push_back(builder.GetType("Ljava/lang/String;")); // method signature string
  if (!is_static) {
    types.push_back(ir_method->decl->parent); // "this" object
  }

  types.insert(types.end(), param_types.begin(), param_types.end()); // parameters

  // register where params start
  dex::u4 current_reg = ir_method->code->registers - ir_method->code->ins_count;
  // reuse not needed anymore register to store indexes
  dex::u4 array_index_reg = array_size_reg;
  int i = 0;
  for (auto type: types) {
    dex::u4 src_reg = 0;
    if (i == 0) { // method signature string
      // e.g. const-string v2, "(I[Ljava/lang/String;)Ljava/lang/String;"
      // for (int, String[]) -> String
      auto const_str_op = code_ir->Alloc<lir::Bytecode>();
      const_str_op->opcode = dex::OP_CONST_STRING;
      const_str_op->operands.push_back(code_ir->Alloc<lir::VReg>(value_reg)); // dst
      auto method_label = builder.GetAsciiString(MethodLabel(ir_method).c_str());
      const_str_op->operands.push_back(
          code_ir->Alloc<lir::String>(method_label, method_label->orig_index)); // src
      code_ir->instructions.InsertBefore(bytecode, const_str_op);
      src_reg = value_reg;
    } else if (type->GetCategory() != ir::Type::Category::Reference) {
      BoxValue(bytecode, code_ir, type, current_reg, value_reg);
      src_reg = value_reg;
      current_reg += 1 + (type->GetCategory() == ir::Type::Category::WideScalar);
    } else {
      src_reg = current_reg;
      current_reg++;
    }

    auto index_const_op = code_ir->Alloc<lir::Bytecode>();
    index_const_op->opcode = dex::OP_CONST;
    index_const_op->operands.push_back(code_ir->Alloc<lir::VReg>(array_index_reg));
    index_const_op->operands.push_back(code_ir->Alloc<lir::Const32>(i++));
    code_ir->instructions.InsertBefore(bytecode, index_const_op);

    auto aput_op = code_ir->Alloc<lir::Bytecode>();
    aput_op->opcode = dex::OP_APUT_OBJECT;
    aput_op->operands.push_back(code_ir->Alloc<lir::VReg>(src_reg));
    aput_op->operands.push_back(code_ir->Alloc<lir::VReg>(array_reg));
    aput_op->operands.push_back(code_ir->Alloc<lir::VReg>(array_index_reg));
    code_ir->instructions.InsertBefore(bytecode, aput_op);

    // if function is static, then jumping over index 1
    //  since null should be be passed in this case
    if (i == 1 && is_static) i++;
  }

  std::vector<ir::Type*> hook_param_types;
  hook_param_types.push_back(obj_array_type);

  auto ir_proto = builder.GetProto(builder.GetType("V"),
                                   builder.GetTypeList(hook_param_types));

  auto ir_method_decl = builder.GetMethodDecl(
      builder.GetAsciiString(hook_method_id_.method_name), ir_proto,
      builder.GetType(hook_method_id_.class_descriptor));

  auto hook_method = code_ir->Alloc<lir::Method>(ir_method_decl, ir_method_decl->orig_index);
  auto args = code_ir->Alloc<lir::VRegRange>(array_reg, 1);
  auto hook_invoke = code_ir->Alloc<lir::Bytecode>();
  hook_invoke->opcode = dex::OP_INVOKE_STATIC_RANGE;
  hook_invoke->operands.push_back(args);
  hook_invoke->operands.push_back(hook_method);
  code_ir->instructions.InsertBefore(bytecode, hook_invoke);

  // clean up registries used by us
  // registers are assigned to a marker value 0xFE_FE_FE_FE (decimal
  // value: -16843010) to help identify use of uninitialized registers.
  for (dex::u2 i = 0; i < regs_count; ++i) {
    auto cleanup = code_ir->Alloc<lir::Bytecode>();
    cleanup->opcode = dex::OP_CONST;
    cleanup->operands.push_back(code_ir->Alloc<lir::VReg>(i));
    cleanup->operands.push_back(code_ir->Alloc<lir::Const32>(0xFEFEFEFE));
    code_ir->instructions.InsertBefore(bytecode, cleanup);
  }

  // now we have to shift params to their original registers
  if (needsExtraRegs) {
    GenerateShiftParamsCode(code_ir, bytecode, regs_count - non_param_regs);
  }
  return true;
}

bool ExitHook::Apply(lir::CodeIr* code_ir) {
  ir::Builder builder(code_ir->dex_ir);
  const auto ir_method = code_ir->ir_method;
  const auto declared_return_type = ir_method->decl->prototype->return_type;
  bool return_as_object = (tweak_ & Tweak::ReturnAsObject) != 0;
  // do we have a void-return method?
  bool return_void = (::strcmp(declared_return_type->descriptor->c_str(), "V") == 0);
  // returnAsObject supports only object return type;
  SLICER_CHECK(!return_as_object ||
      (declared_return_type->GetCategory() == ir::Type::Category::Reference));
  const auto return_type = return_as_object ? builder.GetType("Ljava/lang/Object;")
      : declared_return_type;

  bool pass_method_signature = (tweak_ & Tweak::PassMethodSignature) != 0;
  // construct the hook method declaration
  std::vector<ir::Type*> param_types;
  if (pass_method_signature) {
    param_types.push_back(builder.GetType("Ljava/lang/String;"));
  }
  if (!return_void) {
    param_types.push_back(return_type);
  }

  auto ir_proto = builder.GetProto(return_type, builder.GetTypeList(param_types));

  auto ir_method_decl = builder.GetMethodDecl(
      builder.GetAsciiString(hook_method_id_.method_name), ir_proto,
      builder.GetType(hook_method_id_.class_descriptor));

  auto hook_method = code_ir->Alloc<lir::Method>(ir_method_decl, ir_method_decl->orig_index);

  // find and instrument all return instructions
  for (auto instr : code_ir->instructions) {
    BytecodeConvertingVisitor visitor;
    instr->Accept(&visitor);
    auto bytecode = visitor.out;
    if (bytecode == nullptr) {
      continue;
    }

    dex::Opcode move_result_opcode = dex::OP_NOP;
    dex::u4 reg = 0;
    int reg_count = 0;
    switch (bytecode->opcode) {
      case dex::OP_RETURN_VOID:
        SLICER_CHECK(return_void);
        break;
      case dex::OP_RETURN:
        SLICER_CHECK(!return_void);
        move_result_opcode = dex::OP_MOVE_RESULT;
        reg = bytecode->CastOperand<lir::VReg>(0)->reg;
        reg_count = 1;
        break;
      case dex::OP_RETURN_OBJECT:
        SLICER_CHECK(!return_void);
        move_result_opcode = dex::OP_MOVE_RESULT_OBJECT;
        reg = bytecode->CastOperand<lir::VReg>(0)->reg;
        reg_count = 1;
        break;
      case dex::OP_RETURN_WIDE:
        SLICER_CHECK(!return_void);
        move_result_opcode = dex::OP_MOVE_RESULT_WIDE;
        reg = bytecode->CastOperand<lir::VRegPair>(0)->base_reg;
        reg_count = 2;
        break;
      default:
        // skip the bytecode...
        continue;
    }

    dex::u4 scratch_reg = 0;
    // load method signature into scratch_reg
    if (pass_method_signature) {
      // is there a register that can be overtaken
      bool needsScratchReg = ir_method->code->registers < reg_count + 1;
      if (needsScratchReg) {
        // don't renumber registers underneath us
        slicer::AllocateScratchRegs alloc_regs(1, false);
        alloc_regs.Apply(code_ir);
      }

      // we need use one register before results to put signature there
      // however result starts in register 0, thefore it is shifted
      // to register 1
      if (reg == 0 && bytecode->opcode != dex::OP_RETURN_VOID) {
        auto move_op = code_ir->Alloc<lir::Bytecode>();
        switch (bytecode->opcode) {
          case dex::OP_RETURN_OBJECT:
            move_op->opcode = dex::OP_MOVE_OBJECT_16;
            move_op->operands.push_back(code_ir->Alloc<lir::VReg>(reg + 1));
            move_op->operands.push_back(code_ir->Alloc<lir::VReg>(reg));
            break;
          case dex::OP_RETURN:
            move_op->opcode = dex::OP_MOVE_16;
            move_op->operands.push_back(code_ir->Alloc<lir::VReg>(reg + 1));
            move_op->operands.push_back(code_ir->Alloc<lir::VReg>(reg));
            break;
          case dex::OP_RETURN_WIDE:
            move_op->opcode = dex::OP_MOVE_WIDE_16;
            move_op->operands.push_back(code_ir->Alloc<lir::VRegPair>(reg + 1));
            move_op->operands.push_back(code_ir->Alloc<lir::VRegPair>(reg));
            break;
          default: {
              std::stringstream ss;
              ss <<"Unexpected bytecode opcode: " << bytecode->opcode;
              SLICER_FATAL(ss.str());
            }
        }
        code_ir->instructions.InsertBefore(bytecode, move_op);
        // return is the last call, return is shifted to one, so taking over 0 registry
        scratch_reg = 0;
      } else {
        // return is the last call, so we're taking over previous registry
        scratch_reg = bytecode->opcode == dex::OP_RETURN_VOID ? 0 : reg - 1;
      }


      // return is the last call, so we're taking over previous registry
      auto method_label = builder.GetAsciiString(MethodLabel(ir_method).c_str());
      auto const_str_op = code_ir->Alloc<lir::Bytecode>();
      const_str_op->opcode = dex::OP_CONST_STRING;
      const_str_op->operands.push_back(code_ir->Alloc<lir::VReg>(scratch_reg)); // dst
      const_str_op->operands.push_back(code_ir->Alloc<lir::String>(method_label, method_label->orig_index)); // src
      code_ir->instructions.InsertBefore(bytecode, const_str_op);
    }

    auto args = pass_method_signature
        ? code_ir->Alloc<lir::VRegRange>(scratch_reg, reg_count + 1)
        : code_ir->Alloc<lir::VRegRange>(reg, reg_count);
    auto hook_invoke = code_ir->Alloc<lir::Bytecode>();
    hook_invoke->opcode = dex::OP_INVOKE_STATIC_RANGE;
    hook_invoke->operands.push_back(args);
    hook_invoke->operands.push_back(hook_method);
    code_ir->instructions.InsertBefore(bytecode, hook_invoke);

    // move result back to the right register
    //
    // NOTE: we're reusing the original return's operand,
    //   which is valid and more efficient than allocating
    //   a new LIR node, but it's also fragile: we need to be
    //   very careful about mutating shared nodes.
    //
    if (move_result_opcode != dex::OP_NOP) {
      auto move_result = code_ir->Alloc<lir::Bytecode>();
      move_result->opcode = move_result_opcode;
      move_result->operands.push_back(bytecode->operands[0]);
      code_ir->instructions.InsertBefore(bytecode, move_result);

      if ((tweak_ & Tweak::ReturnAsObject) != 0) {
        auto check_cast = code_ir->Alloc<lir::Bytecode>();
        check_cast->opcode = dex::OP_CHECK_CAST;
        check_cast->operands.push_back(code_ir->Alloc<lir::VReg>(reg));
        check_cast->operands.push_back(
            code_ir->Alloc<lir::Type>(declared_return_type, declared_return_type->orig_index));
        code_ir->instructions.InsertBefore(bytecode, check_cast);
      }
    }
  }

  return true;
}

bool DetourHook::Apply(lir::CodeIr* code_ir) {
  ir::Builder builder(code_ir->dex_ir);

  // search for matching invoke-virtual[/range] bytecodes
  for (auto instr : code_ir->instructions) {
    BytecodeConvertingVisitor visitor;
    instr->Accept(&visitor);
    auto bytecode = visitor.out;
    if (bytecode == nullptr) {
      continue;
    }

    dex::Opcode new_call_opcode = GetNewOpcode(bytecode->opcode);
    if (new_call_opcode == dex::OP_NOP) {
      continue;
    }

    auto orig_method = bytecode->CastOperand<lir::Method>(1)->ir_method;
    if (!orig_method_id_.Match(orig_method)) {
      // this is not the method you're looking for...
      continue;
    }

    // construct the detour method declaration
    // (matching the original method, plus an explicit "this" argument)
    std::vector<ir::Type*> param_types;
    param_types.push_back(orig_method->parent);
    if (orig_method->prototype->param_types != nullptr) {
      const auto& orig_param_types = orig_method->prototype->param_types->types;
      param_types.insert(param_types.end(), orig_param_types.begin(),
                         orig_param_types.end());
    }

    auto ir_proto = builder.GetProto(orig_method->prototype->return_type,
                                     builder.GetTypeList(param_types));

    auto ir_method_decl = builder.GetMethodDecl(
        builder.GetAsciiString(detour_method_id_.method_name), ir_proto,
        builder.GetType(detour_method_id_.class_descriptor));

    auto detour_method =
        code_ir->Alloc<lir::Method>(ir_method_decl, ir_method_decl->orig_index);

    // We mutate the original invoke bytecode in-place: this is ok
    // because lir::Instructions can't be shared (referenced multiple times)
    // in the code IR. It's also simpler and more efficient than allocating a
    // new IR invoke bytecode.
    bytecode->opcode = new_call_opcode;
    bytecode->operands[1] = detour_method;
  }

  return true;
}

dex::Opcode DetourVirtualInvoke::GetNewOpcode(dex::Opcode opcode) {
  switch (opcode) {
    case dex::OP_INVOKE_VIRTUAL:
      return dex::OP_INVOKE_STATIC;
    case dex::OP_INVOKE_VIRTUAL_RANGE:
      return dex::OP_INVOKE_STATIC_RANGE;
    default:
      // skip instruction ...
      return dex::OP_NOP;
  }
}

dex::Opcode DetourInterfaceInvoke::GetNewOpcode(dex::Opcode opcode) {
  switch (opcode) {
    case dex::OP_INVOKE_INTERFACE:
      return dex::OP_INVOKE_STATIC;
    case dex::OP_INVOKE_INTERFACE_RANGE:
      return dex::OP_INVOKE_STATIC_RANGE;
    default:
      // skip instruction ...
      return dex::OP_NOP;
  }
}

// Register re-numbering visitor
// (renumbers vN to vN+shift)
class RegsRenumberVisitor : public lir::Visitor {
 public:
  explicit RegsRenumberVisitor(int shift) : shift_(shift) {
    SLICER_CHECK_GT(shift, 0);
  }

 private:
  virtual bool Visit(lir::Bytecode* bytecode) override {
    for (auto operand : bytecode->operands) {
      operand->Accept(this);
    }
    return true;
  }

  virtual bool Visit(lir::DbgInfoAnnotation* dbg_annotation) override {
    for (auto operand : dbg_annotation->operands) {
      operand->Accept(this);
    }
    return true;
  }

  virtual bool Visit(lir::VReg* vreg) override {
    vreg->reg += shift_;
    return true;
  }

  virtual bool Visit(lir::VRegPair* vreg_pair) override {
    vreg_pair->base_reg += shift_;
    return true;
  }

  virtual bool Visit(lir::VRegList* vreg_list) override {
    for (auto& reg : vreg_list->registers) {
      reg += shift_;
    }
    return true;
  }

  virtual bool Visit(lir::VRegRange* vreg_range) override {
    vreg_range->base_reg += shift_;
    return true;
  }

 private:
  int shift_ = 0;
};

// Try to allocate registers by renumbering the existing allocation
//
// NOTE: we can't bump the register count over 16 since it may
//  make existing bytecodes "unencodable" (if they have 4 bit reg fields)
//
void AllocateScratchRegs::RegsRenumbering(lir::CodeIr* code_ir) {
  SLICER_CHECK_GT(left_to_allocate_, 0);
  int delta = std::min(left_to_allocate_,
                       16 - static_cast<int>(code_ir->ir_method->code->registers));
  if (delta < 1) {
    // can't allocate any registers through renumbering
    return;
  }
  assert(delta <= 16);

  // renumber existing registers
  RegsRenumberVisitor visitor(delta);
  for (auto instr : code_ir->instructions) {
    instr->Accept(&visitor);
  }

  // we just allocated "delta" registers (v0..vX)
  Allocate(code_ir, 0, delta);
}

// Allocates registers by generating prologue code to relocate params
// into their original registers (parameters are allocated in the last IN registers)
//
// There are three types of register moves depending on the value type:
// 1. vreg -> vreg
// 2. vreg/wide -> vreg/wide
// 3. vreg/obj -> vreg/obj
//
void AllocateScratchRegs::ShiftParams(lir::CodeIr* code_ir) {
  const auto ir_method = code_ir->ir_method;
  SLICER_CHECK_GT(left_to_allocate_, 0);

  const dex::u4 shift = left_to_allocate_;
  Allocate(code_ir, ir_method->code->registers, left_to_allocate_);
  assert(left_to_allocate_ == 0);

  // generate the args "relocation" instructions
  auto first_instr = *(code_ir->instructions.begin());
  GenerateShiftParamsCode(code_ir, first_instr, shift);
}

// Mark [first_reg, first_reg + count) as scratch registers
void AllocateScratchRegs::Allocate(lir::CodeIr* code_ir, dex::u4 first_reg, int count) {
  SLICER_CHECK(count > 0 && count <= left_to_allocate_);
  code_ir->ir_method->code->registers += count;
  left_to_allocate_ -= count;
  for (int i = 0; i < count; ++i) {
    SLICER_CHECK(scratch_regs_.insert(first_reg + i).second);
  }
}

// Allocate scratch registers without doing a full register allocation:
//
// 1. if there are not params, increase the method regs count and we're done
// 2. if the method uses less than 16 registers, we can renumber the existing registers
// 3. if we still have registers to allocate, increase the method registers count,
//     and generate prologue code to shift the param regs into their original registers
//
bool AllocateScratchRegs::Apply(lir::CodeIr* code_ir) {
  const auto code = code_ir->ir_method->code;
  // .dex bytecode allows up to 64k vregs
  SLICER_CHECK_LE(code->registers + allocate_count_, (1 << 16));

  scratch_regs_.clear();
  left_to_allocate_ = allocate_count_;

  // can we allocate by simply incrementing the method regs count?
  if (code->ins_count == 0) {
    Allocate(code_ir, code->registers, left_to_allocate_);
    return true;
  }

  // allocate as many registers as possible using renumbering
  if (allow_renumbering_) {
    RegsRenumbering(code_ir);
  }

  // if we still have registers to allocate, generate prologue
  // code to shift the params into their original registers
  if (left_to_allocate_ > 0) {
    ShiftParams(code_ir);
  }

  assert(left_to_allocate_ == 0);
  assert(scratch_regs_.size() == size_t(allocate_count_));
  return true;
}

bool MethodInstrumenter::InstrumentMethod(ir::EncodedMethod* ir_method) {
  SLICER_CHECK_NE(ir_method, nullptr);
  if (ir_method->code == nullptr) {
    // can't instrument abstract methods
    return false;
  }

  // apply all the queued transformations
  lir::CodeIr code_ir(ir_method, dex_ir_);
  for (const auto& transformation : transformations_) {
    if (!transformation->Apply(&code_ir)) {
      // the transformation failed, bail out...
      return false;
    }
  }
  code_ir.Assemble();
  return true;
}

bool MethodInstrumenter::InstrumentMethod(const ir::MethodId& method_id) {
  // locate the method to be instrumented
  ir::Builder builder(dex_ir_);
  auto ir_method = builder.FindMethod(method_id);
  if (ir_method == nullptr) {
    // we couldn't find the specified method
    return false;
  }
  return InstrumentMethod(ir_method);
}

}  // namespace slicer
