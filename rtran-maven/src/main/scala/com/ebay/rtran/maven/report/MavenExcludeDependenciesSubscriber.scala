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


class MavenExcludeDependenciesSubscriber(projectRoot: File) extends IReportEventSubscriber[ExcludeDependencyEvent] {

  private[this] var events = Map.empty[URI, Set[ExcludeDependencyEvent]]

  override def filter(event: scala.Any): Optional[ExcludeDependencyEvent] = {
    val excludeEvent = event match {
      case e: ILoggingEvent =>
        if (e.getLoggerName.endsWith("MavenExcludeDependenciesRule") && e.getMessage == "{} excluded {} from {} in {}") {
          val args = e.getArgumentArray
          Try(ExcludeDependencyEvent(
            args(1).asInstanceOf[Set[_]].map(_.toString),
            args(2).toString,
            args(3).asInstanceOf[File]
          )).toOption
        } else None
      case _ => None
    }

    excludeEvent.asJava
  }

  override def dumpTo(outputStream: OutputStream): Unit = if (events.nonEmpty) {
    val outputTemplate =
      """
        |### MavenExcludeDependenciesRule
        |The following artifacts were excluded:
      """.stripMargin
    val content = events.foldLeft(outputTemplate) {(c, event) =>
      val header = s"\n#### File [${event._1}](${event._1})\n|Artifact|Exclusions|\n|-------|------|\n"
      c + header + event._2.map(e => e.dep -> e.exclusions).toMap.foldLeft("") {(result, entry) =>
        result + s"|${entry._1}|" + entry._2.foldLeft("<ul>")(_ + "<li>" + _ + "</li>") + "</ul>|\n"
      }
    }
    outputStream.write(content.getBytes("utf8"))
  }

  override def doAccept(event: ExcludeDependencyEvent): Unit = {
    val relativePomPath = projectRoot.toURI relativize event.pomFile.toURI
    events get relativePomPath match {
      case Some(set) => events += relativePomPath -> (set + event)
      case None => events += relativePomPath -> Set(event)
    }
  }
}

case class ExcludeDependencyEvent(exclusions: Set[String], dep: String, pomFile: File)
