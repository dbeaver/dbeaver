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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKeyColumn;

/**
 * Virtual FK column
 */
public class DBVEntityForeignKeyColumn implements DBSTableForeignKeyColumn {

    private static final Log log = Log.getLog(DBVEntityForeignKeyColumn.class);

    private final DBVEntityForeignKey foreignKey;
    private final String attributeName;
    private final String refAttributeName;

    public DBVEntityForeignKeyColumn(DBVEntityForeignKey foreignKey, String attributeName, String refAttributeName) {
        this.foreignKey = foreignKey;
        this.attributeName = attributeName;
        this.refAttributeName = refAttributeName;
    }

    public DBVEntityForeignKeyColumn(DBVEntityForeignKey foreignKey, DBVEntityForeignKeyColumn copy) {
        this.foreignKey = foreignKey;
        this.attributeName = copy.attributeName;
        this.refAttributeName = copy.refAttributeName;
    }

    @NotNull
    @Override
    public String getName() {
        return attributeName;
    }

    @Override
    public DBSEntityAttribute getAttribute() {
        return foreignKey.getEntity().getAttribute(new VoidProgressMonitor(), attributeName);
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getRefAttributeName() {
        return refAttributeName;
    }

    @Override
    public DBSEntityAttribute getReferencedColumn() {
        DBSEntity associatedEntity = foreignKey.getAssociatedEntity();
        try {
            return associatedEntity == null ? null : associatedEntity.getAttribute(new VoidProgressMonitor(), refAttributeName);
        } catch (DBException e) {
            log.error("Error getting virtual FK referenced column", e);
            return null;
        }
    }

    @Override
    public String toString() {
        return attributeName + ":" + refAttributeName;
    }
}
