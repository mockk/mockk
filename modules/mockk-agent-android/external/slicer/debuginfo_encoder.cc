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

#include "slicer/debuginfo_encoder.h"

#include "slicer/common.h"

#include <assert.h>
#include <sstream>
#include <iomanip>


namespace lir {

bool DebugInfoEncoder::Visit(DbgInfoHeader* dbg_header) {
  assert(param_names_ == nullptr);
  param_names_ = &dbg_header->param_names;
  return true;
}

bool DebugInfoEncoder::Visit(DbgInfoAnnotation* dbg_annotation) {
  // keep the address in sync
  if (last_address_ != dbg_annotation->offset) {
    SLICER_CHECK_GT(dbg_annotation->offset, last_address_);
    dbginfo_.Push<dex::u1>(dex::DBG_ADVANCE_PC);
    dbginfo_.PushULeb128(dbg_annotation->offset - last_address_);
    last_address_ = dbg_annotation->offset;
  }

  // encode the annotation itself
  switch (dbg_annotation->dbg_opcode) {
    case dex::DBG_ADVANCE_LINE: {
      // DBG_ANDVANCE_LINE is used a bit differently in the code IR
      // vs the .dex image: the code IR uses it exclusively for source
      // location (the .line directive) while .dex format uses it to
      // advance the "line" register without emitting a "position entry"
      int line = dbg_annotation->CastOperand<LineNumber>(0)->line;
      if (line_start_ == 0) {
        // it's not perfectly clear from the .dex specification
        // if initial line == 0 is valid, but a number of existing
        // .dex files do this so we have to support it
        SLICER_CHECK_GE(line, 0);
        line_start_ = line;
      } else {
        SLICER_WEAK_CHECK(line >= 0);
        int delta = line - last_line_;
        int adj_opcode = delta - dex::DBG_LINE_BASE;
        // out of range for special opcode?
        if (adj_opcode < 0 || adj_opcode >= dex::DBG_LINE_RANGE) {
          dbginfo_.Push<dex::u1>(dex::DBG_ADVANCE_LINE);
          dbginfo_.PushSLeb128(delta);
          adj_opcode = -dex::DBG_LINE_BASE;
        }
        assert(adj_opcode >= 0 && dex::DBG_FIRST_SPECIAL + adj_opcode < 256);
        dex::u1 special_opcode = dex::DBG_FIRST_SPECIAL + adj_opcode;
        dbginfo_.Push<dex::u1>(special_opcode);
      }
      last_line_ = line;
    } break;

    case dex::DBG_START_LOCAL: {
      auto reg = dbg_annotation->CastOperand<VReg>(0)->reg;
      auto name_index = dbg_annotation->CastOperand<String>(1)->index;
      auto type_index = dbg_annotation->CastOperand<Type>(2)->index;
      dbginfo_.Push<dex::u1>(dex::DBG_START_LOCAL);
      dbginfo_.PushULeb128(reg);
      dbginfo_.PushULeb128(name_index + 1);
      dbginfo_.PushULeb128(type_index + 1);
    } break;

    case dex::DBG_START_LOCAL_EXTENDED: {
      auto reg = dbg_annotation->CastOperand<VReg>(0)->reg;
      auto name_index = dbg_annotation->CastOperand<String>(1)->index;
      auto type_index = dbg_annotation->CastOperand<Type>(2)->index;
      auto sig_index = dbg_annotation->CastOperand<String>(3)->index;
      dbginfo_.Push<dex::u1>(dex::DBG_START_LOCAL_EXTENDED);
      dbginfo_.PushULeb128(reg);
      dbginfo_.PushULeb128(name_index + 1);
      dbginfo_.PushULeb128(type_index + 1);
      dbginfo_.PushULeb128(sig_index + 1);
    } break;

    case dex::DBG_END_LOCAL:
    case dex::DBG_RESTART_LOCAL: {
      auto reg = dbg_annotation->CastOperand<VReg>(0)->reg;
      dbginfo_.Push<dex::u1>(dbg_annotation->dbg_opcode);
      dbginfo_.PushULeb128(reg);
    } break;

    case dex::DBG_SET_PROLOGUE_END:
    case dex::DBG_SET_EPILOGUE_BEGIN:
      dbginfo_.Push<dex::u1>(dbg_annotation->dbg_opcode);
      break;

    case dex::DBG_SET_FILE: {
      auto file_name = dbg_annotation->CastOperand<String>(0);
      if (file_name->ir_string != source_file_) {
        source_file_ = file_name->ir_string;
        dbginfo_.Push<dex::u1>(dex::DBG_SET_FILE);
        dbginfo_.PushULeb128(file_name->index + 1);
      }
    } break;

    default: {
      std::stringstream ss;
      ss << "Unexpected debug info opcode: " << dbg_annotation->dbg_opcode;
      SLICER_FATAL(ss.str());
    }
  }

  return true;
}

void DebugInfoEncoder::Encode(ir::EncodedMethod* ir_method, std::shared_ptr<ir::DexFile> dex_ir) {
  auto ir_debug_info = ir_method->code->debug_info;

  SLICER_CHECK(dbginfo_.empty());
  SLICER_CHECK_EQ(param_names_, nullptr);
  SLICER_CHECK_EQ(line_start_, 0);
  SLICER_CHECK_EQ(last_line_, 0);
  SLICER_CHECK_EQ(last_address_, 0);
  SLICER_CHECK_EQ(source_file_, nullptr);

  // generate new debug info
  source_file_ = ir_method->decl->parent->class_def->source_file;
  for (auto instr : instructions_) {
    instr->Accept(this);
  }
  dbginfo_.Push<dex::u1>(dex::DBG_END_SEQUENCE);
  dbginfo_.Seal(1);

  SLICER_CHECK(!dbginfo_.empty());

  // update ir::DebugInfo
  ir_debug_info->line_start = line_start_;
  ir_debug_info->data = slicer::MemView(dbginfo_.data(), dbginfo_.size());

  if (param_names_ != nullptr) {
    ir_debug_info->param_names = *param_names_;
  } else {
    ir_debug_info->param_names = {};
  }

  // attach the debug info buffer to the dex IR
  dex_ir->AttachBuffer(std::move(dbginfo_));
}

}  // namespace lir
