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
    CODE_CUSHMAN("code-cushman-001"),
    CODE_DAVINCI("code-davinci-002"),
    TEXT_ADA("text-ada-001"),
    TEXT_CURIE("text-curie-001"),
    TEXT_BABBAGE("text-babbage-001"),
    TEXT_DAVINCI01("text-davinci-003"),
    TEXT_DAVINCI02("text-davinci-002"),
    TEXT_DAVINCI03("text-davinci-001");

    private final String name;

    /**
     * Gets GPT model by name
     */
    @NotNull
    public static GPTModel getByName(@NotNull String name) {
        Optional<GPTModel> model = Arrays.stream(values()).filter(it -> it.name.equals(name)).findFirst();
        return model.orElse(CODE_DAVINCI);
    }

    GPTModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
