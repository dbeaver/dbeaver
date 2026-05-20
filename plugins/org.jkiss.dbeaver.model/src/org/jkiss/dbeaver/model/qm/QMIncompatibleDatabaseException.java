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

/**
 * Indicates that existing QM data is incompatible with this client version.
 */
public class QMIncompatibleDatabaseException extends QMUnavailableException {

    public static final String DEFAULT_MESSAGE =
        "QMDB data was created by a newer DBeaver version and is not supported by this client";

    public QMIncompatibleDatabaseException() {
        super(DEFAULT_MESSAGE);
    }

    public QMIncompatibleDatabaseException(@NotNull Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }

    public QMIncompatibleDatabaseException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }
}
