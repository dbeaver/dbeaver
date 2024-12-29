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
package org.jkiss.dbeaver.model.sql;

/**
 * Control command result.
 *
 * It may finish with no extra information or with parameters:
 *  - message: will be shown in UI
 *  - error: execution error will be shown in UI
 */
public class SQLControlResult {

    private Throwable error;
    private String message;
    private SQLScriptElement[] newElements;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public SQLScriptElement[] getNewElements() {
        return newElements;
    }

    public void setNewElements(SQLScriptElement[] newElements) {
        this.newElements = newElements;
    }

    public boolean isSuccess() {
        return error == null;
    }
}
