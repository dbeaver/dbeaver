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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreClass;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableForeign;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * Postgre foreign table manager
 */
public class PostgreForeignTableManager extends PostgreTableManager {

    @Override
    protected PostgreTableForeign createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        PostgreSchema schema = (PostgreSchema)container;
        final PostgreTableForeign table = (PostgreTableForeign) schema.getDataSource().getServerType().createNewRelation(monitor, schema, PostgreClass.RelKind.f, copyFrom);
        if (CommonUtils.isEmpty(table.getName())) {
            setNewObjectName(monitor, schema, table);
        } else {
            table.setName(getNewChildName(monitor, schema, table.getName()));
        }

        return table;
    }

}
