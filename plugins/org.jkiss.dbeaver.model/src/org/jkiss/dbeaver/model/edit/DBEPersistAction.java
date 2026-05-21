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

package org.jkiss.dbeaver.model.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCSession;

/**
 * Database persist action
 */
public interface DBEPersistAction {

    enum ActionType {
        INITIALIZER,
        NORMAL,
        OPTIONAL,
        FINALIZER,
        COMMENT
    }

    @NotNull
    String getTitle();

    @NotNull
    String getScript();

    void beforeExecute(@NotNull DBCSession session) throws DBException;

    void afterExecute(@NotNull DBCSession session, @Nullable Throwable error) throws DBException;

    @NotNull
    ActionType getType();

    boolean isComplex();

}
