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
import org.rtran.maven.util.MavenModelUtil
import org.rtran.maven.util.MavenModelUtil.SimpleDependency
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}

import scala.collection.JavaConversions._


class MavenDependenciesMappingRuleTest extends FlatSpecLike with Matchers with BeforeAndAfterEach {
  val projectRoot = new File(getClass.getClassLoader.getResource("mvnproject").getFile)
  val destProjectRoot = new File(projectRoot.getParentFile, projectRoot.getName + "-bak")

  override def beforeEach = {
    FileUtils.deleteQuietly(destProjectRoot)
    FileUtils.copyDirectory(projectRoot, destProjectRoot)
  }

  "MavenDependenciesMappingRule" should "be able to alter dependencies according to mapping" in {
    val ruleConfig = MavenDependenciesMappingRuleConfig(
      Set(SimpleDependency("junit", "junit")),
      Set(SimpleDependency("org.slf4j", "slf4j-api"), SimpleDependency("org.slf4j", "slf4j-log4j12"))
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenDependenciesMappingRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.modules foreach { module =>
      module.pomModel.getDependencies.exists(_.getArtifactId == "junit") should be (false)
      module.pomModel.getDependencies.exists(_.getArtifactId == "slf4j-api") should be (true)
      module.pomModel.getDependencies.exists(_.getArtifactId == "slf4j-log4j12") should be (true)
    }
  }

  "MavenDependenciesMappingRule" should "not alter dependencies that don't exist" in {
    val ruleConfig = MavenDependenciesMappingRuleConfig(
      Set(SimpleDependency("org.slf4j", "slf4j-api")),
      Set(SimpleDependency("org.slf4j", "slf4j-log4j12"))
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenDependenciesMappingRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.modules foreach { module =>
      module.pomModel.getDependencies.exists(_.getArtifactId == "slf4j-api") should be (false)
      module.pomModel.getDependencies.exists(_.getArtifactId == "slf4j-log4j12") should be (false)
    }
  }

  "MavenDependenciesMappingRule" should "alter dependencies matches that match other condition" in {
    val ruleConfig = MavenDependenciesMappingRuleConfig(
      Set(SimpleDependency("junit", "junit", Some("4.9"))),
      Set(SimpleDependency("org.slf4j", "slf4j-api"), SimpleDependency("org.slf4j", "slf4j-log4j12"))
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenDependenciesMappingRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.modules foreach { module =>
      if (module.pomModel.getPackaging == "pom") {
        module.pomModel.getDependencies.exists(_.getArtifactId == "junit") should be (true)
      } else {
        module.pomModel.getDependencies.exists(_.getArtifactId == "junit") should be (false)
        module.pomModel.getDependencies.exists(_.getArtifactId == "slf4j-api") should be (true)
        module.pomModel.getDependencies.exists(_.getArtifactId == "slf4j-log4j12") should be (true)
      }
    }
  }

  "MavenDependenciesMappingRule" should "not alter dependencies if other condition doesn't match" in {
    val ruleConfig = MavenDependenciesMappingRuleConfig(
      Set(SimpleDependency("junit", "junit", scope = Some("compile"))),
      Set(SimpleDependency("org.slf4j", "slf4j-api"), SimpleDependency("org.slf4j", "slf4j-log4j12"))
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenDependenciesMappingRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.modules foreach { module =>
      module.pomModel.getDependencies.exists(_.getArtifactId == "junit") should be (true)
      module.pomModel.getDependencies.exists(_.getArtifactId == "slf4j-api") should be (false)
      module.pomModel.getDependencies.exists(_.getArtifactId == "slf4j-log4j12") should be (false)
    }
  }
}
