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

#include "slicer/reader.h"

#include "slicer/chronometer.h"
#include "slicer/dex_bytecode.h"
#include "slicer/dex_leb128.h"

#include <assert.h>
#include <string.h>
#include <type_traits>
#include <cstdlib>

namespace dex {

Reader::Reader(const dex::u1* image, size_t size) : image_(image), size_(size) {
  // init the header reference
  header_ = ptr<dex::Header>(0);
  ValidateHeader();

  // start with an "empty" .dex IR
  dex_ir_ = std::make_shared<ir::DexFile>();
  dex_ir_->magic = slicer::MemView(header_, sizeof(dex::Header::magic));
}

slicer::ArrayView<const dex::ClassDef> Reader::ClassDefs() const {
  return section<dex::ClassDef>(header_->class_defs_off,
                                header_->class_defs_size);
}

slicer::ArrayView<const dex::StringId> Reader::StringIds() const {
  return section<dex::StringId>(header_->string_ids_off,
                                header_->string_ids_size);
}

slicer::ArrayView<const dex::TypeId> Reader::TypeIds() const {
  return section<dex::TypeId>(header_->type_ids_off,
                              header_->type_ids_size);
}

slicer::ArrayView<const dex::FieldId> Reader::FieldIds() const {
  return section<dex::FieldId>(header_->field_ids_off,
                               header_->field_ids_size);
}

slicer::ArrayView<const dex::MethodId> Reader::MethodIds() const {
  return section<dex::MethodId>(header_->method_ids_off,
                                header_->method_ids_size);
}

slicer::ArrayView<const dex::ProtoId> Reader::ProtoIds() const {
  return section<dex::ProtoId>(header_->proto_ids_off,
                               header_->proto_ids_size);
}

slicer::ArrayView<const dex::MethodHandle> Reader::MethodHandles() const {
  const dex::MapList* ml = DexMapList();
  if(ml == nullptr){
    slicer::ArrayView<const dex::MethodHandle> ret;
    return ret;
  }

  // Find MethodHandle entry
  const dex::MapItem* mi = nullptr;
  for(int i = 0; i < ml->size; i++){
    if(ml->list[i].type == dex::kMethodHandleItem){
      mi = &(ml->list[i]);
      break;
    }
  }

  if(mi == nullptr){
    slicer::ArrayView<const dex::MethodHandle> ret;
    return ret;
  }

  return section<dex::MethodHandle>(mi->offset, mi->size);
}

const dex::MapList* Reader::DexMapList() const {
  return dataPtr<dex::MapList>(header_->map_off);
}

const char* Reader::GetStringMUTF8(dex::u4 index) const {
  if (index == dex::kNoIndex) {
    return "<no_string>";
  }
  const dex::u1* strData = GetStringData(index);
  dex::ReadULeb128(&strData);
  return reinterpret_cast<const char*>(strData);
}

void Reader::CreateFullIr() {
  size_t classCount = ClassDefs().size();
  for (size_t i = 0; i < classCount; ++i) {
    CreateClassIr(i);
  }
}

void Reader::CreateClassIr(dex::u4 index) {
  auto ir_class = GetClass(index);
  SLICER_CHECK_NE(ir_class, nullptr);
}

// Returns the index of the class with the specified
// descriptor, or kNoIndex if not found
dex::u4 Reader::FindClassIndex(const char* class_descriptor) const {
  auto classes = ClassDefs();
  auto types = TypeIds();
  for (dex::u4 i = 0; i < classes.size(); ++i) {
    auto typeId = types[classes[i].class_idx];
    const char* descriptor = GetStringMUTF8(typeId.descriptor_idx);
    if (strcmp(class_descriptor, descriptor) == 0) {
      return i;
    }
  }
  return dex::kNoIndex;
}

// map a .dex index to corresponding .dex IR node
//
// NOTES:
//  1. the mapping between an index and the indexed
//     .dex IR nodes is 1:1
//  2. we do a single index lookup for both existing
//     nodes as well as new nodes
//  3. placeholder is an invalid, but non-null pointer value
//     used to check that the mapping lookup/update is atomic
//  4. there should be no recursion with the same index
//     (we use the placeholder value to guard against this too)
//
ir::Class* Reader::GetClass(dex::u4 index) {
  SLICER_CHECK_NE(index, dex::kNoIndex);
  auto& p = dex_ir_->classes_map[index];
  auto placeholder = reinterpret_cast<ir::Class*>(1);
  if (p == nullptr) {
    p = placeholder;
    auto newClass = ParseClass(index);
    SLICER_CHECK_EQ(p, placeholder);
    p = newClass;
    dex_ir_->classes_indexes.MarkUsedIndex(index);
  }
  SLICER_CHECK_NE(p, placeholder);
  return p;
}

// map a .dex index to corresponding .dex IR node
// (see the Reader::GetClass() comments)
ir::Type* Reader::GetType(dex::u4 index) {
  SLICER_CHECK_NE(index, dex::kNoIndex);
  auto& p = dex_ir_->types_map[index];
  auto placeholder = reinterpret_cast<ir::Type*>(1);
  if (p == nullptr) {
    p = placeholder;
    auto newType = ParseType(index);
    SLICER_CHECK_EQ(p, placeholder);
    p = newType;
    dex_ir_->types_indexes.MarkUsedIndex(index);
  }
  SLICER_CHECK_NE(p, placeholder);
  return p;
}

// map a .dex index to corresponding .dex IR node
// (see the Reader::GetClass() comments)
ir::FieldDecl* Reader::GetFieldDecl(dex::u4 index) {
  SLICER_CHECK_NE(index, dex::kNoIndex);
  auto& p = dex_ir_->fields_map[index];
  auto placeholder = reinterpret_cast<ir::FieldDecl*>(1);
  if (p == nullptr) {
    p = placeholder;
    auto newField = ParseFieldDecl(index);
    SLICER_CHECK_EQ(p, placeholder);
    p = newField;
    dex_ir_->fields_indexes.MarkUsedIndex(index);
  }
  SLICER_CHECK_NE(p, placeholder);
  return p;
}

ir::MethodHandle* Reader::GetMethodHandle(dex::u4 index){
  SLICER_CHECK_NE(index, dex::kNoIndex);
  auto& p = dex_ir_->method_handles_map[index];
  auto placeholder = reinterpret_cast<ir::MethodHandle*>(1);
  if(p == nullptr) {
    p = placeholder;
    auto newMethodHandle = ParseMethodHandle(index);
    SLICER_CHECK_EQ(p, placeholder);
    p = newMethodHandle;
    dex_ir_->method_handle_indexes.MarkUsedIndex(index);
  }

  SLICER_CHECK_NE(p, placeholder);
  return p;
}

// map a .dex index to corresponding .dex IR node
// (see the Reader::GetClass() comments)
ir::MethodDecl* Reader::GetMethodDecl(dex::u4 index) {
  SLICER_CHECK_NE(index, dex::kNoIndex);
  auto& p = dex_ir_->methods_map[index];
  auto placeholder = reinterpret_cast<ir::MethodDecl*>(1);
  if (p == nullptr) {
    p = placeholder;
    auto newMethod = ParseMethodDecl(index);
    SLICER_CHECK_EQ(p, placeholder);
    p = newMethod;
    dex_ir_->methods_indexes.MarkUsedIndex(index);
  }
  SLICER_CHECK_NE(p, placeholder);
  return p;
}

// map a .dex index to corresponding .dex IR node
// (see the Reader::GetClass() comments)
ir::Proto* Reader::GetProto(dex::u4 index) {
  SLICER_CHECK_NE(index, dex::kNoIndex);
  auto& p = dex_ir_->protos_map[index];
  auto placeholder = reinterpret_cast<ir::Proto*>(1);
  if (p == nullptr) {
    p = placeholder;
    auto newProto = ParseProto(index);
    SLICER_CHECK_EQ(p, placeholder);
    p = newProto;
    dex_ir_->protos_indexes.MarkUsedIndex(index);
  }
  SLICER_CHECK_NE(p, placeholder);
  return p;
}

// map a .dex index to corresponding .dex IR node
// (see the Reader::GetClass() comments)
ir::String* Reader::GetString(dex::u4 index) {
  SLICER_CHECK_NE(index, dex::kNoIndex);
  auto& p = dex_ir_->strings_map[index];
  auto placeholder = reinterpret_cast<ir::String*>(1);
  if (p == nullptr) {
    p = placeholder;
    auto newString = ParseString(index);
    SLICER_CHECK_EQ(p, placeholder);
    p = newString;
    dex_ir_->strings_indexes.MarkUsedIndex(index);
  }
  SLICER_CHECK_NE(p, placeholder);
  return p;
}

ir::Class* Reader::ParseClass(dex::u4 index) {
  auto& dex_class_def = ClassDefs()[index];
  auto ir_class = dex_ir_->Alloc<ir::Class>();

  ir_class->type = GetType(dex_class_def.class_idx);
  assert(ir_class->type->class_def == nullptr);
  ir_class->type->class_def = ir_class;

  ir_class->access_flags = dex_class_def.access_flags;
  ir_class->interfaces = ExtractTypeList(dex_class_def.interfaces_off);

  if (dex_class_def.superclass_idx != dex::kNoIndex) {
    ir_class->super_class = GetType(dex_class_def.superclass_idx);
  }

  if (dex_class_def.source_file_idx != dex::kNoIndex) {
    ir_class->source_file = GetString(dex_class_def.source_file_idx);
  }

  if (dex_class_def.class_data_off != 0) {
    const dex::u1* class_data = dataPtr<dex::u1>(dex_class_def.class_data_off);

    dex::u4 static_fields_count = dex::ReadULeb128(&class_data);
    dex::u4 instance_fields_count = dex::ReadULeb128(&class_data);
    dex::u4 direct_methods_count = dex::ReadULeb128(&class_data);
    dex::u4 virtual_methods_count = dex::ReadULeb128(&class_data);

    dex::u4 base_index = dex::kNoIndex;
    for (dex::u4 i = 0; i < static_fields_count; ++i) {
      auto field = ParseEncodedField(&class_data, &base_index);
      ir_class->static_fields.push_back(field);
    }

    base_index = dex::kNoIndex;
    for (dex::u4 i = 0; i < instance_fields_count; ++i) {
      auto field = ParseEncodedField(&class_data, &base_index);
      ir_class->instance_fields.push_back(field);
    }

    base_index = dex::kNoIndex;
    for (dex::u4 i = 0; i < direct_methods_count; ++i) {
      auto method = ParseEncodedMethod(&class_data, &base_index);
      ir_class->direct_methods.push_back(method);
    }

    base_index = dex::kNoIndex;
    for (dex::u4 i = 0; i < virtual_methods_count; ++i) {
      auto method = ParseEncodedMethod(&class_data, &base_index);
      ir_class->virtual_methods.push_back(method);
    }
  }

  ir_class->static_init = ExtractEncodedArray(dex_class_def.static_values_off);
  ir_class->annotations = ExtractAnnotations(dex_class_def.annotations_off);
  ir_class->orig_index = index;

  return ir_class;
}

ir::AnnotationsDirectory* Reader::ExtractAnnotations(dex::u4 offset) {
  if (offset == 0) {
    return nullptr;
  }

  SLICER_CHECK_EQ(offset % 4, 0);

  // first check if we already extracted the same "annotations_directory_item"
  auto& ir_annotations = annotations_directories_[offset];
  if (ir_annotations == nullptr) {
    ir_annotations = dex_ir_->Alloc<ir::AnnotationsDirectory>();

    auto dex_annotations = dataPtr<dex::AnnotationsDirectoryItem>(offset);

    ir_annotations->class_annotation =
        ExtractAnnotationSet(dex_annotations->class_annotations_off);

    const dex::u1* ptr = reinterpret_cast<const dex::u1*>(dex_annotations + 1);

    for (dex::u4 i = 0; i < dex_annotations->fields_size; ++i) {
      ir_annotations->field_annotations.push_back(ParseFieldAnnotation(&ptr));
    }

    for (dex::u4 i = 0; i < dex_annotations->methods_size; ++i) {
      ir_annotations->method_annotations.push_back(ParseMethodAnnotation(&ptr));
    }

    for (dex::u4 i = 0; i < dex_annotations->parameters_size; ++i) {
      ir_annotations->param_annotations.push_back(ParseParamAnnotation(&ptr));
    }
  }
  return ir_annotations;
}

ir::Annotation* Reader::ExtractAnnotationItem(dex::u4 offset) {
  SLICER_CHECK_NE(offset, 0);

  // first check if we already extracted the same "annotation_item"
  auto& ir_annotation = annotations_[offset];
  if (ir_annotation == nullptr) {
    auto dexAnnotationItem = dataPtr<dex::AnnotationItem>(offset);
    const dex::u1* ptr = dexAnnotationItem->annotation;
    ir_annotation = ParseAnnotation(&ptr);
    ir_annotation->visibility = dexAnnotationItem->visibility;
  }
  return ir_annotation;
}

ir::AnnotationSet* Reader::ExtractAnnotationSet(dex::u4 offset) {
  if (offset == 0) {
    return nullptr;
  }

  SLICER_CHECK_EQ(offset % 4, 0);

  // first check if we already extracted the same "annotation_set_item"
  auto& ir_annotation_set = annotation_sets_[offset];
  if (ir_annotation_set == nullptr) {
    ir_annotation_set = dex_ir_->Alloc<ir::AnnotationSet>();

    auto dex_annotation_set = dataPtr<dex::AnnotationSetItem>(offset);
    for (dex::u4 i = 0; i < dex_annotation_set->size; ++i) {
      auto ir_annotation = ExtractAnnotationItem(dex_annotation_set->entries[i]);
      assert(ir_annotation != nullptr);
      ir_annotation_set->annotations.push_back(ir_annotation);
    }
  }
  return ir_annotation_set;
}

ir::AnnotationSetRefList* Reader::ExtractAnnotationSetRefList(dex::u4 offset) {
  SLICER_CHECK_EQ(offset % 4, 0);

  auto dex_annotation_set_ref_list = dataPtr<dex::AnnotationSetRefList>(offset);
  auto ir_annotation_set_ref_list = dex_ir_->Alloc<ir::AnnotationSetRefList>();

  for (dex::u4 i = 0; i < dex_annotation_set_ref_list->size; ++i) {
    dex::u4 entry_offset = dex_annotation_set_ref_list->list[i].annotations_off;
    if (entry_offset != 0) {
      auto ir_annotation_set = ExtractAnnotationSet(entry_offset);
      SLICER_CHECK_NE(ir_annotation_set, nullptr);
      ir_annotation_set_ref_list->annotations.push_back(ir_annotation_set);
    }
  }

  return ir_annotation_set_ref_list;
}

ir::FieldAnnotation* Reader::ParseFieldAnnotation(const dex::u1** pptr) {
  auto dex_field_annotation = reinterpret_cast<const dex::FieldAnnotationsItem*>(*pptr);
  auto ir_field_annotation = dex_ir_->Alloc<ir::FieldAnnotation>();

  ir_field_annotation->field_decl = GetFieldDecl(dex_field_annotation->field_idx);

  ir_field_annotation->annotations =
      ExtractAnnotationSet(dex_field_annotation->annotations_off);
  SLICER_CHECK_NE(ir_field_annotation->annotations, nullptr);

  *pptr += sizeof(dex::FieldAnnotationsItem);
  return ir_field_annotation;
}

ir::MethodAnnotation* Reader::ParseMethodAnnotation(const dex::u1** pptr) {
  auto dex_method_annotation =
      reinterpret_cast<const dex::MethodAnnotationsItem*>(*pptr);
  auto ir_method_annotation = dex_ir_->Alloc<ir::MethodAnnotation>();

  ir_method_annotation->method_decl = GetMethodDecl(dex_method_annotation->method_idx);

  ir_method_annotation->annotations =
      ExtractAnnotationSet(dex_method_annotation->annotations_off);
  SLICER_CHECK_NE(ir_method_annotation->annotations, nullptr);

  *pptr += sizeof(dex::MethodAnnotationsItem);
  return ir_method_annotation;
}

ir::ParamAnnotation* Reader::ParseParamAnnotation(const dex::u1** pptr) {
  auto dex_param_annotation =
      reinterpret_cast<const dex::ParameterAnnotationsItem*>(*pptr);
  auto ir_param_annotation = dex_ir_->Alloc<ir::ParamAnnotation>();

  ir_param_annotation->method_decl = GetMethodDecl(dex_param_annotation->method_idx);

  ir_param_annotation->annotations =
      ExtractAnnotationSetRefList(dex_param_annotation->annotations_off);
  SLICER_CHECK_NE(ir_param_annotation->annotations, nullptr);

  *pptr += sizeof(dex::ParameterAnnotationsItem);
  return ir_param_annotation;
}

ir::EncodedField* Reader::ParseEncodedField(const dex::u1** pptr, dex::u4* base_index) {
  auto ir_encoded_field = dex_ir_->Alloc<ir::EncodedField>();

  auto field_index = dex::ReadULeb128(pptr);
  SLICER_CHECK_NE(field_index, dex::kNoIndex);
  if (*base_index != dex::kNoIndex) {
    SLICER_CHECK_NE(field_index, 0);
    field_index += *base_index;
  }
  *base_index = field_index;

  ir_encoded_field->decl = GetFieldDecl(field_index);
  ir_encoded_field->access_flags = dex::ReadULeb128(pptr);

  return ir_encoded_field;
}

// Parse an encoded variable-length integer value
// (sign-extend signed types, zero-extend unsigned types)
template <class T>
static T ParseIntValue(const dex::u1** pptr, size_t size) {
  static_assert(std::is_integral<T>::value, "must be an integral type");

  SLICER_CHECK_GT(size, 0);
  SLICER_CHECK_LE(size, sizeof(T));

  T value = 0;
  for (int i = 0; i < size; ++i) {
    value |= T(*(*pptr)++) << (i * 8);
  }

  // sign-extend?
  if (std::is_signed<T>::value) {
    size_t shift = (sizeof(T) - size) * 8;
    value = T(value << shift) >> shift;
  }

  return value;
}

// Parse an encoded variable-length floating point value
// (zero-extend to the right)
template <class T>
static T ParseFloatValue(const dex::u1** pptr, size_t size) {
  SLICER_CHECK_GT(size, 0);
  SLICER_CHECK_LE(size, sizeof(T));

  T value = 0;
  int start_byte = sizeof(T) - size;
  for (dex::u1* p = reinterpret_cast<dex::u1*>(&value) + start_byte; size > 0;
       --size) {
    *p++ = *(*pptr)++;
  }
  return value;
}

ir::EncodedValue* Reader::ParseEncodedValue(const dex::u1** pptr) {
  auto ir_encoded_value = dex_ir_->Alloc<ir::EncodedValue>();

  SLICER_EXTRA(auto base_ptr = *pptr);

  dex::u1 header = *(*pptr)++;
  dex::u1 type = header & dex::kEncodedValueTypeMask;
  dex::u1 arg = header >> dex::kEncodedValueArgShift;

  ir_encoded_value->type = type;

  switch (type) {
    case dex::kEncodedByte:
      ir_encoded_value->u.byte_value = ParseIntValue<int8_t>(pptr, arg + 1);
      break;

    case dex::kEncodedShort:
      ir_encoded_value->u.short_value = ParseIntValue<int16_t>(pptr, arg + 1);
      break;

    case dex::kEncodedChar:
      ir_encoded_value->u.char_value = ParseIntValue<uint16_t>(pptr, arg + 1);
      break;

    case dex::kEncodedInt:
      ir_encoded_value->u.int_value = ParseIntValue<int32_t>(pptr, arg + 1);
      break;

    case dex::kEncodedLong:
      ir_encoded_value->u.long_value = ParseIntValue<int64_t>(pptr, arg + 1);
      break;

    case dex::kEncodedFloat:
      ir_encoded_value->u.float_value = ParseFloatValue<float>(pptr, arg + 1);
      break;

    case dex::kEncodedDouble:
      ir_encoded_value->u.double_value = ParseFloatValue<double>(pptr, arg + 1);
      break;

    case dex::kEncodedString: {
      dex::u4 index = ParseIntValue<dex::u4>(pptr, arg + 1);
      ir_encoded_value->u.string_value = GetString(index);
    } break;

    case dex::kEncodedType: {
      dex::u4 index = ParseIntValue<dex::u4>(pptr, arg + 1);
      ir_encoded_value->u.type_value = GetType(index);
    } break;

    case dex::kEncodedField: {
      dex::u4 index = ParseIntValue<dex::u4>(pptr, arg + 1);
      ir_encoded_value->u.field_value = GetFieldDecl(index);
    } break;

    case dex::kEncodedMethod: {
      dex::u4 index = ParseIntValue<dex::u4>(pptr, arg + 1);
      ir_encoded_value->u.method_value = GetMethodDecl(index);
    } break;

    case dex::kEncodedEnum: {
      dex::u4 index = ParseIntValue<dex::u4>(pptr, arg + 1);
      ir_encoded_value->u.enum_value = GetFieldDecl(index);
    } break;

    case dex::kEncodedArray:
      SLICER_CHECK_EQ(arg, 0);
      ir_encoded_value->u.array_value = ParseEncodedArray(pptr);
      break;

    case dex::kEncodedAnnotation:
      SLICER_CHECK_EQ(arg, 0);
      ir_encoded_value->u.annotation_value = ParseAnnotation(pptr);
      break;

    case dex::kEncodedNull:
      SLICER_CHECK_EQ(arg, 0);
      break;

    case dex::kEncodedBoolean:
      SLICER_CHECK_LT(arg, 2);
      ir_encoded_value->u.bool_value = (arg == 1);
      break;

    default:
      SLICER_CHECK(!"unexpected value type");
  }

  SLICER_EXTRA(ir_encoded_value->original = slicer::MemView(base_ptr, *pptr - base_ptr));

  return ir_encoded_value;
}

ir::Annotation* Reader::ParseAnnotation(const dex::u1** pptr) {
  auto ir_annotation = dex_ir_->Alloc<ir::Annotation>();

  dex::u4 type_index = dex::ReadULeb128(pptr);
  dex::u4 elements_count = dex::ReadULeb128(pptr);

  ir_annotation->type = GetType(type_index);
  ir_annotation->visibility = dex::kVisibilityEncoded;

  for (dex::u4 i = 0; i < elements_count; ++i) {
    auto ir_element = dex_ir_->Alloc<ir::AnnotationElement>();

    ir_element->name = GetString(dex::ReadULeb128(pptr));
    ir_element->value = ParseEncodedValue(pptr);

    ir_annotation->elements.push_back(ir_element);
  }

  return ir_annotation;
}

ir::EncodedArray* Reader::ParseEncodedArray(const dex::u1** pptr) {
  auto ir_encoded_array = dex_ir_->Alloc<ir::EncodedArray>();

  dex::u4 count = dex::ReadULeb128(pptr);
  for (dex::u4 i = 0; i < count; ++i) {
    ir_encoded_array->values.push_back(ParseEncodedValue(pptr));
  }

  return ir_encoded_array;
}

ir::EncodedArray* Reader::ExtractEncodedArray(dex::u4 offset) {
  if (offset == 0) {
    return nullptr;
  }

  // first check if we already extracted the same "annotation_item"
  auto& ir_encoded_array = encoded_arrays_[offset];
  if (ir_encoded_array == nullptr) {
    auto ptr = dataPtr<dex::u1>(offset);
    ir_encoded_array = ParseEncodedArray(&ptr);
  }
  return ir_encoded_array;
}

ir::DebugInfo* Reader::ExtractDebugInfo(dex::u4 offset) {
  if (offset == 0) {
    return nullptr;
  }

  auto ir_debug_info = dex_ir_->Alloc<ir::DebugInfo>();
  const dex::u1* ptr = dataPtr<dex::u1>(offset);

  ir_debug_info->line_start = dex::ReadULeb128(&ptr);

  // TODO: implicit this param for non-static methods?
  dex::u4 param_count = dex::ReadULeb128(&ptr);
  for (dex::u4 i = 0; i < param_count; ++i) {
    dex::u4 name_index = dex::ReadULeb128(&ptr) - 1;
    auto ir_string =
        (name_index == dex::kNoIndex) ? nullptr : GetString(name_index);
    ir_debug_info->param_names.push_back(ir_string);
  }

  // parse the debug info opcodes and note the
  // references to strings and types (to make sure the IR
  // is the full closure of all referenced items)
  //
  // TODO: design a generic debug info iterator?
  //
  auto base_ptr = ptr;
  dex::u1 opcode = 0;
  while ((opcode = *ptr++) != dex::DBG_END_SEQUENCE) {
    switch (opcode) {
      case dex::DBG_ADVANCE_PC:
        // addr_diff
        dex::ReadULeb128(&ptr);
        break;

      case dex::DBG_ADVANCE_LINE:
        // line_diff
        dex::ReadSLeb128(&ptr);
        break;

      case dex::DBG_START_LOCAL: {
        // register_num
        dex::ReadULeb128(&ptr);

        dex::u4 name_index = dex::ReadULeb128(&ptr) - 1;
        if (name_index != dex::kNoIndex) {
          GetString(name_index);
        }

        dex::u4 type_index = dex::ReadULeb128(&ptr) - 1;
        if (type_index != dex::kNoIndex) {
          GetType(type_index);
        }
      } break;

      case dex::DBG_START_LOCAL_EXTENDED: {
        // register_num
        dex::ReadULeb128(&ptr);

        dex::u4 name_index = dex::ReadULeb128(&ptr) - 1;
        if (name_index != dex::kNoIndex) {
          GetString(name_index);
        }

        dex::u4 type_index = dex::ReadULeb128(&ptr) - 1;
        if (type_index != dex::kNoIndex) {
          GetType(type_index);
        }

        dex::u4 sig_index = dex::ReadULeb128(&ptr) - 1;
        if (sig_index != dex::kNoIndex) {
          GetString(sig_index);
        }
      } break;

      case dex::DBG_END_LOCAL:
      case dex::DBG_RESTART_LOCAL:
        // register_num
        dex::ReadULeb128(&ptr);
        break;

      case dex::DBG_SET_FILE: {
        dex::u4 name_index = dex::ReadULeb128(&ptr) - 1;
        if (name_index != dex::kNoIndex) {
          GetString(name_index);
        }
      } break;
    }
  }

  ir_debug_info->data = slicer::MemView(base_ptr, ptr - base_ptr);

  return ir_debug_info;
}

ir::Code* Reader::ExtractCode(dex::u4 offset) {
  if (offset == 0) {
    return nullptr;
  }

  SLICER_CHECK_EQ(offset % 4, 0);

  auto dex_code = dataPtr<dex::Code>(offset);
  auto ir_code = dex_ir_->Alloc<ir::Code>();

  ir_code->registers = dex_code->registers_size;
  ir_code->ins_count = dex_code->ins_size;
  ir_code->outs_count = dex_code->outs_size;

  // instructions array
  ir_code->instructions =
      slicer::ArrayView<const dex::u2>(dex_code->insns, dex_code->insns_size);

  // parse the instructions to discover references to other
  // IR nodes (see debug info stream parsing too)
  ParseInstructions(ir_code->instructions);

  // try blocks & handlers
  //
  // TODO: a generic try/catch blocks iterator?
  //
  if (dex_code->tries_size != 0) {
    dex::u4 aligned_count = (dex_code->insns_size + 1) / 2 * 2;
    auto tries =
        reinterpret_cast<const dex::TryBlock*>(dex_code->insns + aligned_count);
    auto handlers_list =
        reinterpret_cast<const dex::u1*>(tries + dex_code->tries_size);

    ir_code->try_blocks =
        slicer::ArrayView<const dex::TryBlock>(tries, dex_code->tries_size);

    // parse the handlers list (and discover embedded references)
    auto ptr = handlers_list;

    dex::u4 handlers_count = dex::ReadULeb128(&ptr);
    SLICER_WEAK_CHECK(handlers_count <= dex_code->tries_size);

    for (dex::u4 handler_index = 0; handler_index < handlers_count; ++handler_index) {
      int catch_count = dex::ReadSLeb128(&ptr);

      for (int catch_index = 0; catch_index < std::abs(catch_count); ++catch_index) {
        dex::u4 type_index = dex::ReadULeb128(&ptr);
        GetType(type_index);

        // address
        dex::ReadULeb128(&ptr);
      }

      if (catch_count < 1) {
        // catch_all_addr
        dex::ReadULeb128(&ptr);
      }
    }

    ir_code->catch_handlers = slicer::MemView(handlers_list, ptr - handlers_list);
  }

  ir_code->debug_info = ExtractDebugInfo(dex_code->debug_info_off);

  return ir_code;
}

ir::EncodedMethod* Reader::ParseEncodedMethod(const dex::u1** pptr, dex::u4* base_index) {
  auto ir_encoded_method = dex_ir_->Alloc<ir::EncodedMethod>();

  auto method_index = dex::ReadULeb128(pptr);
  SLICER_CHECK_NE(method_index, dex::kNoIndex);
  if (*base_index != dex::kNoIndex) {
    SLICER_CHECK_NE(method_index, 0);
    method_index += *base_index;
  }
  *base_index = method_index;

  ir_encoded_method->decl = GetMethodDecl(method_index);
  ir_encoded_method->access_flags = dex::ReadULeb128(pptr);

  dex::u4 code_offset = dex::ReadULeb128(pptr);
  ir_encoded_method->code = ExtractCode(code_offset);

  // update the methods lookup table
  dex_ir_->methods_lookup.Insert(ir_encoded_method);

  return ir_encoded_method;
}

ir::Type* Reader::ParseType(dex::u4 index) {
  auto& dex_type = TypeIds()[index];
  auto ir_type = dex_ir_->Alloc<ir::Type>();

  ir_type->descriptor = GetString(dex_type.descriptor_idx);
  ir_type->orig_index = index;

  return ir_type;
}

ir::FieldDecl* Reader::ParseFieldDecl(dex::u4 index) {
  auto& dex_field = FieldIds()[index];
  auto ir_field = dex_ir_->Alloc<ir::FieldDecl>();

  ir_field->name = GetString(dex_field.name_idx);
  ir_field->type = GetType(dex_field.type_idx);
  ir_field->parent = GetType(dex_field.class_idx);
  ir_field->orig_index = index;

  return ir_field;
}

ir::MethodHandle* Reader::ParseMethodHandle(dex::u4 index){
  auto& dex_method_handle = MethodHandles()[index];
  auto ir_method_handle = dex_ir_->Alloc<ir::MethodHandle>();

  ir_method_handle->method_handle_type = dex_method_handle.method_handle_type;

  if(ir_method_handle->IsField()){
    ir_method_handle->field = GetFieldDecl(dex_method_handle.field_or_method_id);
  }
  else {
    ir_method_handle->method = GetMethodDecl(dex_method_handle.field_or_method_id);
  }

  return ir_method_handle;
}

ir::MethodDecl* Reader::ParseMethodDecl(dex::u4 index) {
  auto& dex_method = MethodIds()[index];
  auto ir_method = dex_ir_->Alloc<ir::MethodDecl>();

  ir_method->name = GetString(dex_method.name_idx);
  ir_method->prototype = GetProto(dex_method.proto_idx);
  ir_method->parent = GetType(dex_method.class_idx);
  ir_method->orig_index = index;

  return ir_method;
}

ir::TypeList* Reader::ExtractTypeList(dex::u4 offset) {
  if (offset == 0) {
    return nullptr;
  }

  // first check to see if we already extracted the same "type_list"
  auto& ir_type_list = type_lists_[offset];
  if (ir_type_list == nullptr) {
    ir_type_list = dex_ir_->Alloc<ir::TypeList>();

    auto dex_type_list = dataPtr<dex::TypeList>(offset);
    SLICER_WEAK_CHECK(dex_type_list->size > 0);

    for (dex::u4 i = 0; i < dex_type_list->size; ++i) {
      ir_type_list->types.push_back(GetType(dex_type_list->list[i].type_idx));
    }
  }

  return ir_type_list;
}

ir::Proto* Reader::ParseProto(dex::u4 index) {
  auto& dex_proto = ProtoIds()[index];
  auto ir_proto = dex_ir_->Alloc<ir::Proto>();

  ir_proto->shorty = GetString(dex_proto.shorty_idx);
  ir_proto->return_type = GetType(dex_proto.return_type_idx);
  ir_proto->param_types = ExtractTypeList(dex_proto.parameters_off);
  ir_proto->orig_index = index;

  // update the prototypes lookup table
  dex_ir_->prototypes_lookup.Insert(ir_proto);

  return ir_proto;
}

ir::String* Reader::ParseString(dex::u4 index) {
  auto ir_string = dex_ir_->Alloc<ir::String>();

  auto data = GetStringData(index);
  auto cstr = data;
  dex::ReadULeb128(&cstr);
  size_t size = (cstr - data) + ::strlen(reinterpret_cast<const char*>(cstr)) + 1;

  ir_string->data = slicer::MemView(data, size);
  ir_string->orig_index = index;

  // update the strings lookup table
  dex_ir_->strings_lookup.Insert(ir_string);

  return ir_string;
}

void Reader::ParseInstructions(slicer::ArrayView<const dex::u2> code) {
  const dex::u2* ptr = code.begin();
  while (ptr < code.end()) {
    auto dex_instr = dex::DecodeInstruction(ptr);

    dex::u4 index = dex::kNoIndex;
    dex::u4 index2 = dex::kNoIndex;
    switch (dex::GetFormatFromOpcode(dex_instr.opcode)) {
      case dex::k20bc:
      case dex::k21c:
      case dex::k31c:
      case dex::k35c:
      case dex::k3rc:
        index = dex_instr.vB;
        break;

      case dex::k45cc:
      case dex::k4rcc:
        index = dex_instr.vB;
        index2 = dex_instr.arg[4];
        break;

      case dex::k22c:
        index = dex_instr.vC;
        break;

      default:
        break;
    }

    switch (GetIndexTypeFromOpcode(dex_instr.opcode)) {
      case dex::kIndexStringRef:
        GetString(index);
        break;

      case dex::kIndexTypeRef:
        GetType(index);
        break;

      case dex::kIndexFieldRef:
        GetFieldDecl(index);
        break;

      case dex::kIndexMethodRef:
        GetMethodDecl(index);
        break;

      case dex::kIndexMethodAndProtoRef:
        GetMethodDecl(index);
        GetProto(index2);
        break;

      case dex::kIndexMethodHandleRef:
        GetMethodHandle(index);
        break;

      default:
        break;
    }

    auto isize = dex::GetWidthFromBytecode(ptr);
    SLICER_CHECK_GT(isize, 0);
    ptr += isize;
  }
  SLICER_CHECK_EQ(ptr, code.end());
}

// Basic .dex header structural checks
void Reader::ValidateHeader() {
  SLICER_CHECK_GT(size_, dex::Header::kV40Size);

  // Known issue: For performance reasons the initial size_ passed to jvmti events might be an
  // estimate. b/72402467
  SLICER_CHECK_LE(header_->file_size, size_);
  // Check that we support this version of dex header
  SLICER_CHECK(
      header_->header_size == dex::Header::kV40Size ||
      header_->header_size == dex::Header::kV41Size);
  SLICER_CHECK_EQ(header_->endian_tag, dex::kEndianConstant);
  SLICER_CHECK_EQ(header_->data_size % 4, 0);

  // If the dex file is within container with other dex files,
  // adjust the base address to the start of the container.
  SLICER_CHECK_LE(header_->ContainerSize() - header_->ContainerOff(), size_);
  image_ -= header_->ContainerOff();
  size_ = header_->ContainerSize();

  // Known issue: The fields might be slighly corrupted b/65452964
  // SLICER_CHECK_LE(header_->data_off + header_->data_size, size_);

  SLICER_CHECK_EQ(header_->string_ids_off % 4, 0);
  SLICER_CHECK_LT(header_->type_ids_size, 65536);
  SLICER_CHECK_EQ(header_->type_ids_off % 4, 0);
  SLICER_CHECK_LT(header_->proto_ids_size, 65536);
  SLICER_CHECK_EQ(header_->proto_ids_off % 4, 0);
  SLICER_CHECK_EQ(header_->field_ids_off % 4, 0);
  SLICER_CHECK_EQ(header_->method_ids_off % 4, 0);
  SLICER_CHECK_EQ(header_->class_defs_off % 4, 0);
  SLICER_CHECK_GE(header_->map_off, header_->data_off);
  SLICER_CHECK_LT(header_->map_off, size_);
  SLICER_CHECK_EQ(header_->link_size, 0);
  SLICER_CHECK_EQ(header_->link_off, 0);
  SLICER_CHECK_EQ(header_->data_off % 4, 0);
  SLICER_CHECK_EQ(header_->map_off % 4, 0);

  // we seem to have .dex files with extra bytes at the end ...
  // Known issue: For performance reasons the initial size_ passed to jvmti events might be an
  // estimate. b/72402467
  SLICER_WEAK_CHECK(header_->data_off + header_->data_size <= size_);

  // but we should still have the whole data section

  // Known issue: The fields might be slightly corrupted b/65452964
  // Known issue: For performance reasons the initial size_ passed to jvmti events might be an
  // estimate. b/72402467
  // SLICER_CHECK_LE(header_->data_off + header_->data_size, size_);

  // validate the map
  // (map section size = sizeof(MapList::size) + sizeof(MapList::list[size])
  auto map_list = ptr<dex::MapList>(header_->map_off);
  SLICER_CHECK_GT(map_list->size, 0);
  auto map_section_size =
      sizeof(dex::u4) + sizeof(dex::MapItem) * map_list->size;
  SLICER_CHECK_LE(header_->map_off + map_section_size, size_);
}

}  // namespace dex
