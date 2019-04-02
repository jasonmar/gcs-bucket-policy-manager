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
import java.util.NoSuchElementException

import com.google.cloud.util.bpm.Model._
import com.google.cloud.{Identity, Role}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

case class Config(ttl: Long = Util.DefaultTTL,
                  waitMs: Long = 100,
                  bucketsFile: File = new File("buckets.conf"),
                  groupsFile: File = new File("groups.conf"),
                  policiesFile: File = new File("policies.conf"),
                  rolesFile: File = new File("roles.conf"),
                  cacheFile: File = new File("policyCache.pb"),
                  adminIdentity: String = "",
                  keyFile: Option[File] = None,
                  merge: Boolean = false,
                  remove: Boolean = false) {
  import com.google.cloud.util.bpm.Config._
  val parser = ValidatingIdentityParser()
  def bucketGroups: Map[String,Set[String]] = readBucketGroups(bucketsFile)
  def groups: Map[String,Set[Identity]] = readGroups(groupsFile, parser)
  def policies: Map[String,Seq[RoleGrant]] = readPolicies(policiesFile)
  def roles: Map[String,Role] = readRoleDefs(rolesFile)
  def admin: Set[Identity] = parser.parseIdentity(adminIdentity).toSet
}

object Config {

  sealed trait ConfigLine
  case class Heading(value: String) extends ConfigLine
  case class Entry(values: Seq[String]) extends ConfigLine

  def stripComment(line: String): String = {
    val i = line.indexOf("#")
    if (i >= 0) {
      line.substring(0, i)
    } else line
  }

  def normalizeConfigLine(line: String): Seq[String] = {
    stripComment(line) // remove comment
      .trim // ignore leading and trailing whitespace
      .replaceAllLiterally("\t"," ") // handle tabs
      .split(' ')
      .filter(_.nonEmpty) // handle multiple spaces
  }

  def readConfigLine(line: String): Option[ConfigLine] = {
    line match {
      case s if s.startsWith("#") =>
        None
      case s if s.startsWith("[") && s.endsWith("]") && s.length > 5 =>
        Option(Heading(s.substring(1, s.length-1)))
      case s if s.nonEmpty =>
        Option(Entry(normalizeConfigLine(s)))
      case _ =>
        None
    }
  }

  def readRoleDefLine(line: String): Option[String] = {
    (line.trim, line.indexOf("#")) match {
      case (s,i) if i < 0 && s.nonEmpty =>
        Option(s)
      case (s,i) if i > 0 =>
        Option(s.substring(0, i).trim)
      case _ =>
        None
    }
  }

  /** This is a shared trait that parses configurations from a file
    *
    * @tparam T type of configuration item to read
    */
  trait ConfigIterator[T] extends Iterator[T] {
    protected var nextGroup: Option[T] = None

    /** Function to load nextGroup */
    protected def advance(): Unit

    override def next(): T = {
      if (nextGroup.isEmpty) advance()
      if (nextGroup.isDefined) {
        val result = nextGroup.get
        nextGroup = None
        result
      } else {
        throw new NoSuchElementException("next on empty iterator")
      }
    }

    override def hasNext: Boolean = {
      if (nextGroup.isDefined) {
        true
      } else {
        advance()
        nextGroup.isDefined
      }
    }
  }

  def parseRoleGrant(s: Seq[String]): RoleGrant = {
    RoleGrant(s.head, s(1))
  }

  trait IdentityParser {
    def parseIdentity(s: String): Option[Identity]
  }

  case class ValidatingIdentityParser(
    allowedDomains: Set[String] = Set.empty,
    allowedProjects: Set[String] = Set.empty
  ) extends IdentityParser {
    override def parseIdentity(s: String): Option[Identity] = {
      if (s.endsWith(".iam.gserviceaccount.com"))
        parseServiceAccount(s)
      else parseUser(s)
    }

    def validateServiceAccount(s: String): Boolean = {
      val atSymbol = s.indexOf("@")
      val dot = s.indexOf(".", atSymbol)
      val name = s.substring(0, atSymbol)
      val project = s.substring(atSymbol + 1, dot)

      val namingRules = name.length > 5 && project.length > 6
      if (allowedProjects.nonEmpty)
        allowedProjects.contains(project) && namingRules
      else namingRules
    }

    def validateUser(s: String): Boolean = {
      val atSymbol = s.indexOf("@")
      val name = s.substring(0, atSymbol)
      val domain = s.substring(atSymbol + 1, s.length)
      val namingRules = name.length > 0 && domain.length > 4
      if (allowedDomains.nonEmpty)
        allowedDomains.contains(domain)
      else namingRules
    }

    def parseUser(s: String): Option[Identity] = {
      if (validateUser(s))
        Option(Identity.user(s))
      else None
    }

    def parseServiceAccount(s: String): Option[Identity] = {

      if (validateServiceAccount(s))
        Option(Identity.serviceAccount(s))
      else None
    }
  }

  class BucketGroupPolicyIterator(lines: Iterator[ConfigLine]) extends ConfigIterator[(String,Seq[RoleGrant])] {
    private var id: Option[String] = None
    private var nextId: Option[String] = None
    override def advance(): Unit = {
      if (lines.hasNext) {
        var continue = true
        val buf = ListBuffer.empty[RoleGrant]
        while (continue && lines.hasNext) {
          lines.next() match {
            case Heading(v) =>
              if (id.isEmpty) {
                id = Option(v)
              } else {
                nextId = Option(v)
                continue = false
              }
            case Entry(v) if v.length == 2 =>
              buf.append(parseRoleGrant(v))
            case _ =>
          }
        }
        if (id.isDefined && buf.nonEmpty) {
          nextGroup = Option((id.get, buf.result()))
          id = nextId
        }
      }
    }
  }

  class IdentitiesIterator(lines: Iterator[ConfigLine], parser: IdentityParser) extends ConfigIterator[(String,Set[Identity])] {
    private var id: Option[String] = None
    private var nextId: Option[String] = None
    override def advance(): Unit = {
      if (lines.hasNext) {
        var continue = true
        val buf = mutable.Set.empty[Identity]
        while (continue && lines.hasNext) {
          lines.next() match {
            case Heading(v) =>
              if (id.isEmpty) {
                id = Option(v)
              } else {
                nextId = Option(v)
                continue = false
              }
            case Entry(v) if v.nonEmpty =>
              parser.parseIdentity(v.head).foreach(buf.add)
            case _ =>
          }
        }
        if (id.isDefined && buf.nonEmpty){
          nextGroup = Option((id.get, buf.toSet))
          id = nextId
        }
      }
    }
  }

  class BucketsIterator(lines: Iterator[ConfigLine]) extends ConfigIterator[(String,Set[String])] {
    private var id: Option[String] = None
    private var nextId: Option[String] = None
    override def advance(): Unit = {
      if (lines.hasNext) {
        var continue = true
        val buf = mutable.Set.empty[String]
        while (continue && lines.hasNext) {
          lines.next() match {
            case Heading(v) =>
              if (id.isEmpty) {
                id = Option(v)
              } else {
                nextId = Option(v)
                continue = false
              }
            case Entry(v) if v.nonEmpty =>
              buf.add(v.head)
            case _ =>
              continue = false
          }
        }
        if (id.isDefined && buf.nonEmpty) {
          nextGroup = Option((id.get, buf.result().toSet))
          id = nextId
        }
      }
    }
  }

  def lines(f: java.io.File): Iterator[String] = {
    Source.fromFile(f).getLines()
  }

  def readBucketGroups(f: java.io.File): Map[String,Set[String]] = {
    readBucketGroups(lines(f)).toMap
  }

  def readBucketGroups(lines: Iterator[String]): Seq[(String,Set[String])] = {
    val configLines = lines.flatMap(readConfigLine)
    new BucketsIterator(configLines).toArray.toSeq
  }

  def readGroups(f: java.io.File, parser: IdentityParser): Map[String, Set[Identity]] = {
    readGroups(lines(f), parser).toMap
  }

  def readGroups(lines: Iterator[String], parser: IdentityParser): Seq[(String, Set[Identity])] = {
    val configLines = lines.flatMap(readConfigLine)
    new IdentitiesIterator(configLines, parser).toArray.toSeq
  }

  def readPolicies(f: java.io.File): Map[String,Seq[RoleGrant]] = {
    readPolicies(lines(f)).toMap
  }

  def readPolicies(lines: Iterator[String]): Seq[(String,Seq[RoleGrant])] = {
    val configLines = lines.flatMap(readConfigLine)
    new BucketGroupPolicyIterator(configLines).toArray.toSeq
  }

  def parseGCSRole(line: String): Option[(String,Role)] = {
    val fields = line
      .replaceAllLiterally("\t", " ")
      .split(' ')
      .filter(_.nonEmpty)
    if (fields.length == 3) {
      val role = Role.of(s"projects/${fields(1)}/roles/${fields(2)}")
      Option((fields(0), role))
    } else None
  }

  def readRoleDefs(f: java.io.File): Map[String,Role] = {
    readRoleDefs(lines(f)).toMap
  }

  def readRoleDefs(lines: Iterator[String]): Seq[(String,Role)] = {
    val configLines = lines.flatMap(readRoleDefLine)
    configLines.flatMap(parseGCSRole).toArray.toSeq
  }

  def requireFiles(files: java.io.File*): Unit = {
    var missing = false
    for (f <- files) {
      if (!f.exists()) {
        missing = true
        System.err.println(s"$f not found")
      }
    }
    if (missing)
      System.exit(1)
  }
}
