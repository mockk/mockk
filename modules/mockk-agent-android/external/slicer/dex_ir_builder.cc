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

#include "slicer/dex_ir_builder.h"

#include <sstream>
#include <string.h>

namespace ir {

bool MethodId::Match(MethodDecl* method_decl) const {
  return ::strcmp(class_descriptor, method_decl->parent->descriptor->c_str()) == 0
    && ::strcmp(method_name, method_decl->name->c_str()) == 0
    && method_decl->prototype->Signature() == signature;
}

EncodedMethod* Builder::FindMethod(const MethodId& method_id) const {
  // first, lookup the strings
  auto ir_descriptor = FindAsciiString(method_id.class_descriptor);
  auto ir_method_name = FindAsciiString(method_id.method_name);
  if (ir_descriptor == nullptr || ir_method_name == nullptr) {
    return nullptr;
  }

  // look up the prototype
  auto ir_prototype = FindPrototype(method_id.signature);
  if (ir_prototype == nullptr) {
    return nullptr;
  }

  // look up the method itself
  ir::MethodKey method_key;
  method_key.class_descriptor = ir_descriptor;
  method_key.method_name = ir_method_name;
  method_key.prototype = ir_prototype;
  return dex_ir_->methods_lookup.Lookup(method_key);
}

Proto* Builder::FindPrototype(const char* signature) const {
  return dex_ir_->prototypes_lookup.Lookup(signature);
}

String* Builder::FindAsciiString(const char* cstr) const {
    return dex_ir_->strings_lookup.Lookup(cstr);
}

String* Builder::GetAsciiString(const char* cstr) {
  // look for the string first...
  auto ir_string = FindAsciiString(cstr);
  if(ir_string != nullptr) {
    return ir_string;
  }

  // create a new string data
  dex::u4 len = strlen(cstr);
  slicer::Buffer buff;
  buff.PushULeb128(len);
  buff.Push(cstr, len + 1);
  buff.Seal(1);

  // create the new .dex IR string node
  ir_string = dex_ir_->Alloc<String>();
  ir_string->data = slicer::MemView(buff.data(), buff.size());

  // update the index -> ir node map
  auto new_index = dex_ir_->strings_indexes.AllocateIndex();
  auto& ir_node = dex_ir_->strings_map[new_index];
  SLICER_CHECK_EQ(ir_node, nullptr);
  ir_node = ir_string;
  ir_string->orig_index = new_index;

  // attach the new string data to the .dex IR
  dex_ir_->AttachBuffer(std::move(buff));

  // update the strings lookup table
  dex_ir_->strings_lookup.Insert(ir_string);

  return ir_string;
}

Type* Builder::GetType(String* descriptor) {
  // look for an existing type
  for (const auto& ir_type : dex_ir_->types) {
    if (ir_type->descriptor == descriptor) {
      return ir_type.get();
    }
  }

  // create a new type
  auto ir_type = dex_ir_->Alloc<Type>();
  ir_type->descriptor = descriptor;

  // update the index -> ir node map
  auto new_index = dex_ir_->types_indexes.AllocateIndex();
  auto& ir_node = dex_ir_->types_map[new_index];
  SLICER_CHECK_EQ(ir_node, nullptr);
  ir_node = ir_type;
  ir_type->orig_index = new_index;

  return ir_type;
}

TypeList* Builder::GetTypeList(const std::vector<Type*>& types) {
  if (types.empty()) {
    return nullptr;
  }

  // look for an existing TypeList
  for (const auto& ir_type_list : dex_ir_->type_lists) {
    if (ir_type_list->types == types) {
      return ir_type_list.get();
    }
  }

  // create a new TypeList
  auto ir_type_list = dex_ir_->Alloc<TypeList>();
  ir_type_list->types = types;
  return ir_type_list;
}

// Helper for GetProto()
static std::string CreateShorty(Type* return_type, TypeList* param_types) {
  std::stringstream ss;
  ss << dex::DescriptorToShorty(return_type->descriptor->c_str());
  if (param_types != nullptr) {
    for (auto param_type : param_types->types) {
      ss << dex::DescriptorToShorty(param_type->descriptor->c_str());
    }
  }
  return ss.str();
}

Proto* Builder::GetProto(Type* return_type, TypeList* param_types) {
  // create "shorty" descriptor automatically
  auto shorty = GetAsciiString(CreateShorty(return_type, param_types).c_str());

  // look for an existing proto
  for (const auto& ir_proto : dex_ir_->protos) {
    if (ir_proto->shorty == shorty &&
        ir_proto->return_type == return_type &&
        ir_proto->param_types == param_types) {
      return ir_proto.get();
    }
  }

  // create a new proto
  auto ir_proto = dex_ir_->Alloc<Proto>();
  ir_proto->shorty = shorty;
  ir_proto->return_type = return_type;
  ir_proto->param_types = param_types;

  // update the index -> ir node map
  auto new_index = dex_ir_->protos_indexes.AllocateIndex();
  auto& ir_node = dex_ir_->protos_map[new_index];
  SLICER_CHECK_EQ(ir_node, nullptr);
  ir_node = ir_proto;
  ir_proto->orig_index = new_index;

  // update the prototypes lookup table
  dex_ir_->prototypes_lookup.Insert(ir_proto);

  return ir_proto;
}

FieldDecl* Builder::GetFieldDecl(String* name, Type* type, Type* parent) {
  // look for an existing field
  for (const auto& ir_field : dex_ir_->fields) {
    if (ir_field->name == name &&
        ir_field->type == type &&
        ir_field->parent == parent) {
      return ir_field.get();
    }
  }

  // create a new field declaration
  auto ir_field = dex_ir_->Alloc<FieldDecl>();
  ir_field->name = name;
  ir_field->type = type;
  ir_field->parent = parent;

  // update the index -> ir node map
  auto new_index = dex_ir_->fields_indexes.AllocateIndex();
  auto& ir_node = dex_ir_->fields_map[new_index];
  SLICER_CHECK_EQ(ir_node, nullptr);
  ir_node = ir_field;
  ir_field->orig_index = new_index;

  return ir_field;
}

MethodDecl* Builder::GetMethodDecl(String* name, Proto* proto, Type* parent) {
  // look for an existing method
  for (const auto& ir_method : dex_ir_->methods) {
    if (ir_method->name == name &&
        ir_method->prototype == proto &&
        ir_method->parent == parent) {
      return ir_method.get();
    }
  }

  // create a new method declaration
  auto ir_method = dex_ir_->Alloc<MethodDecl>();
  ir_method->name = name;
  ir_method->prototype = proto;
  ir_method->parent = parent;

  // update the index -> ir node map
  auto new_index = dex_ir_->methods_indexes.AllocateIndex();
  auto& ir_node = dex_ir_->methods_map[new_index];
  SLICER_CHECK_EQ(ir_node, nullptr);
  ir_node = ir_method;
  ir_method->orig_index = new_index;

  return ir_method;
}

} // namespace ir

