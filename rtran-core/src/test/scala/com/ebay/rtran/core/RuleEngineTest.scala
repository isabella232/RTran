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

package com.ebay.rtran.core

import java.io.File

import org.apache.commons.io.FileUtils
import org.json4s.jackson.JsonMethods._
import com.ebay.rtran.core.mock.{MyModifyFileRule, MyProject, MyRenameFileRule, MyRenameFileRuleConfig}
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}

import scala.io.Source


class RuleEngineTest extends FlatSpecLike with Matchers with BeforeAndAfterEach {

  val projectDir = new File(getClass.getClassLoader.getResource("myproject").getFile)
  val backupDir = new File(projectDir.getParentFile, projectDir.getName + "-bak")

  override def beforeEach = {
    FileUtils.copyDirectory(projectDir, backupDir)
  }
  override def afterEach = {
    FileUtils.deleteQuietly(backupDir)
  }

  "RuleEngine" should "execute rules from UpgradeConfiguration" in {
    val engine = new RuleEngine
    val projectRoot = backupDir
    val configuration = JsonUpgradeConfiguration( List(
      JsonRuleConfiguration("ModifyFileRule", None),
      JsonRuleConfiguration("RenameFileRule", Some(parse("""{"newName":"anotherfile"}""")))
    ))
    engine.execute(new MyProject(projectRoot), configuration)
    new File(projectRoot, "somefile").exists should be (false)
    new File(projectRoot, "anotherfile").exists should be (true)
    Source.fromFile(new File(projectRoot, "anotherfile")).getLines.toList should be (List("hi world", "hi Bob"))
  }

  "RuleEngine" should "execute rules from code" in {
    val engine = new RuleEngine
    val projectRoot = backupDir
    engine.execute(
      new MyProject(projectRoot),
      List(
        new MyModifyFileRule(),
        new MyRenameFileRule(MyRenameFileRuleConfig("anotherfile"))
      )
    )
    new File(projectRoot, "somefile").exists should be (false)
    new File(projectRoot, "anotherfile").exists should be (true)
    Source.fromFile(new File(projectRoot, "anotherfile")).getLines.toList should be (List("hi world", "hi Bob"))
  }

}
