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

import java.io.{File, FileOutputStream, OutputStream}
import java.util.Optional
import com.typesafe.scalalogging.LazyLogging
import com.ebay.rtran.report.api.IReportEventSubscriber
import org.scalatest.{FlatSpecLike, Matchers}


class ReportTest extends FlatSpecLike with Matchers with LazyLogging {

  "Report" should "accept log event correctly" in {
    val subscriber = new TestSubscriber
    val outputStream = new FileOutputStream("test")
    Report.createReport(outputStream, subscribers = List(subscriber))(testFunc())
    subscriber.getCount should be (3)
    new File("test").delete
  }

  "Report" should "work in concurrent mode" in {
    val subscribers = (1 to 10) map (_ => new TestSubscriber)

    subscribers foreach {sub =>
      new Thread(new TestThread(sub)).start()
    }

    val waitPeriod = 1000

    Thread.sleep(waitPeriod)

    subscribers foreach (_.getCount should be (3))
  }

  class TestThread(subscriber: IReportEventSubscriber[_]) extends Runnable {
    override def run(): Unit = {
      val file = new File(s"test-${System.nanoTime}")
      val outputStream = new FileOutputStream(file)
      Report.createReport(outputStream, subscribers = List(subscriber))(testFunc())
      file.delete
    }
  }

  def testFunc(): Unit = {
    val str = "hello"
    val number = 2000
    logger.info("String {}", str)
    logger.info(s"number ${number + 2}")
    logger.info("String {} number {}", str, number.toString)
  }

  class TestSubscriber extends IReportEventSubscriber[Int] {
    import scala.compat.java8.OptionConverters._

    private var count = 0

    def getCount = count

    override def filter(event: scala.Any): Optional[Int] = Some(1).asJava

    override def dumpTo(outputStream: OutputStream): Unit = {}

    override def doAccept(event: Int): Unit = count += event
  }

}
