/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.gpt3;

import org.jkiss.code.NotNull;

import java.util.Arrays;
import java.util.Optional;

public enum GPTModel {
    GPT_TURBO("gpt-3.5-turbo", 2048, true),
    TEXT_ADA("text-ada-001", 2048, false),
    TEXT_CURIE("text-curie-001", 2048, false),
    TEXT_BABBAGE("text-babbage-001", 2048, false),
    TEXT_DAVINCI01("text-davinci-003", 4096, false),
    TEXT_DAVINCI02("text-davinci-002", 4096, false),
    TEXT_DAVINCI03("text-davinci-001", 2048, false);

    private final String name;
    private final int maxTokens;
    private final boolean isChatAPI;

    /**
     * Gets GPT model by name
     */
    @NotNull
    public static GPTModel getByName(@NotNull String name) {
        Optional<GPTModel> model = Arrays.stream(values()).filter(it -> it.name.equals(name)).findFirst();
        return model.orElse(GPT_TURBO);
    }

    GPTModel(String name, int maxTokens, boolean isChatAPI) {
        this.name = name;
        this.maxTokens = maxTokens;
        this.isChatAPI = isChatAPI;
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
}
