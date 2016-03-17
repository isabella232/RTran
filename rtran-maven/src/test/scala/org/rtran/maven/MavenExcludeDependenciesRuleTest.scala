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


class MavenExcludeDependenciesRuleTest extends FlatSpecLike with Matchers with BeforeAndAfterEach {

  val projectRoot = new File(getClass.getClassLoader.getResource("mvnproject").getFile)
  val destProjectRoot = new File(projectRoot.getParentFile, projectRoot.getName + "-bak")

  override def beforeEach = {
    FileUtils.deleteQuietly(destProjectRoot)
    FileUtils.copyDirectory(projectRoot, destProjectRoot)
  }

  "MavenExcludeDependenciesRule" should "exclude the dependencies if they are used transitively" in {
    val ruleConfig = MavenExcludeDependenciesRuleConfig(
      Set(SimpleExclusion("org.springframework", "spring-asm"))
    )
    val projectCtx = new MavenProjectCtx(destProjectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider create projectCtx

    val rule = new MavenExcludeDependenciesRule(ruleConfig)
    provider save rule.transform(model)

    val transformed = provider create projectCtx
    transformed.modules foreach { module =>
      if (module.pomModel.getPackaging != "war") {
        module.pomModel.getDependencies.forall(_.getExclusions.size == 0) should be (true)
      }else {
        module.pomModel.getDependencies.exists(_.getExclusions.size > 0) should be (true)
      }
    }
  }

}
