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
package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.oracle.edit.OracleTableManager;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTable;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

/**
 * YashanDBTableManager
 */
public class YashanDBTableManager extends OracleTableManager {

    @Override
    protected YashanDBTable createDatabaseObject(@NotNull DBRProgressMonitor monitor,
                                                 @NotNull DBECommandContext context, Object container, Object copyFrom,
                                                 @NotNull Map<String, Object> options) {
        YashanDBSchema schema = (YashanDBSchema) container;
        YashanDBTable table = new YashanDBTable(schema, ""); //$NON-NLS-1$
        setNewObjectName(monitor, schema, table);
        return table;
    }
}
