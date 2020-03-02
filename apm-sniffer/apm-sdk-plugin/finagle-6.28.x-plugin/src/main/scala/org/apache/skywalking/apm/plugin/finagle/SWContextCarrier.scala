/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.plugin.finagle

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util

import com.twitter.finagle.context.Contexts
import com.twitter.io.Buf
import com.twitter.util.{Return, Try}
import org.apache.skywalking.apm.agent.core.context.ContextCarrier

class SWContextCarrier(val carrier: ContextCarrier) {
  private var operationName: String = ""

  def setOperationName(op: String): Unit = {
    operationName = op
  }

  def getOperationName: String = operationName
}

object SWContextCarrier extends Contexts.broadcast.Key[SWContextCarrier]("org.apache.skywalking.apm.plugin.finagle.SWContextCarrier") {

  def of(carrier: ContextCarrier): SWContextCarrier = {
    new SWContextCarrier(carrier)
  }

  override def marshal(context: SWContextCarrier): Buf = {
    val byteOut = new ByteArrayOutputStream
    val out = new ObjectOutputStream(byteOut)
    val map = new util.HashMap[String, String]

    var next = context.carrier.items();
    while (next.hasNext) {
      next = next.next
      map.put(next.getHeadKey, next.getHeadValue);
    }
    map.put("OP", context.operationName)
    out.writeObject(map)
    Buf.ByteArray.Owned(byteOut.toByteArray)
  }

  override def tryUnmarshal(buf: Buf): Try[SWContextCarrier] = {
    val bytes = Buf.ByteArray.Owned.extract(buf)
    val byteIn = new ByteArrayInputStream(bytes)
    val in = new ObjectInputStream(byteIn)
    val data = in.readObject.asInstanceOf[util.HashMap[String, String]]

    val contextCarrier = new ContextCarrier
    var next = contextCarrier.items
    while (next.hasNext) {
      next = next.next
      next.setHeadValue(data.get(next.getHeadKey))
    }
    val context = new SWContextCarrier(contextCarrier)
    context.setOperationName(data.get("OP"));
    Return(context)
  }
}