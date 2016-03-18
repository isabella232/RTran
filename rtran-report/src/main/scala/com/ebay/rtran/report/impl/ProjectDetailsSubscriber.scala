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

package com.ebay.rtran.report.impl

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
        val NoTaskId = """Starting upgrade (.*) with taskId None""".r
        val HasTaskId = """Starting upgrade (.*) with taskId Some\((.*)\)""".r
        event1.getFormattedMessage match {
          case HasTaskId(pomPath, id) => Some(ProjectDetails(pomPath, Some(id)))
          case NoTaskId(pomPath) => Some(ProjectDetails(pomPath, None))
          case _ => None
        }
      case _ => None
    }
    details.asJava
  }

  override def dumpTo(outputStream: OutputStream): Unit = projectDetails match {
    case Some(ProjectDetails(pathToPom, taskId)) =>
      outputStream.write(outputTemplate(pathToPom, taskId).getBytes("utf8"))
    case None =>
  }

  private def outputTemplate(pathToPom: String, taskId: Option[String]) = {
    s"""
       |# Project upgrade report to Raptor 2.0
       |## Project details
       |Name | Description
       |---- | -----------
       |Path to project POM |	$pathToPom
       |Upgrade job ID | $taskId
       |Full upgrade log | [link](raptor-upgrade-debug${taskId.map("-" + _) getOrElse ""}.log)
       |Upgrade warnings only log | [link](raptor-upgrade-warn${taskId.map("-" + _) getOrElse ""}.log)
       |
     """.stripMargin
  }

  override def doAccept(event: ProjectDetails): Unit = projectDetails = Some(event)

  override val sequence = 0
}

case class ProjectDetails(pathToPom: String, taskId: Option[String])
