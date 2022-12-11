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
#include "arrayview.h"
#include "memview.h"
#include "dex_leb128.h"

#include <assert.h>
#include <string>
#include <algorithm>
#include <vector>
#include <cstring>

namespace slicer {

// A simple growing memory buffer
//
// NOTE: pointers into this buffer are not stable
//   since it may be relocated as it expands.
//
class Buffer {
 public:
  Buffer() = default;

  ~Buffer() { Free(); }

  Buffer(const Buffer&) = delete;
  Buffer& operator=(const Buffer&) = delete;

  Buffer(Buffer&& b) {
    std::swap(buff_, b.buff_);
    std::swap(size_, b.size_);
    std::swap(capacity_, b.capacity_);
  }

  Buffer& operator=(Buffer&& b) {
    Free();
    std::swap(buff_, b.buff_);
    std::swap(size_, b.size_);
    std::swap(capacity_, b.capacity_);
    return *this;
  }

 public:
  // Align the total size and prevent further changes
  size_t Seal(size_t alignment) {
    SLICER_CHECK(!sealed_);
    Align(alignment);
    sealed_ = true;
    return size();
  }

  // Returns a pointer within the buffer
  //
  // NOTE: the returned pointer is "ephemeral" and
  //   is only valid until the next buffer push/alloc
  //
  template <class T>
  T* ptr(size_t offset) {
    SLICER_CHECK(offset + sizeof(T) <= size_);
    return reinterpret_cast<T*>(buff_ + offset);
  }

  // Align the buffer size to the specified alignment
  void Align(size_t alignment) {
    assert(alignment > 0);
    size_t rem = size_ % alignment;
    if (rem != 0) {
      Alloc(alignment - rem);
    }
  }

  size_t Alloc(size_t size) {
    size_t offset = size_;
    Expand(size);
    std::memset(buff_ + offset, 0, size);
    return offset;
  }

  size_t Push(const void* ptr, size_t size) {
    size_t offset = size_;
    Expand(size);
    std::memcpy(buff_ + offset, ptr, size);
    return offset;
  }

  size_t Push(const MemView& memView) {
    return Push(memView.ptr(), memView.size());
  }

  template <class T>
  size_t Push(const ArrayView<T>& a) {
    return Push(a.data(), a.size() * sizeof(T));
  }

  template <class T>
  size_t Push(const std::vector<T>& v) {
    return Push(v.data(), v.size() * sizeof(T));
  }

  size_t Push(const Buffer& buff) {
    SLICER_CHECK(&buff != this);
    return Push(buff.data(), buff.size());
  }

  // TODO: this is really dangerous since it would
  //   write any type - sometimes not what you expect.
  //
  template <class T>
  size_t Push(const T& value) {
    return Push(&value, sizeof(value));
  }

  size_t PushULeb128(dex::u4 value) {
    dex::u1 tmp[4];
    dex::u1* end = dex::WriteULeb128(tmp, value);
    assert(end > tmp && end - tmp <= 4);
    return Push(tmp, end - tmp);
  }

  size_t PushSLeb128(dex::s4 value) {
    dex::u1 tmp[4];
    dex::u1* end = dex::WriteSLeb128(tmp, value);
    assert(end > tmp && end - tmp <= 4);
    return Push(tmp, end - tmp);
  }

  size_t size() const { return size_; }

  bool empty() const { return size_ == 0; }

  void Free() {
    ::free(buff_);
    buff_ = nullptr;
    size_ = 0;
    capacity_ = 0;
  }

  const dex::u1* data() const {
    SLICER_CHECK(buff_ != nullptr);
    return buff_;
  }

 private:
  void Expand(size_t size) {
    SLICER_CHECK(!sealed_);
    if (size_ + size > capacity_) {
      capacity_ = std::max(size_t(capacity_ * 1.5), size_ + size);
      buff_ = static_cast<dex::u1*>(::realloc(buff_, capacity_));
      SLICER_CHECK(buff_ != nullptr);
    }
    size_ += size;
  }

 private:
  dex::u1* buff_ = nullptr;
  size_t size_ = 0;
  size_t capacity_ = 0;
  bool sealed_ = false;
};

} // namespace slicer

