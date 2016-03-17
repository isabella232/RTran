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

package org.rtran.xml

import java.io.ByteArrayInputStream

import org.rtran._
import com.fasterxml.jackson.databind.JsonNode
import org.apache.axiom.om.impl.builder.StAXOMBuilder
import org.apache.axiom.om.xpath.AXIOMXPath
import org.apache.axiom.om.{OMElement, OMNamespace}
import org.json4s.DefaultFormats
import org.json4s.ext.EnumNameSerializer
import org.json4s.jackson.JsonMethods._
import org.rtran.api.{IRule, IRuleConfig, IRuleConfigFactory}
import org.rtran.generic.util.FilePathMatcher
import org.rtran.xml.OperationType.OperationType

import scala.collection.JavaConversions._
import scala.reflect.ManifestFactory


class ModifyXMLFilesRule(ruleConfig: ModifyXMLFilesRuleConfig) extends IRule[XMLFilesModel] {

  override def transform(model: XMLFilesModel): XMLFilesModel = {
    val modified = model.xmlRoots filter {
      case (file, root) => ruleConfig.pathPattern flatMap (FilePathMatcher(model.projectRoot, _).toOption) match {
        case Some(matcher) => matcher.matches(file)
        case None => true
      }
    } map {
      case (file, root) =>
        file -> ruleConfig.operations.foldLeft(root)(applyOperation)
    }
    model.copy(modified = modified)
  }

  private def applyOperation(root: OMElement, operation: ModifyXMLOperation) = {
    import OperationType._
    val xpathExpression = new AXIOMXPath(root, operation.xpath)
    Option(root.getDefaultNamespace).foreach(ns => xpathExpression.addNamespace("defaultns", ns.getNamespaceURI))
    val nodes = xpathExpression.selectNodes(root).map(_.asInstanceOf[OMElement])
    operation match {
      case ModifyXMLOperation(_, Delete, _) =>
        nodes.foreach(_.detach)
        root
      case ModifyXMLOperation(_, _, None) => root
      case ModifyXMLOperation(_, optType, Some(newNodeStr)) =>
        val newNode = new StAXOMBuilder(new ByteArrayInputStream(newNodeStr.getBytes)).getDocumentElement
        Option(root.getDefaultNamespace).foreach(ns => setNamespaceRecursively(newNode, ns))
        optType match {
          case Insert =>
            nodes foreach {node =>
              node.addChild(newNode)
            }
          case Replace =>
            nodes foreach {node =>
              node.getParent.addChild(newNode)
              node.detach
            }
        }
        root
    }
  }

  private def setNamespaceRecursively(root: OMElement, namespace: OMNamespace): Unit = {
    root.setNamespace(namespace, false)
    root.getChildElements foreach { elem =>
      setNamespaceRecursively(elem.asInstanceOf[OMElement], namespace)
    }
  }
}

case class ModifyXMLFilesRuleConfig(pathPattern: Option[String],
                                    operations: List[ModifyXMLOperation]) extends IRuleConfig

case class ModifyXMLOperation(xpath: String,
                              operationType: OperationType,
                              newNodeString: Option[String] = None)

object OperationType extends Enumeration {
  type OperationType = Value
  val Insert, Replace, Delete = Value
}

class ModifyXMLFilesRuleConfigFactory extends IRuleConfigFactory[JsonNode] {

  val formats = DefaultFormats + new EnumNameSerializer(OperationType)

  override def createRuleConfig[Config](configClass: Class[Config], configData: JsonNode): Config = {
    fromJsonNode(configData).extract(formats, ManifestFactory.classType(configClass))
  }
}
