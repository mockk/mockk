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

#pragma once

#include "dex_format.h"

#include <iosfwd>
#include <stddef.h>

// .dex bytecode definitions and helpers:
// https://source.android.com/devices/tech/dalvik/dalvik-bytecode.html

namespace dex {

// The number of Dalvik opcodes
constexpr size_t kNumPackedOpcodes = 0x100;

// Switch table and array data signatures are a code unit consisting
// of "NOP" (0x00) in the low-order byte and a non-zero identifying
// code in the high-order byte. (A true NOP is 0x0000.)
constexpr u2 kPackedSwitchSignature = 0x0100;
constexpr u2 kSparseSwitchSignature = 0x0200;
constexpr u2 kArrayDataSignature = 0x0300;

// Include for  DEX_INSTRUCTION_LIST and DEX_INSTRUCTION_FORMAT_LIST
#include "dex_instruction_list.h"

// Enumeration of all Dalvik opcodes
enum Opcode : u1 {
#define INSTRUCTION_ENUM(opcode, cname, ...) OP_##cname = (opcode),
  DEX_INSTRUCTION_LIST(INSTRUCTION_ENUM)
#undef INSTRUCTION_ENUM
};

// Instruction formats associated with Dalvik opcodes
enum InstructionFormat : u1 {
#define INSTRUCTION_FORMAT_ENUM(name) k##name,
#include "dex_instruction_list.h"
  DEX_INSTRUCTION_FORMAT_LIST(INSTRUCTION_FORMAT_ENUM)
#undef INSTRUCTION_FORMAT_ENUM
};

#undef DEX_INSTRUCTION_FORMAT_LIST
#undef DEX_INSTRUCTION_LIST

using OpcodeFlags = u1;
enum : OpcodeFlags {
  kBranch = 0x01,         // conditional or unconditional branch
  kContinue = 0x02,       // flow can continue to next statement
  kSwitch = 0x04,         // switch statement
  kThrow = 0x08,          // could cause an exception to be thrown
  kReturn = 0x10,         // returns, no additional statements
  kInvoke = 0x20,         // a flavor of invoke
  kUnconditional = 0x40,  // unconditional branch
  kExperimental = 0x80,   // is an experimental opcode
};

using VerifyFlags = u4;
enum : VerifyFlags {
  kVerifyNothing = 0x0000000,
  kVerifyRegA = 0x0000001,
  kVerifyRegAWide = 0x0000002,
  kVerifyRegB = 0x0000004,
  kVerifyRegBField = 0x0000008,
  kVerifyRegBMethod = 0x0000010,
  kVerifyRegBNewInstance = 0x0000020,
  kVerifyRegBString = 0x0000040,
  kVerifyRegBType = 0x0000080,
  kVerifyRegBWide = 0x0000100,
  kVerifyRegC = 0x0000200,
  kVerifyRegCField = 0x0000400,
  kVerifyRegCNewArray = 0x0000800,
  kVerifyRegCType = 0x0001000,
  kVerifyRegCWide = 0x0002000,
  kVerifyArrayData = 0x0004000,
  kVerifyBranchTarget = 0x0008000,
  kVerifySwitchTargets = 0x0010000,
  kVerifyVarArg = 0x0020000,
  kVerifyVarArgNonZero = 0x0040000,
  kVerifyVarArgRange = 0x0080000,
  kVerifyVarArgRangeNonZero = 0x0100000,
  kVerifyRuntimeOnly = 0x0200000,
  kVerifyError = 0x0400000,
  kVerifyRegHPrototype = 0x0800000,
  kVerifyRegBCallSite = 0x1000000,
  kVerifyRegBMethodHandle = 0x2000000,
  kVerifyRegBPrototype = 0x4000000,
};

// Types of indexed reference that are associated with opcodes whose
// formats include such an indexed reference (e.g., 21c and 35c).
enum InstructionIndexType : u1 {
  kIndexUnknown = 0,
  kIndexNone,               // has no index
  kIndexVaries,             // "It depends." Used for throw-verification-error
  kIndexTypeRef,            // type reference index
  kIndexStringRef,          // string reference index
  kIndexMethodRef,          // method reference index
  kIndexFieldRef,           // field reference index
  kIndexInlineMethod,       // inline method index (for inline linked methods)
  kIndexVtableOffset,       // vtable offset (for static linked methods)
  kIndexFieldOffset,        // field offset (for static linked fields)
  kIndexMethodAndProtoRef,  // method index and proto index
  kIndexCallSiteRef,        // call site index
  kIndexMethodHandleRef,    // constant method handle reference index
  kIndexProtoRef,           // constant prototype reference index
};

// Holds the contents of a decoded instruction.
struct Instruction {
  u4 vA;          // the A field of the instruction
  u4 vB;          // the B field of the instruction
  u8 vB_wide;     // 64bit version of the B field (for k51l)
  u4 vC;          // the C field of the instruction
  u4 arg[5];      // vC/D/E/F/G in invoke or filled-new-array
  Opcode opcode;  // instruction opcode
};

// "packed-switch-payload" format
struct PackedSwitchPayload {
  u2 ident;
  u2 size;
  s4 first_key;
  s4 targets[];
};

// "sparse-switch-payload" format
struct SparseSwitchPayload {
  u2 ident;
  u2 size;
  s4 data[];
};

// "fill-array-data-payload" format
struct ArrayData {
  u2 ident;
  u2 element_width;
  u4 size;
  u1 data[];
};

// Collect the enums in a struct for better locality.
struct InstructionDescriptor {
  u4 verify_flags;  // Set of VerifyFlag.
  InstructionFormat format;
  InstructionIndexType index_type;
  u1 flags;  // Set of Flags.
};

// Extracts the opcode from a Dalvik code unit (bytecode)
Opcode OpcodeFromBytecode(u2 bytecode);

// Returns the name of an opcode
const char* GetOpcodeName(Opcode opcode);

// Returns the index type associated with the specified opcode
InstructionIndexType GetIndexTypeFromOpcode(Opcode opcode);

// Returns the format associated with the specified opcode
InstructionFormat GetFormatFromOpcode(Opcode opcode);

// Returns the flags for the specified opcode
OpcodeFlags GetFlagsFromOpcode(Opcode opcode);

// Returns the verify flags for the specified opcode
VerifyFlags GetVerifyFlagsFromOpcode(Opcode opcode);

// Returns the instruction width for the specified opcode format
size_t GetWidthFromFormat(InstructionFormat format);

// Return the width of the specified instruction, or 0 if not defined.  Also
// works for special OP_NOP entries, including switch statement data tables
// and array data.
size_t GetWidthFromBytecode(const u2* bytecode);

// Decode a .dex bytecode
Instruction DecodeInstruction(const u2* bytecode);

// Writes a hex formatted opcode to an output stream.
std::ostream& operator<<(std::ostream& os, Opcode opcode);

// Writes name of format to an outputstream.
std::ostream& operator<<(std::ostream& os, InstructionFormat format);

}  // namespace dex
