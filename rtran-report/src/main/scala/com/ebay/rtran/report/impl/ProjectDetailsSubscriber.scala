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

package com.ebay.rtran.report.impl;

import java.io.OutputStream
import java.util.Optional

import ch.qos.logback.classic.spi.ILoggingEvent
import com.ebay.rtran.report.api.IReportEventSubscriber

import scala.compat.java8.OptionConverters._



class ProjectDetailsSubscriber extends IReportEventSubscriber[ProjectDetails]{

  private[this] var projectDetails: Option[ProjectDetails] = None

  override def filter(event: scala.Any): Optional[ProjectDetails] = {
    val details = event match {
      case event1: ILoggingEvent =>
        // Starting upgrade {} project to {}, pom {}
        val NoTaskId = """Starting upgrade (.*) project to (.*), pom (.*) with taskId None""".r
        val HasTaskId = """Starting upgrade (.*) project to (.*), pom (.*) with taskId Some\((.*)\)""".r

        event1.getFormattedMessage match {
          case HasTaskId(stack, targetVersion, pomPath, id) => Some(ProjectDetails(pomPath, stack, targetVersion, Some(id)))
          case NoTaskId(stack, targetVersion, pomPath) => Some(ProjectDetails(pomPath, stack, targetVersion, None))
          case _ => None
        }
      case _ => None
    }
    details.asJava
  }

  override def dumpTo(outputStream: OutputStream): Unit = projectDetails match {
    case Some(ProjectDetails(pathToPom, stack, targetVersion, taskId)) =>
      outputStream.write(outputTemplate(pathToPom, stack, targetVersion, taskId).getBytes("utf8"))
    case None =>
  }

  private def outputTemplate(pathToPom: String,stack: String, targetVersion: String,  taskId: Option[String]) = {
    s"""
       |# $stack project upgrade report
       |## Project details
       |Name | Description
       |---- | -----------
       |Path to project POM |	$pathToPom
       |Target Version |	$targetVersion
       |Upgrade job ID | $taskId
       |Full upgrade log | [link](raptor-upgrade-debug${taskId.map("-" + _) getOrElse ""}.log)
       |Upgrade warnings only log | [link](raptor-upgrade-warn${taskId.map("-" + _) getOrElse ""}.log)
       |
     """.stripMargin
  }

  override def doAccept(event: ProjectDetails): Unit = projectDetails = Some(event)

  override val sequence = 0
}

case class ProjectDetails(pathToPom: String, stack: String, targetVersion: String, taskId: Option[String])
