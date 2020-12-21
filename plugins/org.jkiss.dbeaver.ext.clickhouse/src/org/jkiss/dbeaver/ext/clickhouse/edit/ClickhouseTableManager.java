/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseTableColumn;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * Clickhouse table manager
 */
public class ClickhouseTableManager extends GenericTableManager {

    private static final Class<?>[] CHILD_TYPES = {
            ClickhouseTableColumn.class,
            GenericUniqueKey.class,
            GenericTableForeignKey.class,
            GenericTableIndex.class
    };

    @NotNull
    @Override
    public Class<?>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Override
    public Collection<? extends DBSObject> getChildObjects(DBRProgressMonitor monitor, GenericTableBase object, Class<? extends DBSObject> childType) throws DBException {
        if (childType == ClickhouseTableColumn.class) {
            return object.getAttributes(monitor);
        }
        return super.getChildObjects(monitor, object, childType);
    }

    @Override
    protected String getDropTableType(GenericTableBase table) {
        // Both tables and views must be deleted with DROP TABLE
        return "TABLE";
    }

}
