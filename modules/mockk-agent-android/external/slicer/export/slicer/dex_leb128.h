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

#include "dex_format.h"

// LEB128 encode/decode helpers:
// https://source.android.com/devices/tech/dalvik/dex-format.html

namespace dex {

// Reads an unsigned LEB128 value, updating the given pointer to
// point just past the end of the read value.
inline u4 ReadULeb128(const u1** pptr) {
  const u1* ptr = *pptr;
  u4 result = *(ptr++);

  if (result > 0x7f) {
    u4 cur = *(ptr++);
    result = (result & 0x7f) | ((cur & 0x7f) << 7);
    if (cur > 0x7f) {
      cur = *(ptr++);
      result |= (cur & 0x7f) << 14;
      if (cur > 0x7f) {
        cur = *(ptr++);
        result |= (cur & 0x7f) << 21;
        if (cur > 0x7f) {
          // We don't check to see if cur is out of
          // range here, meaning we tolerate garbage in the
          // high four-order bits.
          cur = *(ptr++);
          result |= cur << 28;
        }
      }
    }
  }

  *pptr = ptr;
  return result;
}

// Reads a signed LEB128 value, updating the given pointer to
// point just past the end of the read value.
inline s4 ReadSLeb128(const u1** pptr) {
  const u1* ptr = *pptr;
  s4 result = *(ptr++);

  if (result <= 0x7f) {
    result = (result << 25) >> 25;
  } else {
    s4 cur = *(ptr++);
    result = (result & 0x7f) | ((cur & 0x7f) << 7);
    if (cur <= 0x7f) {
      result = (result << 18) >> 18;
    } else {
      cur = *(ptr++);
      result |= (cur & 0x7f) << 14;
      if (cur <= 0x7f) {
        result = (result << 11) >> 11;
      } else {
        cur = *(ptr++);
        result |= (cur & 0x7f) << 21;
        if (cur <= 0x7f) {
          result = (result << 4) >> 4;
        } else {
          // Note: We don't check to see if cur is out of
          // range here, meaning we tolerate garbage in the
          // high four-order bits.
          cur = *(ptr++);
          result |= cur << 28;
        }
      }
    }
  }

  *pptr = ptr;
  return result;
}

// Writes a 32-bit value in unsigned ULEB128 format.
// Returns the updated pointer.
inline u1* WriteULeb128(u1* ptr, u4 data) {
  for (;;) {
    u1 out = data & 0x7f;
    if (out != data) {
      *ptr++ = out | 0x80;
      data >>= 7;
    } else {
      *ptr++ = out;
      break;
    }
  }
  return ptr;
}

// Writes a 32-bit value in signed ULEB128 format.
// Returns the updated pointer.
inline u1* WriteSLeb128(u1* ptr, s4 value) {
  u4 extra_bits = static_cast<u4>(value ^ (value >> 31)) >> 6;
  u1 out = value & 0x7f;
  while (extra_bits != 0u) {
    *ptr++ = out | 0x80;
    value >>= 7;
    out = value & 0x7f;
    extra_bits >>= 7;
  }
  *ptr++ = out;
  return ptr;
}

}  // namespace dex
