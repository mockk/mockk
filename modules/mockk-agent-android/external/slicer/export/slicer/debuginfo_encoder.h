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

#include "buffer.h"
#include "chronometer.h"
#include "common.h"
#include "code_ir.h"
#include "dex_ir.h"

#include <vector>

namespace lir {

// Generates debug info from code IR
class DebugInfoEncoder : public Visitor {
 private:
  virtual bool Visit(DbgInfoHeader* dbg_header) override;
  virtual bool Visit(DbgInfoAnnotation* dbg_annotation) override;

 public:
  explicit DebugInfoEncoder(const InstructionsList& instructions)
    : instructions_(instructions) {
  }

  ~DebugInfoEncoder() = default;

  void Encode(ir::EncodedMethod* ir_method, std::shared_ptr<ir::DexFile> dex_ir);

 private:
  std::vector<ir::String*>* param_names_ = nullptr;
  dex::u4 line_start_ = 0;
  dex::u4 last_line_ = 0;
  dex::u4 last_address_ = 0;
  ir::String* source_file_ = nullptr;
  slicer::Buffer dbginfo_;
  const InstructionsList& instructions_;
};

} // namespace lir

