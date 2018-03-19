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

#include "code_ir.h"
#include "common.h"
#include "dex_ir.h"
#include "dex_ir_builder.h"

#include <memory>
#include <vector>
#include <utility>
#include <set>

namespace slicer {

// Interface for a single transformation operation
class Transformation {
 public:
  virtual ~Transformation() = default;
  virtual bool Apply(lir::CodeIr* code_ir) = 0;
};

// Insert a call to the "entry hook" at the start of the instrumented method:
// The "entry hook" will be forwarded the original incoming arguments plus
// an explicit "this" argument for non-static methods.
class EntryHook : public Transformation {
 public:
  explicit EntryHook(
      const ir::MethodId& hook_method_id,
      bool use_object_type_for_this_argument = false)
      : hook_method_id_(hook_method_id),
        use_object_type_for_this_argument_(use_object_type_for_this_argument) {
    // hook method signature is generated automatically
    SLICER_CHECK(hook_method_id_.signature == nullptr);
  }

  virtual bool Apply(lir::CodeIr* code_ir) override;

 private:
  ir::MethodId hook_method_id_;
  // If true, "this" argument of non-static methods is forwarded as Object type.
  // For example "this" argument of OkHttpClient type is forwared as Object and
  // is used to get OkHttp class loader.
  bool use_object_type_for_this_argument_;
};

// Insert a call to the "exit hook" method before every return
// in the instrumented method. The "exit hook" will be passed the
// original return value and it may return a new return value.
class ExitHook : public Transformation {
 public:
  explicit ExitHook(const ir::MethodId& hook_method_id) : hook_method_id_(hook_method_id) {
    // hook method signature is generated automatically
    SLICER_CHECK(hook_method_id_.signature == nullptr);
  }

  virtual bool Apply(lir::CodeIr* code_ir) override;

 private:
  ir::MethodId hook_method_id_;
};

// Replace every invoke-virtual[/range] to the a specified method with
// a invoke-static[/range] to the detour method. The detour is a static
// method which takes the same arguments as the original method plus
// an explicit "this" argument, and returns the same type as the original method
class DetourVirtualInvoke : public Transformation {
 public:
  DetourVirtualInvoke(const ir::MethodId& orig_method_id, const ir::MethodId& detour_method_id)
    : orig_method_id_(orig_method_id), detour_method_id_(detour_method_id) {
    // detour method signature is automatically created
    // to match the original method and must not be explicitly specified
    SLICER_CHECK(detour_method_id_.signature == nullptr);
  }

  virtual bool Apply(lir::CodeIr* code_ir) override;

 private:
  ir::MethodId orig_method_id_;
  ir::MethodId detour_method_id_;
};

// Allocates scratch registers without doing a full register allocation
class AllocateScratchRegs : public Transformation {
 public:
  explicit AllocateScratchRegs(int allocate_count, bool allow_renumbering = true)
    : allocate_count_(allocate_count), allow_renumbering_(allow_renumbering) {
    SLICER_CHECK(allocate_count > 0);
  }

  virtual bool Apply(lir::CodeIr* code_ir) override;

  const std::set<dex::u4>& ScratchRegs() const {
    SLICER_CHECK(scratch_regs_.size() == static_cast<size_t>(allocate_count_));
    return scratch_regs_;
  }

 private:
  void RegsRenumbering(lir::CodeIr* code_ir);
  void ShiftParams(lir::CodeIr* code_ir);
  void Allocate(lir::CodeIr* code_ir, dex::u4 first_reg, int count);

 private:
  const int allocate_count_;
  const bool allow_renumbering_;
  int left_to_allocate_ = 0;
  std::set<dex::u4> scratch_regs_;
};

// A friendly helper for instrumenting existing methods: it allows batching
// a set of transformations to be applied to method (the batching allow it
// to build and encode the code IR once per method regardless of how many
// transformation are applied)
//
// For example, if we want to add both entry and exit hooks to a
// Hello.Test(int) method, the code would look like this:
//
//    ...
//    slicer::MethodInstrumenter mi(dex_ir);
//    mi.AddTransformation<slicer::EntryHook>(ir::MethodId("LTracer;", "OnEntry"));
//    mi.AddTransformation<slicer::ExitHook>(ir::MethodId("LTracer;", "OnExit"));
//    SLICER_CHECK(mi.InstrumentMethod(ir::MethodId("LHello;", "Test", "(I)I")));
//    ...
//
class MethodInstrumenter {
 public:
  explicit MethodInstrumenter(std::shared_ptr<ir::DexFile> dex_ir) : dex_ir_(dex_ir) {}

  // No copy/move semantics
  MethodInstrumenter(const MethodInstrumenter&) = delete;
  MethodInstrumenter& operator=(const MethodInstrumenter&) = delete;

  // Queue a transformation
  // (T is a class derived from Transformation)
  template<class T, class... Args>
  T* AddTransformation(Args&&... args) {
    T* transformation = new T(std::forward<Args>(args)...);
    transformations_.emplace_back(transformation);
    return transformation;
  }

  // Apply all the queued transformations to the specified method
  bool InstrumentMethod(ir::EncodedMethod* ir_method);
  bool InstrumentMethod(const ir::MethodId& method_id);

 private:
  std::shared_ptr<ir::DexFile> dex_ir_;
  std::vector<std::unique_ptr<Transformation>> transformations_;
};

}  // namespace slicer
