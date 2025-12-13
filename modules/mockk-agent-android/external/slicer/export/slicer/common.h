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

#include <stdint.h>
#include <string>

namespace slicer {

// Encapsulate the runtime check and error reporting policy.
// (currently a simple fail-fast but the the intention is to allow customization)
void _checkFailed(const char* expr, int line, const char* file) __attribute__((noreturn));
#define SLICER_CHECK(expr) do { if(!(expr)) slicer::_checkFailed(#expr, __LINE__, __FILE__); } while(false)

// Helper methods for SLICER_CHECK_OP macro.
void _checkFailedOp(const void* lhs, const void* rhs, const char* op, const char* suffix,
                    int line, const char* file)
  __attribute__((noreturn));
void _checkFailedOp(uint32_t lhs, uint32_t rhs, const char* op, const char* suffix, int line,
                    const char* file)
  __attribute__((noreturn));

#define SLICER_CHECK_OP(lhs, rhs, op, suffix) \
  do { \
    if (!((lhs) op (rhs))) { \
      slicer::_checkFailedOp(lhs, rhs, #op, suffix, __LINE__, __FILE__); \
    } \
  } while(false)

// Macros that check the binary relation between two values. If the relation is not true,
// the values are logged and the process aborts.
#define SLICER_CHECK_EQ(a, b) SLICER_CHECK_OP(a, b, ==, "EQ")
#define SLICER_CHECK_NE(a, b) SLICER_CHECK_OP(a, b, !=, "NE")
#define SLICER_CHECK_LT(a, b) SLICER_CHECK_OP(a, b,  <, "LT")
#define SLICER_CHECK_LE(a, b) SLICER_CHECK_OP(a, b, <=, "LE")
#define SLICER_CHECK_GT(a, b) SLICER_CHECK_OP(a, b,  >, "GT")
#define SLICER_CHECK_GE(a, b) SLICER_CHECK_OP(a, b, >=, "GE")

// A modal check: if the strict mode is enabled, it behaves as a SLICER_CHECK,
// otherwise it will only log a warning and continue
//
// NOTE: we use SLICER_WEAK_CHECK for .dex format validations that are frequently
//   violated by existing apps. So we need to be able to annotate these common
//   problems and potentially ignoring them for parity with the Android runtime.
//
void _weakCheckFailed(const char* expr, int line, const char* file);
#define SLICER_WEAK_CHECK(expr) do { if(!(expr)) slicer::_weakCheckFailed(#expr, __LINE__, __FILE__); } while(false)

// Report a fatal condition with a printf-formatted message
void _fatal(const std::string& msg) __attribute__((noreturn));
#define SLICER_FATAL(msg) slicer::_fatal(msg)

// Annotation customization point for extra validation / state.
#ifdef NDEBUG
#define SLICER_EXTRA(x)
#else
#define SLICER_EXTRA(x) x
#endif

#ifndef FALLTHROUGH_INTENDED
#ifdef __clang__
#define FALLTHROUGH_INTENDED [[clang::fallthrough]]
#else
#define FALLTHROUGH_INTENDED
#endif // __clang__
#endif // FALLTHROUGH_INTENDED

typedef void (*logger_type)(const std::string&);

// By default, slicer prints error messages to stdout. Users can set their own
// callback.
void set_logger(const logger_type new_logger);

} // namespace slicer

