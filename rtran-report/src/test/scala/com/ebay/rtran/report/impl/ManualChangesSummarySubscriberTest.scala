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

import java.io.ByteArrayOutputStream

import ch.qos.logback.classic.spi.LoggingEvent
import org.scalatest.{FlatSpecLike, Matchers}


class ManualChangesSummarySubscriberTest extends FlatSpecLike with Matchers {

  "ManualChangesSummarySubscriber" should "not accept unexpected events" in {
    val outputStream = new ByteArrayOutputStream
    val subscriber = new ManualChangesSummarySubscriber

    subscriber.accept("hahaha")
    subscriber.dumpTo(outputStream)
    outputStream.toByteArray should be (Array.empty[Byte])

    val loggingEvent = new LoggingEvent
    loggingEvent.setMessage("Some random message")
    subscriber.accept(loggingEvent)
    subscriber.dumpTo(outputStream)
    outputStream.toByteArray should be (Array.empty[Byte])
  }

  "ManualChangesSummarySubscriber" should "accept expected events" in {
    val outputStream = new ByteArrayOutputStream
    val subscriber = new ManualChangesSummarySubscriber

    val loggingEvent = new LoggingEvent
    loggingEvent.setMessage("Rule blahblah requires 1000 manual changes")
    subscriber.accept(loggingEvent)
    subscriber.dumpTo(outputStream)
    val result = new String(outputStream.toByteArray)
    result should include ("|[blahblah](#blahblah) | 1000 manual changes required |")
  }
}
