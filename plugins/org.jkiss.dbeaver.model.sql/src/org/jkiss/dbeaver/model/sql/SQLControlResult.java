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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Control command result.
 *
 * It may finish with no extra information or with parameters:
 *  - message: will be shown in UI
 *  - error: execution error will be shown in UI
 */
public class SQLControlResult {

    private final boolean isSuccess;

    @Nullable
    private SQLScriptElement transformed;

    @NotNull
    public static SQLControlResult success() {
        return new SQLControlResult(true);
    }

    @NotNull
    public static SQLControlResult failure() {
        return new SQLControlResult(false);
    }

    @NotNull
    public static SQLControlResult transform(@NotNull SQLScriptElement element) {
        return new SQLControlResult(element, true);
    }


    private SQLControlResult(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    private SQLControlResult(@NotNull SQLScriptElement transformed, boolean isSuccess) {
        this.transformed = transformed;
        this.isSuccess = isSuccess;
    }

    @Nullable
    public SQLScriptElement getTransformed() {
        return transformed;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

}
