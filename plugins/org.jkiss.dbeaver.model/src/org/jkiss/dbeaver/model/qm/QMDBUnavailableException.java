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

package org.jkiss.dbeaver.model.qm;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

/**
 * Indicates that QMDB is unavailable for the current session.
 */
public class QMDBUnavailableException extends DBException {

    public static final String DEFAULT_MESSAGE = "QMDB managed server recovery is disabled for this session";

    public QMDBUnavailableException() {
        super(DEFAULT_MESSAGE);
    }

    public QMDBUnavailableException(@NotNull String message) {
        super(message);
    }

    public QMDBUnavailableException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }
}
