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

#include "slicer/dex_format.h"

namespace dex {

// Retrieve the next UTF-16 character from a UTF-8 string.
// Advances "*pUtf8Ptr" to the start of the next character.
//
// NOTE: If a string is corrupted by dropping a '\0' in the middle
// of a 3-byte sequence, you can end up overrunning the buffer with
// reads (and possibly with the writes if the length was computed and
// cached before the damage). For performance reasons, this function
// assumes that the string being parsed is known to be valid (e.g., by
// already being verified).
static u2 GetUtf16FromUtf8(const char** pUtf8Ptr) {
  u4 one = *(*pUtf8Ptr)++;
  if ((one & 0x80) != 0) {
    // two- or three-byte encoding
    u4 two = *(*pUtf8Ptr)++;
    if ((one & 0x20) != 0) {
      // three-byte encoding
      u4 three = *(*pUtf8Ptr)++;
      return ((one & 0x0f) << 12) | ((two & 0x3f) << 6) | (three & 0x3f);
    } else {
      // two-byte encoding
      return ((one & 0x1f) << 6) | (two & 0x3f);
    }
  } else {
    // one-byte encoding
    return one;
  }
}

int Utf8Cmp(const char* s1, const char* s2) {
  for (;;) {
    if (*s1 == '\0') {
      if (*s2 == '\0') {
        return 0;
      }
      return -1;
    } else if (*s2 == '\0') {
      return 1;
    }

    int utf1 = GetUtf16FromUtf8(&s1);
    int utf2 = GetUtf16FromUtf8(&s2);
    int diff = utf1 - utf2;

    if (diff != 0) {
      return diff;
    }
  }
}

}  // namespace dex
