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

#include "slicer/dex_ir.h"
#include "slicer/chronometer.h"
#include "slicer/dex_utf8.h"
#include "slicer/dex_format.h"

#include <algorithm>
#include <cstdint>
#include <map>
#include <memory>
#include <vector>
#include <sstream>
#include <functional>

namespace ir {

// DBJ2a string hash
static uint32_t HashString(const char* cstr) {
  uint32_t hash = 5381;  // DBJ2 magic prime value
  while (*cstr) {
    hash = ((hash << 5) + hash) ^ *cstr++;
  }
  return hash;
}

uint32_t StringsHasher::Hash(const char* string_key) const {
  return HashString(string_key);
}

bool StringsHasher::Compare(const char* string_key, const String* string) const {
  return dex::Utf8Cmp(string_key, string->c_str()) == 0;
}

uint32_t ProtosHasher::Hash(const std::string& proto_key) const {
  return HashString(proto_key.c_str());
}

bool ProtosHasher::Compare(const std::string& proto_key, const Proto* proto) const {
  return proto_key == proto->Signature();
}

MethodKey MethodsHasher::GetKey(const EncodedMethod* method) const {
  MethodKey method_key;
  method_key.class_descriptor = method->decl->parent->descriptor;
  method_key.method_name = method->decl->name;
  method_key.prototype = method->decl->prototype;
  return method_key;
}

uint32_t MethodsHasher::Hash(const MethodKey& method_key) const {
  return static_cast<uint32_t>(std::hash<void*>{}(method_key.class_descriptor) ^
                               std::hash<void*>{}(method_key.method_name) ^
                               std::hash<void*>{}(method_key.prototype));
}

bool MethodsHasher::Compare(const MethodKey& method_key, const EncodedMethod* method) const {
  return method_key.class_descriptor == method->decl->parent->descriptor &&
         method_key.method_name == method->decl->name &&
         method_key.prototype == method->decl->prototype;
}

// Human-readable type declaration
std::string Type::Decl() const {
  return dex::DescriptorToDecl(descriptor->c_str());
}

Type::Category Type::GetCategory() const {
  switch (*descriptor->c_str()) {
    case 'L':
    case '[':
      return Category::Reference;
    case 'V':
      return Category::Void;
    case 'D':
    case 'J':
      return Category::WideScalar;
    default:
      return Category::Scalar;
  }
}

// Create the corresponding JNI signature:
//  https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/types.html#type_signatures
std::string Proto::Signature() const {
  std::stringstream ss;
  ss << "(";
  if (param_types != nullptr) {
    for (const auto& type : param_types->types) {
      ss << type->descriptor->c_str();
    }
  }
  ss << ")";
  ss << return_type->descriptor->c_str();
  return ss.str();
}

// Helper for IR normalization
// (it sorts items and update the numeric idexes to match)
template <class T, class C>
static void IndexItems(std::vector<T>& items, C comp) {
  std::sort(items.begin(), items.end(), comp);
  for (size_t i = 0; i < items.size(); ++i) {
    items[i]->index = i;
  }
}

// Helper for IR normalization (DFS for topological sort)
//
// NOTE: this recursive version is clean and simple and we know
//  that the max depth is bounded (exactly 1 for JVMTI and a small
//  max for general case - the largest .dex file in AOSP has 5000 classes
//  total)
//
void DexFile::TopSortClassIndex(Class* irClass, dex::u4* nextIndex) {
  if (irClass->index == dex::u4(-1)) {
    if (irClass->super_class && irClass->super_class->class_def) {
      TopSortClassIndex(irClass->super_class->class_def, nextIndex);
    }

    if (irClass->interfaces) {
      for (Type* interfaceType : irClass->interfaces->types) {
        if (interfaceType->class_def) {
          TopSortClassIndex(interfaceType->class_def, nextIndex);
        }
      }
    }

    SLICER_CHECK(*nextIndex < classes.size());
    irClass->index = (*nextIndex)++;
  }
}

// Helper for IR normalization
// (topological sort the classes)
void DexFile::SortClassIndexes() {
  for (auto& irClass : classes) {
    irClass->index = dex::u4(-1);
  }

  dex::u4 nextIndex = 0;
  for (auto& irClass : classes) {
    TopSortClassIndex(irClass.get(), &nextIndex);
  }
}

// Helper for NormalizeClass()
static void SortEncodedFields(std::vector<EncodedField*>* fields) {
  std::sort(fields->begin(), fields->end(),
            [](const EncodedField* a, const EncodedField* b) {
              SLICER_CHECK(a->decl->index != b->decl->index || a == b);
              return a->decl->index < b->decl->index;
            });
}

// Helper for NormalizeClass()
static void SortEncodedMethods(std::vector<EncodedMethod*>* methods) {
  std::sort(methods->begin(), methods->end(),
            [](const EncodedMethod* a, const EncodedMethod* b) {
              SLICER_CHECK(a->decl->index != b->decl->index || a == b);
              return a->decl->index < b->decl->index;
            });
}

// Helper for IR normalization
// (sort the field & method arrays)
static void NormalizeClass(Class* irClass) {
  SortEncodedFields(&irClass->static_fields);
  SortEncodedFields(&irClass->instance_fields);
  SortEncodedMethods(&irClass->direct_methods);
  SortEncodedMethods(&irClass->virtual_methods);
}

// Prepare the IR for generating a .dex image
// (the .dex format requires a specific sort order for some of the arrays, etc...)
//
// TODO: not a great solution - move this logic to the writer!
//
// TODO: the comparison predicate can be better expressed by using std::tie()
//  Ex. FieldDecl has a method comp() returning tie(parent->index, name->index, type->index)
//
void DexFile::Normalize() {
  // sort build the .dex indexes
  IndexItems(strings, [](const own<String>& a, const own<String>& b) {
    // this list must be sorted by std::string contents, using UTF-16 code point values
    // (not in a locale-sensitive manner)
    return dex::Utf8Cmp(a->c_str(), b->c_str()) < 0;
  });

  IndexItems(types, [](const own<Type>& a, const own<Type>& b) {
    // this list must be sorted by string_id index
    return a->descriptor->index < b->descriptor->index;
  });

  IndexItems(protos, [](const own<Proto>& a, const own<Proto>& b) {
    // this list must be sorted in return-type (by type_id index) major order,
    // and then by argument list (lexicographic ordering, individual arguments
    // ordered by type_id index)
    if (a->return_type->index != b->return_type->index) {
      return a->return_type->index < b->return_type->index;
    } else {
      std::vector<Type*> empty;
      const auto& aParamTypes = a->param_types ? a->param_types->types : empty;
      const auto& bParamTypes = b->param_types ? b->param_types->types : empty;
      return std::lexicographical_compare(
          aParamTypes.begin(), aParamTypes.end(), bParamTypes.begin(),
          bParamTypes.end(),
          [](const Type* t1, const Type* t2) { return t1->index < t2->index; });
    }
  });

  IndexItems(fields, [](const own<FieldDecl>& a, const own<FieldDecl>& b) {
    // this list must be sorted, where the defining type (by type_id index) is
    // the major order, field name (by string_id index) is the intermediate
    // order, and type (by type_id index) is the minor order
    return (a->parent->index != b->parent->index)
               ? a->parent->index < b->parent->index
               : (a->name->index != b->name->index)
                     ? a->name->index < b->name->index
                     : a->type->index < b->type->index;
  });

  IndexItems(methods, [](const own<MethodDecl>& a, const own<MethodDecl>& b) {
    // this list must be sorted, where the defining type (by type_id index) is
    // the major order, method name (by string_id index) is the intermediate
    // order, and method prototype (by proto_id index) is the minor order
    return (a->parent->index != b->parent->index)
               ? a->parent->index < b->parent->index
               : (a->name->index != b->name->index)
                     ? a->name->index < b->name->index
                     : a->prototype->index < b->prototype->index;
  });

  // reverse topological sort
  //
  // the classes must be ordered such that a given class's superclass and
  // implemented interfaces appear in the list earlier than the referring
  // class
  //
  // CONSIDER: for the BCI-only scenario we can avoid this
  //
  SortClassIndexes();

  IndexItems(classes, [&](const own<Class>& a, const own<Class>& b) {
    SLICER_CHECK(a->index < classes.size());
    SLICER_CHECK(b->index < classes.size());
    SLICER_CHECK(a->index != b->index || a == b);
    return a->index < b->index;
  });

  // normalize class data
  for (const auto& irClass : classes) {
    NormalizeClass(irClass.get());
  }

  // normalize annotations
  for (const auto& irAnnotation : annotations) {
    // elements must be sorted in increasing order by string_id index
    auto& elements = irAnnotation->elements;
    std::sort(elements.begin(), elements.end(),
              [](const AnnotationElement* a, const AnnotationElement* b) {
                return a->name->index < b->name->index;
              });
  }

  // normalize "annotation_set_item"
  for (const auto& irAnnotationSet : annotation_sets) {
    // The elements must be sorted in increasing order, by type_idx
    auto& annotations = irAnnotationSet->annotations;
    std::sort(annotations.begin(), annotations.end(),
              [](const Annotation* a, const Annotation* b) {
                return a->type->index < b->type->index;
              });
  }

  // normalize "annotations_directory_item"
  for (const auto& irAnnotationDirectory : annotations_directories) {
    // field_annotations: The elements of the list must be
    // sorted in increasing order, by field_idx
    auto& field_annotations = irAnnotationDirectory->field_annotations;
    std::sort(field_annotations.begin(), field_annotations.end(),
              [](const FieldAnnotation* a, const FieldAnnotation* b) {
                return a->field_decl->index < b->field_decl->index;
              });

    // method_annotations: The elements of the list must be
    // sorted in increasing order, by method_idx
    auto& method_annotations = irAnnotationDirectory->method_annotations;
    std::sort(method_annotations.begin(), method_annotations.end(),
              [](const MethodAnnotation* a, const MethodAnnotation* b) {
                return a->method_decl->index < b->method_decl->index;
              });

    // parameter_annotations: The elements of the list must be
    // sorted in increasing order, by method_idx
    auto& param_annotations = irAnnotationDirectory->param_annotations;
    std::sort(param_annotations.begin(), param_annotations.end(),
              [](const ParamAnnotation* a, const ParamAnnotation* b) {
                return a->method_decl->index < b->method_decl->index;
              });
  }
}

} // namespace ir

