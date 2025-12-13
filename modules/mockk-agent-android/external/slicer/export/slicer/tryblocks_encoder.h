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
#include "code_ir.h"
#include "common.h"
#include "dex_ir.h"

namespace lir {

// Generates try/catch blocks from code IR
class TryBlocksEncoder : public Visitor {
 private:
  virtual bool Visit(TryBlockEnd* try_end) override;

 public:
  explicit TryBlocksEncoder(const InstructionsList& instructions)
    : instructions_(instructions) {
  }

  ~TryBlocksEncoder() = default;

  void Encode(ir::Code* ir_code, std::shared_ptr<ir::DexFile> dex_ir);

 private:
  slicer::Buffer handlers_;
  slicer::Buffer tries_;
  const InstructionsList& instructions_;
};

} // namespace lir

