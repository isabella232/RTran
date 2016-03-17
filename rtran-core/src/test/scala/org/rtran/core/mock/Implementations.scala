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

package org.rtran.core.mock

import java.io.File
import java.nio.file.Paths

import org.apache.commons.io.FileUtils
import org.rtran.api._

import scala.io.Source


case class MyProject(rootDir: File) extends IProjectCtx {
  
  val someFile = new File(rootDir, "somefile")
}

case class MyFileModel(path: String, content: String) extends IModel

class MyFileModelProvider extends IModelProvider[MyFileModel, MyProject] {
  override def id(): String = getClass.getName

  override def save(model: MyFileModel): Unit = {
    FileUtils.write(new File(model.path), model.content)
  }

  override def create(project: MyProject): MyFileModel = {
    val content = Source.fromFile(project.someFile).getLines.mkString("\n")
    MyFileModel(project.someFile.getAbsolutePath, content)
  }
}

class MyModifyFileRule extends IRule[MyFileModel] {
  val regexStr = """hello\s(.*)\n"""
  val replacement = "hi $1\n"
  override def transform(model: MyFileModel): MyFileModel =
    model.copy(content = model.content.replaceAll(regexStr, replacement))
}

class MyRenameFileRule(ruleConfig: MyRenameFileRuleConfig) extends IRule[MyFileModel] {
  override def transform(model: MyFileModel): MyFileModel = {
    FileUtils.deleteQuietly(new File(model.path))
    model.copy(path = Paths.get(model.path).resolveSibling(ruleConfig.newName).toString)
  }
}

case class MyRenameFileRuleConfig(newName: String) extends IRuleConfig

class MyDummyModel extends IModel

class MyDummyModel2 extends IModel

class MyDummyModelProvider extends IModelProvider[MyDummyModel, MyProject] {
  override def id(): String = getClass.getName

  override def save(model: MyDummyModel): Unit = {}

  override def create(project: MyProject): MyDummyModel = new MyDummyModel
}