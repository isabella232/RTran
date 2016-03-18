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
import com.ebay.rtran.xml._
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}

import scala.io.Source
import scala.language.postfixOps


class ModifyXMLFilesRuleTest extends FlatSpecLike with Matchers with BeforeAndAfterEach {

  val projectRoot = new File(getClass.getClassLoader.getResource("someproject").getFile)
  val destProjectRoot = new File(projectRoot.getParentFile, projectRoot.getName + "-bak")

  override def beforeEach = {
    FileUtils.deleteQuietly(destProjectRoot)
    FileUtils.copyDirectory(projectRoot, destProjectRoot)
  }

  "ModifyXMLFilesRuleTest" should "able to delete nodes" in {
    val provider = new XMLFilesModelProvider
    val ruleConfig = ModifyXMLFilesRuleConfig(
      Some("**/*.xml"),
      List(
        ModifyXMLOperation(
          "//person[@name=\'Bob\']",
          OperationType.Delete
        )
      )
    )
    val rule = new ModifyXMLFilesRule(ruleConfig)
    val transformedModel = rule.transform(provider.create(new GenericProjectCtx(destProjectRoot)))
    provider save transformedModel

    val transformedContent = Source.fromFile(new File(destProjectRoot, "somedirectory/someXML.xml")).getLines.mkString("\n")
    transformedContent should not include "Bob"
    transformedContent should not include "Salesman"
  }

  "ModifyXMLFilesRuleTest" should "be able to insert nodes" in {
    val node1 =
      <person name="John">
        <job>Software Engineer</job>
      </person>.toString

    val node2 =
      <person name="Linda">
        <job>Recruiter</job>
      </person>.toString

    val provider = new XMLFilesModelProvider
    val ruleConfig = ModifyXMLFilesRuleConfig(
      Some("**/*.xml"),
      List(
        ModifyXMLOperation(
          "//persons",
          OperationType.Insert,
          Some(node1)
        ),
        ModifyXMLOperation(
          "//persons",
          OperationType.Insert,
          Some(node2)
        )
      )
    )
    val rule = new ModifyXMLFilesRule(ruleConfig)
    val transformedModel = rule.transform(provider.create(new GenericProjectCtx(destProjectRoot)))
    provider save transformedModel

    val transformedContent = Source.fromFile(new File(destProjectRoot, "somedirectory/someXML.xml")).getLines.mkString("\n")
    transformedContent should include ("John")
    transformedContent should include ("Software Engineer")
    transformedContent should include ("Linda")
    transformedContent should include ("Recruiter")
  }

  "ModifyXMLFilesRuleTest" should "be able to replace nodes" in {
    val ruleConfig = ModifyXMLFilesRuleConfig(
      Some("**/*.xml"),
      List(
        ModifyXMLOperation(
          "//person[@name=\'Bob\']/job",
          OperationType.Replace,
          Some("<job>Software Engineer</job>")
        )
      )
    )
    val provider = new XMLFilesModelProvider
    val rule = new ModifyXMLFilesRule(ruleConfig)
    val transformedModel = rule.transform(provider.create(new GenericProjectCtx(destProjectRoot)))
    provider save transformedModel

    val transformedContent = Source.fromFile(new File(destProjectRoot, "somedirectory/someXML.xml")).getLines.mkString("\n")
    transformedContent should include ("Bob")
    transformedContent should include ("Software Engineer")
    transformedContent should not include "Salesman"
  }

}
