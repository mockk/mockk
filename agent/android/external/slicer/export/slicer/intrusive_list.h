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

namespace slicer {

// A minimal intrusive linked list with a STL-style container interface
// (It works for any type T which has T* next, prev fields)
//
template <class T>
class IntrusiveList {
 public:
  struct Iterator {
    explicit Iterator(T* p) : p_(p) {}

    bool operator==(Iterator other) const { return p_ == other.p_; }
    bool operator!=(Iterator other) const { return p_ != other.p_; }

    T* operator*() const {
      assert(p_ != nullptr);
      return p_;
    }

    Iterator operator++() {
      assert(p_ != nullptr);
      p_ = p_->next;
      return *this;
    }

    Iterator operator++(int) {
      auto tmp(*this);
      operator++();
      return tmp;
    }

    Iterator operator--() {
      assert(p_ != nullptr);
      p_ = p_->prev;
      return *this;
    }

    Iterator operator--(int) {
      auto tmp(*this);
      operator--();
      return tmp;
    }

   private:
    T* p_;
  };

 public:
  IntrusiveList() = default;
  ~IntrusiveList() = default;

  IntrusiveList(const IntrusiveList&) = delete;
  IntrusiveList& operator=(const IntrusiveList&) = delete;

  void push_back(T* p) {
    insert(end(), p);
  }

  Iterator insert(Iterator it, T* p) {
    return InsertBefore(*it, p);
  }

  Iterator InsertBefore(T* pos, T* p) {
    assert(p != nullptr);
    assert(p->next == nullptr);
    assert(p->prev == nullptr);
    assert(pos != nullptr);
    p->prev = pos->prev;
    if (pos == begin_) {
      assert(pos->prev == nullptr);
      begin_ = p;
    } else {
      assert(pos->prev != nullptr);
      p->prev->next = p;
    }
    p->next = pos;
    pos->prev = p;
    return Iterator(p);
  }

  Iterator InsertAfter(T* pos, T* p) {
    assert(p != nullptr);
    assert(p->next == nullptr);
    assert(p->prev == nullptr);
    assert(pos != nullptr);
    assert(pos != &end_sentinel_);
    p->next = pos->next;
    p->next->prev = p;
    p->prev = pos;
    pos->next = p;
    return Iterator(p);
  }

  void Remove(T* pos) {
    SLICER_CHECK(pos != end_);
    if (pos->prev != nullptr) {
      assert(pos != begin_);
      pos->prev->next = pos->next;
    } else {
      assert(pos == begin_);
      begin_ = pos->next;
    }
    assert(pos->next != nullptr);
    pos->next->prev = pos->prev;
    pos->prev = nullptr;
    pos->next = nullptr;
  }

  bool empty() const { return begin_ == end_; }

  Iterator begin() const { return Iterator(begin_); }
  Iterator end() const { return Iterator(end_); }

 private:
  T* begin_ = &end_sentinel_;
  T* const end_ = &end_sentinel_;
  T end_sentinel_;
};

}  // namespace slicer
