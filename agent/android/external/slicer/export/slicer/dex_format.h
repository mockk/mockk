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

#include <stdint.h>
#include <string>

// Definitions for .dex file format structures and helpers.
//
// The names for the structures and fields follows the specification:
// https://source.android.com/devices/tech/dalvik/dex-format.html

namespace dex {

// These match the definitions in the VM specification
typedef uint8_t u1;
typedef uint16_t u2;
typedef uint32_t u4;
typedef uint64_t u8;
typedef int8_t s1;
typedef int16_t s2;
typedef int32_t s4;
typedef int64_t s8;

// General constants
constexpr u4 kEndianConstant = 0x12345678;
constexpr u4 kNoIndex = 0xffffffff;
constexpr u4 kSHA1DigestLen = 20;

// Annotation visibility
constexpr u1 kVisibilityBuild   = 0x00;
constexpr u1 kVisibilityRuntime = 0x01;
constexpr u1 kVisibilitySystem  = 0x02;

// Special visibility: encoded_annotation, not annotation_item
constexpr u1 kVisibilityEncoded = 0xff;

// encoded_value types
constexpr u1 kEncodedByte           = 0x00;
constexpr u1 kEncodedShort          = 0x02;
constexpr u1 kEncodedChar           = 0x03;
constexpr u1 kEncodedInt            = 0x04;
constexpr u1 kEncodedLong           = 0x06;
constexpr u1 kEncodedFloat          = 0x10;
constexpr u1 kEncodedDouble         = 0x11;
constexpr u1 kEncodedString         = 0x17;
constexpr u1 kEncodedType           = 0x18;
constexpr u1 kEncodedField          = 0x19;
constexpr u1 kEncodedMethod         = 0x1a;
constexpr u1 kEncodedEnum           = 0x1b;
constexpr u1 kEncodedArray          = 0x1c;
constexpr u1 kEncodedAnnotation     = 0x1d;
constexpr u1 kEncodedNull           = 0x1e;
constexpr u1 kEncodedBoolean        = 0x1f;

// encoded_value header
constexpr u1 kEncodedValueTypeMask  = 0x1f;
constexpr u1 kEncodedValueArgShift  = 5;

// access_flags
constexpr u4 kAccPublic                 = 0x0001;     // class, field, method, ic
constexpr u4 kAccPrivate                = 0x0002;     // field, method, ic
constexpr u4 kAccProtected              = 0x0004;     // field, method, ic
constexpr u4 kAccStatic                 = 0x0008;     // field, method, ic
constexpr u4 kAccFinal                  = 0x0010;     // class, field, method, ic
constexpr u4 kAccSynchronized           = 0x0020;     // method (only allowed on natives)
constexpr u4 kAccSuper                  = 0x0020;     // class (not used in dex)
constexpr u4 kAccVolatile               = 0x0040;     // field
constexpr u4 kAccBridge                 = 0x0040;     // method
constexpr u4 kAccTransient              = 0x0080;     // field
constexpr u4 kAccVarargs                = 0x0080;     // method
constexpr u4 kAccNative                 = 0x0100;     // method
constexpr u4 kAccInterface              = 0x0200;     // class, ic
constexpr u4 kAccAbstract               = 0x0400;     // class, method, ic
constexpr u4 kAccStrict                 = 0x0800;     // method
constexpr u4 kAccSynthetic              = 0x1000;     // class, field, method, ic
constexpr u4 kAccAnnotation             = 0x2000;     // class, ic
constexpr u4 kAccEnum                   = 0x4000;     // class, field, ic
constexpr u4 kAccConstructor            = 0x00010000; // method (dex only) <(cl)init>
constexpr u4 kAccDeclaredSynchronized   = 0x00020000; // method (dex only)

// map_item type codes
constexpr u2 kHeaderItem                = 0x0000;
constexpr u2 kStringIdItem              = 0x0001;
constexpr u2 kTypeIdItem                = 0x0002;
constexpr u2 kProtoIdItem               = 0x0003;
constexpr u2 kFieldIdItem               = 0x0004;
constexpr u2 kMethodIdItem              = 0x0005;
constexpr u2 kClassDefItem              = 0x0006;
constexpr u2 kMapList                   = 0x1000;
constexpr u2 kTypeList                  = 0x1001;
constexpr u2 kAnnotationSetRefList      = 0x1002;
constexpr u2 kAnnotationSetItem         = 0x1003;
constexpr u2 kClassDataItem             = 0x2000;
constexpr u2 kCodeItem                  = 0x2001;
constexpr u2 kStringDataItem            = 0x2002;
constexpr u2 kDebugInfoItem             = 0x2003;
constexpr u2 kAnnotationItem            = 0x2004;
constexpr u2 kEncodedArrayItem          = 0x2005;
constexpr u2 kAnnotationsDirectoryItem  = 0x2006;

// debug info opcodes
constexpr u1 DBG_END_SEQUENCE           = 0x00;
constexpr u1 DBG_ADVANCE_PC             = 0x01;
constexpr u1 DBG_ADVANCE_LINE           = 0x02;
constexpr u1 DBG_START_LOCAL            = 0x03;
constexpr u1 DBG_START_LOCAL_EXTENDED   = 0x04;
constexpr u1 DBG_END_LOCAL              = 0x05;
constexpr u1 DBG_RESTART_LOCAL          = 0x06;
constexpr u1 DBG_SET_PROLOGUE_END       = 0x07;
constexpr u1 DBG_SET_EPILOGUE_BEGIN     = 0x08;
constexpr u1 DBG_SET_FILE               = 0x09;
constexpr u1 DBG_FIRST_SPECIAL          = 0x0a;

// special debug info values
constexpr int DBG_LINE_BASE = -4;
constexpr int DBG_LINE_RANGE = 15;

// "header_item"
struct Header {
  u1 magic[8];
  u4 checksum;
  u1 signature[kSHA1DigestLen];
  u4 file_size;
  u4 header_size;
  u4 endian_tag;
  u4 link_size;
  u4 link_off;
  u4 map_off;
  u4 string_ids_size;
  u4 string_ids_off;
  u4 type_ids_size;
  u4 type_ids_off;
  u4 proto_ids_size;
  u4 proto_ids_off;
  u4 field_ids_size;
  u4 field_ids_off;
  u4 method_ids_size;
  u4 method_ids_off;
  u4 class_defs_size;
  u4 class_defs_off;
  u4 data_size;
  u4 data_off;
};

// "map_item"
struct MapItem {
  u2 type;
  u2 unused;
  u4 size;
  u4 offset;
};

// "map_list"
struct MapList {
  u4 size;
  MapItem list[];
};

// "string_id_item"
struct StringId {
  u4 string_data_off;
};

// "type_id_item"
struct TypeId {
  u4 descriptor_idx;
};

// "field_id_item"
struct FieldId {
  u2 class_idx;
  u2 type_idx;
  u4 name_idx;
};

// "method_id_item"
struct MethodId {
  u2 class_idx;
  u2 proto_idx;
  u4 name_idx;
};

// "proto_id_item"
struct ProtoId {
  u4 shorty_idx;
  u4 return_type_idx;
  u4 parameters_off;
};

// "class_def_item"
struct ClassDef {
  u4 class_idx;
  u4 access_flags;
  u4 superclass_idx;
  u4 interfaces_off;
  u4 source_file_idx;
  u4 annotations_off;
  u4 class_data_off;
  u4 static_values_off;
};

// "type_item"
struct TypeItem {
  u2 type_idx;
};

// "type_list"
struct TypeList {
  u4 size;
  TypeItem list[];
};

// "code_item"
struct Code {
  u2 registers_size;
  u2 ins_size;
  u2 outs_size;
  u2 tries_size;
  u4 debug_info_off;
  u4 insns_size;
  u2 insns[];
  // followed by optional u2 padding
  // followed by try_item[tries_size]
  // followed by uleb128 handlersSize
  // followed by catch_handler_item[handlersSize]
};

// "try_item"
struct TryBlock {
  u4 start_addr;
  u2 insn_count;
  u2 handler_off;
};

// "annotations_directory_item"
struct AnnotationsDirectoryItem {
  u4 class_annotations_off;
  u4 fields_size;
  u4 methods_size;
  u4 parameters_size;
  // followed by FieldAnnotationsItem[fields_size]
  // followed by MethodAnnotationsItem[methods_size]
  // followed by ParameterAnnotationsItem[parameters_size]
};

// "field_annotations_item"
struct FieldAnnotationsItem {
  u4 field_idx;
  u4 annotations_off;
};

// "method_annotations_item"
struct MethodAnnotationsItem {
  u4 method_idx;
  u4 annotations_off;
};

// "parameter_annotations_item"
struct ParameterAnnotationsItem {
  u4 method_idx;
  u4 annotations_off;
};

// "annotation_set_ref_item"
struct AnnotationSetRefItem {
  u4 annotations_off;
};

// "annotation_set_ref_list"
struct AnnotationSetRefList {
  u4 size;
  AnnotationSetRefItem list[];
};

// "annotation_set_item"
struct AnnotationSetItem {
  u4 size;
  u4 entries[];
};

// "annotation_item"
struct AnnotationItem {
  u1 visibility;
  u1 annotation[];
};

// Compute DEX checksum
u4 ComputeChecksum(const Header* header);

// Converts a type descriptor to a human-readable declaration
std::string DescriptorToDecl(const char* descriptor);

// Converts a type descriptor to the equivalent shorty type descriptor
char DescriptorToShorty(const char* descriptor);

}  // namespace dex
