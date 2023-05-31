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
package org.jkiss.dbeaver.model.lsm.mapping;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.LinkedList;
import java.util.List;

public class ModelErrorsCollection {
    
    public static class ErrorInfo {
        public final Throwable exception;
        public final String message;

        public ErrorInfo(@Nullable Throwable exception, @NotNull String message) {
            this.exception = exception;
            this.message = message;
        }
    }
    
    private final List<ErrorInfo> errors = new LinkedList<>();

    public void add(@NotNull String message) {
        this.errors.add(new ErrorInfo(null, message));
    }

    public void add(@Nullable Throwable ex, @NotNull String message) {
        this.errors.add(new ErrorInfo(ex, message));
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    public void printToStderr() {
        for (ErrorInfo error : errors) {
            System.err.println(error.message);
            if (error.exception != null) {
                System.err.println("\t" + error.exception.toString().replace("\n", "\n\t"));
            }
        }
    }
    
}
