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

#include <cstdint>
#include <memory>
#include <vector>

namespace slicer {

// A specialized Key -> T* map (note that, unlike std:: containers, the values
// are always pointers here, and we don't explicitly store the lookup keys)
//
// Implemented as an incrementally resizable hash table: we split the logical hash table
// into two internal fixed size tables, the "full table" and a "insertion table".
// When the insertion table overflows, we allocate a larger hashtable to replace
// it and "insertion table" becomes the "full table" (the old "full table" is
// rehashed into the new hash table)
//
// Similar to open addressing hash tables, all the buckets are a single,
// contiguous array. But this table is growing and the collisions are still handled
// as chains (using indexes instead of pointers).
//
// The result is faster than std::unordered_map and uses ~25% of
// the memory used by std::unordered_map<const char*, String*>
//
// The Hash template argument is a type which must implement:
//   1. hash function   : uint32_t Hash(const Key& key)
//   2. key compare     : bool Compare(const Key& key, T* value)
//   3. key extraction  : Key GetKey(T* value)
//   4. copy semantics
//
template<class Key, class T, class Hash>
class HashTable {
 private:
  // the index type inside the bucket array
  using Index = uint32_t;

  static constexpr Index kInitialHashBuckets = (1 << 7) - 1;
  static constexpr Index kAvgChainLength = 2;
  static constexpr Index kInvalidIndex = static_cast<Index>(-1);
  static constexpr double kResizeFactor = 1.6;

  struct __attribute__((packed)) Bucket {
    T* value = nullptr;
    Index next = kInvalidIndex;
  };

  class Partition {
   public:
    Partition(Index size, const Hash& hasher);
    bool Insert(T* value);
    T* Lookup(const Key& key, uint32_t hash_value) const;
    Index HashBuckets() const { return hash_buckets_; }
    void InsertAll(const Partition& src);
    void PrintStats(const char* name, bool verbose);

   private:
    std::vector<Bucket> buckets_;
    const Index hash_buckets_;
    Hash hasher_;
  };

 public:
  explicit HashTable(const Hash& hasher = Hash()) : hasher_(hasher) {
    // we start with full_table_ == nullptr
    insertion_table_.reset(new Partition(kInitialHashBuckets, hasher_));
  }

  ~HashTable() = default;

  // No move or copy semantics
  HashTable(const HashTable&) = delete;
  HashTable& operator=(const HashTable&) = delete;

  // Insert a new, non-nullptr T* into the hash table
  // (we only store unique values so the new value must
  // not be in the table already)
  void Insert(T* value);

  // Lookup an existing value
  // (returns nullptr if the value is not found)
  T* Lookup(const Key& key) const;

  void PrintStats(const char* name, bool verbose);

 private:
  std::unique_ptr<Partition> full_table_;
  std::unique_ptr<Partition> insertion_table_;
  Hash hasher_;
};

template<class Key, class T, class Hash>
HashTable<Key, T, Hash>::Partition::Partition(Index size, const Hash& hasher)
    : hash_buckets_(size), hasher_(hasher) {
  // allocate space for the hash buckets + avg chain length
  buckets_.reserve(hash_buckets_ * kAvgChainLength);
  buckets_.resize(hash_buckets_);
}

// Similar to the "cellar" version of coalesced hashing,
// the buckets array is divided into a fixed set of entries
// addressable by the hash value [0 .. hash_buckets_) and
// extra buckets for the collision chains [hash_buckets_, buckets_.size())
// Unlike coalesced hashing, our "cellar" is growing so we don't actually
// have to coalesce any chains.
//
// Returns true if the insertion succeeded, false if the table overflows
// (we never insert more than the pre-reserved capacity)
//
template<class Key, class T, class Hash>
bool HashTable<Key, T, Hash>::Partition::Insert(T* value) {
  SLICER_CHECK_NE(value, nullptr);
  // overflow?
  if (buckets_.size() + 1 > buckets_.capacity()) {
    return false;
  }
  auto key = hasher_.GetKey(value);
  Index bucket_index = hasher_.Hash(key) % hash_buckets_;
  if (buckets_[bucket_index].value == nullptr) {
    buckets_[bucket_index].value = value;
  } else {
    Bucket new_bucket = {};
    new_bucket.value = value;
    new_bucket.next = buckets_[bucket_index].next;
    buckets_[bucket_index].next = buckets_.size();
    buckets_.push_back(new_bucket);
  }
  return true;
}

template<class Key, class T, class Hash>
T* HashTable<Key, T, Hash>::Partition::Lookup(const Key& key, uint32_t hash_value) const {
  assert(hash_value == hasher_.Hash(key));
  Index bucket_index = hash_value % hash_buckets_;
  for (Index index = bucket_index; index != kInvalidIndex; index = buckets_[index].next) {
    auto value = buckets_[index].value;
    if (value == nullptr) {
      assert(index < hash_buckets_);
      break;
    } else if (hasher_.Compare(key, value)) {
      return value;
    }
  }
  return nullptr;
}

template<class Key, class T, class Hash>
void HashTable<Key, T, Hash>::Partition::InsertAll(const Partition& src) {
  for (const auto& bucket : src.buckets_) {
    if (bucket.value != nullptr) {
      SLICER_CHECK(Insert(bucket.value));
    }
  }
}

// Try to insert into the "insertion table". If that overflows,
// we allocate a new, larger hash table, move "full table" value to it
// and "insertion table" becomes the new "full table".
template<class Key, class T, class Hash>
void HashTable<Key, T, Hash>::Insert(T* value) {
  assert(Lookup(hasher_.GetKey(value)) == nullptr);
  if (!insertion_table_->Insert(value)) {
    std::unique_ptr<Partition> new_hash_table(
        new Partition(insertion_table_->HashBuckets() * kResizeFactor, hasher_));
    if (full_table_) {
      new_hash_table->InsertAll(*full_table_);
    }
    SLICER_CHECK(new_hash_table->Insert(value));
    full_table_ = std::move(insertion_table_);
    insertion_table_ = std::move(new_hash_table);
  }
}

// First look into the "full table" and if the value is
// not found there look into the "insertion table" next
template<class Key, class T, class Hash>
T* HashTable<Key, T, Hash>::Lookup(const Key& key) const {
  auto hash_value = hasher_.Hash(key);
  if (full_table_) {
    auto value = full_table_->Lookup(key, hash_value);
    if (value != nullptr) {
      return value;
    }
  }
  return insertion_table_->Lookup(key, hash_value);
}

template<class Key, class T, class Hash>
void HashTable<Key, T, Hash>::Partition::PrintStats(const char* name, bool verbose) {
  int max_chain_length = 0;
  int sum_chain_length = 0;
  int used_buckets = 0;
  for (Index i = 0; i < hash_buckets_; ++i) {
    if (verbose) printf("%4d : ", i);
    if (buckets_[i].value != nullptr) {
      ++used_buckets;
      int chain_length = 0;
      for (Index ci = i; buckets_[ci].next != kInvalidIndex; ci = buckets_[ci].next) {
        SLICER_CHECK_NE(buckets_[ci].value, nullptr);
        ++chain_length;
        if (verbose) printf("*");
      }
      max_chain_length = std::max(max_chain_length, chain_length);
      sum_chain_length += chain_length;
    }
    if (verbose) printf("\n");
  }

  int avg_chain_length = used_buckets ? sum_chain_length / used_buckets : 0;

  printf("\nHash table partition (%s):\n", name);
  printf("  hash_buckets                   : %u\n", hash_buckets_);
  printf("  size/capacity                  : %zu / %zu\n", buckets_.size(), buckets_.capacity());
  printf("  used_buckets                   : %d\n", used_buckets);
  printf("  max_chain_length               : %d\n", max_chain_length);
  printf("  avg_chain_length               : %d\n", avg_chain_length);
}

template<class Key, class T, class Hash>
void HashTable<Key, T, Hash>::PrintStats(const char* name, bool verbose) {
  printf("\nHash table stats (%s)\n", name);
  if (full_table_) {
    full_table_->PrintStats("full_table", verbose);
  }
  insertion_table_->PrintStats("insertion_table", verbose);
}

}  // namespace slicer
