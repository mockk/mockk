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

// MUTF-8 (Modified UTF-8) Encoding helpers:
// https://source.android.com/devices/tech/dalvik/dex-format.html

namespace dex {

// Compare two '\0'-terminated modified UTF-8 strings, using Unicode
// code point values for comparison. This treats different encodings
// for the same code point as equivalent, except that only a real '\0'
// byte is considered the string terminator. The return value is as
// for strcmp().
int Utf8Cmp(const char* s1, const char* s2);

}  // namespace dex
