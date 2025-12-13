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

#include "slicer/dex_bytecode.h"

#include "slicer/common.h"

#include <array>
#include <iomanip>
#include <sstream>

namespace dex {

Opcode OpcodeFromBytecode(u2 bytecode) {
  Opcode opcode = Opcode(bytecode & 0xff);
  return opcode;
}

// Table that maps each opcode to the index type implied by that opcode
static constexpr std::array<InstructionDescriptor, kNumPackedOpcodes>
    gInstructionDescriptors = {{
#define INSTRUCTION_DESCR(o, c, p, format, index, flags, e, vflags) \
  {                                                                 \
      vflags,                                                       \
      format,                                                       \
      index,                                                        \
      flags,                                                        \
  },
#include "export/slicer/dex_instruction_list.h"
        DEX_INSTRUCTION_LIST(INSTRUCTION_DESCR)
#undef DEX_INSTRUCTION_LIST
#undef INSTRUCTION_DESCR
    }};

InstructionIndexType GetIndexTypeFromOpcode(Opcode opcode) {
  return gInstructionDescriptors[opcode].index_type;
}

InstructionFormat GetFormatFromOpcode(Opcode opcode) {
  return gInstructionDescriptors[opcode].format;
}

OpcodeFlags GetFlagsFromOpcode(Opcode opcode) {
  return gInstructionDescriptors[opcode].flags;
}

VerifyFlags GetVerifyFlagsFromOpcode(Opcode opcode) {
  return gInstructionDescriptors[opcode].verify_flags;
}

size_t GetWidthFromFormat(InstructionFormat format) {
  switch (format) {
    case k10x:
    case k12x:
    case k11n:
    case k11x:
    case k10t:
      return 1;
    case k20t:
    case k20bc:
    case k21c:
    case k22x:
    case k21s:
    case k21t:
    case k21h:
    case k23x:
    case k22b:
    case k22s:
    case k22t:
    case k22c:
    case k22cs:
      return 2;
    case k30t:
    case k31t:
    case k31c:
    case k32x:
    case k31i:
    case k35c:
    case k35ms:
    case k35mi:
    case k3rc:
    case k3rms:
    case k3rmi:
      return 3;
    case k45cc:
    case k4rcc:
      return 4;
    case k51l:
      return 5;
  }
}

size_t GetWidthFromBytecode(const u2* bytecode) {
  size_t width = 0;
  if (*bytecode == kPackedSwitchSignature) {
    width = 4 + bytecode[1] * 2;
  } else if (*bytecode == kSparseSwitchSignature) {
    width = 2 + bytecode[1] * 4;
  } else if (*bytecode == kArrayDataSignature) {
    u2 elemWidth = bytecode[1];
    u4 len = bytecode[2] | (((u4)bytecode[3]) << 16);
    // The plus 1 is to round up for odd size and width.
    width = 4 + (elemWidth * len + 1) / 2;
  } else {
    width = GetWidthFromFormat(
        GetFormatFromOpcode(OpcodeFromBytecode(bytecode[0])));
  }
  return width;
}

// Dalvik opcode names.
static constexpr std::array<const char*, kNumPackedOpcodes> gOpcodeNames = {
#define INSTRUCTION_NAME(o, c, pname, f, i, a, e, v) pname,
#include "export/slicer/dex_instruction_list.h"
    DEX_INSTRUCTION_LIST(INSTRUCTION_NAME)
#undef DEX_INSTRUCTION_LIST
#undef INSTRUCTION_NAME
};

const char* GetOpcodeName(Opcode opcode) { return gOpcodeNames[opcode]; }

// Helpers for DecodeInstruction()
static u4 InstA(u2 inst) { return (inst >> 8) & 0x0f; }
static u4 InstB(u2 inst) { return inst >> 12; }
static u4 InstAA(u2 inst) { return inst >> 8; }

// Helper for DecodeInstruction()
static u4 FetchU4(const u2* ptr) { return ptr[0] | (u4(ptr[1]) << 16); }

// Helper for DecodeInstruction()
static u8 FetchU8(const u2* ptr) {
  return FetchU4(ptr) | (u8(FetchU4(ptr + 2)) << 32);
}

// Decode a Dalvik bytecode and extract the individual fields
Instruction DecodeInstruction(const u2* bytecode) {
  u2 inst = bytecode[0];
  Opcode opcode = OpcodeFromBytecode(inst);
  InstructionFormat format = GetFormatFromOpcode(opcode);

  Instruction dec = {};
  dec.opcode = opcode;

  switch (format) {
    case k10x:  // op
      return dec;
    case k12x:  // op vA, vB
      dec.vA = InstA(inst);
      dec.vB = InstB(inst);
      return dec;
    case k11n:  // op vA, #+B
      dec.vA = InstA(inst);
      dec.vB = s4(InstB(inst) << 28) >> 28;  // sign extend 4-bit value
      return dec;
    case k11x:  // op vAA
      dec.vA = InstAA(inst);
      return dec;
    case k10t:                    // op +AA
      dec.vA = s1(InstAA(inst));  // sign-extend 8-bit value
      return dec;
    case k20t:                   // op +AAAA
      dec.vA = s2(bytecode[1]);  // sign-extend 16-bit value
      return dec;
    case k20bc:  // [opt] op AA, thing@BBBB
    case k21c:   // op vAA, thing@BBBB
    case k22x:   // op vAA, vBBBB
      dec.vA = InstAA(inst);
      dec.vB = bytecode[1];
      return dec;
    case k21s:  // op vAA, #+BBBB
    case k21t:  // op vAA, +BBBB
      dec.vA = InstAA(inst);
      dec.vB = s2(bytecode[1]);  // sign-extend 16-bit value
      return dec;
    case k21h:  // op vAA, #+BBBB0000[00000000]
      dec.vA = InstAA(inst);
      // The value should be treated as right-zero-extended, but we don't
      // actually do that here. Among other things, we don't know if it's
      // the top bits of a 32- or 64-bit value.
      dec.vB = bytecode[1];
      return dec;
    case k23x:  // op vAA, vBB, vCC
      dec.vA = InstAA(inst);
      dec.vB = bytecode[1] & 0xff;
      dec.vC = bytecode[1] >> 8;
      return dec;
    case k22b:  // op vAA, vBB, #+CC
      dec.vA = InstAA(inst);
      dec.vB = bytecode[1] & 0xff;
      dec.vC = s1(bytecode[1] >> 8);  // sign-extend 8-bit value
      return dec;
    case k22s:  // op vA, vB, #+CCCC
    case k22t:  // op vA, vB, +CCCC
      dec.vA = InstA(inst);
      dec.vB = InstB(inst);
      dec.vC = s2(bytecode[1]);  // sign-extend 16-bit value
      return dec;
    case k22c:   // op vA, vB, thing@CCCC
    case k22cs:  // [opt] op vA, vB, field offset CCCC
      dec.vA = InstA(inst);
      dec.vB = InstB(inst);
      dec.vC = bytecode[1];
      return dec;
    case k30t:  // op +AAAAAAAA
      dec.vA = FetchU4(bytecode + 1);
      return dec;
    case k31t:  // op vAA, +BBBBBBBB
    case k31c:  // op vAA, string@BBBBBBBB
      dec.vA = InstAA(inst);
      dec.vB = FetchU4(bytecode + 1);
      return dec;
    case k32x:  // op vAAAA, vBBBB
      dec.vA = bytecode[1];
      dec.vB = bytecode[2];
      return dec;
    case k31i:  // op vAA, #+BBBBBBBB
      dec.vA = InstAA(inst);
      dec.vB = FetchU4(bytecode + 1);
      return dec;
    case k35c:               // op {vC, vD, vE, vF, vG}, thing@BBBB
    case k35ms:              // [opt] invoke-virtual+super
    case k35mi: {            // [opt] inline invoke
      dec.vA = InstB(inst);  // This is labeled A in the spec.
      dec.vB = bytecode[1];

      u2 regList = bytecode[2];

      // Copy the argument registers into the arg[] array, and
      // also copy the first argument (if any) into vC. (The
      // Instruction structure doesn't have separate
      // fields for {vD, vE, vF, vG}, so there's no need to make
      // copies of those.) Note that cases 5..2 fall through.
      switch (dec.vA) {
        case 5:
          // A fifth arg is verboten for inline invokes
          SLICER_CHECK_NE(format, k35mi);

          // Per note at the top of this format decoder, the
          // fifth argument comes from the A field in the
          // instruction, but it's labeled G in the spec.
          dec.arg[4] = InstA(inst);
          FALLTHROUGH_INTENDED;
        case 4:
          dec.arg[3] = (regList >> 12) & 0x0f;
          FALLTHROUGH_INTENDED;
        case 3:
          dec.arg[2] = (regList >> 8) & 0x0f;
          FALLTHROUGH_INTENDED;
        case 2:
          dec.arg[1] = (regList >> 4) & 0x0f;
          FALLTHROUGH_INTENDED;
        case 1:
          dec.vC = dec.arg[0] = regList & 0x0f;
          FALLTHROUGH_INTENDED;
        case 0:
          // Valid, but no need to do anything
          return dec;
      }
    }
      SLICER_CHECK(!"Invalid arg count in 35c/35ms/35mi");
    case k3rc:   // op {vCCCC .. v(CCCC+AA-1)}, meth@BBBB
    case k3rms:  // [opt] invoke-virtual+super/range
    case k3rmi:  // [opt] execute-inline/range
      dec.vA = InstAA(inst);
      dec.vB = bytecode[1];
      dec.vC = bytecode[2];
      return dec;
    case k45cc: {
      // AG op BBBB FEDC HHHH
      dec.vA = InstB(inst);  // This is labelled A in the spec.
      dec.vB = bytecode[1];  // vB meth@BBBB

      u2 regList = bytecode[2];
      dec.vC = regList & 0xf;
      dec.arg[0] = (regList >> 4) & 0xf;  // vD
      dec.arg[1] = (regList >> 8) & 0xf;  // vE
      dec.arg[2] = (regList >> 12);       // vF
      dec.arg[3] = InstA(inst);           // vG
      dec.arg[4] = bytecode[3];           // vH proto@HHHH
    }
      return dec;
    case k4rcc:
      // AA op BBBB CCCC HHHH
      dec.vA = InstAA(inst);
      dec.vB = bytecode[1];
      dec.vC = bytecode[2];
      dec.arg[4] = bytecode[3];  // vH proto@HHHH
      return dec;
    case k51l:  // op vAA, #+BBBBBBBBBBBBBBBB
      dec.vA = InstAA(inst);
      dec.vB_wide = FetchU8(bytecode + 1);
      return dec;
  }

  std::stringstream ss;
  ss << "Can't decode unexpected format " << format << " for " << opcode;
  SLICER_FATAL(ss.str());
}

static inline std::string HexByte(int value) {
  std::stringstream ss;
  ss << "0x" << std::setw(2) << std::setfill('0') << std::hex << value;
  return ss.str();
}

std::ostream& operator<<(std::ostream& os, Opcode opcode) {
  return os << "[" << HexByte(opcode) << "] " << gOpcodeNames[opcode];
}

std::ostream& operator<<(std::ostream& os, InstructionFormat format) {
  switch (format) {
  #define EMIT_INSTRUCTION_FORMAT_NAME(name) \
    case InstructionFormat::k##name: return os << #name;
  #include "export/slicer/dex_instruction_list.h"
  DEX_INSTRUCTION_FORMAT_LIST(EMIT_INSTRUCTION_FORMAT_NAME)
  #undef EMIT_INSTRUCTION_FORMAT_NAME
  #undef DEX_INSTRUCTION_FORMAT_LIST
  #undef DEX_INSTRUCTION_LIST
  }
  return os << "[" << HexByte(format) << "] " << "Unknown";
}

}  // namespace dex
