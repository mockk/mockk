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

namespace slicer {

bool EntryHook::Apply(lir::CodeIr* code_ir) {
  ir::Builder builder(code_ir->dex_ir);
  const auto ir_method = code_ir->ir_method;

  // construct the hook method declaration
  std::vector<ir::Type*> param_types;
  if ((ir_method->access_flags & dex::kAccStatic) == 0) {
    ir::Type* this_argument_type;
    if (use_object_type_for_this_argument_) {
      this_argument_type = builder.GetType("Ljava/lang/Object;");
    } else {
      this_argument_type = ir_method->decl->parent;
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
  for (auto instr : code_ir->instructions) {
    auto bytecode = dynamic_cast<lir::Bytecode*>(instr);
    if (bytecode == nullptr) {
      continue;
    }
    code_ir->instructions.InsertBefore(bytecode, hook_invoke);
    break;
  }

  return true;
}

bool ExitHook::Apply(lir::CodeIr* code_ir) {
  ir::Builder builder(code_ir->dex_ir);
  const auto ir_method = code_ir->ir_method;
  const auto return_type = ir_method->decl->prototype->return_type;

  // do we have a void-return method?
  bool return_void = (::strcmp(return_type->descriptor->c_str(), "V") == 0);

  // construct the hook method declaration
  std::vector<ir::Type*> param_types;
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
    auto bytecode = dynamic_cast<lir::Bytecode*>(instr);
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

    // invoke hook bytecode
    auto args = code_ir->Alloc<lir::VRegRange>(reg, reg_count);
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
    }
  }

  return true;
}

bool DetourVirtualInvoke::Apply(lir::CodeIr* code_ir) {
  ir::Builder builder(code_ir->dex_ir);

  // search for matching invoke-virtual[/range] bytecodes
  for (auto instr : code_ir->instructions) {
    auto bytecode = dynamic_cast<lir::Bytecode*>(instr);
    if (bytecode == nullptr) {
      continue;
    }

    dex::Opcode new_call_opcode = dex::OP_NOP;
    switch (bytecode->opcode) {
      case dex::OP_INVOKE_VIRTUAL:
        new_call_opcode = dex::OP_INVOKE_STATIC;
        break;
      case dex::OP_INVOKE_VIRTUAL_RANGE:
        new_call_opcode = dex::OP_INVOKE_STATIC_RANGE;
        break;
      default:
        // skip instruction ...
        continue;
    }
    assert(new_call_opcode != dex::OP_NOP);

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
      param_types.insert(param_types.end(), orig_param_types.begin(), orig_param_types.end());
    }

    auto ir_proto = builder.GetProto(orig_method->prototype->return_type,
                                     builder.GetTypeList(param_types));

    auto ir_method_decl = builder.GetMethodDecl(
        builder.GetAsciiString(detour_method_id_.method_name), ir_proto,
        builder.GetType(detour_method_id_.class_descriptor));

    auto detour_method = code_ir->Alloc<lir::Method>(ir_method_decl, ir_method_decl->orig_index);

    // We mutate the original invoke bytecode in-place: this is ok
    // because lir::Instructions can't be shared (referenced multiple times)
    // in the code IR. It's also simpler and more efficient than allocating a
    // new IR invoke bytecode.
    bytecode->opcode = new_call_opcode;
    bytecode->operands[1] = detour_method;
  }

  return true;
}

// Register re-numbering visitor
// (renumbers vN to vN+shift)
class RegsRenumberVisitor : public lir::Visitor {
 public:
  RegsRenumberVisitor(int shift) : shift_(shift) {
    SLICER_CHECK(shift > 0);
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
  SLICER_CHECK(left_to_allocate_ > 0);
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
  SLICER_CHECK(ir_method->code->ins_count > 0);
  SLICER_CHECK(left_to_allocate_ > 0);

  // build a param list with the explicit "this" argument for non-static methods
  std::vector<ir::Type*> param_types;
  if ((ir_method->access_flags & dex::kAccStatic) == 0) {
    param_types.push_back(ir_method->decl->parent);
  }
  if (ir_method->decl->prototype->param_types != nullptr) {
    const auto& orig_param_types = ir_method->decl->prototype->param_types->types;
    param_types.insert(param_types.end(), orig_param_types.begin(), orig_param_types.end());
  }

  const dex::u4 shift = left_to_allocate_;

  Allocate(code_ir, ir_method->code->registers, left_to_allocate_);
  assert(left_to_allocate_ == 0);

  const dex::u4 regs = ir_method->code->registers;
  const dex::u4 ins_count = ir_method->code->ins_count;
  SLICER_CHECK(regs >= ins_count);

  // generate the args "relocation" instructions
  auto first_instr = code_ir->instructions.begin();
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
    code_ir->instructions.insert(first_instr, move);
  }
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
  SLICER_CHECK(code->registers + allocate_count_ <= (1 << 16));

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
  SLICER_CHECK(ir_method != nullptr);
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
