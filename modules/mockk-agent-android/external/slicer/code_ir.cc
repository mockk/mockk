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

#include "slicer/code_ir.h"

#include "slicer/bytecode_encoder.h"
#include "slicer/common.h"
#include "slicer/debuginfo_encoder.h"
#include "slicer/dex_bytecode.h"
#include "slicer/dex_format.h"
#include "slicer/dex_ir.h"
#include "slicer/dex_leb128.h"
#include "slicer/tryblocks_encoder.h"

#include <algorithm>
#include <iomanip>
#include <sstream>
#include <vector>

namespace lir {

void CodeIr::Assemble() {
  auto ir_code = ir_method->code;
  SLICER_CHECK_NE(ir_code, nullptr);

  // new .dex bytecode
  //
  // NOTE: this must be done before the debug information and
  //   try/catch blocks since here is where we update the final offsets
  //
  BytecodeEncoder bytecode_encoder(instructions);
  bytecode_encoder.Encode(ir_code, dex_ir);

  // debug information
  if (ir_code->debug_info != nullptr) {
    DebugInfoEncoder dbginfo_encoder(instructions);
    dbginfo_encoder.Encode(ir_method, dex_ir);
  }

  // try/catch blocks
  TryBlocksEncoder try_blocks_encoder(instructions);
  try_blocks_encoder.Encode(ir_code, dex_ir);
}

void CodeIr::DisassembleTryBlocks(const ir::Code* ir_code) {
  int nextTryBlockId = 1;
  for (const auto& tryBlock : ir_code->try_blocks) {
    auto try_block_begin = Alloc<TryBlockBegin>();
    try_block_begin->id = nextTryBlockId++;
    try_block_begin->offset = tryBlock.start_addr;

    auto try_block_end = Alloc<TryBlockEnd>();
    try_block_end->try_begin = try_block_begin;
    try_block_end->offset = tryBlock.start_addr + tryBlock.insn_count;

    // parse the catch handlers
    const dex::u1* ptr =
        ir_code->catch_handlers.ptr<dex::u1>() + tryBlock.handler_off;
    int catchCount = dex::ReadSLeb128(&ptr);

    for (int catchIndex = 0; catchIndex < std::abs(catchCount); ++catchIndex) {
      CatchHandler handler = {};

      // type
      dex::u4 type_index = dex::ReadULeb128(&ptr);
      handler.ir_type = dex_ir->types_map[type_index];
      SLICER_CHECK_NE(handler.ir_type, nullptr);

      // address
      dex::u4 address = dex::ReadULeb128(&ptr);
      handler.label = GetLabel(address);

      try_block_end->handlers.push_back(handler);
    }

    // catch_all handler?
    //
    // NOTE: this is used to generate code for the "finally" blocks
    //  (see Java Virtual Machine Specification - 3.13 "Compiling finally")
    //
    if (catchCount < 1) {
      dex::u4 address = dex::ReadULeb128(&ptr);
      try_block_end->catch_all = GetLabel(address);
    }

    // we should have at least one handler
    SLICER_CHECK(!try_block_end->handlers.empty() ||
          try_block_end->catch_all != nullptr);

    try_begins_.push_back(try_block_begin);
    try_ends_.push_back(try_block_end);
  }
}

void CodeIr::DisassembleDebugInfo(const ir::DebugInfo* ir_debug_info) {
  if (ir_debug_info == nullptr) {
    return;
  }

  // debug info state machine registers
  dex::u4 address = 0;
  int line = ir_debug_info->line_start;
  ir::String* source_file = ir_method->decl->parent->class_def->source_file;

  // header
  if (!ir_debug_info->param_names.empty()) {
    auto dbg_header = Alloc<DbgInfoHeader>();
    dbg_header->param_names = ir_debug_info->param_names;
    dbg_header->offset = 0;
    dbg_annotations_.push_back(dbg_header);
  }

  // initial source file
  {
    auto annotation = Alloc<DbgInfoAnnotation>(dex::DBG_SET_FILE);
    annotation->offset = 0;
    annotation->operands.push_back(Alloc<String>(
        source_file, source_file ? source_file->orig_index : dex::kNoIndex));
    dbg_annotations_.push_back(annotation);
  }

  // initial line number - redundant?
  {
    auto annotation = Alloc<DbgInfoAnnotation>(dex::DBG_ADVANCE_LINE);
    annotation->offset = 0;
    annotation->operands.push_back(Alloc<LineNumber>(line));
    dbg_annotations_.push_back(annotation);
  }

  // debug info annotations
  const dex::u1* ptr = ir_debug_info->data.ptr<dex::u1>();
  dex::u1 opcode = 0;
  while ((opcode = *ptr++) != dex::DBG_END_SEQUENCE) {
    DbgInfoAnnotation* annotation = nullptr;

    switch (opcode) {
      case dex::DBG_ADVANCE_PC:
        // addr_diff
        address += dex::ReadULeb128(&ptr);
        break;

      case dex::DBG_ADVANCE_LINE:
        // line_diff
        line += dex::ReadSLeb128(&ptr);
        SLICER_WEAK_CHECK(line >= 0);
        break;

      case dex::DBG_START_LOCAL: {
        annotation = Alloc<DbgInfoAnnotation>(opcode);

        // register_num
        annotation->operands.push_back(Alloc<VReg>(dex::ReadULeb128(&ptr)));

        // name
        dex::u4 name_index = dex::ReadULeb128(&ptr) - 1;
        annotation->operands.push_back(GetString(name_index));

        // type
        dex::u4 type_index = dex::ReadULeb128(&ptr) - 1;
        annotation->operands.push_back(GetType(type_index));
      } break;

      case dex::DBG_START_LOCAL_EXTENDED: {
        annotation = Alloc<DbgInfoAnnotation>(opcode);

        // register_num
        annotation->operands.push_back(Alloc<VReg>(dex::ReadULeb128(&ptr)));

        // name
        dex::u4 name_index = dex::ReadULeb128(&ptr) - 1;
        annotation->operands.push_back(GetString(name_index));

        // type
        dex::u4 type_index = dex::ReadULeb128(&ptr) - 1;
        annotation->operands.push_back(GetType(type_index));

        // signature
        dex::u4 sig_index = dex::ReadULeb128(&ptr) - 1;
        annotation->operands.push_back(GetString(sig_index));
      } break;

      case dex::DBG_END_LOCAL:
      case dex::DBG_RESTART_LOCAL:
        annotation = Alloc<DbgInfoAnnotation>(opcode);
        // register_num
        annotation->operands.push_back(Alloc<VReg>(dex::ReadULeb128(&ptr)));
        break;

      case dex::DBG_SET_PROLOGUE_END:
        annotation = Alloc<DbgInfoAnnotation>(opcode);
        break;

      case dex::DBG_SET_EPILOGUE_BEGIN:
        annotation = Alloc<DbgInfoAnnotation>(opcode);
        break;

      case dex::DBG_SET_FILE: {
        annotation = Alloc<DbgInfoAnnotation>(opcode);

        // source file name
        dex::u4 name_index = dex::ReadULeb128(&ptr) - 1;
        source_file = (name_index == dex::kNoIndex)
                          ? nullptr
                          : dex_ir->strings_map[name_index];
        annotation->operands.push_back(Alloc<String>(source_file, name_index));
      } break;

      default: {
        int adjusted_opcode = opcode - dex::DBG_FIRST_SPECIAL;
        line += dex::DBG_LINE_BASE + (adjusted_opcode % dex::DBG_LINE_RANGE);
        address += (adjusted_opcode / dex::DBG_LINE_RANGE);
        SLICER_WEAK_CHECK(line >= 0);
        annotation = Alloc<DbgInfoAnnotation>(dex::DBG_ADVANCE_LINE);
        annotation->operands.push_back(Alloc<LineNumber>(line));
      } break;
    }

    if (annotation != nullptr) {
      annotation->offset = address;
      dbg_annotations_.push_back(annotation);
    }
  }
}

void CodeIr::DisassembleBytecode(const ir::Code* ir_code) {
  const dex::u2* begin = ir_code->instructions.begin();
  const dex::u2* end = ir_code->instructions.end();
  const dex::u2* ptr = begin;

  while (ptr < end) {
    auto isize = dex::GetWidthFromBytecode(ptr);
    SLICER_CHECK_GT(isize, 0);

    dex::u4 offset = ptr - begin;

    Instruction* instr = nullptr;
    switch (*ptr) {
      case dex::kPackedSwitchSignature:
        instr = DecodePackedSwitch(ptr, offset);
        break;

      case dex::kSparseSwitchSignature:
        instr = DecodeSparseSwitch(ptr, offset);
        break;

      case dex::kArrayDataSignature:
        instr = DecodeArrayData(ptr, offset);
        break;

      default:
        instr = DecodeBytecode(ptr, offset);
        break;
    }

    instr->offset = offset;
    instructions.push_back(instr);
    ptr += isize;
  }
  SLICER_CHECK_EQ(ptr, end);
}

void CodeIr::FixupSwitches() {
  const dex::u2* begin = ir_method->code->instructions.begin();

  // packed switches
  for (auto& fixup : packed_switches_) {
    FixupPackedSwitch(fixup.second.instr, fixup.second.base_offset,
                      begin + fixup.first);
  }

  // sparse switches
  for (auto& fixup : sparse_switches_) {
    FixupSparseSwitch(fixup.second.instr, fixup.second.base_offset,
                      begin + fixup.first);
  }
}

// merge a set of extra instructions into the instruction list
template <class I_LIST, class E_LIST>
static void MergeInstructions(I_LIST& instructions, const E_LIST& extra) {

  // the extra instructins must be sorted by offset
  SLICER_CHECK(std::is_sorted(extra.begin(), extra.end(),
                        [](const Instruction* a, const Instruction* b) {
                          return a->offset < b->offset;
                        }));

  auto instrIt = instructions.begin();
  auto extraIt = extra.begin();

  while (extraIt != extra.end()) {
    if (instrIt == instructions.end() ||
        (*extraIt)->offset == (*instrIt)->offset) {
      instructions.insert(instrIt, *extraIt);
      ++extraIt;
    } else {
      ++instrIt;
    }
  }
}

void CodeIr::Disassemble() {
  nodes_.clear();
  labels_.clear();

  try_begins_.clear();
  try_ends_.clear();
  dbg_annotations_.clear();
  packed_switches_.clear();
  sparse_switches_.clear();

  auto ir_code = ir_method->code;
  if (ir_code == nullptr) {
    return;
  }

  // decode the .dex bytecodes
  DisassembleBytecode(ir_code);

  // try/catch blocks
  DisassembleTryBlocks(ir_code);

  // debug information
  DisassembleDebugInfo(ir_code->debug_info);

  // fixup switches
  FixupSwitches();

  // assign label ids
  std::vector<Label*> tmp_labels;
  int nextLabelId = 1;
  for (auto& label : labels_) {
    label.second->id = nextLabelId++;
    tmp_labels.push_back(label.second);
  }

  // merge the labels into the instructions stream
  MergeInstructions(instructions, dbg_annotations_);
  MergeInstructions(instructions, try_begins_);
  MergeInstructions(instructions, tmp_labels);
  MergeInstructions(instructions, try_ends_);
}

PackedSwitchPayload* CodeIr::DecodePackedSwitch(const dex::u2* /*ptr*/,
                                         dex::u4 offset) {
  // actual decoding is delayed to FixupPackedSwitch()
  // (since the label offsets are relative to the referring
  //  instruction, not the switch data)
  SLICER_CHECK_EQ(offset % 2, 0);
  auto& instr = packed_switches_[offset].instr;
  SLICER_CHECK_EQ(instr, nullptr);
  instr = Alloc<PackedSwitchPayload>();
  return instr;
}

void CodeIr::FixupPackedSwitch(PackedSwitchPayload* instr, dex::u4 base_offset,
                               const dex::u2* ptr) {
  SLICER_CHECK(instr->targets.empty());

  auto dex_packed_switch = reinterpret_cast<const dex::PackedSwitchPayload*>(ptr);
  SLICER_CHECK_EQ(dex_packed_switch->ident, dex::kPackedSwitchSignature);

  instr->first_key = dex_packed_switch->first_key;
  for (dex::u2 i = 0; i < dex_packed_switch->size; ++i) {
    instr->targets.push_back(
        GetLabel(base_offset + dex_packed_switch->targets[i]));
  }
}

SparseSwitchPayload* CodeIr::DecodeSparseSwitch(const dex::u2* /*ptr*/,
                                         dex::u4 offset) {
  // actual decoding is delayed to FixupSparseSwitch()
  // (since the label offsets are relative to the referring
  //  instruction, not the switch data)
  SLICER_CHECK_EQ(offset % 2, 0);
  auto& instr = sparse_switches_[offset].instr;
  SLICER_CHECK_EQ(instr, nullptr);
  instr = Alloc<SparseSwitchPayload>();
  return instr;
}

void CodeIr::FixupSparseSwitch(SparseSwitchPayload* instr, dex::u4 base_offset,
                               const dex::u2* ptr) {
  SLICER_CHECK(instr->switch_cases.empty());

  auto dex_sparse_switch = reinterpret_cast<const dex::SparseSwitchPayload*>(ptr);
  SLICER_CHECK_EQ(dex_sparse_switch->ident, dex::kSparseSwitchSignature);

  auto& data = dex_sparse_switch->data;
  auto& size = dex_sparse_switch->size;

  for (dex::u2 i = 0; i < size; ++i) {
    SparseSwitchPayload::SwitchCase switch_case = {};
    switch_case.key = data[i];
    switch_case.target = GetLabel(base_offset + data[i + size]);
    instr->switch_cases.push_back(switch_case);
  }
}

ArrayData* CodeIr::DecodeArrayData(const dex::u2* ptr, dex::u4 offset) {
  auto dex_array_data = reinterpret_cast<const dex::ArrayData*>(ptr);
  SLICER_CHECK_EQ(dex_array_data->ident, dex::kArrayDataSignature);
  SLICER_CHECK_EQ(offset % 2, 0);

  auto instr = Alloc<ArrayData>();
  instr->data = slicer::MemView(ptr, dex::GetWidthFromBytecode(ptr) * 2);
  return instr;
}

Operand* CodeIr::GetRegA(const dex::Instruction& dex_instr) {
  auto verify_flags = dex::GetVerifyFlagsFromOpcode(dex_instr.opcode);
  if ((verify_flags & dex::kVerifyRegAWide) != 0) {
    return Alloc<VRegPair>(dex_instr.vA);
  } else {
    return Alloc<VReg>(dex_instr.vA);
  }
}

Operand* CodeIr::GetRegB(const dex::Instruction& dex_instr) {
  auto verify_flags = dex::GetVerifyFlagsFromOpcode(dex_instr.opcode);
  if ((verify_flags & dex::kVerifyRegBWide) != 0) {
    return Alloc<VRegPair>(dex_instr.vB);
  } else {
    return Alloc<VReg>(dex_instr.vB);
  }
}

Operand* CodeIr::GetRegC(const dex::Instruction& dex_instr) {
  auto verify_flags = dex::GetVerifyFlagsFromOpcode(dex_instr.opcode);
  if ((verify_flags & dex::kVerifyRegCWide) != 0) {
    return Alloc<VRegPair>(dex_instr.vC);
  } else {
    return Alloc<VReg>(dex_instr.vC);
  }
}

Bytecode* CodeIr::DecodeBytecode(const dex::u2* ptr, dex::u4 offset) {
  auto dex_instr = dex::DecodeInstruction(ptr);

  auto instr = Alloc<Bytecode>();
  instr->opcode = dex_instr.opcode;

  auto index_type = dex::GetIndexTypeFromOpcode(dex_instr.opcode);
  auto format = dex::GetFormatFromOpcode(dex_instr.opcode);
  switch (format) {
    case dex::k10x:  // op
      break;

    case dex::k12x:  // op vA, vB
    case dex::k22x:  // op vAA, vBBBB
    case dex::k32x:  // op vAAAA, vBBBB
      instr->operands.push_back(GetRegA(dex_instr));
      instr->operands.push_back(GetRegB(dex_instr));
      break;

    case dex::k11n:  // op vA, #+B
    case dex::k21s:  // op vAA, #+BBBB
    case dex::k31i:  // op vAA, #+BBBBBBBB
      instr->operands.push_back(GetRegA(dex_instr));
      instr->operands.push_back(Alloc<Const32>(dex_instr.vB));
      break;

    case dex::k11x:  // op vAA
      instr->operands.push_back(GetRegA(dex_instr));
      break;

    case dex::k10t:  // op +AA
    case dex::k20t:  // op +AAAA
    case dex::k30t:  // op +AAAAAAAA
    {
      auto label = GetLabel(offset + dex::s4(dex_instr.vA));
      instr->operands.push_back(Alloc<CodeLocation>(label));
    } break;

    case dex::k21t:  // op vAA, +BBBB
    case dex::k31t:  // op vAA, +BBBBBBBB
    {
      dex::u4 targetOffset = offset + dex::s4(dex_instr.vB);
      instr->operands.push_back(GetRegA(dex_instr));
      auto label = GetLabel(targetOffset);
      instr->operands.push_back(Alloc<CodeLocation>(label));

      if (dex_instr.opcode == dex::OP_PACKED_SWITCH) {
        label->aligned = true;
        dex::u4& base_offset = packed_switches_[targetOffset].base_offset;
        SLICER_CHECK_EQ(base_offset, kInvalidOffset);
        base_offset = offset;
      } else if (dex_instr.opcode == dex::OP_SPARSE_SWITCH) {
        label->aligned = true;
        dex::u4& base_offset = sparse_switches_[targetOffset].base_offset;
        SLICER_CHECK_EQ(base_offset, kInvalidOffset);
        base_offset = offset;
      } else if (dex_instr.opcode == dex::OP_FILL_ARRAY_DATA) {
        label->aligned = true;
      }
    } break;

    case dex::k23x:  // op vAA, vBB, vCC
      instr->operands.push_back(GetRegA(dex_instr));
      instr->operands.push_back(GetRegB(dex_instr));
      instr->operands.push_back(GetRegC(dex_instr));
      break;

    case dex::k22t:  // op vA, vB, +CCCC
    {
      instr->operands.push_back(GetRegA(dex_instr));
      instr->operands.push_back(GetRegB(dex_instr));
      auto label = GetLabel(offset + dex::s4(dex_instr.vC));
      instr->operands.push_back(Alloc<CodeLocation>(label));
    } break;

    case dex::k22b:  // op vAA, vBB, #+CC
    case dex::k22s:  // op vA, vB, #+CCCC
      instr->operands.push_back(GetRegA(dex_instr));
      instr->operands.push_back(GetRegB(dex_instr));
      instr->operands.push_back(Alloc<Const32>(dex_instr.vC));
      break;

    case dex::k22c:  // op vA, vB, thing@CCCC
      instr->operands.push_back(GetRegA(dex_instr));
      instr->operands.push_back(GetRegB(dex_instr));
      instr->operands.push_back(GetIndexedOperand(index_type, dex_instr.vC));
      break;

    case dex::k21c:  // op vAA, thing@BBBB
    case dex::k31c:  // op vAA, string@BBBBBBBB
      instr->operands.push_back(GetRegA(dex_instr));
      instr->operands.push_back(GetIndexedOperand(index_type, dex_instr.vB));
      break;

    case dex::k35c:  // op {vC,vD,vE,vF,vG}, thing@BBBB
    {
      SLICER_CHECK_LE(dex_instr.vA, 5);
      auto vreg_list = Alloc<VRegList>();
      for (dex::u4 i = 0; i < dex_instr.vA; ++i) {
        vreg_list->registers.push_back(dex_instr.arg[i]);
      }
      instr->operands.push_back(vreg_list);
      instr->operands.push_back(GetIndexedOperand(index_type, dex_instr.vB));
    } break;

    case dex::k3rc:  // op {vCCCC .. v(CCCC+AA-1)}, thing@BBBB
    {
      auto vreg_range = Alloc<VRegRange>(dex_instr.vC, dex_instr.vA);
      instr->operands.push_back(vreg_range);
      instr->operands.push_back(GetIndexedOperand(index_type, dex_instr.vB));
    } break;

    case dex::k45cc: // op {vC, vD, vE, vF, vG}, thing@BBBB, other@HHHH
    {
      auto vreg_list = Alloc<VRegList>();
      SLICER_CHECK_LE(dex_instr.vA, 5);
      // vC if necessary.
      if (dex_instr.vA > 1) {
        vreg_list->registers.push_back(dex_instr.vC);
      }
      // Add vD,vE,vF,vG as necessary.
      for (dex::u4 i = 1; i < dex_instr.vA; ++i) {
        vreg_list->registers.push_back(dex_instr.arg[i - 1]);
      }
      instr->operands.push_back(vreg_list);
      instr->operands.push_back(GetIndexedOperand(index_type, dex_instr.vB));
      dex::u4 vH = dex_instr.arg[4];
      auto proto_operand = GetSecondIndexedOperand(index_type, vH);
      instr->operands.push_back(proto_operand);
    } break;

    case dex::k4rcc:  // op {vCCCC .. v(CCCC+AA-1)}, thing@BBBB, other@HHHH
    {
      auto vreg_range = Alloc<VRegRange>(dex_instr.vC, dex_instr.vA);
      instr->operands.push_back(vreg_range);
      instr->operands.push_back(GetIndexedOperand(index_type, dex_instr.vB));
      dex::u4 vH = dex_instr.arg[4];
      auto proto_operand = GetSecondIndexedOperand(index_type, vH);
      instr->operands.push_back(proto_operand);
    } break;

    case dex::k21h:  // op vAA, #+BBBB0000[00000000]
      switch (dex_instr.opcode) {
        case dex::OP_CONST_HIGH16:
          instr->operands.push_back(GetRegA(dex_instr));
          instr->operands.push_back(Alloc<Const32>(dex_instr.vB << 16));
          break;

        case dex::OP_CONST_WIDE_HIGH16:
          instr->operands.push_back(GetRegA(dex_instr));
          instr->operands.push_back(Alloc<Const64>(dex::u8(dex_instr.vB) << 48));
          break;

        default: {
          std::stringstream ss;
          ss << "Unexpected opcode: " << dex_instr.opcode;
          SLICER_FATAL(ss.str());
        }
      }
      break;

    case dex::k51l:  // op vAA, #+BBBBBBBBBBBBBBBB
      instr->operands.push_back(GetRegA(dex_instr));
      instr->operands.push_back(Alloc<Const64>(dex_instr.vB_wide));
      break;

    default: {
      std::stringstream ss;
      ss << "Unexpected bytecode format " << format << " for opcode " << dex_instr.opcode;
      SLICER_FATAL(ss.str());
    }
  }

  return instr;
}

// Get a indexed object (string, field, ...)
// (index must be valid != kNoIndex)
IndexedOperand* CodeIr::GetIndexedOperand(dex::InstructionIndexType index_type,
                                          dex::u4 index) {
  SLICER_CHECK_NE(index, dex::kNoIndex);
  switch (index_type) {
    case dex::kIndexStringRef:
      return Alloc<String>(dex_ir->strings_map[index], index);

    case dex::kIndexTypeRef:
      return Alloc<Type>(dex_ir->types_map[index], index);

    case dex::kIndexFieldRef:
      return Alloc<Field>(dex_ir->fields_map[index], index);

    case dex::kIndexMethodRef:
    case dex::kIndexMethodAndProtoRef:
      return Alloc<Method>(dex_ir->methods_map[index], index);

    case dex::kIndexMethodHandleRef:
      return Alloc<MethodHandle>(dex_ir->method_handles_map[index], index);

    default:
      std::stringstream ss;
      ss << "Unexpected index type 0x";
      ss << std::hex << std::setfill('0') << std::setw(2) << index_type;
      SLICER_FATAL(ss.str());
  }
}

// Get the second indexed object (if any).
IndexedOperand* CodeIr::GetSecondIndexedOperand(dex::InstructionIndexType index_type,
                                                dex::u4 index) {
  SLICER_CHECK_NE(index, dex::kNoIndex);
  SLICER_CHECK_EQ(index_type, dex::kIndexMethodAndProtoRef);
  return Alloc<Proto>(dex_ir->protos_map[index], index);
}

// Get a type based on its index (potentially kNoIndex)
Type* CodeIr::GetType(dex::u4 index) {
  auto ir_type = (index == dex::kNoIndex) ? nullptr : dex_ir->types_map[index];
  return Alloc<Type>(ir_type, index);
}

// Get a string based on its index (potentially kNoIndex)
String* CodeIr::GetString(dex::u4 index) {
  auto ir_string = (index == dex::kNoIndex) ? nullptr : dex_ir->strings_map[index];
  return Alloc<String>(ir_string, index);
}

// Get en existing, or new label for a particular offset
Label* CodeIr::GetLabel(dex::u4 offset) {
  auto& p = labels_[offset];
  if (p == nullptr) {
    p = Alloc<Label>(offset);
  }
  ++p->refCount;
  return p;
}

}  // namespace lir
