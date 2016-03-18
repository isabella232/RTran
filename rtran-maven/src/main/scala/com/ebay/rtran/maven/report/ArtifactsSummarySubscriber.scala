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


class ArtifactsSummarySubscriber(projectRoot: File) extends IReportEventSubscriber[(URI, String)] {

  private[this] var artifacts = Set.empty[(URI, String)]

  override def filter(event: scala.Any): Optional[(URI, String)] = {
    val artifact = event match {
      case e: ILoggingEvent =>
        e.getMessage match {
          case "Found maven pom {} for artifact {}" =>
            val args = e.getArgumentArray
            Try((projectRoot.toURI.relativize(args(0).asInstanceOf[File].toURI), args(1).toString)).toOption
          case _ => None
        }
      case _ => None
    }

    artifact.asJava
  }

  override def dumpTo(outputStream: OutputStream): Unit = if (artifacts.nonEmpty) {
    val outputTemplate = s"\r### Artifacts\n" +
      s"This upgrade request processed only the ${artifacts.size} Maven project artifacts\n" +
      s"that were referenced directly or indirectly by the project's parent POM:\n\n" +
      s"| No. | POM file | Artifact ID |\n" +
      s"| --- | -------- | ----------- |\n"
    var pomCount = 0
    val content = artifacts.foldLeft(outputTemplate) {(c, artifact) =>
      pomCount += 1
      c + s"| $pomCount | [${artifact._1}](${artifact._1}) | ${artifact._2} |\n"
    }
    outputStream.write(content.getBytes("utf8"))
  }

  override def doAccept(event: (URI, String)): Unit = if (event._1.toString != "pom.xml") artifacts += event

  override val sequence = 1
}
