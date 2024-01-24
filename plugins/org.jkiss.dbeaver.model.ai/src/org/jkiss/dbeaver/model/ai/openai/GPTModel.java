/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.openai;

import org.jkiss.code.NotNull;

import java.util.Arrays;
import java.util.Optional;

public enum GPTModel {
    GPT_TURBO("gpt-3.5-turbo", 4096, true),
    GPT_TURBO16("gpt-3.5-turbo-16k", 16384, true),
    GPT_TURBO_INSTRUCT("gpt-3.5-turbo-instruct", 4096, false),
    GPT_4("gpt-4", 8192, true),
    @Deprecated
    TEXT_ADA("text-ada-001", 2048, false, GPT_TURBO_INSTRUCT),
    @Deprecated
    TEXT_CURIE("text-curie-001", 2048, false, GPT_TURBO_INSTRUCT),
    @Deprecated
    TEXT_BABBAGE("text-babbage-001", 2048, false, GPT_TURBO_INSTRUCT),
    @Deprecated
    TEXT_DAVINCI01("text-davinci-003", 4096, false, GPT_TURBO_INSTRUCT),
    @Deprecated
    TEXT_DAVINCI02("text-davinci-002", 4096, false, GPT_TURBO_INSTRUCT),
    @Deprecated
    TEXT_DAVINCI03("text-davinci-001", 2048, false, GPT_TURBO_INSTRUCT);

    private final String name;
    private final int maxTokens;
    private final boolean isChatAPI;

    private GPTModel deprecationReplacementModel = null;

    /**
     * Gets GPT model by name
     */
    @NotNull
    public static GPTModel getByName(@NotNull String name) {
        Optional<GPTModel> model = Arrays.stream(values()).filter(it -> it.name.equals(name)).findFirst();
        GPTModel gptModel = model.orElse(GPT_TURBO16);
        if (gptModel.getDeprecationReplacementModel() != null) {
            return gptModel.getDeprecationReplacementModel();
        } else {
            return gptModel;
        }
    }

    GPTModel(String name, int maxTokens, boolean isChatAPI) {
        this.name = name;
        this.maxTokens = maxTokens;
        this.isChatAPI = isChatAPI;
    }

    GPTModel(String name, int maxTokens, boolean isChatAPI, GPTModel deprecationReplacementModel) {
        this.name = name;
        this.maxTokens = maxTokens;
        this.isChatAPI = isChatAPI;
        this.deprecationReplacementModel = deprecationReplacementModel;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public boolean isChatAPI() {
        return isChatAPI;
    }

    public String getName() {
        return name;
    }

    public GPTModel getDeprecationReplacementModel() {
        return deprecationReplacementModel;
    }
}
