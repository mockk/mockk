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

#include "slicer/dex_format.h"

#include "slicer/common.h"

#include <sstream>
#include <cstdlib>
#include <zlib.h>

namespace dex {

// The expected format of the magic is dex\nXXX\0 where XXX are digits. We extract this value.
// Returns 0 if the version can not be parsed.
u4 Header::GetVersion(const void* magic) {
  const char* version = reinterpret_cast<const char*>(magic) + 4;
  return version[3] == '\0' ? strtol(version, nullptr, 10) : 0;
}

// Compute the DEX file checksum for a memory-mapped DEX file
u4 ComputeChecksum(const Header* header) {
  const u1* start = reinterpret_cast<const u1*>(header);

  uLong adler = adler32(0L, Z_NULL, 0);
  const int non_sum = sizeof(header->magic) + sizeof(header->checksum);

  return static_cast<u4>(
      adler32(adler, start + non_sum, header->file_size - non_sum));
}

// Returns the human-readable name for a primitive type
static const char* PrimitiveTypeName(char type_char) {
  switch (type_char) {
    case 'B': return "byte";
    case 'C': return "char";
    case 'D': return "double";
    case 'F': return "float";
    case 'I': return "int";
    case 'J': return "long";
    case 'S': return "short";
    case 'V': return "void";
    case 'Z': return "boolean";
  }
  SLICER_CHECK(!"unexpected type");
  return nullptr;
}

// Converts a type descriptor to human-readable "dotted" form.  For
// example, "Ljava/lang/String;" becomes "java.lang.String", and
// "[I" becomes "int[]".
std::string DescriptorToDecl(const char* descriptor) {
  std::stringstream ss;

  int array_dimensions = 0;
  while (*descriptor == '[') {
    ++array_dimensions;
    ++descriptor;
  }

  if (*descriptor == 'L') {
    for (++descriptor; *descriptor != ';'; ++descriptor) {
      SLICER_CHECK_NE(*descriptor, '\0');
      ss << (*descriptor == '/' ? '.' : *descriptor);
    }
  } else {
    ss << PrimitiveTypeName(*descriptor);
  }

  SLICER_CHECK_EQ(descriptor[1], '\0');

  // add the array brackets
  for (int i = 0; i < array_dimensions; ++i) {
    ss << "[]";
  }

  return ss.str();
}

// Converts a type descriptor to a single "shorty" char
// (ex. "LFoo;" and "[[I" become 'L', "I" stays 'I')
char DescriptorToShorty(const char* descriptor) {
  // skip array dimensions
  int array_dimensions = 0;
  while (*descriptor == '[') {
    ++array_dimensions;
    ++descriptor;
  }

  char short_descriptor = *descriptor;
  if (short_descriptor == 'L') {
    // skip the full class name
    for(; *descriptor && *descriptor != ';'; ++descriptor);
    SLICER_CHECK_EQ(*descriptor, ';');
  }

  SLICER_CHECK_EQ(descriptor[1], '\0');
  SLICER_CHECK(short_descriptor == 'L' || PrimitiveTypeName(short_descriptor) != nullptr);

  return array_dimensions > 0 ? 'L' : short_descriptor;
}

}  // namespace dex
