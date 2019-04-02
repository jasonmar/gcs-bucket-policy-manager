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

import java.io.File

object OptionParser extends scopt.OptionParser[Config]("bpm") {
  head("bpm", "0.1")

  opt[String]('a', "adminIdentity")
    .required()
    .valueName("<value>")
    .action((x, c) => c.copy(adminIdentity = x))
    .text("REQUIRED email of admin user or service account")

  opt[File]('k', "keyFile")
    .valueName("<file>")
    .action( (x, c) => c.copy(keyFile = Option(x)) )
    .text("(optional) path to GoogleCredentials json key file")

  opt[File]('b', "buckets")
    .valueName("<file>")
    .action( (x, c) => c.copy(bucketsFile = x) )
    .text("(optional) path to buckets.conf")

  opt[File]('g', "groups")
    .valueName("<file>")
    .action( (x, c) => c.copy(groupsFile = x) )
    .text("(optional) path to groups.conf")

  opt[File]('p', "policies")
    .valueName("<file>")
    .action( (x, c) => c.copy(policiesFile = x) )
    .text("(optional) path to policies.conf")

  opt[File]('r', "roles")
    .valueName("<file>")
    .action( (x, c) => c.copy(rolesFile = x) )
    .text("(optional) path to roles.conf")

  opt[File]('c', "cache")
    .valueName("<file>")
    .action( (x, c) => c.copy(cacheFile = x) )
    .text("(optional) path to policyCache.pb")

  opt[Long]('t', "ttl")
    .action((x, c) => c.copy(ttl = x))
    .text("(optional) cache ttl in milliseconds; default 1 week")

  opt[Long]('w', "wait")
    .action((x, c) => c.copy(waitMs = x))
    .text("(optional) wait time between requests in milliseconds; default 100ms")

  opt[Boolean]('m', "merge")
    .action((x, c) => c.copy(merge = x))
    .text("(optional) merge with existing policy; default false")

  opt[Boolean]('e', "remove")
    .action((x, c) => c.copy(remove = x))
    .text("(optional) remove roles from existing policy during merge; default true")

  help("help")
    .text("prints this usage text")

  note("running without arguments will load configuration from default location and overwrite any existing bucket IAM policies with the generated policies")
}
