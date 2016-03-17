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

package org.rtran.report

import java.io.OutputStream

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.rtran.report.api.IReportEventSubscriber


class ReportEventPublisher extends AppenderBase[ILoggingEvent] {

  private[this] var _subscribers = List.empty[IReportEventSubscriber[_]]

  setName("ReportEventPublisher")

  override def append(eventObject: ILoggingEvent): Unit = {
    _subscribers foreach {_.accept(eventObject)}
  }

  def startWithSubscribers(subscribers: List[IReportEventSubscriber[_]]): Unit = {
    _subscribers = subscribers
    super.start()
  }

  def stopAndSave(outputStream: OutputStream): Unit = {
    _subscribers foreach (_ dumpTo outputStream)
    outputStream.flush()
    outputStream.close()
    super.stop()
  }
}
