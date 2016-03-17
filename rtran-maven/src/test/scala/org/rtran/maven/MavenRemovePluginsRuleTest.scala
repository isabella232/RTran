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

package org.rtran.maven

import java.io.File

import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}

import scala.collection.JavaConversions._


class MavenRemovePluginsRuleTest extends FlatSpecLike with Matchers with BeforeAndAfterEach {
  val projectRoot = new File(getClass.getClassLoader.getResource("mvnproject").getFile)
  val destProjectRoot = new File(projectRoot.getParentFile, projectRoot.getName + "-bak")

  override def beforeEach = {
    FileUtils.deleteQuietly(destProjectRoot)
    FileUtils.copyDirectory(projectRoot, destProjectRoot)
  }

  "MavenRemovePluginsRule" should "be able to remove both plugins and managed plugins" in {
    val ruleConfig = MavenRemoveManagedPluginsRuleConfig(
      Set(SimplePlugin(artifactId = "maven-source-plugin"))
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenRemovePluginsRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.parents.head
      .pomModel.getBuild.getPluginManagement.getPlugins
      .exists(_.getArtifactId == "maven-source-plugin") should be (false)

    transformed.parents.head
      .pomModel.getBuild.getPlugins
      .exists(_.getArtifactId == "maven-source-plugin") should be (false)
  }

  "MavenRemovePluginsRule" should "not remove plugins or managed plugins that don't exist" in {
    val ruleConfig = MavenRemoveManagedPluginsRuleConfig(
      Set(SimplePlugin(artifactId = "maven-surefire-plugin"))
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenRemovePluginsRule(ruleConfig)
    val mpSize = model.parents.head.pomModel.getBuild.getPluginManagement.getPlugins.size
    val pluginSize = model.parents.head.pomModel.getBuild.getPlugins.size
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.parents.head
      .pomModel.getBuild.getPluginManagement.getPlugins.size should be (mpSize)

    transformed.parents.head
      .pomModel.getBuild.getPlugins.size should be (pluginSize)
  }

  "MavenRemovePluginsRule" should "remove both plugins and managed plugins matches that match other condition" in {
    val ruleConfig = MavenRemoveManagedPluginsRuleConfig(
      Set(SimplePlugin(artifactId = "maven-source-plugin", version = Some("2.2.1")))
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenRemovePluginsRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.parents.head
      .pomModel.getBuild.getPluginManagement.getPlugins
      .exists(_.getArtifactId == "maven-source-plugin") should be (false)

    transformed.parents.head
      .pomModel.getBuild.getPlugins
      .exists(_.getArtifactId == "maven-source-plugin") should be (false)
  }

  "MavenRemoveManagedPluginsRule" should "not remove plugins or managed plugins if other condition doesn't match" in {
    val ruleConfig = MavenRemoveManagedPluginsRuleConfig(
      Set(SimplePlugin(artifactId = "maven-source-plugin", version = Some("2.2.0")))
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenRemovePluginsRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.parents.head
      .pomModel.getBuild.getPluginManagement.getPlugins
      .exists(_.getArtifactId == "maven-source-plugin") should be (true)

    transformed.parents.head
      .pomModel.getBuild.getPlugins
      .exists(_.getArtifactId == "maven-source-plugin") should be (true)
  }
}
