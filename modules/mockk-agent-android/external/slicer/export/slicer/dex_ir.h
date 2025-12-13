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

#include "arrayview.h"
#include "buffer.h"
#include "common.h"
#include "dex_format.h"
#include "dex_leb128.h"
#include "hash_table.h"
#include "index_map.h"
#include "memview.h"

#include <stdlib.h>
#include <map>
#include <memory>
#include <vector>
#include <string>

// A simple, lightweight IR to abstract the key .dex structures
//
// 1. All the cross-IR references are modeled as plain pointers.
// 2. Newly allocated nodes are mem-zeroed first
//
// This IR can mirror any .dex file, although for JVMTI BCI
// it's expected to construct the IR for the single modified class only
// (and include only the nodes referenced from that class)

#define SLICER_IR_TYPE     \
  using Node::Node; \
  friend struct DexFile;

#define SLICER_IR_INDEXED_TYPE           \
  using IndexedNode::IndexedNode; \
  friend struct DexFile;

namespace ir {

// convenience notation
template <class T>
using own = std::unique_ptr<T>;

struct Node;
struct IndexedNode;
struct EncodedValue;
struct EncodedArray;
struct String;
struct Type;
struct TypeList;
struct Proto;
struct MethodHandle;
struct FieldDecl;
struct EncodedField;
struct DebugInfo;
struct Code;
struct MethodDecl;
struct EncodedMethod;
struct AnnotationElement;
struct Annotation;
struct AnnotationSet;
struct AnnotationSetRefList;
struct FieldAnnotation;
struct MethodAnnotation;
struct ParamAnnotation;
struct AnnotationsDirectory;
struct Class;
struct DexFile;

// The base class for all the .dex IR types:
//   This is not a polymorphic interface, but
//   a way to constrain the allocation and ownership
//   of .dex IR nodes.
struct Node {
  void* operator new(size_t size) {
    return ::calloc(1, size);
  }

  void* operator new[](size_t size) {
    return ::calloc(1, size);
  }

  void operator delete(void* ptr) {
    ::free(ptr);
  }

  void operator delete[](void* ptr) {
    ::free(ptr);
  }

 public:
  Node(const Node&) = delete;
  Node& operator=(const Node&) = delete;

 protected:
  Node() = default;
  ~Node() = default;
};

// a concession for the convenience of the .dex writer
//
// TODO: consider moving the indexing to the writer.
//
struct IndexedNode : public Node {
  SLICER_IR_TYPE;

  // this is the index in the generated image
  // (not the original index)
  dex::u4 index;

  // original indexe
  // (from the source .dex image or allocated post reader)
  dex::u4 orig_index;
};

struct EncodedValue : public Node {
  SLICER_IR_TYPE;

  dex::u1 type;
  union {
    int8_t byte_value;
    int16_t short_value;
    uint16_t char_value;
    int32_t int_value;
    int64_t long_value;
    float float_value;
    double double_value;
    String* string_value;
    Type* type_value;
    FieldDecl* field_value;
    MethodDecl* method_value;
    FieldDecl* enum_value;
    EncodedArray* array_value;
    Annotation* annotation_value;
    bool bool_value;
  } u;

  SLICER_EXTRA(slicer::MemView original);
};

struct EncodedArray : public Node {
  SLICER_IR_TYPE;

  std::vector<EncodedValue*> values;
};

struct String : public IndexedNode {
  SLICER_IR_INDEXED_TYPE;

  // opaque DEX "string_data_item"
  slicer::MemView data;

  const char* c_str() const {
    const dex::u1* strData = data.ptr<dex::u1>();
    dex::ReadULeb128(&strData);
    return reinterpret_cast<const char*>(strData);
  }
};

struct Type : public IndexedNode {
  SLICER_IR_INDEXED_TYPE;

  enum class Category { Void, Scalar, WideScalar, Reference };

  String* descriptor;
  Class* class_def;

  std::string Decl() const;
  Category GetCategory() const;
};

struct TypeList : public Node {
  SLICER_IR_TYPE;

  std::vector<Type*> types;
};

struct Proto : public IndexedNode {
  SLICER_IR_INDEXED_TYPE;

  String* shorty;
  Type* return_type;
  TypeList* param_types;

  std::string Signature() const;
};

struct MethodHandle : public IndexedNode {
  SLICER_IR_INDEXED_TYPE;

  dex::u2 method_handle_type;
  MethodDecl* method;
  FieldDecl* field;

  bool IsField();
};

struct FieldDecl : public IndexedNode {
  SLICER_IR_INDEXED_TYPE;

  String* name;
  Type* type;
  Type* parent;
};

struct EncodedField : public Node {
  SLICER_IR_TYPE;

  FieldDecl* decl;
  dex::u4 access_flags;
};

struct DebugInfo : public Node {
  SLICER_IR_TYPE;

  dex::u4 line_start;
  std::vector<String*> param_names;

  // original debug info opcodes stream
  // (must be "relocated" when creating a new .dex image)
  slicer::MemView data;
};

struct Code : public Node {
  SLICER_IR_TYPE;

  dex::u2 registers;
  dex::u2 ins_count;
  dex::u2 outs_count;
  slicer::ArrayView<const dex::u2> instructions;
  slicer::ArrayView<const dex::TryBlock> try_blocks;
  slicer::MemView catch_handlers;
  DebugInfo* debug_info;
};

struct MethodDecl : public IndexedNode {
  SLICER_IR_INDEXED_TYPE;

  String* name;
  Proto* prototype;
  Type* parent;
};

struct EncodedMethod : public Node {
  SLICER_IR_TYPE;

  MethodDecl* decl;
  Code* code;
  dex::u4 access_flags;
};

struct AnnotationElement : public Node {
  SLICER_IR_TYPE;

  String* name;
  EncodedValue* value;
};

struct Annotation : public Node {
  SLICER_IR_TYPE;

  Type* type;
  std::vector<AnnotationElement*> elements;
  dex::u1 visibility;
};

struct AnnotationSet : public Node {
  SLICER_IR_TYPE;

  std::vector<Annotation*> annotations;
};

struct AnnotationSetRefList : public Node {
  SLICER_IR_TYPE;

  std::vector<AnnotationSet*> annotations;
};

struct FieldAnnotation : public Node {
  SLICER_IR_TYPE;

  FieldDecl* field_decl;
  AnnotationSet* annotations;
};

struct MethodAnnotation : public Node {
  SLICER_IR_TYPE;

  MethodDecl* method_decl;
  AnnotationSet* annotations;
};

struct ParamAnnotation : public Node {
  SLICER_IR_TYPE;

  MethodDecl* method_decl;
  AnnotationSetRefList* annotations;
};

struct AnnotationsDirectory : public Node {
  SLICER_IR_TYPE;

  AnnotationSet* class_annotation;
  std::vector<FieldAnnotation*> field_annotations;
  std::vector<MethodAnnotation*> method_annotations;
  std::vector<ParamAnnotation*> param_annotations;
};

struct Class : public IndexedNode {
  SLICER_IR_INDEXED_TYPE;

  Type* type;
  dex::u4 access_flags;
  Type* super_class;
  TypeList* interfaces;
  String* source_file;
  AnnotationsDirectory* annotations;
  EncodedArray* static_init;

  std::vector<EncodedField*> static_fields;
  std::vector<EncodedField*> instance_fields;
  std::vector<EncodedMethod*> direct_methods;
  std::vector<EncodedMethod*> virtual_methods;
};

// ir::String hashing
struct StringsHasher {
  const char* GetKey(const String* string) const { return string->c_str(); }
  uint32_t Hash(const char* string_key) const;
  bool Compare(const char* string_key, const String* string) const;
};

// ir::Proto hashing
struct ProtosHasher {
  std::string GetKey(const Proto* proto) const { return proto->Signature(); }
  uint32_t Hash(const std::string& proto_key) const;
  bool Compare(const std::string& proto_key, const Proto* proto) const;
};

// ir::EncodedMethod hashing
struct MethodKey {
  String* class_descriptor = nullptr;
  String* method_name = nullptr;
  Proto* prototype = nullptr;
};

struct MethodsHasher {
  MethodKey GetKey(const EncodedMethod* method) const;
  uint32_t Hash(const MethodKey& method_key) const;
  bool Compare(const MethodKey& method_key, const EncodedMethod* method) const;
};

using StringsLookup = slicer::HashTable<const char*, String, StringsHasher>;
using PrototypesLookup = slicer::HashTable<const std::string&, Proto, ProtosHasher>;
using MethodsLookup = slicer::HashTable<const MethodKey&, EncodedMethod, MethodsHasher>;

// The main container/root for a .dex IR
struct DexFile {
  // indexed structures
  std::vector<own<String>> strings;
  std::vector<own<Type>> types;
  std::vector<own<Proto>> protos;
  std::vector<own<FieldDecl>> fields;
  std::vector<own<MethodDecl>> methods;
  std::vector<own<Class>> classes;
  std::vector<own<MethodHandle>> method_handles;

  // data segment structures
  std::vector<own<EncodedField>> encoded_fields;
  std::vector<own<EncodedMethod>> encoded_methods;
  std::vector<own<TypeList>> type_lists;
  std::vector<own<Code>> code;
  std::vector<own<DebugInfo>> debug_info;
  std::vector<own<EncodedValue>> encoded_values;
  std::vector<own<EncodedArray>> encoded_arrays;
  std::vector<own<Annotation>> annotations;
  std::vector<own<AnnotationElement>> annotation_elements;
  std::vector<own<AnnotationSet>> annotation_sets;
  std::vector<own<AnnotationSetRefList>> annotation_set_ref_lists;
  std::vector<own<AnnotationsDirectory>> annotations_directories;
  std::vector<own<FieldAnnotation>> field_annotations;
  std::vector<own<MethodAnnotation>> method_annotations;
  std::vector<own<ParamAnnotation>> param_annotations;

  // original index to IR node mappings
  //
  // CONSIDER: we only need to carry around
  //   the relocation for the referenced items
  //
  std::map<dex::u4, Type*> types_map;
  std::map<dex::u4, String*> strings_map;
  std::map<dex::u4, Proto*> protos_map;
  std::map<dex::u4, FieldDecl*> fields_map;
  std::map<dex::u4, MethodDecl*> methods_map;
  std::map<dex::u4, Class*> classes_map;
  std::map<dex::u4, MethodHandle*> method_handles_map;

  // original .dex header "magic" signature
  slicer::MemView magic;

  // keep track of the used index values
  // (so we can easily allocate new ones)
  IndexMap strings_indexes;
  IndexMap types_indexes;
  IndexMap protos_indexes;
  IndexMap fields_indexes;
  IndexMap methods_indexes;
  IndexMap classes_indexes;
  IndexMap method_handle_indexes;

  // lookup hash tables
  StringsLookup strings_lookup;
  MethodsLookup methods_lookup;
  PrototypesLookup prototypes_lookup;

 public:
  DexFile() = default;

  // No copy/move semantics
  DexFile(const DexFile&) = delete;
  DexFile& operator=(const DexFile&) = delete;

  template <class T>
  T* Alloc() {
    T* p = new T();
    Track(p);
    return p;
  }

  void AttachBuffer(slicer::Buffer&& buffer) {
    buffers_.push_back(std::move(buffer));
  }

  void Normalize();

 private:
  void TopSortClassIndex(Class* irClass, dex::u4* nextIndex);
  void SortClassIndexes();

  template <class T>
  void PushOwn(std::vector<own<T>>& v, T* p) {
    v.push_back(own<T>(p));
  }

  void Track(String* p) { PushOwn(strings, p); }
  void Track(Type* p) { PushOwn(types, p); }
  void Track(Proto* p) { PushOwn(protos, p); }
  void Track(FieldDecl* p) { PushOwn(fields, p); }
  void Track(MethodDecl* p) { PushOwn(methods, p); }
  void Track(Class* p) { PushOwn(classes, p); }
  void Track(MethodHandle* p) { PushOwn(method_handles, p); }

  void Track(EncodedField* p) { PushOwn(encoded_fields, p); }
  void Track(EncodedMethod* p) { PushOwn(encoded_methods, p); }
  void Track(TypeList* p) { PushOwn(type_lists, p); }
  void Track(Code* p) { PushOwn(code, p); }
  void Track(DebugInfo* p) { PushOwn(debug_info, p); }
  void Track(EncodedValue* p) { PushOwn(encoded_values, p); }
  void Track(EncodedArray* p) { PushOwn(encoded_arrays, p); }
  void Track(Annotation* p) { PushOwn(annotations, p); }
  void Track(AnnotationElement* p) { PushOwn(annotation_elements, p); }
  void Track(AnnotationSet* p) { PushOwn(annotation_sets, p); }
  void Track(AnnotationSetRefList* p) { PushOwn(annotation_set_ref_lists, p); }
  void Track(AnnotationsDirectory* p) { PushOwn(annotations_directories, p); }
  void Track(FieldAnnotation* p) { PushOwn(field_annotations, p); }
  void Track(MethodAnnotation* p) { PushOwn(method_annotations, p); }
  void Track(ParamAnnotation* p) { PushOwn(param_annotations, p); }

private:
  // additional memory buffers owned by this .dex IR
  std::vector<slicer::Buffer> buffers_;
};

}  // namespace ir

#undef SLICER_IR_TYPE
#undef SLICER_IR_INDEXED_TYPE
