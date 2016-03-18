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


class ProjectDetailsSubscriberTest extends FlatSpecLike with Matchers {

  "ProjectDetailsSubscriber" should "not accept unexpected events" in {
    val outputStream = new ByteArrayOutputStream
    val subscriber = new ProjectDetailsSubscriber

    subscriber.accept("hahah")
    subscriber.dumpTo(outputStream)
    outputStream.toByteArray should be (Array.empty[Byte])

    val loggingEvent = new LoggingEvent
    loggingEvent.setMessage("Some random message")
    subscriber.accept(loggingEvent)
    subscriber.dumpTo(outputStream)
    outputStream.toByteArray should be (Array.empty[Byte])
  }

  "ProjectDetailsSubscriber" should "accept expected events" in {
    val outputStream = new ByteArrayOutputStream
    val subscriber = new ProjectDetailsSubscriber

    val loggingEvent = new LoggingEvent
    loggingEvent.setMessage("Starting upgrade pom.xml with taskId None")
    subscriber.accept(loggingEvent)
    subscriber.dumpTo(outputStream)
    val result = new String(outputStream.toByteArray)
    result should include ("pom.xml")
    result should include ("Upgrade job ID | None")
    result should include ("Full upgrade log | [link](raptor-upgrade-debug.log)")
    result should include ("Upgrade warnings only log | [link](raptor-upgrade-warn.log)")
    outputStream.reset()

    val loggingEvent2 = new LoggingEvent
    loggingEvent2.setMessage("Starting upgrade pom.xml with taskId Some(1234)")
    subscriber.accept(loggingEvent2)
    subscriber.dumpTo(outputStream)
    val result2 = new String(outputStream.toByteArray)
    result2 should include ("pom.xml")
    result2 should include ("Upgrade job ID | Some(1234)")
    result2 should include ("Full upgrade log | [link](raptor-upgrade-debug-1234.log)")
    result2 should include ("Upgrade warnings only log | [link](raptor-upgrade-warn-1234.log)")
  }
}
