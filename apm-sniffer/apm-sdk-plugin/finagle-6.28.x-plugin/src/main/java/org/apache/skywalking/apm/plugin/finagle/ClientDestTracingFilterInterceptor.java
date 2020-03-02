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

import com.twitter.finagle.Address;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import static org.apache.skywalking.apm.plugin.finagle.ContextCarrierHelper.setPeerHost;
import static org.apache.skywalking.apm.plugin.finagle.FinagleCtxs.getContextCarrier;
import static org.apache.skywalking.apm.plugin.finagle.FinagleCtxs.getSpan;

public class ClientDestTracingFilterInterceptor implements InstanceConstructorInterceptor, InstanceMethodsAroundInterceptor {

    @Override
    public void onConstruct(EnhancedInstance enhancedInstance, Object[] objects) {
        enhancedInstance.setSkyWalkingDynamicField(getRemote(objects));
    }

    @Override
    public void beforeMethod(EnhancedInstance enhancedInstance, Method method, Object[] objects, Class<?>[] classes, MethodInterceptResult methodInterceptResult) throws Throwable {
        String peer = (String) enhancedInstance.getSkyWalkingDynamicField();
        AbstractSpan span = getSpan();
        if (span != null) {
            span.setPeer(peer);
        }
        SWContextCarrier swContextCarrier = getContextCarrier();
        if (swContextCarrier != null) {
            setPeerHost(swContextCarrier.carrier(), peer);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance enhancedInstance, Method method, Object[] objects, Class<?>[] classes, Object o) throws Throwable {
        return o;
    }

    @Override
    public void handleMethodException(EnhancedInstance enhancedInstance, Method method, Object[] objects, Class<?>[] classes, Throwable throwable) {

    }

    private String getRemote(Object[] objects) {
        if (objects == null || objects.length == 0) {
            return "";
        }
        if (objects[0] instanceof InetSocketAddress) {
            /*
             * Compatible with versions below 6.33.0
             * 6.33.0 and below use {@link java.net.SocketAddress} as parameter
             */
            return formatPeer((InetSocketAddress) objects[0]);
        } else if (objects[0] instanceof Address.Inet) {
            return formatPeer(((Address.Inet) objects[0]).addr());
        }
        return "";
    }

    private String formatPeer(InetSocketAddress socketAddress) {
        return socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort();
    }
}
