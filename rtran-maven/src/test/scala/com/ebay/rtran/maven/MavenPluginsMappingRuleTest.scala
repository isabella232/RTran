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

package com.ebay.rtran.maven

import java.io.File

import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}

import scala.collection.JavaConversions._


class MavenPluginsMappingRuleTest extends FlatSpecLike with Matchers with BeforeAndAfterEach {
  val projectRoot = new File(getClass.getClassLoader.getResource("mvnproject").getFile)
  val destProjectRoot = new File(projectRoot.getParentFile, projectRoot.getName + "-bak")

  override def beforeEach = {
    FileUtils.deleteQuietly(destProjectRoot)
    FileUtils.copyDirectory(projectRoot, destProjectRoot)
  }

  "MavenPluginsMappingRule" should "be able to alter both plugins and managed plugins" in {
    val ruleConfig = MavenPluginsMappingRuleConfig(
      List(
        PluginMapping(
          SimplePlugin(Some("com.ebay.rtran.old"), "some-maven-plugin"),
          SimplePlugin(Some("com.ebay.rtran.new"), "some-maven-plugin")
        )
      )
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenPluginsMappingRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.parents.head
      .pomModel.getBuild.getPluginManagement.getPlugins
      .exists(_.getGroupId == "com.ebay.rtran.old") should be (false)

    transformed.parents.head
      .pomModel.getBuild.getPluginManagement.getPlugins
      .exists(_.getGroupId == "com.ebay.rtran.new") should be (true)

    transformed.parents.head
      .pomModel.getBuild.getPlugins
      .exists(_.getGroupId == "com.ebay.rtran.old") should be (false)

    transformed.parents.head
      .pomModel.getBuild.getPlugins
      .exists(_.getGroupId == "com.ebay.rtran.new") should be (true)
  }

  "MavenPluginsMappingRule" should "not alter plugins or managed plugins that don't exist" in {
    val ruleConfig = MavenPluginsMappingRuleConfig(
      List(
        PluginMapping(
          SimplePlugin(Some("com.ebay.rtran.old"), "non-exist"),
          SimplePlugin(Some("com.ebay.rtran.new"), "non-exist")
        )
      )
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenPluginsMappingRule(ruleConfig)
    val mpSize = model.parents.head
      .pomModel.getBuild.getPluginManagement.getPlugins.size
    val pluginSize = model.parents.head
      .pomModel.getBuild.getPlugins.size
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.parents.head
      .pomModel.getBuild.getPluginManagement.getPlugins.size should be (mpSize)

    transformed.parents.head
      .pomModel.getBuild.getPluginManagement.getPlugins
      .exists(_.getGroupId == "com.ebay.rtran.old") should be (true)

    transformed.parents.head
      .pomModel.getBuild.getPlugins.size should be (pluginSize)

    transformed.parents.head
      .pomModel.getBuild.getPlugins
      .exists(_.getGroupId == "com.ebay.rtran.old") should be (true)
  }
}
