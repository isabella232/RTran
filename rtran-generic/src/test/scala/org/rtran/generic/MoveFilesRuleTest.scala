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

package org.rtran.generic

import java.io.File

import org.apache.commons.io.FileUtils
import org.json4s.jackson.JsonMethods._
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}


class MoveFilesRuleTest extends FlatSpecLike with Matchers with BeforeAndAfterEach {

  val projectRoot = new File(getClass.getClassLoader.getResource("someproject").getFile)
  val destProjectRoot = new File(projectRoot.getParentFile, projectRoot.getName + "-bak")

  override def beforeEach = {
    FileUtils.deleteQuietly(destProjectRoot)
    FileUtils.copyDirectory(projectRoot, destProjectRoot)
  }

  "MoveFilesRule" should "move file to the dest directory" in {
    val ruleConfigJson = asJsonNode(parse(
      """
        |{
        | "moves":[
        |   {
        |     "pathPattern":"*/*.txt",
        |     "destDir":"otherdirectory/dest"
        |   },
        |   {
        |     "pathPattern":"*.txt",
        |     "destDir":"otherdirectory"
        |   }
        | ]
        |}
      """.stripMargin
    ))
    val ruleConfig = MoveFilesRuleConfig(
      List(
        Move("*/*.txt", "otherdirectory/dest"),
        Move("*.txt", "otherdirectory")
      )
    )
    val projectCtx = new GenericProjectCtx(destProjectRoot)
    val provider = new AllFilesModelProvider
    val model = provider create projectCtx
    val rule = new MoveFilesRule(ruleConfig)
    val result = rule transform model
    result.files forall (_.exists) should be (true)
  }

}
