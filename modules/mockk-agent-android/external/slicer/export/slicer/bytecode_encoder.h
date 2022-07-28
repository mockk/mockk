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
#include "code_ir.h"
#include "dex_ir.h"
#include "buffer.h"

#include <assert.h>
#include <vector>

namespace lir {

// Generates .dex bytecode from code IR
class BytecodeEncoder : public Visitor {
 public:
  explicit BytecodeEncoder(const InstructionsList& instructions)
    : instructions_(instructions) {
  }

  ~BytecodeEncoder() = default;

  void Encode(ir::Code* ir_code, std::shared_ptr<ir::DexFile> dex_ir);

 private:
  // the visitor interface
  virtual bool Visit(Bytecode* bytecode) override;
  virtual bool Visit(PackedSwitchPayload* packed_switch) override;
  virtual bool Visit(SparseSwitchPayload* sparse_switch) override;
  virtual bool Visit(ArrayData* array_data) override;
  virtual bool Visit(Label* label) override;
  virtual bool Visit(DbgInfoHeader* dbg_header) override;
  virtual bool Visit(DbgInfoAnnotation* dbg_annotation) override;
  virtual bool Visit(TryBlockBegin* try_begin) override;
  virtual bool Visit(TryBlockEnd* try_end) override;

  // fixup helpers
  void FixupSwitchOffsets();
  void FixupPackedSwitch(dex::u4 base_offset, dex::u4 payload_offset);
  void FixupSparseSwitch(dex::u4 base_offset, dex::u4 payload_offset);
  void FixupLabels();

 private:
  // Structure used to track code location fixups
  struct LabelFixup {
    dex::u4 offset;       // instruction to be fixed up
    const Label* label;   // target label
    bool short_fixup;     // 16bit or 32bit fixup?

    LabelFixup(dex::u4 offset, Label* label, bool short_fixup) :
      offset(offset), label(label), short_fixup(short_fixup) {}
  };

 private:
  slicer::Buffer bytecode_;
  std::vector<LabelFixup> fixups_;

  // Current bytecode offset (in 16bit units)
  dex::u4 offset_ = 0;

  // Number of registers using for outgoing arguments
  dex::u4 outs_count_ = 0;

  // Keeping track of the switch payload instructions for late fixups
  // (map encoded bytecode offset -> LIR instruction)
  std::map<dex::u4, const PackedSwitchPayload*> packed_switches_;
  std::map<dex::u4, const SparseSwitchPayload*> sparse_switches_;

  const InstructionsList& instructions_;
};

} // namespace lir

