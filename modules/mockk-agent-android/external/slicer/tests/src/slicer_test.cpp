/*
 * Copyright (C) 2021 The Android Open Source Project
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

#include <iostream>
#include <string>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "slicer/common.h"
#include "slicer/dex_bytecode.h"

static std::string customLoggerMsg;
static void testCustomLogger(const std::string& msg) {
  customLoggerMsg = msg;
}

TEST(Slicer, CustomLogger) {
  slicer::set_logger(testCustomLogger);
  slicer::_weakCheckFailed("expr1", 1, "file");
  std::string expected("\nSLICER_WEAK_CHECK failed [expr1] at file:1\n\n");
  ASSERT_EQ(customLoggerMsg, expected);
}

TEST(Slicer, OpcodeToStringStream) {
  std::stringstream ss;
  ss << dex::Opcode::OP_IF_GTZ;
  ASSERT_EQ("[0x3c] if-gtz", ss.str());
}

TEST(Slicer, KnownInstructionFormatToStringStream) {
  std::stringstream ss;
  ss << dex::InstructionFormat::k20bc;
  ASSERT_EQ("20bc", ss.str());
}

TEST(Slicer, UnknownInstructionFormatToStringStream) {
  std::stringstream ss;
  ss << static_cast<dex::InstructionFormat>(0xfe);
  ASSERT_EQ("[0xfe] Unknown", ss.str());
}