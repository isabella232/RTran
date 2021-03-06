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

package com.ebay.rtran.maven.report

import java.io.{File, OutputStream}
import java.net.URI
import java.util.Optional

import ch.qos.logback.classic.spi.ILoggingEvent
import com.ebay.rtran.report.api.IReportEventSubscriber

import scala.compat.java8.OptionConverters._
import scala.util.Try


class MavenAddDependenciesSubscriber(projectRoot: File) extends IReportEventSubscriber[AddDependencyEvent] {

  private[this] var details = Map.empty[URI, List[String]]

  override def filter(event: scala.Any): Optional[AddDependencyEvent] = {
    val artifact = event match {
      case e: ILoggingEvent =>
        if (e.getLoggerName.endsWith("MavenAddDependenciesRule") && e.getMessage == "{} added dependency {} to {}") {
          val args = e.getArgumentArray
          Try(AddDependencyEvent(args(1).toString, args(2).asInstanceOf[File])).toOption
        } else None
      case _ => None
    }

    artifact.asJava
  }

  override def dumpTo(outputStream: OutputStream): Unit = if (details.nonEmpty) {
    val outputTemplate =
      """
        |### MavenAddDependenciesRule
        |The following artifacts were added to the POM:
      """.stripMargin
    val content = details.foldLeft(outputTemplate) {(c, detail) =>
     val header = s"\n#### File [${detail._1}](${detail._1})\n|Artifacts|\n|---------|\n"
     c + detail._2.foldLeft(header) {(result, artifact) =>
       result + s"|$artifact|\n"
     }
    }
    outputStream.write(content.getBytes("utf8"))
  }

  override def doAccept(event: AddDependencyEvent): Unit = {
    val relativePomPath = projectRoot.toURI relativize event.pomFile.toURI
    details get relativePomPath match {
      case Some(list) => details += relativePomPath -> (event.dependency :: list)
      case None => details += relativePomPath -> List(event.dependency)
    }
  }
}

case class AddDependencyEvent(dependency: String, pomFile: File)