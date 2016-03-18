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
import com.ebay.rtran.maven.util.MavenModelUtil
import com.ebay.rtran.maven.util.MavenModelUtil.SimpleDependency
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}

import scala.collection.JavaConversions._


class MavenAddDependenciesRuleTest extends FlatSpecLike with Matchers with BeforeAndAfterEach {

  val projectRoot = new File(getClass.getClassLoader.getResource("mvnproject").getFile)
  val destProjectRoot = new File(projectRoot.getParentFile, projectRoot.getName + "-bak")

  override def beforeEach = {
    FileUtils.deleteQuietly(destProjectRoot)
    FileUtils.copyDirectory(projectRoot, destProjectRoot)
  }

  "MavenAddDependenciesRule" should "be able to add dependencies" in {
    val ruleConfig = MavenAddDependenciesRuleConfig(
      Set(
        SimpleDependency("org.slf4j", "slf4j-api"),
        SimpleDependency("org.slf4j", "slf4j-log4j12")
      )
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenAddDependenciesRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.modules foreach { module =>
      module.pomModel.getDependencies.exists(_.getArtifactId == "slf4j-api") should be (true)
      module.pomModel.getDependencies.exists(_.getArtifactId == "slf4j-log4j12") should be (true)
    }
  }

  "MavenAddDependenciesRule" should "not add dependencies that already exist" in {
    val ruleConfig = MavenAddDependenciesRuleConfig(
      Set(
        SimpleDependency("junit", "junit")
      )
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenAddDependenciesRule(ruleConfig)
    val originalSize = model.modules
      .find(_.pomModel.getPackaging == "pom")
      .map(_.pomModel.getDependencies.size)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.modules
      .find(_.pomModel.getPackaging == "pom")
      .map(_.pomModel.getDependencies.size) should be (originalSize)
    transformed.modules foreach { module =>
      module.pomModel.getDependencies.exists(_.getArtifactId == "junit") should be (true)
    }
  }
}
