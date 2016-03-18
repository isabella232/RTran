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

import java.io.{File, FileOutputStream}

import ch.qos.logback.classic
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.FileAppender
import org.reflections.Reflections
import com.ebay.rtran.report.api.IReportEventSubscriber
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.{Success, Try}


trait ReportAndLogSupport {

  val reportFilePrefix: String
  val warnLogPrefix: String
  val debugLogPrefix: String

  def createReportAndLogs[T](projectRoot: File,
                             taskId: Option[String], packages: String*)(fn: => T): T = {
    val appenders = prepareAppenders(projectRoot, taskId)
    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[classic.Logger]
    appenders foreach rootLogger.addAppender
    appenders foreach (_.start)
    val reportFile = new File(projectRoot, s"$reportFilePrefix${taskId.map("-" + _) getOrElse ""}.md")
    val result = Report.createReport(
      new FileOutputStream(reportFile),
      subscribers = allSubscribers(projectRoot, packages: _*)
    )(fn)
    appenders foreach (_.stop)
    appenders foreach rootLogger.detachAppender
    result
  }

  def allSubscribers(projectRoot: File, packages: String*) = {
    val subscribers = packages flatMap {prefix =>
      new Reflections(prefix).getSubTypesOf(classOf[IReportEventSubscriber[_]])
    } map {clazz =>
      Try(clazz.getDeclaredConstructor(classOf[File]).newInstance(projectRoot)) orElse Try(clazz.newInstance)
    } collect {
      case Success(subscriber) => subscriber
    } toList

    subscribers.sortBy(_.sequence)
  }

  private def prepareAppenders(projectRoot: File, taskId: Option[String]) = {
    val lc = LoggerFactory.getILoggerFactory.asInstanceOf[classic.LoggerContext]
    val encoders = Array(new PatternLayoutEncoder, new PatternLayoutEncoder)
    encoders foreach (_ setContext lc)
    encoders foreach (_ setPattern "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}.%M:%L - %m%n")
    encoders foreach (_.start)

    val warnFileAppender = new FileAppender[ILoggingEvent]
    warnFileAppender.setName("warnFileAppender")
    warnFileAppender.setFile(s"${projectRoot.getAbsolutePath}/$warnLogPrefix${taskId.map("-" + _) getOrElse ""}.log")
    warnFileAppender.addFilter(new SameThreadFilter)
    val warnFilter = new ThresholdFilter
    warnFilter.setLevel("WARN")
    warnFilter.start()
    warnFileAppender.addFilter(warnFilter)

    val debugFileAppender = new FileAppender[ILoggingEvent]
    debugFileAppender.setName("debugFileAppender")
    debugFileAppender.setFile(s"${projectRoot.getAbsolutePath}/$debugLogPrefix${taskId.map("-" + _) getOrElse ""}.log")
    debugFileAppender.addFilter(new SameThreadFilter)
    val debugFilter = new ThresholdFilter
    debugFilter.setLevel("DEBUG")
    debugFilter.start()
    debugFileAppender.addFilter(debugFilter)

    val result = List(warnFileAppender, debugFileAppender)
    result.foreach(_ setContext lc)
    result zip encoders foreach (entry => entry._1 setEncoder entry._2)
    result
  }
}
