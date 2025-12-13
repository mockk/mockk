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

#include "code_ir.h"
#include "common.h"

#include <vector>

namespace lir {

// Represents a contiguous code "region"
struct Region {
  Instruction* first = nullptr;
  Instruction* last = nullptr;
};

struct BasicBlock {
  int id = 0;       // real basic blocks have id > 0
  Region region;
};

// LIR visitor used to build the list of basic blocks
class BasicBlocksVisitor : public Visitor {
  enum class State { Outside, BlockHeader, BlockBody };

 public:
  explicit BasicBlocksVisitor(bool model_exceptions) : model_exceptions_(model_exceptions) {
    current_block_.id = 0;
  }

  ~BasicBlocksVisitor() {
    assert(state_ == State::Outside);
  }

  // Used to mark the end of instruction stream
  // Returns the list of basic blocks
  std::vector<BasicBlock> Finish();

 private:
  bool Visit(Bytecode* bytecode) override;
  bool Visit(Label* label) override;

  // Debug info annotations
  bool Visit(DbgInfoHeader* dbg_header) override { return HandleAnnotation(dbg_header); }
  bool Visit(DbgInfoAnnotation* dbg_annotation) override { return HandleAnnotation(dbg_annotation); }

  // EH annotations
  bool Visit(TryBlockBegin* try_begin) override { return SkipInstruction(try_begin); }
  bool Visit(TryBlockEnd* try_end) override { return SkipInstruction(try_end); }

  // data payload
  bool Visit(PackedSwitchPayload* packed_switch) override  { return SkipInstruction(packed_switch); }
  bool Visit(SparseSwitchPayload* sparse_switch) override { return SkipInstruction(sparse_switch); }
  bool Visit(ArrayData* array_data) override { return SkipInstruction(array_data); }

  bool HandleAnnotation(Instruction* instr);
  bool SkipInstruction(Instruction* instr);

  // Starts a new basic block starting with the specified instruction
  void StartBlock(Instruction* instr);

  // Ends the current basic block at the specified instruction
  void EndBlock(Instruction* instr);

 private:
  State state_ = State::Outside;
  BasicBlock current_block_;
  std::vector<BasicBlock> basic_blocks_;
  const bool model_exceptions_;
};

// The Control Flow Graph (CFG) for the specified method LIR
struct ControlFlowGraph {
  // The list of basic blocks, as non-overlapping regions,
  // sorted by the byte offset of the region start
  std::vector<BasicBlock> basic_blocks;

  const CodeIr* code_ir;

 public:
  ControlFlowGraph(const CodeIr* code_ir, bool model_exceptions) : code_ir(code_ir) {
    CreateBasicBlocks(model_exceptions);
  }

 private:
  void CreateBasicBlocks(bool model_exceptions);
};

}  // namespace lir
