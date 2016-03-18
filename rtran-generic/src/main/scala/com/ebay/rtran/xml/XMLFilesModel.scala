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

package com.ebay.rtran.xml

import java.io.{File, FileInputStream}

import org.apache.axiom.om.{OMElement, OMXMLBuilderFactory}
import org.apache.commons.io.FileUtils
import com.ebay.rtran.api.{IModel, IModelProvider}
import com.ebay.rtran.generic.GenericProjectCtx
import com.ebay.rtran.xml.util.XmlUtil

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.{Success, Try}


case class XMLFilesModel(projectRoot: File,
                         xmlRoots: Map[File, OMElement],
                         modified: Map[File, OMElement] = Map.empty) extends IModel

class XMLFilesModelProvider extends IModelProvider[XMLFilesModel, GenericProjectCtx] {
  override def id(): String = getClass.getName

  override def save(model: XMLFilesModel): Unit = {
    model.modified foreach {
      case (file, root) => XmlUtil.writeOMElement2File(file, root)
    }
  }

  override def create(projectCtx: GenericProjectCtx): XMLFilesModel = XMLFilesModel(
    projectCtx.rootDir,
    FileUtils.listFiles(projectCtx.rootDir, Array("xml"), true) map {file =>
      file -> Try(OMXMLBuilderFactory.createOMBuilder(new FileInputStream(file)).getDocumentElement)
    } collect {
      case (f, Success(r)) => f -> r
    } toMap
  )
}