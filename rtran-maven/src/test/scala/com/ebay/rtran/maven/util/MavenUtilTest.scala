/*
 * Copyright (c) 2016 eBay Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ebay.rtran.maven.util

import java.io.FileReader

import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.{model => maven}
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.scalatest.{FlatSpecLike, Matchers}

import scala.collection.JavaConversions._


class MavenUtilTest extends FlatSpecLike with Matchers {

  import MavenUtil._

  "MavenUtil" should "be able resolve an artifact" in {
    val artifact = MavenUtil.resolveArtifact(new DefaultArtifact("org.springframework:spring-parent:pom:3.1.4.RELEASE"))
    val model = new MavenXpp3Reader().read(new FileReader(artifact.getFile))
    model.getDependencyManagement.getDependencies.size should not be 0
  }

  "MavenUtil" should "be able to get all dependencies for an artifact" in {
    val dependencies = MavenUtil.allDependencies(new DefaultArtifact("com.typesafe.akka:akka-remote_2.11:jar:2.3.12"))
    dependencies.size should not be 0
    // com.typesafe:config:jar:1.2.1 is transitive dependency for com.typesafe.akka:akka-actor_2.11:jar:2.3.12
    dependencies exists (_.getArtifactId == "config") should be (true)
  }

  "MavenUtil" should "be able to get all release versions" in {
    val versions = MavenUtil.findAvailableVersions("org.springframework", "spring-parent")
    versions.size should not be 0
    versions forall {version => !version.contains("SNAPSHOT")} should be (true)
  }

  "MavenUtil" should "be able to get all snapshot versions" in {
    val versions = MavenUtil.findAvailableVersions("org.springframework", "spring-parent", snapshot = true)
    versions.size should be (0)
  }

  "MavenUtil" should "be able to get all micro release versions" in {
    val versions = MavenUtil.findAvailableVersions("org.springframework", "spring-parent", "3.1")
    versions.size should not be 0
    versions foreach (_.startsWith("3.1") should be (true))
    versions forall {version => !version.contains("SNAPSHOT")} should be (true)
  }

  "MavenUtil" should "be able to get all micro snapshot versions" in {
    val versions = MavenUtil.findAvailableVersions("org.springframework", "spring-parent", "3.1", snapshot = true)
    versions.size should be (0)
  }

  "MavenUtil" should "be able to convert maven.model.Dependency to aether.graph.dependency" in {
    val mavenDependency = new maven.Dependency
    mavenDependency.setArtifactId("spring-parent")
    mavenDependency.setGroupId("org.springframework")
    mavenDependency.setVersion("3.1.4.RELEASE")
    val aetherDependency: Dependency = mavenDependency
    aetherDependency.getArtifact.getArtifactId should be (mavenDependency.getArtifactId)
    aetherDependency.getArtifact.getGroupId should be (mavenDependency.getGroupId)
    aetherDependency.getArtifact.getVersion should be (mavenDependency.getVersion)
  }

}
