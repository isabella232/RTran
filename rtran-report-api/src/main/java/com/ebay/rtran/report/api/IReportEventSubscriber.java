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

package com.ebay.rtran.report.api;

import java.io.OutputStream;
import java.util.Optional;


public interface IReportEventSubscriber<EventType> {

  default void accept(Object event) {
    try {
      Optional<EventType> e = filter(event);
      if (e.isPresent()) {
        doAccept(e.get());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  Optional<EventType> filter(Object event);

  void doAccept(EventType event);

  void dumpTo(OutputStream outputStream);

  default int sequence() {
    return 100;
  }
}