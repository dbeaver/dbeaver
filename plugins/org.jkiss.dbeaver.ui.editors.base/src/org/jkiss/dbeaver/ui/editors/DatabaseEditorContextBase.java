/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObject;

public final class DatabaseEditorContextBase implements DatabaseEditorContext {
    private final DBPDataSourceContainer container;
    private final DBSObject object;

    public DatabaseEditorContextBase(@NotNull DBPDataSourceContainer container, @Nullable DBSObject object) {
        this.container = container;
        this.object = object;
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        return null;
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return container;
    }

    @Nullable
    @Override
    public DBSObject getSelectedObject() {
        return object;
    }
}
