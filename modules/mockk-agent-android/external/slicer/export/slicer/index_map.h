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
#include "dex_format.h"

#include <vector>

namespace ir {

// A simple index tracking and allocator
class IndexMap {
 public:
  dex::u4 AllocateIndex() {
    const auto size = indexes_map_.size();
    while (alloc_pos_ < size && indexes_map_[alloc_pos_]) {
      ++alloc_pos_;
    }
    MarkUsedIndex(alloc_pos_);
    return alloc_pos_++;
  }

  void MarkUsedIndex(dex::u4 index) {
    if (index >= indexes_map_.size()) {
      indexes_map_.resize(index + 1);
    }
    SLICER_CHECK(!indexes_map_[index]);
    indexes_map_[index] = true;
  }

 private:
  std::vector<bool> indexes_map_;
  dex::u4 alloc_pos_ = 0;
};

}  // namespace ir
