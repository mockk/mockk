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
#include "dex_ir.h"

#include <memory>

namespace ir {

// Packing together the name components which identify a Java method
// (depending on the use some fields, ex. signature, are optional)
//
// NOTE: the "signature" uses the JNI signature syntax:
//  https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/types.html#type_signatures
//
struct MethodId {
  const char* class_descriptor;
  const char* method_name;
  const char* signature;

  MethodId(const char* class_descriptor, const char* method_name, const char* signature = nullptr)
      : class_descriptor(class_descriptor), method_name(method_name), signature(signature) {
    assert(class_descriptor != nullptr);
    assert(method_name != nullptr);
  }

  bool Match(MethodDecl* method_decl) const;
};

// This class enables modifications to a .dex IR
class Builder {
 public:
  explicit Builder(std::shared_ptr<ir::DexFile> dex_ir) : dex_ir_(dex_ir) {}

  // No copy/move semantics
  Builder(const Builder&) = delete;
  Builder& operator=(const Builder&) = delete;

  // Get/Create .dex IR nodes
  // (get existing instance or create a new one)
  String* GetAsciiString(const char* cstr);
  Type* GetType(String* descriptor);
  Proto* GetProto(Type* return_type, TypeList* param_types);
  FieldDecl* GetFieldDecl(String* name, Type* type, Type* parent);
  MethodDecl* GetMethodDecl(String* name, Proto* proto, Type* parent);
  TypeList* GetTypeList(const std::vector<Type*>& types);

  // Convenience overloads
  Type* GetType(const char* descriptor) {
    return GetType(GetAsciiString(descriptor));
  }

  // Locate an existing method definition
  // (returns nullptr if the method is not found)
  EncodedMethod* FindMethod(const MethodId& method_id) const;

 private:
  // Locate an existing .dex IR string
  // (returns nullptr if the string is not found)
  String* FindAsciiString(const char* cstr) const;

  // Locate an existing .dex IR prototype
  // (returns nullptr if the prototype is not found)
  Proto* FindPrototype(const char* signature) const;

 private:
  std::shared_ptr<ir::DexFile> dex_ir_;
};

}  // namespace ir
