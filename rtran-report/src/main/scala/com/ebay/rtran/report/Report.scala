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

package com.ebay.rtran.report

import java.io.OutputStream

import ch.qos.logback.classic
import ch.qos.logback.classic.LoggerContext
import com.ebay.rtran.report.api.IReportEventSubscriber
import org.slf4j.{Logger, LoggerFactory}


object Report {

  /**
   *
   * @param outputStream The output stream to which the report is written
   * @param subscribers All the subscribers that subscribes to the log events
   * @param fn Function that generates the log and it has to be blocking operations that executed in the same thread
   * @tparam T  Return type of the function
   * @return
   */
  def createReport[T](outputStream: OutputStream,
                      loggerNames: Set[String] = Set.empty,
                      subscribers: List[IReportEventSubscriber[_]] = List.empty)
                     (fn: => T): T = {
    val lc = LoggerFactory.getILoggerFactory.asInstanceOf[classic.LoggerContext]
    val reportAppender = prepareReportAppender(lc, loggerNames)
    reportAppender.startWithSubscribers(subscribers)
    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[classic.Logger]
    rootLogger.addAppender(reportAppender)
    val result = fn
    reportAppender.stopAndSave(outputStream)
    rootLogger.detachAppender(reportAppender)
    result
  }

  private def prepareReportAppender(lc: LoggerContext, loggerNames: Set[String]): ReportEventPublisher = {
    val reportAppender = new ReportEventPublisher
    reportAppender.setContext(lc)
    reportAppender.addFilter(new SameThreadFilter)
    reportAppender.addFilter(new LoggerNameFilter(loggerNames))
    reportAppender
  }

}
