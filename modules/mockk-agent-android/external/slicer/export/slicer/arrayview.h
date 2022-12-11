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

#include <stdlib.h>

namespace slicer {

// A shallow array view
template <class T>
class ArrayView {
 public:
  ArrayView() = default;

  ArrayView(const ArrayView&) = default;
  ArrayView& operator=(const ArrayView&) = default;

  ArrayView(T* ptr, size_t count) : begin_(ptr), end_(ptr + count) {}

  T* begin() const { return begin_; }
  T* end() const { return end_; }

  T* data() const { return begin_; }

  T& operator[](size_t i) const {
    SLICER_CHECK(i < size());
    return *(begin_ + i);
  }

  size_t size() const { return end_ - begin_; }
  bool empty() const { return begin_ == end_; }

 private:
  T* begin_ = nullptr;
  T* end_ = nullptr;
};

} // namespace slicer

