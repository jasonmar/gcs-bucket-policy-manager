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

  opt[File]('b', "buckets")
    .valueName("<file>")
    .action( (x, c) => c.copy(bucketsFile = x) )
    .text("buckets is an optional file property")

  opt[File]('g', "groups")
    .valueName("<file>")
    .action( (x, c) => c.copy(groupsFile = x) )
    .text("groups is an optional file property")

  opt[File]('p', "policies")
    .valueName("<file>")
    .action( (x, c) => c.copy(policiesFile = x) )
    .text("policies is an optional file property")

  opt[File]('r', "roles")
    .valueName("<file>")
    .action( (x, c) => c.copy(rolesFile = x) )
    .text("roles is an optional file property")

  opt[File]('c', "cache")
    .valueName("<file>")
    .action( (x, c) => c.copy(cacheFile = x) )
    .text("cache is an optional file property")

  opt[Long]('t', "ttl")
    .action((x, c) => c.copy(ttl = x))
    .text("cache ttl in milliseconds")

  opt[Long]('w', "wait")
    .action((x, c) => c.copy(waitMs = x))
    .text("wait time between requests in milliseconds")

  opt[Boolean]('m', "merge")
    .action((x, c) => c.copy(merge = x))
    .text("merge with existing policy")

  opt[Boolean]('e', "remove")
    .action((x, c) => c.copy(remove = x))
    .text("remove roles from existing policy during merge")

  help("help")
    .text("prints this usage text")

  note("running without arguments will load configuration from default location and overwrite any existing bucket IAM policies with the generated policies")
}
