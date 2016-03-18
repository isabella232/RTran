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


class MavenDependenciesMappingSubscriber(projectRoot: File) extends IReportEventSubscriber[DependencyMappingEvent] {

  private[this] var details = Map.empty[URI, List[DependencyMappingEvent]]

  override def filter(event: scala.Any): Optional[DependencyMappingEvent] = {
    val artifact = event match {
      case e: ILoggingEvent =>
        if (e.getLoggerName.endsWith("MavenDependenciesMappingRule") && e.getMessage == "{} mapped {} to {} in {}") {
          val args = e.getArgumentArray
          Try(DependencyMappingEvent(
            args(1).asInstanceOf[Set[_]].map(_.toString),
            args(2).asInstanceOf[Set[_]].map(_.toString),
            args(3).asInstanceOf[File]
          )).toOption
        } else None
      case _ => None
    }

    artifact.asJava
  }

  override def dumpTo(outputStream: OutputStream): Unit = if (details.nonEmpty) {
    val outputTemplate =
      """
        |### MavenDependenciesMappingRule
        |The following groups of artifacts were mapped to new ones.
      """.stripMargin
    val content = details.foldLeft(outputTemplate) {(c, detail) =>
      val header = s"\n#### File [${detail._1}](${detail._1})\n|from|to|\n|----|---|\n"
      val body = detail._2.foldLeft(header) {(b, event) =>
        val from = event.from.foldLeft("<ul>") {(f, x) =>
          f + s"<li>$x</li>"
        } + "</ul>"
        val to = event.to.foldLeft("<ul>") {(f, x) =>
          f + s"<li>$x</li>"
        } + "</ul>"
        b + s"| $from | $to |\n"
      }
      c + body
    }
    outputStream.write(content.getBytes("utf8"))
  }

  override def doAccept(event: DependencyMappingEvent): Unit = {
    val relativePomPath = projectRoot.toURI relativize event.pomFile.toURI
    details get relativePomPath match {
      case Some(list) => details += relativePomPath -> (event :: list)
      case None => details += relativePomPath -> List(event)
    }
  }
}

case class DependencyMappingEvent(from: Set[String], to: Set[String], pomFile: File)
