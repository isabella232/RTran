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
import com.ebay.rtran.xml.XMLFilesModelProvider
import org.scalatest.{FlatSpecLike, Matchers}

import scala.io.Source


class XMLFilesModelProviderTest extends FlatSpecLike with Matchers {

  val projectRoot = new File(getClass.getClassLoader.getResource("someproject").getFile)

  "XMLFilesModeProvider" should "get all xml files in the project" in {
    val provider = new XMLFilesModelProvider
    val model = provider.create(new GenericProjectCtx(projectRoot))
    model.xmlRoots.size should be (1)
  }

  "XMLFilesModeProvider" should "be able to save the files that are marked modified" in {
    val provider = new XMLFilesModelProvider
    val model = provider.create(new GenericProjectCtx(projectRoot))
    val (file, root) = model.xmlRoots.head
    val newFile = new File(file.getParentFile, file.getName + ".new")
    provider.save(model.copy(modified = Map(newFile -> root)))
    val content = Source.fromFile(newFile).getLines.filterNot(_.matches("\\s+")).map(_.trim).mkString
    content should not be ""
    FileUtils.deleteQuietly(newFile)
  }

}
