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
import com.twitter.util.Future;
import com.twitter.util.FutureEventListener;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

import java.lang.reflect.Method;

import static org.apache.skywalking.apm.network.trace.component.ComponentsDefine.FINAGLE;
import static org.apache.skywalking.apm.plugin.finagle.ContextHolderFactory.getLocalContextHolder;

public class ServerTracingFilterInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance enhancedInstance, Method method, Object[] objects, Class<?>[] classes, MethodInterceptResult methodInterceptResult) throws Throwable {

        ContextHolder localContextHolder = getLocalContextHolder();
        AbstractSpan span = null;
        if (Contexts.broadcast().contains(SWContextCarrier$.MODULE$)) {
            SWContextCarrier swContextCarrier = Contexts.broadcast().apply(SWContextCarrier$.MODULE$);
            span = ContextManager.createEntrySpan(swContextCarrier.getOperationName(), swContextCarrier.carrier());
        } else {
            span = ContextManager.createEntrySpan("unknown", new ContextCarrier());
        }

        span.setComponent(FINAGLE);
        SpanLayer.asRPCFramework(span);

        localContextHolder.let(FinagleCtxs.SW_SPAN, span);

        enhancedInstance.setSkyWalkingDynamicField(localContextHolder);
    }

    @Override
    public Object afterMethod(EnhancedInstance enhancedInstance, Method method, Object[] objects, Class<?>[] classes, Object ret) throws Throwable {
        Object skyWalkingDynamicField = enhancedInstance.getSkyWalkingDynamicField();
        final AbstractSpan finagleSpan = ((ContextHolder) skyWalkingDynamicField).remove(FinagleCtxs.SW_SPAN);
        finagleSpan.prepareForAsync();
        ContextManager.stopSpan(finagleSpan);
        ((Future<?>) ret).addEventListener(new FutureEventListener<Object>() {
            @Override
            public void onSuccess(Object value) {
                finagleSpan.asyncFinish();
            }

            @Override
            public void onFailure(Throwable cause) {
                finagleSpan.errorOccurred();
                finagleSpan.log(cause);
                finagleSpan.asyncFinish();
            }
        });
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance enhancedInstance, Method method, Object[] objects, Class<?>[] classes, Throwable throwable) {
        ContextManager.activeSpan().errorOccurred().log(throwable);
    }
}
