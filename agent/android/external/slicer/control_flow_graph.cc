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

#include "slicer/control_flow_graph.h"
#include "slicer/chronometer.h"

namespace lir {

std::vector<BasicBlock> BasicBlocksVisitor::Finish() {
  // the .dex format specification has the following constraint:
  //
  //  B17	The last reachable instruction of a method must either be a
  //  backwards goto or branch, a return, or a throw instruction. It must not
  //  be possible to leave the insns array at the bottom.	4.8.2.20
  //
  // NOTE: this is a very aggressive check though since in the LIR we also
  //  have labels, annotations, directives, etc. For example it's possible to have
  //  debug annotations (.line, .endlocal, ...) after the last bytecode.
  //
  SLICER_WEAK_CHECK(state_ == State::Outside);
  SLICER_CHECK(state_ != State::BlockBody);
  current_block_.region = {};
  state_ = State::Outside;
  return std::move(basic_blocks_);
}

bool BasicBlocksVisitor::Visit(Bytecode* bytecode) {
  switch (state_) {
    case State::Outside:
      StartBlock(bytecode);
      state_ = State::BlockBody;
      break;
    case State::BlockHeader:
      state_ = State::BlockBody;
      break;
    case State::BlockBody:
      // inside basic block body, nothing to do.
      break;
  }

  // terminate the current block?
  bool terminate_block = false;
  const auto flags = dex::GetFlagsFromOpcode(bytecode->opcode);
  if (model_exceptions_) {
    constexpr auto exit_instr_flags =
        dex::kInstrCanBranch |
        dex::kInstrCanSwitch |
        dex::kInstrCanThrow |
        dex::kInstrCanReturn;
    terminate_block = (flags & exit_instr_flags) != 0;
  } else {
    constexpr auto exit_instr_flags =
        dex::kInstrCanBranch |
        dex::kInstrCanSwitch |
        dex::kInstrCanReturn;
    terminate_block = bytecode->opcode == dex::OP_THROW || (flags & exit_instr_flags) != 0;
  }
  if (terminate_block) {
      EndBlock(bytecode);
  }

  return true;
}

bool BasicBlocksVisitor::Visit(Label* label) {
  switch (state_) {
    case State::Outside:
      StartBlock(label);
      break;
    case State::BlockBody:
      EndBlock(label->prev);
      StartBlock(label);
      break;
    case State::BlockHeader:
      break;
  }
  return true;
}

bool BasicBlocksVisitor::HandleAnnotation(Instruction* instr) {
  if (state_ == State::Outside) {
    StartBlock(instr);
  }
  return true;
}

bool BasicBlocksVisitor::SkipInstruction(Instruction* instr) {
  if (state_ != State::Outside) {
    EndBlock(instr->prev);
  }
  return true;
}

void BasicBlocksVisitor::StartBlock(Instruction* instr) {
  assert(instr != nullptr);
  assert(state_ == State::Outside);
  // mark the location of the "first" instruction,
  // "last" will be set when we end the basic block.
  current_block_.region.first = instr;
  current_block_.region.last = nullptr;
  state_ = State::BlockHeader;
}

void BasicBlocksVisitor::EndBlock(Instruction* instr) {
  assert(instr != nullptr);
  if (state_ == State::BlockBody) {
    ++current_block_.id;
    assert(current_block_.region.first != nullptr);
    current_block_.region.last = instr;
    basic_blocks_.push_back(current_block_);
  } else {
    assert(state_ == State::BlockHeader);
  }
  current_block_.region = {};
  state_ = State::Outside;
}

void ControlFlowGraph::CreateBasicBlocks(bool model_exceptions) {
  BasicBlocksVisitor visitor(model_exceptions);
  for (auto instr : code_ir->instructions) {
    instr->Accept(&visitor);
  }
  basic_blocks = visitor.Finish();
}

}  // namespace lir
