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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * AI Assistant response
 */
public class AIAssistantResponse {

    public enum Type {
        TEXT,
        FUNCTION,
        ERROR
    }

    private final Type type;
    private final Object result;
    private final AIMessageMeta meta;
    private List<AIFunctionReference> functionsRefs;

    public AIAssistantResponse(
        @NotNull Type type,
        @NotNull Object result,
        @NotNull AIMessageMeta meta
    ) {
        this.type = type;
        this.result = result;
        this.meta = meta;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    public boolean isText() {
        return type == Type.TEXT;
    }

    public boolean isError() {
        return type == Type.ERROR;
    }

    public String getText() {
        return CommonUtils.toString(result);
    }

    @NotNull
    public AIMessageMeta getMeta() {
        return meta;
    }

    @Nullable
    public List<AIFunctionReference> getFunctionsRefs() {
        return functionsRefs;
    }

    public void setFunctionsRefs(@NotNull List<AIFunctionReference> functionsRefs) {
        this.functionsRefs = functionsRefs;
    }

}
