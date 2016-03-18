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

package com.ebay.rtran.generic

import java.io.File

import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}

import scala.io.Source


class ModifyFilesRuleTest extends FlatSpecLike with Matchers with BeforeAndAfterEach {

  val projectRoot = new File(getClass.getClassLoader.getResource("someproject").getFile)
  val destProjectRoot = new File(projectRoot.getParentFile, projectRoot.getName + "-bak")

  override def beforeEach = {
    FileUtils.deleteQuietly(destProjectRoot)
    FileUtils.copyDirectory(projectRoot, destProjectRoot)
  }

  "ModifyFilesRule" should "modify the file correctly" in {
    val ruleConfig = ModifyFilesRuleConfig(
      "**/fileA.txt",
      None,
      List(
        ContentMapping("hello\\s(.+)\\n", "hallo $1\n"),
        ContentMapping("(.+)\\sBob", "$1 Alice")
      )
    )
    val projectCtx = new GenericProjectCtx(destProjectRoot)
    val provider = new AllFilesModelProvider
    val model = provider create projectCtx
    val rule = new ModifyFilesRule(ruleConfig)
    val result = rule transform model
    val file = result.files.find(_.getName == "fileA.txt")
    file.nonEmpty should be (true)
    Source.fromFile(file.get).getLines.toList should be (List("hallo world", "hi Alice"))
  }

}
