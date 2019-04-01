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

import com.google.cloud.util.bpm.Config.ValidatingIdentityParser
import com.google.cloud.util.bpm.Model._
import com.google.cloud.{Identity, Role}
import org.scalatest.FlatSpec

class ConfigSpec extends FlatSpec {
  "Config" should "parse service account" in {
    val input = "serviceaccount1@project.iam.gserviceaccount.com"
    val expected = Some(Identity.serviceAccount("serviceaccount1@project.iam.gserviceaccount.com"))
    val parser = ValidatingIdentityParser(allowedProjects = Set("project"))
    assert(parser.parseIdentity(input) == expected)
  }

  it should "parse user" in {
    val input = "user1@example.com"
    val expected = Some(Identity.user("user1@example.com"))
    val parser = ValidatingIdentityParser(allowedDomains = Set("example.com"))
    assert(parser.parseIdentity(input) == expected)
  }

  it should "parse role defs" in {
    val input = Seq(
      "# comment",
      "role1 project1 role1",
      "role2 project2 role2 # comment"
    )
    val expected = Seq(
      ("role1", Role.of("projects/project1/roles/role1")),
      ("role2", Role.of("projects/project2/roles/role2"))
    )
    assert(Config.readRoleDefs(input.iterator) == expected)
  }

  it should "parse policies" in {
    val input =
      """# comment
      |[buckets1]
      |role1 group1
      |role2 group2
      |
      |[buckets2]
      |role1 group3
      |role2 group4""".stripMargin
    val expected = Seq(
      ("buckets1",
        Seq(
          RoleGrant("role1", "group1"),
          RoleGrant("role2", "group2")
        )),
      ("buckets2",
        Seq(
          RoleGrant("role1", "group3"),
          RoleGrant("role2", "group4")
        ))
    )
    assert(Config.readPolicies(input.lines) == expected)
  }

  it should "parse buckets" in {
    val input =
      """# comment
        |[buckets1]
        |bucket1
        |bucket2
        |
        |[buckets2]
        |bucket3
        |bucket4""".stripMargin
    val expected = Seq(
      ("buckets1", Set("bucket1", "bucket2")),
      ("buckets2", Set("bucket3", "bucket4"))
    )
    assert(Config.readBucketGroups(input.lines) == expected)
  }

  it should "parse groups" in {
    val input =
      """# comment
        |[group1]
        |serviceaccount1@project1.iam.gserviceaccount.com
        |serviceaccount2@project2.iam.gserviceaccount.com
        |
        |[group2]
        |serviceaccount3@project3.iam.gserviceaccount.com
        |serviceaccount4@project4.iam.gserviceaccount.com""".stripMargin
    val expected = Seq(
      ("group1", Set(
        Identity.serviceAccount("serviceaccount1@project1.iam.gserviceaccount.com"),
        Identity.serviceAccount("serviceaccount2@project2.iam.gserviceaccount.com")
      )),
      ("group2", Set(
        Identity.serviceAccount("serviceaccount3@project3.iam.gserviceaccount.com"),
        Identity.serviceAccount("serviceaccount4@project4.iam.gserviceaccount.com")
      ))
    )
    val parser = ValidatingIdentityParser(allowedProjects = Set("project1","project2","project3","project4"))
    assert(Config.readGroups(input.lines, parser) == expected)
  }
}
