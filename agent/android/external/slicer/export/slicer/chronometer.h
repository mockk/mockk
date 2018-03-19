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

#include <chrono>

namespace slicer {

// A very simple timing chronometer
class Chronometer {
  using Clock = std::chrono::high_resolution_clock;

 public:
  // elapsed time is in milliseconds
  Chronometer(double& elapsed, bool cumulative = false) :
              elapsed_(elapsed), cumulative_(cumulative) {
    start_time_ = Clock::now();
  }

  ~Chronometer() {
    Clock::time_point end_time = Clock::now();
    std::chrono::duration<double, std::milli> ms = end_time - start_time_;
    if (cumulative_) {
      elapsed_ += ms.count();
    } else {
      elapsed_ = ms.count();
    }
  }

  Chronometer(const Chronometer&) = delete;
  Chronometer& operator=(const Chronometer&) = delete;

 private:
  double& elapsed_;
  Clock::time_point start_time_;
  bool cumulative_;
};

} // namespace slicer

