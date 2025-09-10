/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIAssistant;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIPromptGenerator;
import org.jkiss.dbeaver.model.ai.AISqlFormatter;
import org.jkiss.dbeaver.model.ai.engine.*;
import org.jkiss.dbeaver.model.ai.registry.AIAssistantRegistry;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.ai.utils.ThrowableSupplier;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class AIAssistantImpl implements AIAssistant {
    private static final Log log = Log.getLog(AIAssistantImpl.class);

    private static final int MANY_REQUESTS_RETRIES = 3;
    private static final int MANY_REQUESTS_TIMEOUT = 500;
    public static final String LOG_INDENT = "\t";

    protected final DBPWorkspace workspace;

    protected final AIEngineRequestFactory requestFactory;
    protected AISqlFormatter sqlFormatter;

    public AIAssistantImpl(@NotNull DBPWorkspace workspace) {
        this.workspace = workspace;
        this.requestFactory = createRequestFactory();
        try {
            this.sqlFormatter = AIAssistantRegistry.getInstance().getDescriptor().createSqlFormatter();
        } catch (DBException e) {
            log.error("Error creating SQL formatter", e);
            this.sqlFormatter = new SimpleSqlFormatterImpl();
        }
    }

    protected AIEngineRequestFactory createRequestFactory() {
        return new AIEngineRequestFactory(
            new AIDatabaseSnapshotService(),
            new DummyTokenCounter()
        );
    }

    @NotNull
    @Override
    public String generateText(
        @NotNull DBRProgressMonitor monitor,
        @Nullable AIDatabaseContext context,
        @NotNull AIPromptGenerator systemGenerator,
        @NotNull List<AIMessage> messages
    ) throws DBException {
        checkAiEnablement();

        try (AIEngine engine = createEngine()) {
            String systemPrompt = systemGenerator.build();

            AIEngineRequest completionRequest = requestFactory.build(
                monitor,
                systemPrompt,
                context,
                messages,
                engine.getContextWindowSize(monitor)
            );

            AIEngineResponse completionResponse = requestCompletion(engine, monitor, completionRequest);

            return completionResponse.variants().getFirst();
        }
    }

    protected static void checkAiEnablement() throws DBException {
        if (AISettingsManager.getInstance().getSettings().isAiDisabled()) {
            throw new DBException("AI integration is disabled");
        }
    }

    public static String getActiveEngineId() {
        return AISettingsManager.getInstance().getSettings().activeEngine();
    }

    public boolean isEngineSupports(Class<?> api) {
        return AIEngineRegistry.getInstance().isEngineSupports(
            getActiveEngineId(),
            api);
    }

    @NotNull
    public AIEngine createEngine() throws DBException {
        return AIEngineRegistry.getInstance().createEngine(getActiveEngineId());
    }

    protected AIEngineResponse requestCompletion(
        @NotNull AIEngine engine,
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request
    ) throws DBException {
        try {
            boolean loggingEnabled = isLoggingEnabled();
            if (loggingEnabled) {
                log.debug("AI request:\n" + CommonUtils.addTextIndent(request.toString(), LOG_INDENT));
            }

            AIEngineResponse completionResponse = callWithRetry(() -> engine.requestCompletion(monitor, request));

            if (loggingEnabled) {
                log.debug("AI response:\n" + CommonUtils.addTextIndent(completionResponse.toString(), LOG_INDENT));
            }

            return completionResponse;
        } catch (Exception e) {
            if (e instanceof DBException) {
                throw (DBException) e;
            } else {
                throw new DBException("Error requesting completion", e);
            }
        }
    }

    protected boolean isLoggingEnabled() throws DBException {
        AIEngineProperties activeEngineConfiguration = getActiveEngineConfiguration();
        if (activeEngineConfiguration == null) {
            log.warn("No active AI engine configuration found");
            return false;
        }

        return activeEngineConfiguration.isLoggingEnabled();
    }

    @Nullable
    private AIEngineProperties getActiveEngineConfiguration() throws DBException {
        AISettingsManager settingsManager = AISettingsManager.getInstance();
        String activeEngine = settingsManager.getSettings().activeEngine();
        if (activeEngine == null || activeEngine.isEmpty()) {
            log.warn("No active AI engine configured");
            return null;
        }
        return settingsManager.getSettings().getEngineConfiguration(activeEngine);
    }


    protected static <T> T callWithRetry(ThrowableSupplier<T, DBException> supplier) throws DBException {
        int retry = 0;
        while (retry < MANY_REQUESTS_RETRIES) {
            try {
                return supplier.get();
            } catch (TooManyRequestsException e) {
                retry++;
                if (retry < MANY_REQUESTS_RETRIES) {
                    log.debug("Too many engine requests. Retry after " + MANY_REQUESTS_TIMEOUT + "ms");
                    RuntimeUtils.pause(MANY_REQUESTS_TIMEOUT);
                }
            }
        }
        throw new DBException("Request failed after " + MANY_REQUESTS_RETRIES + " attempts");
    }

}
