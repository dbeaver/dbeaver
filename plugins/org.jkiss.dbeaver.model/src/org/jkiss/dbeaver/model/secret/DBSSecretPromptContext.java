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
package org.jkiss.dbeaver.model.secret;

import org.jkiss.code.NotNull;

/** Signals that a scoped operation must not prompt for the DBeaver master password. */
public final class DBSSecretPromptContext {
    private static final ThreadLocal<Boolean> PROMPTS_DISABLED = new ThreadLocal<>();

    private DBSSecretPromptContext() {
    }

    /** Suppresses DBeaver master-password prompts on the current thread until the scope is closed. */
    @NotNull
    public static Scope disablePrompts() {
        boolean previouslyDisabled = isPromptDisabled();
        PROMPTS_DISABLED.set(Boolean.TRUE);
        return new Scope(previouslyDisabled);
    }

    public static boolean isPromptDisabled() {
        return Boolean.TRUE.equals(PROMPTS_DISABLED.get());
    }

    public static final class Scope implements AutoCloseable {
        private final boolean previouslyDisabled;

        private Scope(boolean previouslyDisabled) {
            this.previouslyDisabled = previouslyDisabled;
        }

        @Override
        public void close() {
            if (previouslyDisabled) {
                PROMPTS_DISABLED.set(Boolean.TRUE);
            } else {
                PROMPTS_DISABLED.remove();
            }
        }
    }
}
