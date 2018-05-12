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
#include "buffer.h"
#include "arrayview.h"
#include "dex_format.h"
#include "dex_ir.h"

#include <map>
#include <memory>
#include <vector>

namespace dex {

// Specialized buffer for creating a .dex image section
// (tracking the section offset, section type, ...)
class Section : public slicer::Buffer {
 public:
  Section(dex::u2 mapEntryType) : map_entry_type_(mapEntryType) {}
  ~Section() = default;

  Section(const Section&) = delete;
  Section& operator=(const Section&) = delete;

  void SetOffset(dex::u4 offset) {
    SLICER_CHECK(offset > 0 && offset % 4 == 0);
    offset_ = offset;
  }

  dex::u4 SectionOffset() const {
    SLICER_CHECK(offset_ > 0 && offset_ % 4 == 0);
    return ItemsCount() > 0 ? offset_ : 0;
  }

  dex::u4 AbsoluteOffset(dex::u4 itemOffset) const {
    SLICER_CHECK(offset_ > 0);
    SLICER_CHECK(itemOffset < size());
    return offset_ + itemOffset;
  }

  // TODO: return absolute offsets?
  dex::u4 AddItem(dex::u4 alignment = 1) {
    ++count_;
    Align(alignment);
    return size();
  }

  dex::u4 ItemsCount() const { return count_; }

  dex::u2 MapEntryType() const { return map_entry_type_; }

 private:
  dex::u4 offset_ = 0;
  dex::u4 count_ = 0;
  const dex::u2 map_entry_type_;
};

// A specialized container for an .dex index section
// (strings, types, fields, methods, ...)
template <class T>
class Index {
 public:
  Index(dex::u2 mapEntryType) : map_entry_type_(mapEntryType) {}
  ~Index() = default;

  Index(const Index&) = delete;
  Index& operator=(const Index&) = delete;

  dex::u4 Init(dex::u4 offset, dex::u4 count) {
    values_.reset(new T[count]);
    offset_ = offset;
    count_ = count;
    return size();
  }

  void Free() {
    values_.reset();
    offset_ = 0;
    count_ = 0;
  }

  dex::u4 SectionOffset() const {
    SLICER_CHECK(offset_ > 0 && offset_ % 4 == 0);
    return ItemsCount() > 0 ? offset_ : 0;
  }

  T* begin() { return values_.get(); }
  T* end() { return begin() + count_; }

  bool empty() const { return count_ == 0; }

  dex::u4 ItemsCount() const { return count_; }
  const T* data() const { return values_.get(); }
  dex::u4 size() const { return count_ * sizeof(T); }

  T& operator[](int i) {
    SLICER_CHECK(i >= 0 && i < count_);
    return values_[i];
  }

  dex::u2 MapEntryType() const { return map_entry_type_; }

 private:
  dex::u4 offset_ = 0;
  dex::u4 count_ = 0;
  std::unique_ptr<T[]> values_;
  const dex::u2 map_entry_type_;
};

// Creates an in-memory .dex image from a .dex IR
class Writer {
  // The container for the individual sections in a .dex image
  // (factored out from Writer for a more granular lifetime control)
  struct DexImage {
    DexImage()
        : string_ids(dex::kStringIdItem),
          type_ids(dex::kTypeIdItem),
          proto_ids(dex::kProtoIdItem),
          field_ids(dex::kFieldIdItem),
          method_ids(dex::kMethodIdItem),
          class_defs(dex::kClassDefItem),
          string_data(dex::kStringDataItem),
          type_lists(dex::kTypeList),
          debug_info(dex::kDebugInfoItem),
          encoded_arrays(dex::kEncodedArrayItem),
          code(dex::kCodeItem),
          class_data(dex::kClassDataItem),
          ann_directories(dex::kAnnotationsDirectoryItem),
          ann_set_ref_lists(dex::kAnnotationSetRefList),
          ann_sets(dex::kAnnotationSetItem),
          ann_items(dex::kAnnotationItem),
          map_list(dex::kMapList) {}

    Index<dex::StringId> string_ids;
    Index<dex::TypeId> type_ids;
    Index<dex::ProtoId> proto_ids;
    Index<dex::FieldId> field_ids;
    Index<dex::MethodId> method_ids;
    Index<dex::ClassDef> class_defs;

    Section string_data;
    Section type_lists;
    Section debug_info;
    Section encoded_arrays;
    Section code;
    Section class_data;
    Section ann_directories;
    Section ann_set_ref_lists;
    Section ann_sets;
    Section ann_items;
    Section map_list;
  };

 public:
  // interface for allocating the final in-memory image
  struct Allocator {
    virtual void* Allocate(size_t size) = 0;
    virtual void Free(void* ptr) = 0;
    virtual ~Allocator() = default;
  };

 public:
  Writer(std::shared_ptr<ir::DexFile> dex_ir) : dex_ir_(dex_ir) {}
  ~Writer() = default;

  Writer(const Writer&) = delete;
  Writer& operator=(const Writer&) = delete;

  // .dex image creation
  dex::u1* CreateImage(Allocator* allocator, size_t* new_image_size);

 private:
  // helpers for creating various .dex sections
  dex::u4 CreateStringDataSection(dex::u4 section_offset);
  dex::u4 CreateMapSection(dex::u4 section_offset);
  dex::u4 CreateAnnItemSection(dex::u4 section_offset);
  dex::u4 CreateAnnSetsSection(dex::u4 section_offset);
  dex::u4 CreateAnnSetRefListsSection(dex::u4 section_offset);
  dex::u4 CreateTypeListsSection(dex::u4 section_offset);
  dex::u4 CreateCodeItemSection(dex::u4 section_offset);
  dex::u4 CreateDebugInfoSection(dex::u4 section_offset);
  dex::u4 CreateClassDataSection(dex::u4 section_offset);
  dex::u4 CreateAnnDirectoriesSection(dex::u4 section_offset);
  dex::u4 CreateEncodedArrayItemSection(dex::u4 section_offset);

  // back-fill the indexes
  void FillTypes();
  void FillProtos();
  void FillFields();
  void FillMethods();
  void FillClassDefs();

  // helpers for writing .dex structures
  dex::u4 WriteTypeList(const std::vector<ir::Type*>& types);
  dex::u4 WriteAnnotationItem(const ir::Annotation* ir_annotation);
  dex::u4 WriteAnnotationSet(const ir::AnnotationSet* ir_annotation_set);
  dex::u4 WriteAnnotationSetRefList(const ir::AnnotationSetRefList* ir_annotation_set_ref_list);
  dex::u4 WriteClassAnnotations(const ir::Class* ir_class);
  dex::u4 WriteDebugInfo(const ir::DebugInfo* ir_debug_info);
  dex::u4 WriteCode(const ir::Code* ir_code);
  dex::u4 WriteClassData(const ir::Class* ir_class);
  dex::u4 WriteClassStaticValues(const ir::Class* ir_class);

  // Map indexes from the original .dex to the
  // corresponding index in the new image
  dex::u4 MapStringIndex(dex::u4 index) const;
  dex::u4 MapTypeIndex(dex::u4 index) const;
  dex::u4 MapFieldIndex(dex::u4 index) const;
  dex::u4 MapMethodIndex(dex::u4 index) const;

  // writing parts of a class definition
  void WriteInstructions(slicer::ArrayView<const dex::u2> instructions);
  void WriteTryBlocks(const ir::Code* ir_code);
  void WriteEncodedField(const ir::EncodedField* irEncodedField, dex::u4* base_index);
  void WriteEncodedMethod(const ir::EncodedMethod* irEncodedMethod, dex::u4* base_index);

  dex::u4 FilePointer(const ir::Node* ir_node) const;

 private:
  std::shared_ptr<ir::DexFile> dex_ir_;
  std::unique_ptr<DexImage> dex_;

  // CONSIDER: we can have multiple maps per IR node type
  //  (that's what the reader does)
  std::map<const ir::Node*, dex::u4> node_offset_;
};

}  // namespace dex
