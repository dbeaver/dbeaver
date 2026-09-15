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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.qm.AIChatStorage;
import org.jkiss.dbeaver.model.ai.quota.QuotaStatus;
import org.jkiss.dbeaver.model.ai.quota.UserTokenQuotaService;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

public class AIChatSessionTest extends DBeaverUnitTest {
    @Test
    public void reportsEmptyCustomScopeWithoutConnectionWarning() throws DBException {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.isConnected()).thenReturn(true);
        AIContextSettings settings = Mockito.mock(AIContextSettings.class);
        Mockito.when(settings.getDataSourceContainer()).thenReturn(container);
        Mockito.when(settings.getScope()).thenReturn(AIDatabaseScope.CUSTOM);

        AIChatContextProvider contextProvider = Mockito.mock(AIChatContextProvider.class);
        Mockito.when(contextProvider.getExecutionContext(container)).thenReturn(Mockito.mock(DBCExecutionContext.class));
        UserTokenQuotaService quotaService = Mockito.mock(UserTokenQuotaService.class);
        Mockito.when(quotaService.getUserQuotaStatus("session", "engine")).thenReturn(QuotaStatus.EMPTY);
        AIChatSession session = new AIChatSession(
            Mockito.mock(DBPWorkspace.class), contextProvider, Mockito.mock(AIChatStorage.class), monitor -> "session", quotaService
        );
        AIConfigurationProfile profile = Mockito.mock(AIConfigurationProfile.class);
        Mockito.when(profile.getEngineId()).thenReturn("engine");
        AIPromptGenerator promptGenerator = Mockito.mock(AIPromptGenerator.class);
        Mockito.when(promptGenerator.configureDatabaseContext(Mockito.any(AIDatabaseContext.Builder.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        AIChatConversation conversation = Mockito.mock(AIChatConversation.class);
        Mockito.when(conversation.getProfile()).thenReturn(profile);
        Mockito.when(conversation.getPromptGenerator()).thenReturn(promptGenerator);
        AIChatResponseConsumer consumer = Mockito.mock(AIChatResponseConsumer.class);

        Assertions.assertSame(
            conversation,
            session.processAICompletion(new VoidProgressMonitor(), conversation, consumer, settings, null).join()
        );

        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(consumer).error(error.capture());
        Assertions.assertEquals(
            "Custom scope is empty. Add database objects or select All objects in the AI context settings.",
            error.getValue().getMessage()
        );
        Mockito.verify(consumer, Mockito.never()).warning(Mockito.anyString());
        Mockito.verify(consumer).complete(List.of(), true, false);
    }
}
