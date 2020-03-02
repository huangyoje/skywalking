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

package org.apache.skywalking.apm.plugin.finagle;

import com.twitter.finagle.context.Contexts;
import com.twitter.finagle.context.LocalContext;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

import javax.annotation.Nullable;

class FinagleCtxs {

    static LocalContext.Key<AbstractSpan> SW_SPAN = Contexts.local().newKey();

    static LocalContext.Key<String> RPC = Contexts.local().newKey();

    @Nullable
    static AbstractSpan getSpan() {
        if (Contexts.local().contains(SW_SPAN)) {
            AbstractSpan abstractSpan = Contexts.local().apply(SW_SPAN);
            return abstractSpan;
        }
        return null;
    }

    @Nullable
    static SWContextCarrier getContextCarrier() {
        if (Contexts.broadcast().contains(SWContextCarrier$.MODULE$)) {
            SWContextCarrier swContextCarrier = Contexts.broadcast().apply(SWContextCarrier$.MODULE$);
            return swContextCarrier;
        }
        return null;
    }
}
