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

#include <assert.h>
#include <stdlib.h>

namespace slicer {

// A shallow, non-owning reference to a "view" inside a memory buffer
class MemView {
 public:
  MemView() : ptr_(nullptr), size_(0) {}

  MemView(const void* ptr, size_t size) : ptr_(ptr), size_(size) {
    assert(size > 0);
  }

  ~MemView() = default;

  template <class T = void>
  const T* ptr() const {
    return static_cast<const T*>(ptr_);
  }

  size_t size() const { return size_; }

 private:
  const void* ptr_;
  size_t size_;
};

} // namespace slicer

