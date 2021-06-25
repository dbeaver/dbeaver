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
package org.jkiss.dbeaver.ext.vertica.edit;

import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.vertica.VerticaConstants;
import org.jkiss.dbeaver.ext.vertica.model.VerticaFlexTable;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

public class VerticaFlexTableManager extends GenericTableManager {

    @Override
    protected GenericTableBase createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) {
        GenericStructContainer structContainer = (GenericStructContainer) container;
        String tableName = getNewChildName(monitor, structContainer, "new_flex_table");
        return structContainer.getDataSource().getMetaModel().createTableImpl((GenericStructContainer) container , tableName, VerticaConstants.TYPE_FLEX_TABLE, null);
    }

    @Override
    protected String getCreateTableType(GenericTableBase table) {
        if (table instanceof VerticaFlexTable) {
            return VerticaConstants.TYPE_FLEX_TABLE;
        }
        return super.getCreateTableType(table);
    }

    @Override
    protected String getDropTableType(GenericTableBase table) {
        return "TABLE";
    }
}
