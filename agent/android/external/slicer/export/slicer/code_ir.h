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

#include "common.h"
#include "memview.h"
#include "dex_bytecode.h"
#include "dex_format.h"
#include "dex_ir.h"
#include "intrusive_list.h"

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>

#include <algorithm>
#include <cstdint>
#include <list>
#include <map>
#include <memory>
#include <utility>
#include <vector>

namespace lir {

template <class T>
using own = std::unique_ptr<T>;

constexpr dex::u4 kInvalidOffset = dex::u4(-1);

struct Bytecode;
struct PackedSwitchPayload;
struct SparseSwitchPayload;
struct ArrayData;
struct Label;
struct TryBlockBegin;
struct TryBlockEnd;
struct Const32;
struct Const64;
struct VReg;
struct VRegPair;
struct VRegList;
struct VRegRange;
struct CodeLocation;
struct String;
struct Type;
struct Field;
struct Method;
struct DbgInfoHeader;
struct LineNumber;
struct DbgInfoAnnotation;

// Code IR visitor interface
class Visitor {
 public:
  Visitor() = default;
  virtual ~Visitor() = default;

  Visitor(const Visitor&) = delete;
  Visitor& operator=(const Visitor&) = delete;

  // instructions
  virtual bool Visit(Bytecode* bytecode) { return false; }
  virtual bool Visit(PackedSwitchPayload* packed_switch) { return false; }
  virtual bool Visit(SparseSwitchPayload* sparse_switch) { return false; }
  virtual bool Visit(ArrayData* array_data) { return false; }
  virtual bool Visit(Label* label) { return false; }
  virtual bool Visit(DbgInfoHeader* dbg_header) { return false; }
  virtual bool Visit(DbgInfoAnnotation* dbg_annotation) { return false; }
  virtual bool Visit(TryBlockBegin* try_begin) { return false; }
  virtual bool Visit(TryBlockEnd* try_end) { return false; }

  // operands
  virtual bool Visit(CodeLocation* location) { return false; }
  virtual bool Visit(Const32* const32) { return false; }
  virtual bool Visit(Const64* const64) { return false; }
  virtual bool Visit(VReg* vreg) { return false; }
  virtual bool Visit(VRegPair* vreg_pair) { return false; }
  virtual bool Visit(VRegList* vreg_list) { return false; }
  virtual bool Visit(VRegRange* vreg_range) { return false; }
  virtual bool Visit(String* string) { return false; }
  virtual bool Visit(Type* type) { return false; }
  virtual bool Visit(Field* field) { return false; }
  virtual bool Visit(Method* method) { return false; }
  virtual bool Visit(LineNumber* line) { return false; }
};

// The root of the polymorphic code IR nodes hierarchy
//
// NOTE: in general it's possible to "reuse" code IR nodes
//   (ie. refcount > 1) although extra care is required since
//   modifications to shared nodes will be visible in multiple places
//   (notable exception: instruction nodes can't be reused)
//
struct Node {
  Node() = default;
  virtual ~Node() = default;

  Node(const Node&) = delete;
  Node& operator=(const Node&) = delete;

  virtual bool Accept(Visitor* visitor) { return false; }

  template<class T>
  bool IsA() const {
    return dynamic_cast<const T*>(this) != nullptr;
  }
};

struct Operand : public Node {};

struct Const32 : public Operand {
  union {
    dex::s4 s4_value;
    dex::u4 u4_value;
    float float_value;
  } u;

  Const32(dex::u4 value) { u.u4_value = value; }

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct Const64 : public Operand {
  union {
    dex::s8 s8_value;
    dex::u8 u8_value;
    double double_value;
  } u;

  Const64(dex::u8 value) { u.u8_value = value; }

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct VReg : public Operand {
  dex::u4 reg;

  VReg(dex::u4 reg) : reg(reg) {}

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct VRegPair : public Operand {
  dex::u4 base_reg;

  VRegPair(dex::u4 base_reg) : base_reg(base_reg) {}

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct VRegList : public Operand {
  std::vector<dex::u4> registers;

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct VRegRange : public Operand {
  dex::u4 base_reg;
  int count;

  VRegRange(dex::u4 base_reg, int count) : base_reg(base_reg), count(count) {}

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct IndexedOperand : public Operand {
  dex::u4 index;

  IndexedOperand(dex::u4 index) : index(index) {}
};

struct String : public IndexedOperand {
  ir::String* ir_string;

  String(ir::String* ir_string, dex::u4 index) : IndexedOperand(index), ir_string(ir_string) {}

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct Type : public IndexedOperand {
  ir::Type* ir_type;

  Type(ir::Type* ir_type, dex::u4 index) : IndexedOperand(index), ir_type(ir_type) {}

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct Field : public IndexedOperand {
  ir::FieldDecl* ir_field;

  Field(ir::FieldDecl* ir_field, dex::u4 index) : IndexedOperand(index), ir_field(ir_field) {}

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct Method : public IndexedOperand {
  ir::MethodDecl* ir_method;

  Method(ir::MethodDecl* ir_method, dex::u4 index) : IndexedOperand(index), ir_method(ir_method) {}

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct CodeLocation : public Operand {
  Label* label;

  CodeLocation(Label* label) : label(label) {}

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

// Code IR is a linked list of Instructions
struct Instruction : public Node {
  // absolute offset from the start of the method
  dex::u4 offset = 0;

  Instruction* prev = nullptr;
  Instruction* next = nullptr;
};

using InstructionsList = slicer::IntrusiveList<Instruction>;

struct Bytecode : public Instruction {
  dex::Opcode opcode = dex::OP_NOP;
  std::vector<Operand*> operands;

  template<class T>
  T* CastOperand(int index) const {
    T* operand = dynamic_cast<T*>(operands[index]);
    SLICER_CHECK(operand != nullptr);
    return operand;
  }

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct PackedSwitchPayload : public Instruction {
  dex::s4 first_key = 0;
  std::vector<Label*> targets;

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct SparseSwitchPayload : public Instruction {
  struct SwitchCase {
    dex::s4 key = 0;
    Label* target = nullptr;
  };

  std::vector<SwitchCase> switch_cases;

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct ArrayData : public Instruction {
  slicer::MemView data;

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct Label : public Instruction {
  int id = 0;
  int refCount = 0;
  bool aligned = false;

  Label(dex::u4 offset) { this->offset = offset; }

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct TryBlockBegin : public Instruction {
  int id = 0;

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct CatchHandler {
  ir::Type* ir_type = nullptr;
  Label* label = nullptr;
};

struct TryBlockEnd : public Instruction {
  TryBlockBegin* try_begin = nullptr;
  std::vector<CatchHandler> handlers;
  Label* catch_all = nullptr;

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct DbgInfoHeader : public Instruction {
  std::vector<ir::String*> param_names;

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct LineNumber : public Operand {
  int line = 0;

  LineNumber(int line) : line(line) {
    SLICER_WEAK_CHECK(line > 0);
  }

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

struct DbgInfoAnnotation : public Instruction {
  dex::u1 dbg_opcode = 0;
  std::vector<Operand*> operands;

  DbgInfoAnnotation(dex::u1 dbg_opcode) : dbg_opcode(dbg_opcode) {}

  template<class T>
  T* CastOperand(int index) const {
    T* operand = dynamic_cast<T*>(operands[index]);
    SLICER_CHECK(operand != nullptr);
    return operand;
  }

  virtual bool Accept(Visitor* visitor) override { return visitor->Visit(this); }
};

// Code IR container and manipulation interface
struct CodeIr {
  // linked list of the method's instructions
  InstructionsList instructions;

  ir::EncodedMethod* ir_method = nullptr;
  std::shared_ptr<ir::DexFile> dex_ir;

 public:
  CodeIr(ir::EncodedMethod* ir_method, std::shared_ptr<ir::DexFile> dex_ir)
      : ir_method(ir_method), dex_ir(dex_ir) {
    Dissasemble();
  }

  // No copy/move semantics
  CodeIr(const CodeIr&) = delete;
  CodeIr& operator=(const CodeIr&) = delete;

  void Assemble();

  void Accept(Visitor* visitor) {
    for (auto instr : instructions) {
      instr->Accept(visitor);
    }
  }

  template <class T, class... Args>
  T* Alloc(Args&&... args) {
    auto p = new T(std::forward<Args>(args)...);
    nodes_.push_back(own<T>(p));
    return p;
  }

 private:
  void Dissasemble();
  void DissasembleBytecode(const ir::Code* ir_code);
  void DissasembleTryBlocks(const ir::Code* ir_code);
  void DissasembleDebugInfo(const ir::DebugInfo* ir_debug_info);

  void FixupSwitches();
  void FixupPackedSwitch(PackedSwitchPayload* instr, dex::u4 base_offset, const dex::u2* ptr);
  void FixupSparseSwitch(SparseSwitchPayload* instr, dex::u4 base_offset, const dex::u2* ptr);

  SparseSwitchPayload* DecodeSparseSwitch(const dex::u2* /*ptr*/, dex::u4 offset);
  PackedSwitchPayload* DecodePackedSwitch(const dex::u2* /*ptr*/, dex::u4 offset);
  ArrayData* DecodeArrayData(const dex::u2* ptr, dex::u4 offset);
  Bytecode* DecodeBytecode(const dex::u2* ptr, dex::u4 offset);

  IndexedOperand* GetIndexedOperand(dex::InstructionIndexType index_type, dex::u4 index);

  Type* GetType(dex::u4 index);
  String* GetString(dex::u4 index);
  Label* GetLabel(dex::u4 offset);

  Operand* GetRegA(const dex::Instruction& dex_instr);
  Operand* GetRegB(const dex::Instruction& dex_instr);
  Operand* GetRegC(const dex::Instruction& dex_instr);

 private:
  // the "master index" of all the LIR owned nodes
  std::vector<own<Node>> nodes_;

  // data structures for fixing up switch payloads
  struct PackedSwitchFixup {
    PackedSwitchPayload* instr = nullptr;
    dex::u4 base_offset = kInvalidOffset;
  };

  struct SparseSwitchFixup {
    SparseSwitchPayload* instr = nullptr;
    dex::u4 base_offset = kInvalidOffset;
  };

  // used during bytecode raising
  std::map<dex::u4, Label*> labels_;
  std::map<dex::u4, PackedSwitchFixup> packed_switches_;
  std::map<dex::u4, SparseSwitchFixup> sparse_switches_;

  // extra instructions/annotations created during raising
  // (intended to be merged in with the main instruction
  //  list at end of the IR raising phase)
  std::vector<TryBlockBegin*> try_begins_;
  std::vector<TryBlockEnd*> try_ends_;
  std::vector<Instruction*> dbg_annotations_;
};

}  // namespace lir
