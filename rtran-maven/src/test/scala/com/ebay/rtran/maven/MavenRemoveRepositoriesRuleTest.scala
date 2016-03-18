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


class MavenRemoveRepositoriesRuleTest extends FlatSpecLike with Matchers with BeforeAndAfterEach {
  val projectRoot = new File(getClass.getClassLoader.getResource("mvnproject").getFile)
  val destProjectRoot = new File(projectRoot.getParentFile, projectRoot.getName + "-bak")

  override def beforeEach = {
    FileUtils.deleteQuietly(destProjectRoot)
    FileUtils.copyDirectory(projectRoot, destProjectRoot)
  }

  "MavenRemoveRepositoriesRule" should "remove repository that matches given patterns" in {
    val ruleConfig = MavenRemoveRepositoriesRuleConfig(
      Set(
        ".*/content/repositories/releases[/]?",
        ".*/content/repositories/snapshots[/]?"
      )
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val rule = new MavenRemoveRepositoriesRule(ruleConfig)
    val model = provider create projectCtx
    provider save (rule transform model)

    val transformed = provider create projectCtx
    transformed.modules foreach { module =>
      module.pomModel.getRepositories.size should be (0)
    }
  }
}
