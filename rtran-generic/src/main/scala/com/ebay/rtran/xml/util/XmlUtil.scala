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

package com.ebay.rtran.xml.util

import java.io.{File, FileOutputStream}
import javax.xml.stream.XMLOutputFactory

import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter
import org.apache.axiom.om.OMElement


object XmlUtil {

  def writeOMElement2File(file: File, root: OMElement, indenting: Boolean = false): Unit = {
    val writer = XMLOutputFactory.newInstance.createXMLStreamWriter(new FileOutputStream(file))
    val indentedWriter = if (indenting) new IndentingXMLStreamWriter(writer) else writer
    indentedWriter.writeStartDocument()
    indentedWriter.writeCharacters("\n")
    root.serialize(indentedWriter)
    indentedWriter.writeCharacters("\n")
    indentedWriter.flush()
    indentedWriter.close()
  }
}
