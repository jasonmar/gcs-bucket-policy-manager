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
import java.nio.file.{Files, Paths, StandardOpenOption}

import com.google.cloud.storage.{Storage, StorageOptions}
import com.google.cloud.util.bpm.Model.{Bindings, BucketPolicy, PolicyMembership}
import com.google.cloud.util.bpm.PolicyCache.Hash
import com.google.cloud.{Identity, Policy, Role}
import com.google.common.hash.Hashing

import scala.collection.JavaConverters.{asScalaIteratorConverter, asScalaSetConverter}

object Util {

  /** Calculates a SHA256 hash of a policy
    * Used to identify whether a policy has changed
    * @param policy IAM policy to be hashed
    * @return
    */
  def hash(policy: Policy): String = {
    hashBindings(policy.getBindings)
  }

  def hashBindings(bindings: Bindings): String = {
    val h = Hashing.sha256().newHasher()
    for (role <- sortedRoles(bindings)) {
      h.putString(role.getValue, StandardCharsets.UTF_8)
      for (identity <- sortedIdentities(bindings, role)) {
        h.putString(identity.strValue, StandardCharsets.UTF_8)
      }
    }
    h.hash().toString
  }

  /**
    *
    * @param l existing policy
    * @param r new policy
    * @param remove remove extra roles and identities from existing policy
    * @return
    */
  def mergePolicies(l: Policy, r: Policy, remove: Boolean = false): Policy = {
    val p = l.toBuilder

    val lRoles = l.getBindings.keySet().asScala
    val rRoles = r.getBindings.keySet().asScala

    val leftOnly = lRoles.diff(rRoles)
    val rightOnly = rRoles.diff(lRoles)
    val both = rRoles.diff(rightOnly)

    // add new roles
    for (role <- rightOnly) {
      val rIdentities = r.getBindings.get(role).asScala
      for (identity <- rIdentities) {
        p.addIdentity(role, identity)
      }
    }

    // merge common roles
    for (role <- both) {
      val li = l.getBindings.get(role).asScala
      val ri = r.getBindings.get(role).asScala
      for (identity <- ri.diff(li)) {
        p.addIdentity(role, identity)
      }
      if (remove) {
        for (identity <- li.diff(ri)) {
          p.removeIdentity(role, identity)
        }
      }
    }

    // remove roles
    if (remove) {
      for (role <- leftOnly) {
        p.removeRole(role)
      }
    }

    p.build()
  }

  def assertMembers(policy: Policy, role: Role, identities: Set[Identity]): PolicyMembership = {
    val bindings = Option(policy.getBindings.get(role))
      .map(_.asScala.toSet)
      .getOrElse(Set.empty)

    PolicyMembership(
      unexpected = bindings.diff(identities),
      missing = identities.diff(bindings)
    )
  }

  def shadowPolicy(bucketGroups: Map[String,Set[String]]): Map[String, BucketPolicy] = {
    bucketGroups.values.flatten
      .toArray.distinct
      .map(name => (name, BucketPolicy(name))).toMap
  }

  def initCache(policies: Iterable[BucketPolicy]): PolicyCache = {
    val c = PolicyCache.newBuilder()
    val t = System.currentTimeMillis()
    c.setTimestamp(t)
    for (policy <- policies) {
      val h = hash(policy.result())
      val cached = Hash.newBuilder()
        .setTimestamp(t)
        .setSha256(h)
        .build()
      c.putBucket(policy.name, cached)
    }
    c.build()
  }

  def clearCache(path: String): Unit = {
    import StandardOpenOption.{CREATE, SYNC, TRUNCATE_EXISTING, WRITE}
    Files.write(Paths.get(path), PolicyCache.getDefaultInstance.toByteArray, CREATE, WRITE, TRUNCATE_EXISTING, SYNC)
  }

  def writeCache(path: String, cache: EnhancedPolicyCache): Unit = {
    import StandardOpenOption.{CREATE, SYNC, TRUNCATE_EXISTING, WRITE}
    Files.write(Paths.get(path), cache.toByteArray, CREATE, WRITE, TRUNCATE_EXISTING, SYNC)
  }

  def readCache(path: String): EnhancedPolicyCache = {
    val p = Paths.get("policyCache.pb")
    val f = p.toFile
    if (f.exists() && f.isFile) {
      val cache = PolicyCache.parseFrom(Files.readAllBytes(p))
      new EnhancedPolicyCache(cache.toBuilder)
    } else new EnhancedPolicyCache(PolicyCache.newBuilder())
  }

  val DefaultTTL: Long = 1000 * 60 * 60 * 24 * 7 // One week

  class EnhancedPolicyCache(private val c: PolicyCache.Builder) {
    def toByteArray: Array[Byte] = c
      .setTimestamp(System.currentTimeMillis())
      .build().toByteArray

    def update(bucket: String, p: Policy, ttl: Long): Boolean = {
      val h = hash(p)
      val t = System.currentTimeMillis()
      val expiration = math.min(t, t - ttl)
      val cached = c.getBucketOrDefault(bucket, PolicyCache.Hash.getDefaultInstance)
      if (cached.getSha256 != h || cached.getTimestamp < expiration) {
        val updated = Hash.newBuilder()
          .setSha256(h)
          .setTimestamp(t)
          .build()
        c.putBucket(bucket, updated)
        true
      } else false
    }
  }

  def defaultClient(): Storage = {
    StorageOptions.getDefaultInstance.getService
  }

  case class EnhancedStorage(storage: Storage, cache: EnhancedPolicyCache) {
    private var cacheHit: Long = 0
    private var cacheMiss: Long = 0
    def getHitCount: Long = cacheHit
    def getMissCount: Long = cacheMiss

    def update(p: BucketPolicy, ttl: Long, merge: Boolean, remove: Boolean): Option[Policy] = {
      val newPolicy = p.result()
      // check if new policy differs from what's in the cache
      if (cache.update(p.name, newPolicy, ttl)) {
        cacheMiss += 1
        val existing = storage.getIamPolicy(p.name)
        if (hash(existing) != newPolicy.getEtag) {
          if (merge) {
            val merged = Util.mergePolicies(existing, newPolicy, remove)
            Option(storage.setIamPolicy(p.name, merged))
          } else Option(storage.setIamPolicy(p.name, newPolicy))
        } else None
      } else {
        cacheHit += 1
        None
      }
    }
  }

  object RoleOrdering extends Ordering[Role] {
    override def compare(x: Role, y: Role): Int = {
      if (x.getValue < y.getValue) -1
      else if (y.getValue > x.getValue) 1
      else 0
    }
  }

  object IdentityOrdering extends Ordering[Identity] {
    override def compare(x: Identity, y: Identity): Int = {
      if (x.strValue < y.strValue) -1
      else if (y.strValue > x.strValue) 1
      else 0
    }
  }

  object BucketPolicyOrdering extends Ordering[BucketPolicy] {
    override def compare(x: BucketPolicy, y: BucketPolicy): Int = {
      if (x.name < y.name) -1
      else if (y.name > x.name) 1
      else 0
    }
  }

  def sortPolicies(p: Map[String,BucketPolicy]): Seq[BucketPolicy] = {
    val a = p.values.toArray
    scala.util.Sorting.quickSort(a)(BucketPolicyOrdering)
    a
  }

  def sortedRoles(bindings: Bindings): Array[Role] = {
    val roles = bindings.keySet().iterator()
      .asScala.toArray
    scala.util.Sorting.quickSort(roles)(RoleOrdering)
    roles
  }

  def sortedIdentities(bindings: Bindings, role: Role): Array[Identity] = {
    val identities = bindings.get(role).iterator()
      .asScala.toArray
    scala.util.Sorting.quickSort(identities)(IdentityOrdering)
    identities
  }
}
