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

package org.rtran.report.impl

import java.io.OutputStream
import java.util.Optional

import ch.qos.logback.classic.spi.ILoggingEvent
import org.rtran.report.api.IReportEventSubscriber

import scala.compat.java8.OptionConverters._


class UpgradeSummarySubscriber extends IReportEventSubscriber[(String, Int)] {

  private[this] var ruleSummary = Map.empty[String, Int]

  override def filter(event: scala.Any): Optional[(String, Int)] = {
    val Regex = """Rule (.+) was applied to (\d+).*""".r
    val info = event match {
      case event1: ILoggingEvent =>
        event1.getFormattedMessage match {
          case Regex(rule, num) => Some((rule, num.toInt))
          case _ => None
        }

      case _ => None
    }

    info.asJava
  }

  override def dumpTo(outputStream: OutputStream): Unit = if (ruleSummary.nonEmpty) {
    val outputTemplate = "\r## Summary\n\n| Operation | Details |\n| ---- | ----------- |\n"
    val content = ruleSummary.foldLeft(outputTemplate) {(c, summary) =>
      c + s"|[${summary._1}](#${summary._1.split("\\.").lastOption getOrElse ""}) | impacted ${summary._2} file(s) |\n"
    }
    outputStream.write(content.getBytes("utf8"))
  }

  override def doAccept(event: (String, Int)): Unit = ruleSummary get event._1 match {
    case Some(num) => ruleSummary += event._1 -> (event._2 + num)
    case None => ruleSummary += event._1 -> event._2
  }

  override val sequence = 3
}
