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

#include "slicer/tryblocks_encoder.h"

#include "slicer/chronometer.h"
#include "slicer/common.h"

namespace lir {

bool TryBlocksEncoder::Visit(TryBlockEnd* try_end) {
  const dex::u4 begin_offset = try_end->try_begin->offset;
  const dex::u4 end_offset = try_end->offset;
  SLICER_CHECK_GT(end_offset, begin_offset);
  SLICER_CHECK_LT(end_offset - begin_offset, (1 << 16));

  // generate the "try_item"
  dex::TryBlock try_block = {};
  try_block.start_addr = begin_offset;
  try_block.insn_count = end_offset - begin_offset;
  try_block.handler_off = handlers_.size();
  tries_.Push(try_block);

  // generate the "encoded_catch_handler"
  dex::s4 catch_count = try_end->handlers.size();
  handlers_.PushSLeb128(try_end->catch_all ? -catch_count : catch_count);
  for (int catch_index = 0; catch_index < catch_count; ++catch_index) {
    const CatchHandler& handler = try_end->handlers[catch_index];
    // type_idx
    handlers_.PushULeb128(handler.ir_type->orig_index);
    // address
    SLICER_CHECK_NE(handler.label->offset, kInvalidOffset);
    handlers_.PushULeb128(handler.label->offset);
  }
  if (try_end->catch_all != nullptr) {
    // address
    SLICER_CHECK_NE(try_end->catch_all->offset, kInvalidOffset);
    handlers_.PushULeb128(try_end->catch_all->offset);
  }

  return true;
}

void TryBlocksEncoder::Encode(ir::Code* ir_code, std::shared_ptr<ir::DexFile> dex_ir) {
  SLICER_CHECK(handlers_.empty());
  SLICER_CHECK(tries_.empty());

  // first, count the number of try blocks
  struct TryBlockEndVisitor : public Visitor {
    int tries_count = 0;
    bool Visit(TryBlockEnd* try_end) override {
      ++tries_count;
      return true;
    }
  };
  TryBlockEndVisitor visitor;
  for (auto instr : instructions_) {
    instr->Accept(&visitor);
  }
  int tries_count = visitor.tries_count;
  SLICER_CHECK_LT(tries_count, (1 << 16));

  // no try blocks?
  if (tries_count == 0) {
    ir_code->try_blocks = {};
    ir_code->catch_handlers = {};
    return;
  }

  // "encoded_catch_handler_list.size"
  handlers_.PushULeb128(tries_count);

  // generate the try blocks & encoded catch handlers
  //
  // NOTE: try_item[tries_count] :
  //  "Elements of the array must be non-overlapping in range and
  //  in order from low to high address. This element is only present
  //  if tries_size is non-zero"
  //
  // NOTE: we're not de-duplicating catch_handlers
  //   (generate one catch_handler for each try block)
  //
  for (auto instr : instructions_) {
    instr->Accept(this);
  }
  SLICER_CHECK(!tries_.empty());
  SLICER_CHECK(!handlers_.empty());
  tries_.Seal(1);
  handlers_.Seal(1);

  // update ir::Code
  auto tries_ptr = tries_.ptr<const dex::TryBlock>(0);
  ir_code->try_blocks = slicer::ArrayView<const dex::TryBlock>(tries_ptr, tries_count);
  ir_code->catch_handlers = slicer::MemView(handlers_.data(), handlers_.size());

  // attach the generated try/catch blocks to the dex IR
  dex_ir->AttachBuffer(std::move(tries_));
  dex_ir->AttachBuffer(std::move(handlers_));
}

}  // namespace lir
