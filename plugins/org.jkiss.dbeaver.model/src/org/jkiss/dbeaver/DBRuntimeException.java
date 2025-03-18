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
package org.jkiss.dbeaver;

import java.io.Serial;

/**
 * It's like {@link org.jkiss.dbeaver.DBException}, but unchecked.
 */
public class DBRuntimeException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -2505466592570927749L;

    /**
     * Constructs a new exception with the specified cause.
     * @param cause the cause
     */
    public DBRuntimeException(Throwable cause) {
        super(cause);
    }

    /** Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public DBRuntimeException(String message) {
        super(message);
    }
}
