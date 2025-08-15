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
package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;

public abstract class BaseCompletionEngine<PROPS extends AIEngineProperties> implements AIEngine {

    protected final PROPS properties;

    public BaseCompletionEngine() throws DBException {
        this.properties = AISettingsManager.getInstance().getSettings()
            .getEngineConfiguration(getEngineId());
    }

    public BaseCompletionEngine(@NotNull PROPS properties) throws DBException {
        this.properties = properties;
    }

    @Override
    public void requestCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException {
        if (useStreamMode()) {
            doRequestCompletionStream(monitor, request, listener);
        } else {
            AIEngineResponse response = requestCompletion(monitor, request);
            if (response.getFunctionCall() != null) {
                listener.nextChunk(new AIEngineResponseChunk(response.getFunctionCall()));
            } else if (response.getVariants() != null) {
                listener.nextChunk(new AIEngineResponseChunk(response.getVariants()));
            } else {
                listener.error(new DBException("Empty response"));
                return;
            }

            listener.close();
        }
    }

    protected abstract void doRequestCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException;

    @NotNull
    protected abstract String getEngineId();

    private static boolean useStreamMode() {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AIConstants.AI_USE_STREAM_MODE);
    }
}
