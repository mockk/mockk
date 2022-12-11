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

namespace slicer {

// Encapsulate the runtime check and error reporting policy.
// (currently a simple fail-fast but the the intention is to allow customization)
void _checkFailed(const char* expr, int line, const char* file) __attribute__((noreturn));
#define SLICER_CHECK(expr) do { if(!(expr)) slicer::_checkFailed(#expr, __LINE__, __FILE__); } while(false)

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
void _fatal(const char* format, ...) __attribute__((noreturn));
#define SLICER_FATAL(format, ...) slicer::_fatal("\nSLICER_FATAL: " format "\n\n", ##__VA_ARGS__);

// Annotation customization point for extra validation / state.
#ifdef NDEBUG
#define SLICER_EXTRA(x)
#else
#define SLICER_EXTRA(x) x
#endif

} // namespace slicer

