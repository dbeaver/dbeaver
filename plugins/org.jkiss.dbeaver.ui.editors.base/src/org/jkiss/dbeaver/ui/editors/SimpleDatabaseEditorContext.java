/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class SimpleDatabaseEditorContext implements DatabaseEditorContext {

    private final DBPDataSourceContainer dataSourceContainer;

    public SimpleDatabaseEditorContext(DBPDataSourceContainer dataSourceContainer) {
        this.dataSourceContainer = dataSourceContainer;
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    @Override
    public DBSObject getSelectedObject() {
        return null;
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return null;
    }
}
