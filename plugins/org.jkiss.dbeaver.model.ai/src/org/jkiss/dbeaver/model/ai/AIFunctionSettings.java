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

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;

import java.util.*;

/**
 * AI function settings.
 * Stores function-related configuration: enabled/disabled states,
 * initialized categories tracking.
 */
public final class AIFunctionSettings {
    @SerializedName("enabled")
    private boolean functionsEnabled = true;
    private final Map<String, ToolboxSettings> functions = new LinkedHashMap<>();

    /**
     * Keeps information about function which were explicitly enabled or disabled for the particular toolbox.
     * User can modify enabled/disable functions set.
     */
    public static class ToolboxSettings {
        @SerializedName("enabled")
        private boolean enabled = true;
        @SerializedName("ef")
        private Set<String> enabledFunctions;
        @SerializedName("df")
        private Set<String> disabledFunctions;

        public ToolboxSettings() {
            this.enabledFunctions = new LinkedHashSet<>();
            this.disabledFunctions = new LinkedHashSet<>();
        }

        public ToolboxSettings(@NotNull ToolboxSettings src) {
            this.enabled = src.enabled;
            this.enabledFunctions = new LinkedHashSet<>(src.enabledFunctions);
            this.disabledFunctions = new LinkedHashSet<>(src.disabledFunctions);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @NotNull
        public Set<String> getEnabledFunctions() {
            return enabledFunctions;
        }

        public void setEnabledFunctions(@NotNull Collection<String> enabledFunctions) {
            this.enabledFunctions = new LinkedHashSet<>(enabledFunctions);
        }

        @NotNull
        public Set<String> getDisabledFunctions() {
            return disabledFunctions;
        }

        public void setDisabledFunctions(@NotNull Collection<String> disabledFunctions) {
            this.disabledFunctions = new LinkedHashSet<>(disabledFunctions);
        }

        public boolean isFunctionEnabled(@NotNull AIFunctionDescriptor function) {
            if (function.isEnabledByDefault()) {
                return !disabledFunctions.contains(function.getId());
            } else {
                return enabledFunctions.contains(function.getId());
            }
        }

    }

    public boolean isFunctionsEnabled() {
        return functionsEnabled;
    }

    public void setFunctionsEnabled(boolean functionsEnabled) {
        this.functionsEnabled = functionsEnabled;
    }

    @NotNull
    public ToolboxSettings getToolboxSettings(@NotNull AIToolbox toolbox) {
        return functions.computeIfAbsent(toolbox.getToolboxId(), s -> new ToolboxSettings());
    }

}

