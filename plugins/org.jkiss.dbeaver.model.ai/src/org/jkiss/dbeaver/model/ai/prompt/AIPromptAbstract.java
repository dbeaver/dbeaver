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
package org.jkiss.dbeaver.model.ai.prompt;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIPromptGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for prompt generators.
 *
 * Each prompt must implement function 'PromptClass create(DBSLogicalDataSourceSupplier)' in order to support
 * prompt usage in chat conversations. It is used on persisted conversation loading.
 */
public abstract class AIPromptAbstract implements AIPromptGenerator {
    private final List<String> instructions = new ArrayList<>();
    private final List<String> examples = new ArrayList<>();
    private final List<String> contexts = new ArrayList<>();
    private final List<String> outputFormats = new ArrayList<>();

    protected AIPromptAbstract() {
    }

    public AIPromptAbstract addExamples(@NotNull String... examples) {
        this.examples.addAll(Arrays.asList(examples));
        return this;
    }

    public AIPromptAbstract addInstructions(@NotNull String... instructions) {
        this.instructions.addAll(Arrays.asList(instructions));
        return this;
    }

    public AIPromptAbstract addContexts(@NotNull String... contexts) {
        this.contexts.addAll(Arrays.asList(contexts));
        return this;
    }

    public AIPromptAbstract addOutputFormats(@NotNull String... outputFormats) {
        this.outputFormats.addAll(Arrays.asList(outputFormats));
        return this;
    }

    @NotNull
    public final String build() {
        StringBuilder prompt = new StringBuilder();

        if (!instructions.isEmpty()) {
            prompt.append("Instructions:\n");
            instructions.forEach(instruction -> prompt.append("- ").append(instruction).append("\n"));
        }

        if (!examples.isEmpty()) {
            prompt.append("\nExamples:\n");
            examples.forEach(example -> prompt.append("- ").append(example).append("\n"));
        }

        if (!contexts.isEmpty()) {
            prompt.append("\nContext:\n");
            contexts.forEach(context -> prompt.append("- ").append(context).append("\n"));
        }

        if (!outputFormats.isEmpty()) {
            prompt.append("\nOutput Format:\n");
            outputFormats.forEach(outputFormat -> prompt.append("- ").append(outputFormat).append("\n"));
        }

        return prompt.toString();
    }

    @NotNull
    @Override
    public AIPromptAbstract copy() {
        AIPromptAbstract copy = create();
        copy.goals.addAll(goals);
        copy.instructions.addAll(instructions);
        copy.examples.addAll(examples);
        copy.contexts.addAll(contexts);
        copy.outputFormats.addAll(outputFormats);
        return copy;
    }

    @NotNull
    protected abstract AIPromptAbstract create();
}
