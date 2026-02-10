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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * AI function settings.
 * Stores function-related configuration: enabled/disabled states,
 * initialized categories tracking.
 */
public final class AIFunctionSettings {
    private boolean functionsEnabled = true;
    private final Set<String> enabledFunctionCategories = new LinkedHashSet<>();
    private final Set<String> enabledFunctions = new LinkedHashSet<>();
    private final Set<String> initializedDefaultCategories = new LinkedHashSet<>();

    AIFunctionSettings() {
    }

    public boolean isFunctionsEnabled() {
        return functionsEnabled;
    }

    public void setFunctionsEnabled(boolean functionsEnabled) {
        this.functionsEnabled = functionsEnabled;
    }

    @NotNull
    public Set<String> getEnabledFunctions() {
        return Set.copyOf(enabledFunctions);
    }

    public void setEnabledFunctions(@Nullable Set<String> functions) {
        this.enabledFunctions.clear();
        if (functions != null) {
            this.enabledFunctions.addAll(functions);
        }
    }

    public boolean isFunctionEnabled(@NotNull String functionId) {
        return enabledFunctions.contains(functionId);
    }

    public void enableFunction(@NotNull String functionId) {
        enabledFunctions.add(functionId);
    }

    public void disableFunction(@NotNull String functionId) {
        enabledFunctions.remove(functionId);
    }

    @NotNull
    public Set<String> getEnabledFunctionCategories() {
        return new HashSet<>(enabledFunctionCategories);
    }

    public void setEnabledFunctionCategories(@Nullable Set<String> categories) {
        this.enabledFunctionCategories.clear();
        if (categories != null) {
            this.enabledFunctionCategories.addAll(categories);
        }
    }

    public boolean isFunctionCategoryEnabled(@NotNull String category) {
        return enabledFunctionCategories.contains(category);
    }

    public void enableFunctionCategory(@NotNull String category) {
        enabledFunctionCategories.add(category);
    }

    public void disableFunctionCategory(@NotNull String category) {
        enabledFunctionCategories.remove(category);
    }

    @NotNull
    public Set<String> getInitializedDefaultCategories() {
        return new HashSet<>(initializedDefaultCategories);
    }

    public void setInitializedDefaultCategories(@Nullable Set<String> categories) {
        this.initializedDefaultCategories.clear();
        if (categories != null) {
            this.initializedDefaultCategories.addAll(categories);
        }
    }

    public void markCategoryAsInitialized(@NotNull String categoryId) {
        this.initializedDefaultCategories.add(categoryId);
    }

    public boolean isCategoryInitialized(@NotNull String categoryId) {
        return initializedDefaultCategories.contains(categoryId);
    }
}

