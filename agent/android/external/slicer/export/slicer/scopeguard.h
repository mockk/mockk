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

// A simple and lightweight scope guard and macro
// (inspired by Andrei Alexandrescu's C++11 Scope Guard)
//
// Here is how it's used:
//
//  FILE* file = std::fopen(...);
//  SLICER_SCOPE_EXIT {
//      std::fclose(file);
//  };
//
// "file" will be closed at the end of the enclosing scope,
//  regardless of how the scope is exited
//
class ScopeGuardHelper
{
    template<class T>
    class ScopeGuard
    {
    public:
        explicit ScopeGuard(T closure) :
            closure_(std::move(closure))
        {
        }

        ~ScopeGuard()
        {
            closure_();
        }

        // move constructor only
        ScopeGuard(ScopeGuard&&) = default;
        ScopeGuard(const ScopeGuard&) = delete;
        ScopeGuard& operator=(const ScopeGuard&) = delete;
        ScopeGuard& operator=(ScopeGuard&&) = delete;

    private:
        T closure_;
    };

public:
    template<class T>
    ScopeGuard<T> operator<<(T closure)
    {
        return ScopeGuard<T>(std::move(closure));
    }
};

#define SLICER_SG_MACRO_CONCAT2(a, b) a ## b
#define SLICER_SG_MACRO_CONCAT(a, b) SLICER_SG_MACRO_CONCAT2(a, b)
#define SLICER_SG_ANONYMOUS(prefix)  SLICER_SG_MACRO_CONCAT(prefix, __COUNTER__)

#define SLICER_SCOPE_EXIT \
    auto SLICER_SG_ANONYMOUS(_scope_guard_) = slicer::ScopeGuardHelper() << [&]()

} // namespace slicer

