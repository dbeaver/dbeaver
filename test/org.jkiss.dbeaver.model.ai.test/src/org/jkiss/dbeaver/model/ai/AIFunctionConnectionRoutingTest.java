/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.functions.AIFunctionParams;
import org.jkiss.dbeaver.model.ai.registry.AIFunctionInternalDescriptor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

public class AIFunctionConnectionRoutingTest extends DBeaverUnitTest {
    @Test
    public void externalConnectionSelectorsDoNotResolveDatabaseContexts() throws DBException {
        final AIFunctionContext.DatabaseContextProvider provider = Mockito.mock(AIFunctionContext.DatabaseContextProvider.class);
        final AIFunctionContext context = new AIFunctionContext(new VoidProgressMonitor(), null,
            Mockito.mock(AIPromptGenerator.class), Mockito.mock(AIChatConversation.class), provider);
        for (String parameter : List.of(AIFunctionParams.CONNECTION_ID_PARAM,
            AIFunctionParams.SOURCE_CONNECTION_ID_PARAM, AIFunctionParams.TARGET_CONNECTION_ID_PARAM)) {
            for (Object value : List.of("remote-service-connection", 42)) {
                AIFunctionDescriptor function = Mockito.mock(AIFunctionDescriptor.class, Mockito.CALLS_REAL_METHODS);
                Mockito.when(function.getParameter(parameter)).thenReturn(Mockito.mock(AIFunctionParameter.class));
                Map<String, Object> arguments = Map.of(parameter, value);
                AIFunctionContext selected = context.forFunction(function, arguments);
                Assertions.assertSame(context, selected);
                Assertions.assertEquals(value, arguments.get(parameter));
            }
        }
        Mockito.verifyNoInteractions(provider);
    }

    @Test
    public void externalEndpointParametersKeepTheOriginalContext() throws DBException {
        final AIFunctionContext.DatabaseContextProvider provider = Mockito.mock(AIFunctionContext.DatabaseContextProvider.class);
        final AIFunctionContext context = new AIFunctionContext(new VoidProgressMonitor(), Mockito.mock(AIDatabaseContext.class),
            Mockito.mock(AIPromptGenerator.class), Mockito.mock(AIChatConversation.class), provider);
        AIFunctionDescriptor function = Mockito.mock(AIFunctionDescriptor.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(function.getParameter(AIFunctionParams.SOURCE_CONNECTION_ID_PARAM))
            .thenReturn(Mockito.mock(AIFunctionParameter.class));
        Mockito.when(function.getParameter(AIFunctionParams.TARGET_CONNECTION_ID_PARAM))
            .thenReturn(Mockito.mock(AIFunctionParameter.class));
        Map<String, Object> arguments = Map.of(
            AIFunctionParams.CONNECTION_ID_PARAM, 12,
            AIFunctionParams.SOURCE_CONNECTION_ID_PARAM, 34,
            AIFunctionParams.TARGET_CONNECTION_ID_PARAM, 56);
        Assertions.assertSame(context, context.forFunction(function, arguments));
        Assertions.assertSame(context, context.forParameter(function, arguments, "sourceTable"));
        Assertions.assertSame(context, context.forParameter(function, arguments, "targetSchema"));
        Mockito.verifyNoInteractions(provider);
    }

    @Test
    public void internalRoutingRequiresDeclaredConnectionSelectors() {
        AIFunctionInternalDescriptor function = Mockito.mock(AIFunctionInternalDescriptor.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(null).when(function).getParameter(Mockito.anyString());
        Assertions.assertFalse(function.supportsConnectionRouting());
        for (String parameter : List.of(AIFunctionParams.CONNECTION_ID_PARAM,
            AIFunctionParams.SOURCE_CONNECTION_ID_PARAM, AIFunctionParams.TARGET_CONNECTION_ID_PARAM)) {
            Mockito.doReturn(Mockito.mock(AIFunctionParameter.class)).when(function).getParameter(parameter);
            Assertions.assertTrue(function.supportsConnectionRouting());
            Mockito.doReturn(null).when(function).getParameter(parameter);
            Assertions.assertFalse(function.supportsConnectionRouting());
        }
    }
}
