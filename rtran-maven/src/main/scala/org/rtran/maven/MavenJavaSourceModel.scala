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
import org.rtran.api.{IModel, IModelProvider}

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.{Success, Try}


case class MavenJavaSourceModel(sources: Map[File, List[File]], modified: List[File] = List.empty) extends IModel

class MavenJavaSourceModelProvider extends IModelProvider[MavenJavaSourceModel, MavenProjectCtx] {

  private[this] val mavenModelProvider = new MultiModuleMavenModelProvider

  override def id(): String = getClass.getName

  override def save(model: MavenJavaSourceModel): Unit = {}

  override def create(project: MavenProjectCtx): MavenJavaSourceModel = {
    val modules = mavenModelProvider.create(project).modules
    val sources = modules map {m =>
      Try {
        val moduleDir = m.pomFile.getParentFile
        moduleDir -> FileUtils.listFiles(new File(m.pomFile.getParent, "src"), Array("java"), true).toList
      }
    } collect {
      case Success(entry) => entry
    } toMap

    MavenJavaSourceModel(sources)
  }
}
