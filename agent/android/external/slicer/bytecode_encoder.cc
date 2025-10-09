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

#include "slicer/bytecode_encoder.h"
#include "slicer/common.h"
#include "slicer/chronometer.h"

#include <assert.h>

namespace lir {

// Pack a 16bit word: 00AA
static dex::u2 Pack_Z_8(dex::u4 a) {
  dex::u2 fa = (a & 0xff);
  SLICER_CHECK(fa == a);
  return fa;
}

// Pack a 16bit word: AABB
static dex::u2 Pack_8_8(dex::u4 a, dex::u4 b) {
  dex::u2 fa = (a & 0xff);
  SLICER_CHECK(fa == a);
  dex::u2 fb = (b & 0xff);
  SLICER_CHECK(fb == b);
  return (fa << 8) | fb;
}

// Pack a 16bit word: ABCC
static dex::u2 Pack_4_4_8(dex::u4 a, dex::u4 b, dex::u4 c) {
  dex::u2 fa = (a & 0xf);
  SLICER_CHECK(fa == a);
  dex::u2 fb = (b & 0xf);
  SLICER_CHECK(fb == b);
  dex::u2 fc = (c & 0xff);
  SLICER_CHECK(fc == c);
  return (fa << 12) | (fb << 8) | fc;
}

// Pack a 16bit word: ABCD
static dex::u2 Pack_4_4_4_4(dex::u4 a, dex::u4 b, dex::u4 c, dex::u4 d) {
  dex::u2 fa = (a & 0xf);
  SLICER_CHECK(fa == a);
  dex::u2 fb = (b & 0xf);
  SLICER_CHECK(fb == b);
  dex::u2 fc = (c & 0xf);
  SLICER_CHECK(fc == c);
  dex::u2 fd = (d & 0xf);
  SLICER_CHECK(fd == d);
  return (fa << 12) | (fb << 8) | (fc << 4) | fd;
}

// Pack a 16bit word: AAAA
static dex::u2 Pack_16(dex::u4 a) {
  dex::u2 fa = (a & 0xffff);
  SLICER_CHECK(fa == a);
  return fa;
}

// Trim a 4bit signed integer, making sure we're not discarding significant bits
static dex::u4 Trim_S0(dex::u4 value) {
  dex::u4 trim = value & 0xf;
  SLICER_CHECK(dex::u4(dex::s4(trim << 28) >> 28) == value);
  return trim;
}

// Trim a 8bit signed integer, making sure we're not discarding significant bits
static dex::u4 Trim_S1(dex::u4 value) {
  dex::u4 trim = value & 0xff;
  SLICER_CHECK(dex::u4(dex::s4(trim << 24) >> 24) == value);
  return trim;
}

// Trim a 16bit signed integer, making sure we're not discarding significant bits
static dex::u4 Trim_S2(dex::u4 value) {
  dex::u4 trim = value & 0xffff;
  SLICER_CHECK(dex::u4(dex::s4(trim << 16) >> 16) == value);
  return trim;
}

// Returns a register operand, checking the match between format and type
// (register fields can encode either a single 32bit vreg or a wide 64bit vreg pair)
static dex::u4 GetRegA(const Bytecode* bytecode, int index) {
  auto flags = dex::GetFlagsFromOpcode(bytecode->opcode);
  return (flags & dex::kInstrWideRegA) != 0
             ? bytecode->CastOperand<VRegPair>(index)->base_reg
             : bytecode->CastOperand<VReg>(index)->reg;
}

// Returns a register operand, checking the match between format and type
// (register fields can encode either a single 32bit vreg or a wide 64bit vreg pair)
static dex::u4 GetRegB(const Bytecode* bytecode, int index) {
  auto flags = dex::GetFlagsFromOpcode(bytecode->opcode);
  return (flags & dex::kInstrWideRegB) != 0
             ? bytecode->CastOperand<VRegPair>(index)->base_reg
             : bytecode->CastOperand<VReg>(index)->reg;
}

// Returns a register operand, checking the match between format and type
// (register fields can encode either a single 32bit vreg or a wide 64bit vreg pair)
static dex::u4 GetRegC(const Bytecode* bytecode, int index) {
  auto flags = dex::GetFlagsFromOpcode(bytecode->opcode);
  return (flags & dex::kInstrWideRegC) != 0
             ? bytecode->CastOperand<VRegPair>(index)->base_reg
             : bytecode->CastOperand<VReg>(index)->reg;
}

// Encode one instruction into a .dex bytecode
//
// NOTE: the formats and the operand notation is documented here:
//   https://source.android.com/devices/tech/dalvik/instruction-formats.html
//
bool BytecodeEncoder::Visit(Bytecode* bytecode) {
  bytecode->offset = offset_;
  dex::Opcode opcode = bytecode->opcode;

  // Unconditionally replace short (8bit) branches with
  // medium-range (16bit) branches. This should cover 99.999% of
  // the cases and it avoids a more complex branch length handling.
  if (opcode == dex::OP_GOTO) {
    opcode = dex::OP_GOTO_16;
  }

  auto buff_offset = bytecode_.size();
  auto format = dex::GetFormatFromOpcode(opcode);

  switch (format) {
    case dex::kFmt10x:  // op
    {
      SLICER_CHECK(bytecode->operands.size() == 0);
      bytecode_.Push<dex::u2>(Pack_Z_8(opcode));
    } break;

    case dex::kFmt12x:  // op vA, vB
    {
      SLICER_CHECK(bytecode->operands.size() == 2);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u4 vB = GetRegB(bytecode, 1);
      bytecode_.Push<dex::u2>(Pack_4_4_8(vB, vA, opcode));
    } break;

    case dex::kFmt22x:  // op vAA, vBBBB
    {
      SLICER_CHECK(bytecode->operands.size() == 2);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u4 vB = GetRegB(bytecode, 1);
      bytecode_.Push<dex::u2>(Pack_8_8(vA, opcode));
      bytecode_.Push<dex::u2>(Pack_16(vB));
    } break;

    case dex::kFmt32x:  // op vAAAA, vBBBB
    {
      SLICER_CHECK(bytecode->operands.size() == 2);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u4 vB = GetRegB(bytecode, 1);
      bytecode_.Push<dex::u2>(Pack_Z_8(opcode));
      bytecode_.Push<dex::u2>(Pack_16(vA));
      bytecode_.Push<dex::u2>(Pack_16(vB));
    } break;

    case dex::kFmt11n:  // op vA, #+B
    {
      SLICER_CHECK(bytecode->operands.size() == 2);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u4 B = Trim_S0(bytecode->CastOperand<Const32>(1)->u.u4_value);
      bytecode_.Push<dex::u2>(Pack_4_4_8(B, vA, opcode));
    } break;

    case dex::kFmt21s:  // op vAA, #+BBBB
    {
      SLICER_CHECK(bytecode->operands.size() == 2);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u4 B = Trim_S2(bytecode->CastOperand<Const32>(1)->u.u4_value);
      bytecode_.Push<dex::u2>(Pack_8_8(vA, opcode));
      bytecode_.Push<dex::u2>(Pack_16(B));
    } break;

    case dex::kFmt11x:  // op vAA
    {
      SLICER_CHECK(bytecode->operands.size() == 1);
      dex::u4 vA = GetRegA(bytecode, 0);
      bytecode_.Push<dex::u2>(Pack_8_8(vA, opcode));
    } break;

    case dex::kFmt31i:  // op vAA, #+BBBBBBBB
    {
      SLICER_CHECK(bytecode->operands.size() == 2);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u4 B = bytecode->CastOperand<Const32>(1)->u.u4_value;
      bytecode_.Push<dex::u2>(Pack_8_8(vA, opcode));
      bytecode_.Push<dex::u2>(Pack_16(B & 0xffff));
      bytecode_.Push<dex::u2>(Pack_16(B >> 16));
    } break;

    case dex::kFmt20t:  // op +AAAA
    {
      SLICER_CHECK(bytecode->operands.size() == 1);
      auto label = bytecode->CastOperand<CodeLocation>(0)->label;
      dex::u4 A = 0;
      if (label->offset != kInvalidOffset) {
        assert(label->offset <= offset_);
        A = label->offset - offset_;
        SLICER_CHECK(A != 0);
        SLICER_CHECK((A >> 16) == 0xffff);  // TODO: out of range!
      } else {
        fixups_.push_back(LabelFixup(offset_, label, true));
      }
      bytecode_.Push<dex::u2>(Pack_Z_8(opcode));
      bytecode_.Push<dex::u2>(Pack_16(A & 0xffff));
    } break;

    case dex::kFmt30t:  // op +AAAAAAAA
    {
      SLICER_CHECK(bytecode->operands.size() == 1);
      auto label = bytecode->CastOperand<CodeLocation>(0)->label;
      dex::u4 A = 0;
      if (label->offset != kInvalidOffset) {
        // NOTE: goto/32 can branch to itself
        assert(label->offset <= offset_);
        A = label->offset - offset_;
      } else {
        fixups_.push_back(LabelFixup(offset_, label, false));
      }
      bytecode_.Push<dex::u2>(Pack_Z_8(opcode));
      bytecode_.Push<dex::u2>(Pack_16(A & 0xffff));
      bytecode_.Push<dex::u2>(Pack_16(A >> 16));
    } break;

    case dex::kFmt21t:  // op vAA, +BBBB
    {
      SLICER_CHECK(bytecode->operands.size() == 2);
      dex::u4 vA = GetRegA(bytecode, 0);
      auto label = bytecode->CastOperand<CodeLocation>(1)->label;
      dex::u4 B = 0;
      if (label->offset != kInvalidOffset) {
        assert(label->offset <= offset_);
        B = label->offset - offset_;
        SLICER_CHECK(B != 0);
        SLICER_CHECK((B >> 16) == 0xffff);  // TODO: out of range!
      } else {
        fixups_.push_back(LabelFixup(offset_, label, true));
      }
      bytecode_.Push<dex::u2>(Pack_8_8(vA, opcode));
      bytecode_.Push<dex::u2>(Pack_16(B & 0xffff));
    } break;

    case dex::kFmt22t:  // op vA, vB, +CCCC
    {
      SLICER_CHECK(bytecode->operands.size() == 3);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u4 vB = GetRegB(bytecode, 1);
      auto label = bytecode->CastOperand<CodeLocation>(2)->label;
      dex::u4 C = 0;
      if (label->offset != kInvalidOffset) {
        assert(label->offset <= offset_);
        C = label->offset - offset_;
        SLICER_CHECK(C != 0);
        SLICER_CHECK((C >> 16) == 0xffff);  // TODO: out of range!
      } else {
        fixups_.push_back(LabelFixup(offset_, label, true));
      }
      bytecode_.Push<dex::u2>(Pack_4_4_8(vB, vA, opcode));
      bytecode_.Push<dex::u2>(Pack_16(C & 0xffff));
    } break;

    case dex::kFmt31t:  // op vAA, +BBBBBBBB
    {
      SLICER_CHECK(bytecode->operands.size() == 2);
      dex::u4 vA = GetRegA(bytecode, 0);
      auto label = bytecode->CastOperand<CodeLocation>(1)->label;
      dex::u4 B = 0;
      if (label->offset != kInvalidOffset) {
        assert(label->offset <= offset_);
        B = label->offset - offset_;
        SLICER_CHECK(B != 0);
      } else {
        fixups_.push_back(LabelFixup(offset_, label, false));
      }
      bytecode_.Push<dex::u2>(Pack_8_8(vA, opcode));
      bytecode_.Push<dex::u2>(Pack_16(B & 0xffff));
      bytecode_.Push<dex::u2>(Pack_16(B >> 16));
    } break;

    case dex::kFmt23x:  // op vAA, vBB, vCC
    {
      SLICER_CHECK(bytecode->operands.size() == 3);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u4 vB = GetRegB(bytecode, 1);
      dex::u4 vC = GetRegC(bytecode, 2);
      bytecode_.Push<dex::u2>(Pack_8_8(vA, opcode));
      bytecode_.Push<dex::u2>(Pack_8_8(vC, vB));
    } break;

    case dex::kFmt22b:  // op vAA, vBB, #+CC
    {
      SLICER_CHECK(bytecode->operands.size() == 3);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u4 vB = GetRegB(bytecode, 1);
      dex::u4 C = Trim_S1(bytecode->CastOperand<Const32>(2)->u.u4_value);
      bytecode_.Push<dex::u2>(Pack_8_8(vA, opcode));
      bytecode_.Push<dex::u2>(Pack_8_8(C, vB));
    } break;

    case dex::kFmt22s:  // op vA, vB, #+CCCC
    {
      SLICER_CHECK(bytecode->operands.size() == 3);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u4 vB = GetRegB(bytecode, 1);
      dex::u4 C = Trim_S2(bytecode->CastOperand<Const32>(2)->u.u4_value);
      bytecode_.Push<dex::u2>(Pack_4_4_8(vB, vA, opcode));
      bytecode_.Push<dex::u2>(Pack_16(C));
    } break;

    case dex::kFmt22c:  // op vA, vB, thing@CCCC
    {
      SLICER_CHECK(bytecode->operands.size() == 3);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u4 vB = GetRegB(bytecode, 1);
      dex::u4 C = bytecode->CastOperand<IndexedOperand>(2)->index;
      bytecode_.Push<dex::u2>(Pack_4_4_8(vB, vA, opcode));
      bytecode_.Push<dex::u2>(Pack_16(C));
    } break;

    case dex::kFmt21c:  // op vAA, thing@BBBB
    {
      SLICER_CHECK(bytecode->operands.size() == 2);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u4 B = bytecode->CastOperand<IndexedOperand>(1)->index;
      bytecode_.Push<dex::u2>(Pack_8_8(vA, opcode));
      bytecode_.Push<dex::u2>(Pack_16(B));
    } break;

    case dex::kFmt31c:  // op vAA, string@BBBBBBBB
    {
      SLICER_CHECK(bytecode->operands.size() == 2);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u4 B = bytecode->CastOperand<IndexedOperand>(1)->index;
      bytecode_.Push<dex::u2>(Pack_8_8(vA, opcode));
      bytecode_.Push<dex::u2>(Pack_16(B & 0xffff));
      bytecode_.Push<dex::u2>(Pack_16(B >> 16));
    } break;

    case dex::kFmt35c:  // op {vC,vD,vE,vF,vG}, thing@BBBB
    {
      SLICER_CHECK(bytecode->operands.size() == 2);
      const auto& regs = bytecode->CastOperand<VRegList>(0)->registers;
      dex::u4 B = bytecode->CastOperand<IndexedOperand>(1)->index;
      dex::u4 A = regs.size();
      dex::u4 C = (A > 0) ? regs[0] : 0;
      dex::u4 D = (A > 1) ? regs[1] : 0;
      dex::u4 E = (A > 2) ? regs[2] : 0;
      dex::u4 F = (A > 3) ? regs[3] : 0;
      dex::u4 G = (A > 4) ? regs[4] : 0;
      bytecode_.Push<dex::u2>(Pack_4_4_8(A, G, opcode));
      bytecode_.Push<dex::u2>(Pack_16(B));
      bytecode_.Push<dex::u2>(Pack_4_4_4_4(F, E, D, C));

      // keep track of the outs_count
      if ((dex::GetFlagsFromOpcode(opcode) & dex::kInstrInvoke) != 0) {
        outs_count_ = std::max(outs_count_, A);
      }
    } break;

    case dex::kFmt3rc:  // op {vCCCC .. v(CCCC+AA-1)}, thing@BBBB
    {
      SLICER_CHECK(bytecode->operands.size() == 2);
      auto vreg_range = bytecode->CastOperand<VRegRange>(0);
      dex::u4 A = vreg_range->count;
      dex::u4 B = bytecode->CastOperand<IndexedOperand>(1)->index;
      dex::u4 C = vreg_range->base_reg;
      bytecode_.Push<dex::u2>(Pack_8_8(A, opcode));
      bytecode_.Push<dex::u2>(Pack_16(B));
      bytecode_.Push<dex::u2>(Pack_16(C));

      // keep track of the outs_count
      if ((dex::GetFlagsFromOpcode(opcode) & dex::kInstrInvoke) != 0) {
        outs_count_ = std::max(outs_count_, A);
      }
    } break;

    case dex::kFmt51l:  // op vAA, #+BBBBBBBBBBBBBBBB
    {
      SLICER_CHECK(bytecode->operands.size() == 2);
      dex::u4 vA = GetRegA(bytecode, 0);
      dex::u8 B = bytecode->CastOperand<Const64>(1)->u.u8_value;
      bytecode_.Push<dex::u2>(Pack_8_8(vA, opcode));
      bytecode_.Push<dex::u2>(Pack_16((B >> 0) & 0xffff));
      bytecode_.Push<dex::u2>(Pack_16((B >> 16) & 0xffff));
      bytecode_.Push<dex::u2>(Pack_16((B >> 32) & 0xffff));
      bytecode_.Push<dex::u2>(Pack_16((B >> 48) & 0xffff));
    } break;

    case dex::kFmt21h:  // op vAA, #+BBBB0000[00000000]
      SLICER_CHECK(bytecode->operands.size() == 2);
      switch (opcode) {
        case dex::OP_CONST_HIGH16: {
          dex::u4 vA = GetRegA(bytecode, 0);
          dex::u4 B = bytecode->CastOperand<Const32>(1)->u.u4_value >> 16;
          bytecode_.Push<dex::u2>(Pack_8_8(vA, opcode));
          bytecode_.Push<dex::u2>(Pack_16(B));
        } break;

        case dex::OP_CONST_WIDE_HIGH16: {
          dex::u4 vA = GetRegA(bytecode, 0);
          dex::u4 B = bytecode->CastOperand<Const64>(1)->u.u8_value >> 48;
          bytecode_.Push<dex::u2>(Pack_8_8(vA, opcode));
          bytecode_.Push<dex::u2>(Pack_16(B));
        } break;

        default:
          SLICER_FATAL("Unexpected fmt21h opcode: 0x%02x", opcode);
      }
      break;

    default:
      SLICER_FATAL("Unexpected format: 0x%02x", format);
  }

  SLICER_CHECK(bytecode_.size() - buff_offset == 2 * GetWidthFromOpcode(opcode));
  offset_ += GetWidthFromOpcode(opcode);
  return true;
}

bool BytecodeEncoder::Visit(PackedSwitchPayload* packed_switch) {
  SLICER_CHECK(offset_ % 2 == 0);

  // keep track of the switches
  packed_switch->offset = offset_;
  auto& instr = packed_switches_[offset_];
  SLICER_CHECK(instr == nullptr);
  instr = packed_switch;

  // we're going to fix up the offsets in a later pass
  auto orig_size = bytecode_.size();
  bytecode_.Push<dex::u2>(dex::kPackedSwitchSignature);
  bytecode_.Push<dex::u2>(Pack_16(packed_switch->targets.size()));
  bytecode_.Push<dex::s4>(packed_switch->first_key);
  for (size_t i = 0; i < packed_switch->targets.size(); ++i) {
    bytecode_.Push<dex::u4>(0);
  }

  // offset is in 16bit units, not bytes
  offset_ += (bytecode_.size() - orig_size) / 2;

  return true;
}

bool BytecodeEncoder::Visit(SparseSwitchPayload* sparse_switch) {
  SLICER_CHECK(offset_ % 2 == 0);

  // keep track of the switches
  sparse_switch->offset = offset_;
  auto& instr = sparse_switches_[offset_];
  SLICER_CHECK(instr == nullptr);
  instr = sparse_switch;

  // we're going to fix up the offsets in a later pass
  auto orig_size = bytecode_.size();
  bytecode_.Push<dex::u2>(dex::kSparseSwitchSignature);
  bytecode_.Push<dex::u2>(Pack_16(sparse_switch->switch_cases.size()));
  for (const auto& switch_case : sparse_switch->switch_cases) {
    bytecode_.Push<dex::s4>(switch_case.key);
  }
  for (size_t i = 0; i < sparse_switch->switch_cases.size(); ++i) {
    bytecode_.Push<dex::u4>(0);
  }
  offset_ += (bytecode_.size() - orig_size) / 2;

  return true;
}

bool BytecodeEncoder::Visit(ArrayData* array_data) {
  SLICER_CHECK(offset_ % 2 == 0);

  array_data->offset = offset_;
  auto orig_size = bytecode_.size();
  // kArrayDataSignature is already included by array_data->data
  // (no need to emit here)
  bytecode_.Push(array_data->data);
  offset_ += (bytecode_.size() - orig_size) / 2;
  return true;
}

bool BytecodeEncoder::Visit(Label* label) {
  // aligned label?
  if (label->aligned && offset_ % 2 == 1) {
    bytecode_.Push<dex::u2>(dex::OP_NOP);
    ++offset_;
  }

  label->offset = offset_;
  return true;
}

bool BytecodeEncoder::Visit(DbgInfoHeader* dbg_header) {
  dbg_header->offset = offset_;
  return true;
}

bool BytecodeEncoder::Visit(DbgInfoAnnotation* dbg_annotation) {
  dbg_annotation->offset = offset_;
  return true;
}

bool BytecodeEncoder::Visit(TryBlockBegin* try_begin) {
  try_begin->offset = offset_;
  return true;
}

bool BytecodeEncoder::Visit(TryBlockEnd* try_end) {
  try_end->offset = offset_;
  return true;
}

void BytecodeEncoder::FixupSwitchOffsets() {
  dex::u2* const begin = bytecode_.ptr<dex::u2>(0);
  dex::u2* const end = begin + bytecode_.size() / 2;
  dex::u2* ptr = begin;
  while (ptr < end) {
    const auto opcode = dex::OpcodeFromBytecode(*ptr);
    const auto offset = ptr - begin;
    if (opcode == dex::OP_PACKED_SWITCH) {
      auto dex_instr = dex::DecodeInstruction(ptr);
      FixupPackedSwitch(offset, offset + dex::s4(dex_instr.vB));
    } else if (opcode == dex::OP_SPARSE_SWITCH) {
      auto dex_instr = dex::DecodeInstruction(ptr);
      FixupSparseSwitch(offset, offset + dex::s4(dex_instr.vB));
    }
    auto isize = dex::GetWidthFromBytecode(ptr);
    SLICER_CHECK(isize > 0);
    ptr += isize;
  }
  SLICER_CHECK(ptr == end);
}

void BytecodeEncoder::FixupPackedSwitch(dex::u4 base_offset,
                                        dex::u4 payload_offset) {
  auto instr = packed_switches_[payload_offset];
  SLICER_CHECK(instr != nullptr);

  auto payload = bytecode_.ptr<dex::PackedSwitchPayload>(payload_offset * 2);
  SLICER_CHECK(payload->ident == dex::kPackedSwitchSignature);
  SLICER_CHECK(reinterpret_cast<dex::u1*>(payload->targets + payload->size) <=
        bytecode_.data() + bytecode_.size());

  for (int i = 0; i < payload->size; ++i) {
    auto label = instr->targets[i];
    assert(label->offset != kInvalidOffset);
    payload->targets[i] = label->offset - base_offset;
  }
}

void BytecodeEncoder::FixupSparseSwitch(dex::u4 base_offset,
                                        dex::u4 payload_offset) {
  auto instr = sparse_switches_[payload_offset];
  SLICER_CHECK(instr != nullptr);

  auto payload = bytecode_.ptr<dex::SparseSwitchPayload>(payload_offset * 2);
  SLICER_CHECK(payload->ident == dex::kSparseSwitchSignature);

  dex::s4* const targets = payload->data + payload->size;
  SLICER_CHECK(reinterpret_cast<dex::u1*>(targets + payload->size) <=
        bytecode_.data() + bytecode_.size());

  for (int i = 0; i < payload->size; ++i) {
    auto label = instr->switch_cases[i].target;
    assert(label->offset != kInvalidOffset);
    targets[i] = label->offset - base_offset;
  }
}

void BytecodeEncoder::FixupLabels() {
  for (const LabelFixup& fixup : fixups_) {
    dex::u4 label_offset = fixup.label->offset;
    assert(label_offset != kInvalidOffset);
    assert(label_offset > fixup.offset);
    dex::u4 rel_offset = label_offset - fixup.offset;
    SLICER_CHECK(rel_offset != 0);
    dex::u2* instr = bytecode_.ptr<dex::u2>(fixup.offset * 2);
    if (fixup.short_fixup) {
      // TODO: explicit out-of-range check
      assert(instr[1] == 0);
      instr[1] = Pack_16(rel_offset);
    } else {
      assert(instr[1] == 0);
      assert(instr[2] == 0);
      instr[1] = Pack_16(rel_offset & 0xffff);
      instr[2] = Pack_16(rel_offset >> 16);
    }
  }
}

void BytecodeEncoder::Encode(ir::Code* ir_code, std::shared_ptr<ir::DexFile> dex_ir) {
  SLICER_CHECK(bytecode_.empty());
  SLICER_CHECK(offset_ == 0);
  SLICER_CHECK(outs_count_ == 0);

  packed_switches_.clear();
  sparse_switches_.clear();

  // reset all instruction offsets
  for (auto instr : instructions_) {
    instr->offset = kInvalidOffset;
  }

  // generate the .dex bytecodes
  for (auto instr : instructions_) {
    instr->Accept(this);
  }

  // no more appending (read & write is ok)
  bytecode_.Seal(2);

  FixupLabels();
  FixupSwitchOffsets();

  // update ir::Code
  ir_code->instructions = slicer::ArrayView<const dex::u2>(
      bytecode_.ptr<dex::u2>(0), bytecode_.size() / 2);
  ir_code->outs_count = outs_count_;

  // attach the new bytecode
  dex_ir->AttachBuffer(std::move(bytecode_));
}

}  // namespace lir
