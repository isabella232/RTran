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

import java.io.File

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}

import scala.io.Source


class ReportAndLogSupportTest extends FlatSpecLike with Matchers with BeforeAndAfterEach with LazyLogging {

  val projectRoot = new File(getClass.getClassLoader.getResource(".").getFile, "testdir")
  projectRoot.mkdirs

  val report = new ReportAndLogSupport {
    override val warnLogPrefix: String = "test-warn-log"
    override val debugLogPrefix: String = "test-debug-log"
    override val reportFilePrefix: String = "test-report"
  }

  "report" should "get all subscribers that implement IReportEventSubscriber" in {
    report.allSubscribers(projectRoot, "com.ebay.rtran.report").size should not be 0
  }

  "report" should "create the logs and report" in {
    report.createReportAndLogs(projectRoot, None) {
      logger.info("This is an info")
      logger.warn("This is a warning")
      logger.debug("Debug this")
    }
    val reportFile = new File(projectRoot, report.reportFilePrefix + ".md")
    reportFile.exists should be (true)
    val warnLog = new File(projectRoot, report.warnLogPrefix + ".log")
    warnLog.exists should be (true)
    Source.fromFile(warnLog).getLines.mkString should include ("This is a warning")
    val debugLog = new File(projectRoot, report.debugLogPrefix + ".log")
    debugLog.exists should be (true)
    val content = Source.fromFile(debugLog).getLines.mkString
    content should include ("This is an info")
    content should include ("This is a warning")
    content should include ("Debug this")

    reportFile.delete
    warnLog.delete
    debugLog.delete
  }
}
