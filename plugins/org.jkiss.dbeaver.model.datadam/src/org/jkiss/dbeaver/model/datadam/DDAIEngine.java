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
package org.jkiss.dbeaver.model.datadam;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClientResponses;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIEngine;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIRequestFilter;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Set;

public class DDAIEngine extends OpenAIEngine<DDAIEngineProperties> {

    public DDAIEngine(@NotNull DDAIEngineProperties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public List<AIModel> getModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        return openAiService.getInstance().getModels(monitor).stream()
            .map(model -> new AIModel(model.id(), null, Set.of(AIModelFeature.CHAT, AIModelFeature.STREAMING)))
            .toList();
    }

    @NotNull
    @Override
    protected OpenAIClientResponses createClient() throws DBException {
        String token = properties.getToken();
        if (CommonUtils.isEmpty(token)) {
            throw new DBException("DataDam API key is not set");
        }
        return new DDAIClient(
            properties.getBaseUrl(),
            List.of(new OpenAIRequestFilter(token))
        );
    }
}
