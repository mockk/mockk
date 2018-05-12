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

#include "slicer/common.h"

#include <stdio.h>
#include <stdlib.h>
#include <cstdarg>
#include <set>
#include <utility>

namespace slicer {

// Helper for the default SLICER_CHECK() policy
void _checkFailed(const char* expr, int line, const char* file) {
  printf("\nSLICER_CHECK failed [%s] at %s:%d\n\n", expr, file, line);
  abort();
}

// keep track of the failures we already saw to avoid spamming with duplicates
thread_local std::set<std::pair<int, const char*>> weak_failures;

// Helper for the default SLICER_WEAK_CHECK() policy
//
// TODO: implement a modal switch (abort/continue)
//
void _weakCheckFailed(const char* expr, int line, const char* file) {
  auto failure_id = std::make_pair(line, file);
  if (weak_failures.find(failure_id) == weak_failures.end()) {
    printf("\nSLICER_WEAK_CHECK failed [%s] at %s:%d\n\n", expr, file, line);
    weak_failures.insert(failure_id);
  }
}

// Prints a formatted message and aborts
void _fatal(const char* format, ...) {
  va_list args;
  va_start(args, format);
  vprintf(format, args);
  va_end(args);
  abort();
}

} // namespace slicer

