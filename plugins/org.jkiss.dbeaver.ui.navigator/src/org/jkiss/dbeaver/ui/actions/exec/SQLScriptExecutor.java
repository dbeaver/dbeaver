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
package org.jkiss.dbeaver.ui.actions.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.nio.file.Path;

/**
 * Allows opening native execution wizards
 *
 * @param <CONTAINER> container to read settings from
 */
public interface SQLScriptExecutor<CONTAINER extends DBSObject> {
    /**
     * Opens the wizard for the database
     *
     * @param container container to read settings from
     * @param file SQL file
     * @throws DBException if failed to open the wizard
     */
    void execute(@NotNull CONTAINER container, @Nullable Path file) throws DBException;

}
