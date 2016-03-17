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
import org.apache.commons.io.filefilter.TrueFileFilter
import org.rtran.api.{IModel, IModelProvider}

import scala.collection.JavaConversions._


case class AllFilesModel(projectRoot: File, files: List[File], modified: List[File] = List.empty) extends IModel

class AllFilesModelProvider extends IModelProvider[AllFilesModel, GenericProjectCtx] {
  override def id(): String = getClass.getName

  override def save(model: AllFilesModel): Unit = {
    // all files operations are taken in place
    // simply validate the model
    if (!model.files.forall(_.exists)) {
      throw new IllegalStateException(s"${model.files.filterNot(_.exists)} does not exist")
    }
  }

  override def create(project: GenericProjectCtx): AllFilesModel = AllFilesModel(
    project.rootDir,
    FileUtils.listFiles(project.rootDir, TrueFileFilter.TRUE, TrueFileFilter.TRUE).toList
  )
}