/* Copyright 2019 Google LLC. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/
package com.google.cloud.util.bpm

import java.nio.charset.StandardCharsets

import com.google.cloud.util.bpm.Config.ValidatingIdentityParser
import com.google.cloud.util.bpm.Util.EnhancedStorage
import com.google.common.io.Resources
import org.scalatest.FlatSpec

class BPMSpec extends FlatSpec {
  def lines(r: String): Iterator[String] = {
    import scala.collection.JavaConverters.asScalaIteratorConverter
    Resources.readLines(Resources.getResource(r), StandardCharsets.UTF_8).iterator().asScala
  }

  "BPM" should "update bucket IAM policies" in {
    val parser = ValidatingIdentityParser()

    val config = new Config {
      override def bucketGroups = Config.readBucketGroups(lines("buckets.conf")).toMap
      override def groups = Config.readGroups(lines("groups.conf"), parser).toMap
      override def policies = Config.readPolicies(lines("policies.conf")).toMap
      override def roles = Config.readRoleDefs(lines("roles.conf")).toMap
    }

    Util.clearCache(config.cacheFile.getAbsolutePath)
    val mockStorage = new NoOpStorage

    val storage = EnhancedStorage(mockStorage, Util.readCache(config.cacheFile.getAbsolutePath))

    BPM.applyPolicies(config, storage, 0)
    val setCount = mockStorage.setCount
    val getCount = mockStorage.getCount
    val hitCount = storage.getHitCount
    val missCount = storage.getMissCount
    assert(setCount > 0)
    assert(getCount > 0)
    assert(hitCount == 0)
    assert(missCount > 0)

    // Save the cache
    Util.writeCache(config.cacheFile.getAbsolutePath, storage.cache)

    // Run again, everything should be cached
    BPM.applyPolicies(config, storage, 0)
    assert(mockStorage.setCount == setCount)
    assert(mockStorage.getCount == getCount)
    assert(storage.getHitCount == missCount)
    assert(storage.getMissCount == missCount)

    // Wait for ttl to expire so that cache will not be used
    Thread.sleep(100)
    BPM.applyPolicies(config, storage, 0, ttl = -1)
    assert(mockStorage.setCount == setCount * 2)
    assert(mockStorage.getCount == getCount * 2)
    assert(storage.getHitCount == missCount)
    assert(storage.getMissCount == missCount * 2)
  }
}
