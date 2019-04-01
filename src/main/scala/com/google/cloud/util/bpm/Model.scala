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

import com.google.cloud.{Identity, Policy, Role}

import scala.collection.mutable


object Model {
  case class RoleGrant(roleId: String, groupId: String)

  case class PolicyMembership(unexpected: Set[Identity], missing: Set[Identity])
  type Bindings = java.util.Map[Role, java.util.Set[Identity]]

  /** Used to model a bucket's IAM policies
    *
    * @param name GCS bucket name
    */
  case class BucketPolicy(name: String) {
    private val policy: mutable.Map[Role, mutable.Set[Identity]] = mutable.Map.empty

    def size(): Int = policy.foldLeft(0)(_ + _._2.size)

    def result(): Policy = {
      val p = Policy.newBuilder()
      import scala.collection.JavaConverters.{mapAsJavaMapConverter, setAsJavaSetConverter}
      val bindings: Bindings = policy.mapValues(_.asJava).asJava
      p.setBindings(bindings)
      p.build()
    }

    def grantRole(role: Role, identities: Set[Identity]): Unit = {
      if (policy.contains(role))
        policy(role) ++= identities
      else
        policy.update(role, mutable.Set.empty[Identity] ++ identities)
    }
  }


}
