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

import java.io.{ByteArrayOutputStream, File}

import ch.qos.logback.classic.spi.LoggingEvent
import org.scalatest.{FlatSpecLike, Matchers}


class MavenDependenciesMappingSubscriberTest extends FlatSpecLike with Matchers {

  val projectRoot = new File(getClass.getClassLoader.getResource(".").getFile)

  "MavenDependenciesMappingSubscriber" should "not accept unexpected events" in {
    val outputStream = new ByteArrayOutputStream
    val subscriber = new MavenDependenciesMappingSubscriber(projectRoot)

    subscriber.accept("hahah")
    subscriber.dumpTo(outputStream)
    outputStream.toByteArray should be (Array.empty[Byte])

    val loggingEvent = new LoggingEvent
    loggingEvent.setLoggerName("fake")
    loggingEvent.setMessage("Some random message")
    subscriber.accept(loggingEvent)
    subscriber.dumpTo(outputStream)
    outputStream.toByteArray should be (Array.empty[Byte])

    val loggingEvent2 = new LoggingEvent
    loggingEvent2.setLoggerName("MavenDependenciesMappingRule")
    loggingEvent2.setMessage("Some random message")
    subscriber.accept(loggingEvent2)
    subscriber.dumpTo(outputStream)
    outputStream.toByteArray should be (Array.empty[Byte])
  }

  "MavenDependenciesMappingSubscriber" should "accept expected events" in {
    val outputStream = new ByteArrayOutputStream
    val subscriber = new MavenDependenciesMappingSubscriber(projectRoot)

    val loggingEvent = new LoggingEvent
    loggingEvent.setLoggerName("MavenDependenciesMappingRule")
    loggingEvent.setMessage("{} mapped {} to {} in {}")
    loggingEvent.setArgumentArray(Array(
      "MavenDependenciesMappingRule",
      Set("zzzz:yyyy:xxxx"),
      Set("xxxx:yyyy:zzzz"),
      new File(projectRoot, "pom.xml")
    ))
    subscriber.accept(loggingEvent)
    subscriber.dumpTo(outputStream)
    val result = new String(outputStream.toByteArray)
    result should include ("#### File [pom.xml](pom.xml)")
    result should include ("xxxx:yyyy:zzzz")
    result should include ("zzzz:yyyy:xxxx")
  }
}
