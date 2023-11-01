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

package org.jkiss.dbeaver.model.ai.completion;

import org.eclipse.core.runtime.Assert;
import org.jkiss.code.NotNull;

/**
 * Completion request
 */
public class DAICompletionRequest {
    private final String promptText;
    private final DAICompletionContext context;

    private DAICompletionRequest(
        @NotNull String promptText,
        @NotNull DAICompletionContext context
    ) {
        this.promptText = promptText;
        this.context = context;
    }

    @NotNull
    public String getPromptText() {
        return promptText;
    }

    @NotNull
    public DAICompletionContext getContext() {
        return context;
    }

    public static class Builder {
        private String promptText;
        private DAICompletionContext context;

        @NotNull
        public Builder setPromptText(@NotNull String promptText) {
            this.promptText = promptText;
            return this;
        }

        @NotNull
        public Builder setContext(@NotNull DAICompletionContext context) {
            this.context = context;
            return this;
        }

        @NotNull
        public DAICompletionRequest build() {
            Assert.isLegal(promptText != null, "Prompt text must be specified");
            Assert.isLegal(context != null, "Context must be specified");

            return new DAICompletionRequest(promptText, context);
        }
    }
}
