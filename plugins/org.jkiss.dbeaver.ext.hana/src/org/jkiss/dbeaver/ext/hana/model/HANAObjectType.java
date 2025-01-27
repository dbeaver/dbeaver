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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericObjectContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

public enum HANAObjectType implements DBSObjectType {

    PROCEDURE("PROCEDURE", "HANA procedure", HANAProcedure.class, DBIcon.TREE_PROCEDURE),
    SYNONYM("SYNONYM", "HANA synonym", HANASynonym.class, DBIcon.TREE_SYNONYM),
    TABLE("TABLE", "HANA table", GenericTable.class, DBIcon.TREE_TABLE),
    VIEW("VIEW", "HANA view", HANAView.class, DBIcon.TREE_VIEW),
    SCHEMA("SCHEMA", "HANA schema", HANASchema.class, DBIcon.TREE_SCHEMA);

    private final String type;
    private final String description;
    private final Class<? extends DBSObject> theClass;
    private final DBPImage icon;

    private static final Log log = Log.getLog(HANAObjectType.class);

    HANAObjectType(String type, String description, Class<? extends DBSObject> theClass, DBPImage icon) {
        this.type = type;
        this.description = description;
        this.theClass = theClass;
        this.icon = icon;
    }

    @Override
    public String getTypeName() {
        return type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public DBPImage getImage() {
        return icon;
    }

    @Override
    public Class<? extends DBSObject> getTypeClass() {
        return theClass;
    }

    @Override
    public String toString() {
        return type;
    }

    public DBSObject findObject(DBRProgressMonitor monitor, GenericObjectContainer schema, String objectName) throws DBException {
        if (schema == null) {
            log.debug("Null schema in table " + objectName + " search (" + objectName + ")");
            return null;
        }

        if (GenericTable.class.isAssignableFrom(theClass) || HANAView.class.isAssignableFrom(theClass)) {
            return schema.getChild(monitor, objectName);
        } else if (HANAProcedure.class.isAssignableFrom(theClass)) {
            return schema.getProcedure(monitor, objectName);
        } else if (HANASynonym.class.isAssignableFrom(theClass)) {
            return schema.getSynonym(monitor, objectName);
        } else {
            log.debug("Unsupported object for SQL Server search: " + name());
            return null;
        }
    }
}
