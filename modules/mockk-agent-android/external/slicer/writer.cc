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

#include "slicer/writer.h"

#include "slicer/common.h"
#include "slicer/scopeguard.h"
#include "slicer/dex_bytecode.h"
#include "slicer/dex_format.h"
#include "slicer/dex_ir.h"
#include "slicer/dex_leb128.h"

#include <assert.h>
#include <type_traits>
#include <vector>
#include <string.h>
#include <algorithm>

namespace dex {

// Returns the IR node index, or kNoIndex for null IR nodes
template <class T>
static dex::u4 OptIndex(const T* ir_node) {
  return ir_node != nullptr ? ir_node->index : dex::kNoIndex;
}

// Helper for creating the header of an encoded value
static void WriteEncodedValueHeader(dex::u1 type, int arg, Section& data) {
  assert((type & ~dex::kEncodedValueTypeMask) == 0);
  assert(arg >= 0 && arg < 8);
  dex::u1 header = dex::u1(type | (arg << dex::kEncodedValueArgShift));
  data.Push<dex::u1>(header);
}

// Writes an integer encoded value
template <class T>
static void WriteIntValue(dex::u1 type, T value, Section& data) {
  dex::u1 buff[sizeof(T)] = {};
  dex::u1* dst = buff;

  if (std::is_signed<T>::value) {
    const bool positive = (value >= 0);
    while (positive ? value >= 0x80 : value < -0x80) {
      *dst++ = value & 0xff;
      value >>= 8;
    }
    *dst++ = value & 0xff;
  } else {
    do {
      *dst++ = value & 0xff;
      value >>= 8;
    } while (value != 0);
  }

  size_t size = dst - buff;
  assert(size > 0 && size <= sizeof(T));
  WriteEncodedValueHeader(type, size - 1, data);
  data.Push(buff, size);
}

// Writes a floating point encoded value
template <class T>
static void WriteFloatValue(dex::u1 type, T value, Section& data) {
  dex::u1 buff[sizeof(T)] = {};
  auto src = reinterpret_cast<const dex::u1*>(&value);
  size_t size = sizeof(T);

  // skip "rightmost" zero bytes
  while (size > 1 && *src == 0) {
    --size;
    ++src;
  }

  // copy the rest...
  for (size_t i = 0; i < size; ++i) {
    buff[i] = src[i];
  }

  assert(size > 0 && size <= sizeof(T));
  WriteEncodedValueHeader(type, size - 1, data);
  data.Push(buff, size);
}

static void WriteEncodedArray(const ir::EncodedArray* ir_array, Section& data);
static void WriteAnnotation(const ir::Annotation* ir_annotation, Section& data);

// "encoded_value"
static void WriteEncodedValue(const ir::EncodedValue* ir_value, Section& data) {
  SLICER_EXTRA(auto offset = data.size());

  dex::u1 type = ir_value->type;
  switch (type) {
    case dex::kEncodedByte:
      WriteIntValue(type, ir_value->u.byte_value, data);
      break;

    case dex::kEncodedShort:
      WriteIntValue(type, ir_value->u.short_value, data);
      break;

    case dex::kEncodedChar:
      WriteIntValue(type, ir_value->u.char_value, data);
      break;

    case dex::kEncodedInt:
      WriteIntValue(type, ir_value->u.int_value, data);
      break;

    case dex::kEncodedLong:
      WriteIntValue(type, ir_value->u.long_value, data);
      break;

    case dex::kEncodedFloat:
      WriteFloatValue(type, ir_value->u.float_value, data);
      break;

    case dex::kEncodedDouble:
      WriteFloatValue(type, ir_value->u.double_value, data);
      break;

    case dex::kEncodedString:
      WriteIntValue<dex::u4>(type, ir_value->u.string_value->index, data);
      break;

    case dex::kEncodedType:
      WriteIntValue<dex::u4>(type, ir_value->u.type_value->index, data);
      break;

    case dex::kEncodedField:
      WriteIntValue<dex::u4>(type, ir_value->u.field_value->index, data);
      break;

    case dex::kEncodedMethod:
      WriteIntValue<dex::u4>(type, ir_value->u.method_value->index, data);
      break;

    case dex::kEncodedEnum:
      WriteIntValue<dex::u4>(type, ir_value->u.enum_value->index, data);
      break;

    case dex::kEncodedArray:
      WriteEncodedValueHeader(type, 0, data);
      WriteEncodedArray(ir_value->u.array_value, data);
      break;

    case dex::kEncodedAnnotation:
      WriteEncodedValueHeader(type, 0, data);
      WriteAnnotation(ir_value->u.annotation_value, data);
      break;

    case dex::kEncodedNull:
      WriteEncodedValueHeader(type, 0, data);
      break;

    case dex::kEncodedBoolean: {
      int arg = ir_value->u.bool_value ? 1 : 0;
      WriteEncodedValueHeader(type, arg, data);
    } break;

    default:
      SLICER_CHECK(!"unexpected value type");
  }

  // optionally check the encoding against the original one
  // (if possible, some of the values contain relocated indexes)
  SLICER_EXTRA({
    switch (type) {
      case dex::kEncodedByte:
      case dex::kEncodedShort:
      case dex::kEncodedChar:
      case dex::kEncodedInt:
      case dex::kEncodedLong:
      case dex::kEncodedFloat:
      case dex::kEncodedDouble:
      case dex::kEncodedNull:
      case dex::kEncodedBoolean:
        auto ptr = data.ptr<const dex::u1>(offset);
        auto size = data.size() - offset;
        SLICER_CHECK_EQ(size, ir_value->original.size());
        SLICER_CHECK_EQ(memcmp(ptr, ir_value->original.ptr(), size), 0);
        break;
    }
  });
}

// "encoded_annotation"
static void WriteAnnotation(const ir::Annotation* ir_annotation, Section& data) {
  data.PushULeb128(ir_annotation->type->index);
  data.PushULeb128(ir_annotation->elements.size());
  for (auto irAnnotationElement : ir_annotation->elements) {
    data.PushULeb128(irAnnotationElement->name->index);
    WriteEncodedValue(irAnnotationElement->value, data);
  }
}

// "encoded_array"
static void WriteEncodedArray(const ir::EncodedArray* ir_array, Section& data) {
  const auto& values = ir_array->values;
  data.PushULeb128(values.size());
  for (auto irEncodedValue : values) {
    WriteEncodedValue(irEncodedValue, data);
  }
}

// helper for concatenating .dex sections into the final image
template <class T>
static void CopySection(const T& section, dex::u1* image, dex::u4 image_size) {
  if (section.size() == 0) {
    SLICER_CHECK_EQ(section.ItemsCount(), 0);
    return;
  }

  SLICER_CHECK_GT(section.ItemsCount(), 0);
  dex::u4 offset = section.SectionOffset();
  dex::u4 size = section.size();
  SLICER_CHECK_GE(offset, dex::Header::kV40Size);
  SLICER_CHECK_LE(offset + size, image_size);

  ::memcpy(image + offset, section.data(), size);
}

static u4 ReadU4(const u2* ptr) { return ptr[0] | (u4(ptr[1]) << 16); }

static void WriteU4(u2* ptr, u4 val) {
  ptr[0] = val & 0xffff;
  ptr[1] = val >> 16;
}

// This is the main interface for the .dex writer
// (returns nullptr on failure)
dex::u1* Writer::CreateImage(Allocator* allocator, size_t* new_image_size) {
  // create a new DexImage
  dex_.reset(new DexImage);

  SLICER_SCOPE_EXIT {
      dex_.reset();
  };

  // TODO: revisit IR normalization
  // (ideally we shouldn't change the IR while generating an image)
  dex_ir_->Normalize();

  int version = Header::GetVersion(dex_ir_->magic.ptr());
  SLICER_CHECK_NE(version, 0);
  SLICER_CHECK_GE(version, Header::kMinVersion);
  SLICER_CHECK_LE(version, Header::kMaxVersion);
  u4 header_size = version >= Header::kV41 ? Header::kV41Size : Header::kV40Size;

  // track the current offset within the .dex image
  dex::u4 offset = 0;

  // allocate the image and index sections
  // (they will be back-filled)
  offset += header_size;
  offset += dex_->string_ids.Init(offset, dex_ir_->strings.size());
  offset += dex_->type_ids.Init(offset, dex_ir_->types.size());
  offset += dex_->proto_ids.Init(offset, dex_ir_->protos.size());
  offset += dex_->field_ids.Init(offset, dex_ir_->fields.size());
  offset += dex_->method_ids.Init(offset, dex_ir_->methods.size());
  offset += dex_->class_defs.Init(offset, dex_ir_->classes.size());
  offset += dex_->method_handles.Init(offset, dex_ir_->method_handles.size());

  // the base offset for the "data" meta-section
  SLICER_CHECK_EQ(offset % 4, 0);
  const dex::u4 data_offset = offset;

  // we must create the sections in a very specific
  // order due to file pointers across sections
  offset += CreateStringDataSection(offset);
  offset += CreateTypeListsSection(offset);
  offset += CreateDebugInfoSection(offset);
  offset += CreateEncodedArrayItemSection(offset);
  offset += CreateCodeItemSection(offset);
  offset += CreateClassDataSection(offset);
  offset += CreateAnnItemSection(offset);
  offset += CreateAnnSetsSection(offset);
  offset += CreateAnnSetRefListsSection(offset);
  offset += CreateAnnDirectoriesSection(offset);
  offset += CreateMapSection(offset);

  // back-fill the indexes
  FillTypes();
  FillFields();
  FillProtos();
  FillMethods();
  FillClassDefs();
  FillMethodHandles();

  // allocate the final buffer for the .dex image
  SLICER_CHECK_EQ(offset % 4, 0);
  const dex::u4 image_size = offset;
  dex::u1* image = static_cast<dex::u1*>(allocator->Allocate(image_size));
  if (image == nullptr) {
    // memory allocation failed, bailing out...
    return nullptr;
  }
  memset(image, 0, image_size);

  // finally, back-fill the header
  SLICER_CHECK_GT(image_size, header_size);

  dex::Header* header = reinterpret_cast<dex::Header*>(image + 0);

  // magic signature
  memcpy(header->magic, dex_ir_->magic.ptr(), dex_ir_->magic.size());

  header->file_size = image_size;
  header->header_size = header_size;
  header->endian_tag = dex::kEndianConstant;

  header->link_size = 0;
  header->link_off = 0;

  header->map_off = dex_->map_list.SectionOffset();
  header->string_ids_size = dex_->string_ids.ItemsCount();
  header->string_ids_off = dex_->string_ids.SectionOffset();
  header->type_ids_size = dex_->type_ids.ItemsCount();
  header->type_ids_off = dex_->type_ids.SectionOffset();
  header->proto_ids_size = dex_->proto_ids.ItemsCount();
  header->proto_ids_off = dex_->proto_ids.SectionOffset();
  header->field_ids_size = dex_->field_ids.ItemsCount();
  header->field_ids_off = dex_->field_ids.SectionOffset();
  header->method_ids_size = dex_->method_ids.ItemsCount();
  header->method_ids_off = dex_->method_ids.SectionOffset();
  header->class_defs_size = dex_->class_defs.ItemsCount();
  header->class_defs_off = dex_->class_defs.SectionOffset();
  header->data_size = image_size - data_offset;
  header->data_off = data_offset;
  if (version >= Header::kV41) {
    header->data_size = 0;
    header->data_off = 0;
    header->SetContainer(0, header->file_size);
  }

  // copy the individual sections to the final image
  CopySection(dex_->string_ids, image, image_size);
  CopySection(dex_->type_ids, image, image_size);
  CopySection(dex_->proto_ids, image, image_size);
  CopySection(dex_->field_ids, image, image_size);
  CopySection(dex_->method_ids, image, image_size);
  CopySection(dex_->class_defs, image, image_size);
  CopySection(dex_->method_handles, image, image_size);
  CopySection(dex_->string_data, image, image_size);
  CopySection(dex_->type_lists, image, image_size);
  CopySection(dex_->debug_info, image, image_size);
  CopySection(dex_->encoded_arrays, image, image_size);
  CopySection(dex_->code, image, image_size);
  CopySection(dex_->class_data, image, image_size);
  CopySection(dex_->ann_directories, image, image_size);
  CopySection(dex_->ann_set_ref_lists, image, image_size);
  CopySection(dex_->ann_sets, image, image_size);
  CopySection(dex_->ann_items, image, image_size);
  CopySection(dex_->map_list, image, image_size);

  // checksum
  header->checksum = dex::ComputeChecksum(header);

  *new_image_size = image_size;
  return image;
}

// "string_id_item" + string data section
dex::u4 Writer::CreateStringDataSection(dex::u4 section_offset) {
  auto& section = dex_->string_data;
  section.SetOffset(section_offset);

  const auto& strings = dex_ir_->strings;
  for (size_t i = 0; i < strings.size(); ++i) {
    const auto& ir_string = strings[i];
    auto dexStringId = &dex_->string_ids[i];

    dex::u4 offset = section.AddItem();
    section.Push(ir_string->data);
    dexStringId->string_data_off = section.AbsoluteOffset(offset);
  }

  dex::u4 size = section.Seal(4);
  return size;
}

// Helper for creating the map section
template <class T>
static void AddMapItem(const T& section, std::vector<dex::MapItem>& items) {
  if (section.ItemsCount() > 0) {
    SLICER_CHECK_GE(section.SectionOffset(), dex::Header::kV40Size);
    dex::MapItem map_item = {};
    map_item.type = section.MapEntryType();
    map_item.size = section.ItemsCount();
    map_item.offset = section.SectionOffset();
    items.push_back(map_item);
  }
}

// map_list section
dex::u4 Writer::CreateMapSection(dex::u4 section_offset) {
  auto& section = dex_->map_list;
  section.SetOffset(section_offset);
  section.AddItem(4);

  std::vector<dex::MapItem> map_items;

  dex::MapItem headerItem = {};
  headerItem.type = dex::kHeaderItem;
  headerItem.size = 1;
  headerItem.offset = 0;
  map_items.push_back(headerItem);

  AddMapItem(dex_->string_ids, map_items);
  AddMapItem(dex_->type_ids, map_items);
  AddMapItem(dex_->proto_ids, map_items);
  AddMapItem(dex_->field_ids, map_items);
  AddMapItem(dex_->method_ids, map_items);
  AddMapItem(dex_->class_defs, map_items);
  AddMapItem(dex_->method_handles, map_items);
  AddMapItem(dex_->string_data, map_items);
  AddMapItem(dex_->type_lists, map_items);
  AddMapItem(dex_->debug_info, map_items);
  AddMapItem(dex_->encoded_arrays, map_items);
  AddMapItem(dex_->code, map_items);
  AddMapItem(dex_->class_data, map_items);
  AddMapItem(dex_->ann_directories, map_items);
  AddMapItem(dex_->ann_set_ref_lists, map_items);
  AddMapItem(dex_->ann_sets, map_items);
  AddMapItem(dex_->ann_items, map_items);
  AddMapItem(dex_->map_list, map_items);

  std::sort(map_items.begin(), map_items.end(),
            [](const dex::MapItem& a, const dex::MapItem& b) {
              SLICER_CHECK_NE(a.offset, b.offset);
              return a.offset < b.offset;
            });

  section.Push<dex::u4>(map_items.size());
  section.Push(map_items);
  return section.Seal(4);
}

// annotation_item section
dex::u4 Writer::CreateAnnItemSection(dex::u4 section_offset) {
  dex_->ann_items.SetOffset(section_offset);

  for (const auto& ir_node : dex_ir_->annotations) {
    if (ir_node->visibility != dex::kVisibilityEncoded) {
      // TODO: factor out the node_offset_ updating
      dex::u4& offset = node_offset_[ir_node.get()];
      SLICER_CHECK_EQ(offset, 0);
      offset = WriteAnnotationItem(ir_node.get());
    }
  }

  return dex_->ann_items.Seal(4);
}

// annotation_set_item section
dex::u4 Writer::CreateAnnSetsSection(dex::u4 section_offset) {
  dex_->ann_sets.SetOffset(section_offset);

  for (const auto& ir_node : dex_ir_->annotation_sets) {
    dex::u4& offset = node_offset_[ir_node.get()];
    SLICER_CHECK_EQ(offset, 0);
    offset = WriteAnnotationSet(ir_node.get());
  }

  return dex_->ann_sets.Seal(4);
}

// annotation_set_ref_list section
dex::u4 Writer::CreateAnnSetRefListsSection(dex::u4 section_offset) {
  dex_->ann_set_ref_lists.SetOffset(section_offset);

  for (const auto& ir_node : dex_ir_->annotation_set_ref_lists) {
    dex::u4& offset = node_offset_[ir_node.get()];
    SLICER_CHECK_EQ(offset, 0);
    offset = WriteAnnotationSetRefList(ir_node.get());
  }

  return dex_->ann_set_ref_lists.Seal(4);
}

// type_list section
dex::u4 Writer::CreateTypeListsSection(dex::u4 section_offset) {
  dex_->type_lists.SetOffset(section_offset);

  for (const auto& ir_type_list : dex_ir_->type_lists) {
    dex::u4& offset = node_offset_[ir_type_list.get()];
    SLICER_CHECK_EQ(offset, 0);
    offset = WriteTypeList(ir_type_list->types);
  }

  return dex_->type_lists.Seal(4);
}

// code_item section
dex::u4 Writer::CreateCodeItemSection(dex::u4 section_offset) {
  dex_->code.SetOffset(section_offset);

  for (const auto& ir_node : dex_ir_->code) {
    dex::u4& offset = node_offset_[ir_node.get()];
    SLICER_CHECK_EQ(offset, 0);
    offset = WriteCode(ir_node.get());
  }

  dex::u4 size = dex_->code.Seal(4);
  return size;
}

// debug info section
dex::u4 Writer::CreateDebugInfoSection(dex::u4 section_offset) {
  dex_->debug_info.SetOffset(section_offset);

  for (const auto& ir_node : dex_ir_->debug_info) {
    dex::u4& offset = node_offset_[ir_node.get()];
    SLICER_CHECK_EQ(offset, 0);
    offset = WriteDebugInfo(ir_node.get());
  }

  dex::u4 size = dex_->debug_info.Seal(4);
  return size;
}

// class_data_item section
dex::u4 Writer::CreateClassDataSection(dex::u4 section_offset) {
  dex_->class_data.SetOffset(section_offset);

  const auto& classes = dex_ir_->classes;
  for (size_t i = 0; i < classes.size(); ++i) {
    auto ir_class = classes[i].get();
    auto dex_class_def = &dex_->class_defs[i];
    dex_class_def->class_data_off = WriteClassData(ir_class);
  }

  dex::u4 size = dex_->class_data.Seal(4);
  return size;
}

// annotations_directory section
dex::u4 Writer::CreateAnnDirectoriesSection(dex::u4 section_offset) {
  dex_->ann_directories.SetOffset(section_offset);

  const auto& classes = dex_ir_->classes;
  for (size_t i = 0; i < classes.size(); ++i) {
    auto ir_class = classes[i].get();
    auto dex_class_def = &dex_->class_defs[i];
    dex_class_def->annotations_off = WriteClassAnnotations(ir_class);
  }

  return dex_->ann_directories.Seal(4);
}

// encoded_array_item section
dex::u4 Writer::CreateEncodedArrayItemSection(dex::u4 section_offset) {
  dex_->encoded_arrays.SetOffset(section_offset);

  const auto& classes = dex_ir_->classes;
  for (size_t i = 0; i < classes.size(); ++i) {
    auto ir_class = classes[i].get();
    auto dex_class_def = &dex_->class_defs[i];
    dex_class_def->static_values_off = WriteClassStaticValues(ir_class);
  }

  return dex_->encoded_arrays.Seal(4);
}

// "type_id_item"
void Writer::FillTypes() {
  const auto& types = dex_ir_->types;
  for (size_t i = 0; i < types.size(); ++i) {
    const auto& ir_type = types[i];
    auto dexTypeId = &dex_->type_ids[i];
    // CONSIDER: an automatic index check would be nice
    dexTypeId->descriptor_idx = ir_type->descriptor->index;
  }
}

// "proto_id_item"
void Writer::FillProtos() {
  const auto& protos = dex_ir_->protos;
  for (size_t i = 0; i < protos.size(); ++i) {

    const auto& irProto = protos[i];
    auto dexProtoId = &dex_->proto_ids[i];

    dexProtoId->shorty_idx = irProto->shorty->index;
    dexProtoId->return_type_idx = irProto->return_type->index;
    dexProtoId->parameters_off = FilePointer(irProto->param_types);
  }
}

void Writer::FillMethodHandles(){
  const auto& methodHandles = dex_ir_->method_handles;
  for(size_t i = 0; i < methodHandles.size(); ++i){

    const auto& irMethodHandle = methodHandles[i];
    auto dexMethodHandle = &dex_->method_handles[i];

    dexMethodHandle->method_handle_type = irMethodHandle->method_handle_type;

    if(irMethodHandle->IsField()){
      dexMethodHandle->field_or_method_id = irMethodHandle->field->index;
    }
    else{
      dexMethodHandle->field_or_method_id = irMethodHandle->method->index;
    }
  }
}

// "field_id_item"
void Writer::FillFields() {
  const auto& fields = dex_ir_->fields;
  for (size_t i = 0; i < fields.size(); ++i) {
    const auto& ir_field = fields[i];
    auto dexFieldId = &dex_->field_ids[i];
    dexFieldId->class_idx = ir_field->parent->index;
    dexFieldId->type_idx = ir_field->type->index;
    dexFieldId->name_idx = ir_field->name->index;
  }
}

// "method_id_item"
void Writer::FillMethods() {
  const auto& methods = dex_ir_->methods;
  for (size_t i = 0; i < methods.size(); ++i) {
    const auto& ir_method = methods[i];
    auto dexMethodId = &dex_->method_ids[i];
    dexMethodId->class_idx = ir_method->parent->index;
    dexMethodId->proto_idx = ir_method->prototype->index;
    dexMethodId->name_idx = ir_method->name->index;
  }
}

// "class_def_item"
void Writer::FillClassDefs() {
  const auto& classes = dex_ir_->classes;
  for (size_t i = 0; i < classes.size(); ++i) {
    auto ir_class = classes[i].get();
    auto dex_class_def = &dex_->class_defs[i];
    dex_class_def->class_idx = ir_class->type->index;
    dex_class_def->access_flags = ir_class->access_flags;
    dex_class_def->superclass_idx = OptIndex(ir_class->super_class);
    dex_class_def->source_file_idx = OptIndex(ir_class->source_file);
    dex_class_def->interfaces_off = FilePointer(ir_class->interfaces);

    // NOTE: we already set some offsets when we created the
    //  corresponding .dex section:
    //
    //  ->annotations_off
    //  ->class_data_off
    //  ->static_values_off
  }
}

// "type_list"
dex::u4 Writer::WriteTypeList(const std::vector<ir::Type*>& types) {
  if (types.empty()) {
    return 0;
  }

  auto& data = dex_->type_lists;
  dex::u4 offset = data.AddItem(4);
  data.Push<dex::u4>(types.size());
  for (auto ir_type : types) {
    data.Push<dex::u2>(ir_type->index);
  }
  return data.AbsoluteOffset(offset);
}

// "annotation_item"
dex::u4 Writer::WriteAnnotationItem(const ir::Annotation* ir_annotation) {
  SLICER_CHECK_NE(ir_annotation->visibility, dex::kVisibilityEncoded);

  auto& data = dex_->ann_items;
  dex::u4 offset = data.AddItem();
  data.Push<dex::u1>(ir_annotation->visibility);
  WriteAnnotation(ir_annotation, data);
  return data.AbsoluteOffset(offset);
}

// "annotation_set_item"
dex::u4 Writer::WriteAnnotationSet(const ir::AnnotationSet* ir_annotation_set) {
  SLICER_CHECK_NE(ir_annotation_set, nullptr);

  const auto& annotations = ir_annotation_set->annotations;

  auto& data = dex_->ann_sets;
  dex::u4 offset = data.AddItem(4);
  data.Push<dex::u4>(annotations.size());
  for (auto ir_annotation : annotations) {
    data.Push<dex::u4>(FilePointer(ir_annotation));
  }
  return data.AbsoluteOffset(offset);
}

// "annotation_set_ref_list"
dex::u4 Writer::WriteAnnotationSetRefList(
    const ir::AnnotationSetRefList* ir_annotation_set_ref_list) {
  SLICER_CHECK_NE(ir_annotation_set_ref_list, nullptr);

  const auto& annotations = ir_annotation_set_ref_list->annotations;

  auto& data = dex_->ann_set_ref_lists;
  dex::u4 offset = data.AddItem(4);
  data.Push<dex::u4>(annotations.size());
  for (auto ir_annotation_set : annotations) {
    data.Push<dex::u4>(FilePointer(ir_annotation_set));
  }
  return data.AbsoluteOffset(offset);
}

// "annotations_directory_item"
dex::u4 Writer::WriteClassAnnotations(const ir::Class* ir_class) {
  if (ir_class->annotations == nullptr) {
    return 0;
  }

  auto ir_annotations = ir_class->annotations;

  dex::u4& offset = node_offset_[ir_annotations];
  if (offset == 0) {
    // in order to write a contiguous "annotations_directory_item" we do two
    // passes :
    // 1. write the field/method/params annotations content
    // 2. write the directory (including the field/method/params arrays)
    std::vector<dex::FieldAnnotationsItem> dex_field_annotations;
    std::vector<dex::MethodAnnotationsItem> dex_method_annotations;
    std::vector<dex::ParameterAnnotationsItem> dex_param_annotations;

    for (auto irItem : ir_annotations->field_annotations) {
      dex::FieldAnnotationsItem dex_item = {};
      dex_item.field_idx = irItem->field_decl->index;
      dex_item.annotations_off = FilePointer(irItem->annotations);
      dex_field_annotations.push_back(dex_item);
    }

    for (auto irItem : ir_annotations->method_annotations) {
      dex::MethodAnnotationsItem dex_item = {};
      dex_item.method_idx = irItem->method_decl->index;
      dex_item.annotations_off = FilePointer(irItem->annotations);
      dex_method_annotations.push_back(dex_item);
    }

    for (auto irItem : ir_annotations->param_annotations) {
      dex::ParameterAnnotationsItem dex_item = {};
      dex_item.method_idx = irItem->method_decl->index;
      dex_item.annotations_off = FilePointer(irItem->annotations);
      dex_param_annotations.push_back(dex_item);
    }

    dex::u4 class_annotations_offset =
        FilePointer(ir_annotations->class_annotation);

    // now that the annotations content is written,
    // we can write down the "annotations_directory_item"
    dex::AnnotationsDirectoryItem dex_annotations = {};
    dex_annotations.class_annotations_off = class_annotations_offset;
    dex_annotations.fields_size = ir_annotations->field_annotations.size();
    dex_annotations.methods_size = ir_annotations->method_annotations.size();
    dex_annotations.parameters_size = ir_annotations->param_annotations.size();

    auto& data = dex_->ann_directories;
    offset = data.AddItem(4);
    data.Push(dex_annotations);
    data.Push(dex_field_annotations);
    data.Push(dex_method_annotations);
    data.Push(dex_param_annotations);
    offset = data.AbsoluteOffset(offset);
  }
  return offset;
}

// "debug_info_item"
dex::u4 Writer::WriteDebugInfo(const ir::DebugInfo* ir_debug_info) {
  SLICER_CHECK_NE(ir_debug_info, nullptr);

  auto& data = dex_->debug_info;
  dex::u4 offset = data.AddItem();

  // debug info "header"
  data.PushULeb128(ir_debug_info->line_start);
  data.PushULeb128(ir_debug_info->param_names.size());
  for (auto ir_string : ir_debug_info->param_names) {
    data.PushULeb128(OptIndex(ir_string) + 1);
  }

  // debug info "state machine bytecodes"
  const dex::u1* src = ir_debug_info->data.ptr<dex::u1>();
  dex::u1 opcode = 0;
  while ((opcode = *src++) != dex::DBG_END_SEQUENCE) {
    data.Push<dex::u1>(opcode);

    switch (opcode) {
      case dex::DBG_ADVANCE_PC:
        // addr_diff
        data.PushULeb128(dex::ReadULeb128(&src));
        break;

      case dex::DBG_ADVANCE_LINE:
        // line_diff
        data.PushSLeb128(dex::ReadSLeb128(&src));
        break;

      case dex::DBG_START_LOCAL: {
        // register_num
        data.PushULeb128(dex::ReadULeb128(&src));

        dex::u4 name_index = dex::ReadULeb128(&src) - 1;
        data.PushULeb128(MapStringIndex(name_index) + 1);

        dex::u4 type_index = dex::ReadULeb128(&src) - 1;
        data.PushULeb128(MapTypeIndex(type_index) + 1);
      } break;

      case dex::DBG_START_LOCAL_EXTENDED: {
        // register_num
        data.PushULeb128(dex::ReadULeb128(&src));

        dex::u4 name_index = dex::ReadULeb128(&src) - 1;
        data.PushULeb128(MapStringIndex(name_index) + 1);

        dex::u4 type_index = dex::ReadULeb128(&src) - 1;
        data.PushULeb128(MapTypeIndex(type_index) + 1);

        dex::u4 sig_index = dex::ReadULeb128(&src) - 1;
        data.PushULeb128(MapStringIndex(sig_index) + 1);
      } break;

      case dex::DBG_END_LOCAL:
      case dex::DBG_RESTART_LOCAL:
        // register_num
        data.PushULeb128(dex::ReadULeb128(&src));
        break;

      case dex::DBG_SET_FILE: {
        dex::u4 name_index = dex::ReadULeb128(&src) - 1;
        data.PushULeb128(MapStringIndex(name_index) + 1);
      } break;
    }
  }
  data.Push<dex::u1>(dex::DBG_END_SEQUENCE);

  return data.AbsoluteOffset(offset);
}

// instruction[] array
void Writer::WriteInstructions(slicer::ArrayView<const dex::u2> instructions) {
  SLICER_CHECK(!instructions.empty());

  auto offset = dex_->code.Push(instructions);
  dex::u2* ptr = dex_->code.ptr<dex::u2>(offset);
  dex::u2* const end = ptr + instructions.size();

  // relocate the instructions
  while (ptr < end) {
    auto opcode = dex::OpcodeFromBytecode(*ptr);
    dex::u2* idx = &ptr[1];
    dex::u2* idx2 = nullptr;

    size_t idx_size = 0;
    switch (dex::GetFormatFromOpcode(opcode)) {
      case dex::k20bc:
      case dex::k21c:
      case dex::k35c:
      case dex::k3rc:
      case dex::k22c:
        idx_size = 2;
        break;

      case dex::k31c:
        idx_size = 4;
        break;

      case dex::k45cc:
      case dex::k4rcc:
        idx_size = 2;
        idx2 = &ptr[3];
      break;

      default:
        break;
    }

    switch (dex::GetIndexTypeFromOpcode(opcode)) {
      case dex::kIndexStringRef:
        if (idx_size == 4) {
          dex::u4 new_index = MapStringIndex(ReadU4(idx));
          SLICER_CHECK_NE(new_index, dex::kNoIndex);
          WriteU4(idx, new_index);
        } else {
          SLICER_CHECK_EQ(idx_size, 2);
          dex::u4 new_index = MapStringIndex(*idx);
          SLICER_CHECK_NE(new_index, dex::kNoIndex);
          SLICER_CHECK_EQ(dex::u2(new_index), new_index);
          *idx = dex::u2(new_index);
        }
        break;

      case dex::kIndexTypeRef: {
        SLICER_CHECK_EQ(idx_size, 2);
        dex::u4 new_index = MapTypeIndex(*idx);
        SLICER_CHECK_NE(new_index, dex::kNoIndex);
        SLICER_CHECK_EQ(dex::u2(new_index), new_index);
        *idx = dex::u2(new_index);
      } break;

      case dex::kIndexFieldRef: {
        SLICER_CHECK_EQ(idx_size, 2);
        dex::u4 new_index = MapFieldIndex(*idx);
        SLICER_CHECK_NE(new_index, dex::kNoIndex);
        SLICER_CHECK_EQ(dex::u2(new_index), new_index);
        *idx = dex::u2(new_index);
      } break;

      case dex::kIndexMethodRef: {
        SLICER_CHECK_EQ(idx_size, 2);
        dex::u4 new_index = MapMethodIndex(*idx);
        SLICER_CHECK_NE(new_index, dex::kNoIndex);
        SLICER_CHECK_EQ(dex::u2(new_index), new_index);
        *idx = dex::u2(new_index);
      } break;

      case dex::kIndexMethodAndProtoRef: {
        SLICER_CHECK_EQ(idx_size, 2);
        dex::u4 new_index = MapMethodIndex(*idx);
        SLICER_CHECK_NE(new_index, dex::kNoIndex);
        SLICER_CHECK_EQ(dex::u2(new_index), new_index);
        *idx = dex::u2(new_index);
        dex::u4 new_index2 = MapProtoIndex(*idx2);
        SLICER_CHECK_NE(new_index2, dex::kNoIndex);
        SLICER_CHECK_EQ(dex::u2(new_index2), new_index2);
        *idx2 = dex::u2(new_index2);
      } break;

      case dex::kIndexMethodHandleRef: {
        SLICER_CHECK_EQ(idx_size, 2);
        dex::u4 new_index = MapMethodHandleIndex(*idx);
        SLICER_CHECK_NE(new_index, dex::kNoIndex);
        SLICER_CHECK_EQ(dex::u2(new_index), new_index);
        *idx = dex::u2(new_index);
      } break;

      default:
        break;
    }

    auto isize = dex::GetWidthFromBytecode(ptr);
    SLICER_CHECK_GT(isize, 0);
    ptr += isize;
  }
  SLICER_CHECK_EQ(ptr, end);
}

// "try_item[] + encoded_catch_handler_list"
void Writer::WriteTryBlocks(const ir::Code* irCode) {
  SLICER_CHECK(!irCode->try_blocks.empty());

  // use a temporary buffer to build the "encoded_catch_handler_list"
  slicer::Buffer handlers_list;
  auto original_list = irCode->catch_handlers.ptr<dex::u1>();
  auto ptr = original_list;
  std::map<dex::u2, dex::u2> handlers_offset_map;

  dex::u4 handlers_count = dex::ReadULeb128(&ptr);
  handlers_list.PushULeb128(handlers_count);

  for (dex::u4 handler_index = 0; handler_index < handlers_count; ++handler_index) {
    // track the oldOffset/newOffset mapping
    handlers_offset_map[ptr - original_list] = handlers_list.size();

    // parse each "encoded_catch_handler"
    int catch_count = dex::ReadSLeb128(&ptr);
    handlers_list.PushSLeb128(catch_count);

    for (int catch_index = 0; catch_index < std::abs(catch_count); ++catch_index) {
      // type_idx
      dex::u4 type_index = dex::ReadULeb128(&ptr);
      handlers_list.PushULeb128(MapTypeIndex(type_index));

      // address
      handlers_list.PushULeb128(dex::ReadULeb128(&ptr));
    }

    if (catch_count < 1) {
      // catch_all_addr
      handlers_list.PushULeb128(dex::ReadULeb128(&ptr));
    }
  }

  handlers_list.Seal(1);

  // now write everything (try_item[] and encoded_catch_handler_list)
  auto& data = dex_->code;
  dex::u4 tries_offset = data.size();
  data.Push(irCode->try_blocks);
  data.Push(handlers_list);

  // finally relocate the offsets to handlers
  for (dex::TryBlock& dex_try : slicer::ArrayView<dex::TryBlock>(
           data.ptr<dex::TryBlock>(tries_offset), irCode->try_blocks.size())) {
    dex::u2 new_Handler_offset = handlers_offset_map[dex_try.handler_off];
    SLICER_CHECK_NE(new_Handler_offset, 0);
    dex_try.handler_off = new_Handler_offset;
  }
}

// "code_item"
dex::u4 Writer::WriteCode(const ir::Code* irCode) {
  SLICER_CHECK_NE(irCode, nullptr);

  dex::Code dex_code = {};
  dex_code.registers_size = irCode->registers;
  dex_code.ins_size = irCode->ins_count;
  dex_code.outs_size = irCode->outs_count;
  dex_code.tries_size = irCode->try_blocks.size();
  dex_code.debug_info_off = FilePointer(irCode->debug_info);
  dex_code.insns_size = irCode->instructions.size();

  auto& data = dex_->code;
  dex::u4 offset = data.AddItem(4);
  data.Push(&dex_code, offsetof(dex::Code, insns));
  WriteInstructions(irCode->instructions);
  if (!irCode->try_blocks.empty()) {
    data.Align(4);
    WriteTryBlocks(irCode);
  }
  return data.AbsoluteOffset(offset);
}

// "encoded_field"
void Writer::WriteEncodedField(const ir::EncodedField* ir_encoded_field,
                       dex::u4* base_index) {
  dex::u4 index_delta = ir_encoded_field->decl->index;
  SLICER_CHECK_NE(index_delta, dex::kNoIndex);
  if (*base_index != dex::kNoIndex) {
    SLICER_CHECK_GT(index_delta, *base_index);
    index_delta = index_delta - *base_index;
  }
  *base_index = ir_encoded_field->decl->index;

  auto& data = dex_->class_data;
  data.PushULeb128(index_delta);
  data.PushULeb128(ir_encoded_field->access_flags);
}

// "encoded_method"
void Writer::WriteEncodedMethod(const ir::EncodedMethod* ir_encoded_method,
                        dex::u4* base_index) {
  dex::u4 index_delta = ir_encoded_method->decl->index;
  SLICER_CHECK_NE(index_delta, dex::kNoIndex);
  if (*base_index != dex::kNoIndex) {
    SLICER_CHECK_GT(index_delta, *base_index);
    index_delta = index_delta - *base_index;
  }
  *base_index = ir_encoded_method->decl->index;

  dex::u4 code_offset = FilePointer(ir_encoded_method->code);

  auto& data = dex_->class_data;
  data.PushULeb128(index_delta);
  data.PushULeb128(ir_encoded_method->access_flags);
  data.PushULeb128(code_offset);
}

// "class_data_item"
dex::u4 Writer::WriteClassData(const ir::Class* ir_class) {
  if (ir_class->static_fields.empty() && ir_class->instance_fields.empty() &&
      ir_class->direct_methods.empty() && ir_class->virtual_methods.empty()) {
    return 0;
  }

  auto& data = dex_->class_data;
  dex::u4 offset = data.AddItem();

  data.PushULeb128(ir_class->static_fields.size());
  data.PushULeb128(ir_class->instance_fields.size());
  data.PushULeb128(ir_class->direct_methods.size());
  data.PushULeb128(ir_class->virtual_methods.size());

  dex::u4 base_index = dex::kNoIndex;
  for (auto ir_encoded_field : ir_class->static_fields) {
    WriteEncodedField(ir_encoded_field, &base_index);
  }

  base_index = dex::kNoIndex;
  for (auto ir_encoded_field : ir_class->instance_fields) {
    WriteEncodedField(ir_encoded_field, &base_index);
  }

  base_index = dex::kNoIndex;
  for (auto ir_encoded_method : ir_class->direct_methods) {
    WriteEncodedMethod(ir_encoded_method, &base_index);
  }

  base_index = dex::kNoIndex;
  for (auto ir_encoded_method : ir_class->virtual_methods) {
    WriteEncodedMethod(ir_encoded_method, &base_index);
  }

  return data.AbsoluteOffset(offset);
}

// "encoded_array_item"
dex::u4 Writer::WriteClassStaticValues(const ir::Class* ir_class) {
  if (ir_class->static_init == nullptr) {
    return 0;
  }

  dex::u4& offset = node_offset_[ir_class->static_init];
  if (offset == 0) {
    auto& data = dex_->encoded_arrays;
    offset = data.AddItem();
    WriteEncodedArray(ir_class->static_init, data);
    offset = data.AbsoluteOffset(offset);
  }
  return offset;
}

// Map an index from the original .dex to the new index
dex::u4 Writer::MapStringIndex(dex::u4 index) const {
  if (index != dex::kNoIndex) {
    index = dex_ir_->strings_map.at(index)->index;
    SLICER_CHECK_NE(index, dex::kNoIndex);
  }
  return index;
}

// Map an index from the original .dex to the new index
dex::u4 Writer::MapTypeIndex(dex::u4 index) const {
  if (index != dex::kNoIndex) {
    index = dex_ir_->types_map.at(index)->index;
    SLICER_CHECK_NE(index, dex::kNoIndex);
  }
  return index;
}

// Map an index from the original .dex to the new index
dex::u4 Writer::MapFieldIndex(dex::u4 index) const {
  if (index != dex::kNoIndex) {
    index = dex_ir_->fields_map.at(index)->index;
    SLICER_CHECK_NE(index, dex::kNoIndex);
  }
  return index;
}

// Map an index from the original .dex to the new index
dex::u4 Writer::MapMethodIndex(dex::u4 index) const {
  if (index != dex::kNoIndex) {
    index = dex_ir_->methods_map.at(index)->index;
    SLICER_CHECK_NE(index, dex::kNoIndex);
  }
  return index;
}

// Map an index from the original .dex to the new index
dex::u4 Writer::MapMethodHandleIndex(dex::u4 index) const {
  if (index != dex::kNoIndex) {
    index = dex_ir_->method_handles_map.at(index)->index;
    SLICER_CHECK_NE(index, dex::kNoIndex);
  }
  return index;
}

// Map an index from the original .dex to the new index
dex::u4 Writer::MapProtoIndex(dex::u4 index) const {
  if (index != dex::kNoIndex) {
    index = dex_ir_->protos_map.at(index)->index;
    SLICER_CHECK_NE(index, dex::kNoIndex);
  }
  return index;
}

// .dex IR node to file pointer (absolute offset)
dex::u4 Writer::FilePointer(const ir::Node* ir_node) const {
  if (ir_node == nullptr) {
    return 0;
  }
  auto it = node_offset_.find(ir_node);
  SLICER_CHECK(it != node_offset_.end());
  dex::u4 offset = it->second;
  SLICER_CHECK_GT(offset, 0);
  return offset;
}

}  // namespace dex
