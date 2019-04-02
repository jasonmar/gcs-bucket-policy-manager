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

import com.google.cloud.storage.StorageRoles
import com.google.cloud.util.bpm.Util.EnhancedStorage
import com.google.cloud.{Identity, Role}

object BPM {
  def main(args: Array[String]): Unit = {
    OptionParser.parse(args, Config()) match {
      case Some(config) =>
        Config.requireFiles(config.bucketsFile,
                            config.groupsFile,
                            config.policiesFile,
                            config.rolesFile)

        if (config.admin.isEmpty) {
          System.err.println("valid admin identity is required")
          System.exit(1)
        }

        val storage = EnhancedStorage(
          storage = Util.defaultClient(config.keyFile),
          cache = Util.readCache(config.cacheFile))

        applyPolicies(config, storage)

        Util.writeCache(config.cacheFile, storage.cache)

      case None =>
        System.err.println(s"Failed to parse args '${args.mkString(" ")}'")
        System.exit(1)
    }
  }

  def applyPolicies(config: Config, storage: EnhancedStorage, pause: Long = 100, ttl: Long = Util.DefaultTTL): Unit = {
    // Initialize shadow policy for GCS buckets
    val buckets = config.bucketGroups
    val policies = Util.shadowPolicy(buckets)

    // Resolve policies from configuration
    for ((group, roleGrants) <- config.policies) {

      // Expand role grants
      val expandedRoleGrants: Seq[(Role,Set[Identity])] = roleGrants.flatMap{grant =>
        // Resolve identities
        val identities = config.groups.getOrElse(grant.groupId, Set.empty)
        // Resolve role
        config.roles.get(grant.roleId)
          .map{role => (role, identities)}
      }

      // Expand buckets
      val groupPolicies = buckets.getOrElse(group, Set.empty)
        .flatMap(policies.get)

      // Update shadow policy on each bucket
      for (policy <- groupPolicies) {
        for ((role, identities) <- expandedRoleGrants) {
          policy.grantRole(role, identities)
        }
      }
    }

    // Apply policies
    for (policy <- Util.sortPolicies(policies)) {
      policy.grantRole(StorageRoles.admin(), config.admin)
      try {
        val policySize = policy.size()
        if (policySize > 1500){
          val msg = s"Policy size $policySize exceeds limit of 1500"
          throw new RuntimeException(msg)
        }
        val updated = storage.update(policy, ttl, config.merge, config.remove)
        if (updated.isDefined && pause > 0)
          Thread.sleep(pause)
      } catch {
        case e: Exception =>
          System.err.println(s"Failed to update policy on gs://${policy.name}: ${e.getMessage}")
          e.printStackTrace(System.err)
          if (pause > 0) Thread.sleep(pause)
      }
    }
  }
}
