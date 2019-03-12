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


class MavenAddManagedDependenciesRuleTest extends FlatSpecLike with Matchers with BeforeAndAfterEach {

  val projectRoot = new File(getClass.getClassLoader.getResource("mvnproject").getFile)
  val destProjectRoot = new File(projectRoot.getParentFile, projectRoot.getName + "-bak")

  override def beforeEach = {
    FileUtils.deleteQuietly(destProjectRoot)
    FileUtils.copyDirectory(projectRoot, destProjectRoot)
  }

  "MavenAddManagedDependenciesRule" should "be able to add dependencies to dependency management" in {
    val ruleConfig = MavenAddManagedDependenciesRuleConfig(
      Set(
        SimpleDependency("org.slf4j", "slf4j-api", Some("1.7.12")),
        SimpleDependency("com.typesafe.akka", "akka-actor_2.11", Some("2.3.9"))
      )
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx
    val rule = new MavenAddManagedDependenciesRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    val parent = transformed.parents.head
    val dm1 = parent.managedDependencies.values.find(_.getArtifactId == "slf4j-api")
    dm1 should not be None
    dm1.get.getVersion should be ("1.7.12")
    val dm2 = parent.managedDependencies.values.find(_.getArtifactId == "akka-actor_2.11")
    dm2 should not be None
    dm2.get.getVersion should be ("2.4.17")
  }
}
