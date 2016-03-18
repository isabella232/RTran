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

import com.ebay.rtran.maven.util.MavenModelUtil
import MavenModelUtil.SimpleDependency
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfterEach, Matchers, FlatSpecLike}

import scala.collection.JavaConversions._


class MavenRemoveManagedDependenciesRuleTest extends FlatSpecLike with Matchers with BeforeAndAfterEach {
  val projectRoot = new File(getClass.getClassLoader.getResource("mvnproject").getFile)
  val destProjectRoot = new File(projectRoot.getParentFile, projectRoot.getName + "-bak")

  override def beforeEach = {
    FileUtils.deleteQuietly(destProjectRoot)
    FileUtils.copyDirectory(projectRoot, destProjectRoot)
  }

  "MavenRemoveManagedDependenciesRule" should "be able to remove managed dependencies" in {
    val ruleConfig = MavenRemoveManagedDependenciesRuleConfig(
      Set(SimpleDependency("org.eclipse.aether", "aether-spi"))
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenRemoveManagedDependenciesRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.parents.head
      .pomModel.getDependencyManagement.getDependencies.exists(_.getArtifactId == "aether-spi") should be (false)
  }

  "MavenRemoveManagedDependenciesRule" should "not remove managed dependencies that don't exist" in {
    val ruleConfig = MavenRemoveManagedDependenciesRuleConfig(
      Set(SimpleDependency("org.slf4j", "slf4j-api"))
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenRemoveManagedDependenciesRule(ruleConfig)
    val originalSize = model.parents.head
      .pomModel.getDependencyManagement.getDependencies.size
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.parents.head
      .pomModel.getDependencyManagement.getDependencies.size should be (originalSize)
  }

  "MavenRemoveManagedDependenciesRule" should "remove managed dependencies matches that match other condition" in {
    val ruleConfig = MavenRemoveManagedDependenciesRuleConfig(
      Set(SimpleDependency("org.eclipse.aether", "aether-spi", version = Some("1.0.2.v20150114")))
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenRemoveManagedDependenciesRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.parents.head
      .pomModel.getDependencyManagement.getDependencies.exists(_.getArtifactId == "aether-spi") should be (false)
  }

  "MavenRemoveManagedDependenciesRule" should "not remove managed dependencies if other condition doesn't match" in {
    val ruleConfig = MavenRemoveManagedDependenciesRuleConfig(
      Set(SimpleDependency("org.eclipse.aether", "aether-spi", version = Some("1.0.3.v20150114")))
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenRemoveManagedDependenciesRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.parents.head
      .pomModel.getDependencyManagement.getDependencies.exists(_.getArtifactId == "aether-spi") should be (true)
  }
}
