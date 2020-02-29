/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.util.HashMap;
import java.util.Map;

public enum ExasolObjectType implements DBSObjectType {
    COLUMN(DBIcon.TREE_COLUMN, ExasolTableColumn.class, new ObjectFinder() {
        @Override
        public ExasolTableColumn findObject(DBRProgressMonitor monitor, ExasolTable exasolTable, String objectName) throws DBException {
            return exasolTable.getAttribute(monitor, objectName);
        }
    }),
    SCHEMA(DBIcon.TREE_SCHEMA, ExasolSchema.class, null),
    TABLE(DBIcon.TREE_TABLE, ExasolTable.class, new ObjectFinder() {
        @Override
        public ExasolTable findObject(DBRProgressMonitor monitor, ExasolSchema schema, String objectname) throws DBException {
            return schema.getTableCache().getObject(monitor, schema, objectname);
        }

    }),
    VIEW(DBIcon.TREE_VIEW, ExasolView.class, new ObjectFinder());


    private final DBPImage image;
    private final Class<? extends DBSObject> typeClass;
    private final ObjectFinder finder;


    // -----------
    // Constructor
    // -----------
    <OBJECT_TYPE extends DBSObject> ExasolObjectType(DBPImage image, Class<OBJECT_TYPE> typeClass, ObjectFinder finder) {
        this.image = image;
        this.typeClass = typeClass;
        this.finder = finder;
    }


    @Override
    public String getTypeName() {
        return this.name();
    }

    public boolean isBrowsable() {
        return finder != null;
    }

    public DBSObject findObject(DBRProgressMonitor monitor, ExasolDataSource exasolDataSource, String objectName) throws DBException {
        if (finder != null) {
            return finder.findObject(monitor, exasolDataSource, objectName);
        } else {
            return null;
        }
    }

    public DBSObject findObject(DBRProgressMonitor monitor, ExasolSchema schema, String objectName) throws DBException {
        if (finder != null) {
            return finder.findObject(monitor, schema, objectName);
        } else {
            return null;
        }
    }

    public DBSObject findObject(DBRProgressMonitor monitor, ExasolTable exasolTable, String objectName) throws DBException {
        if (finder != null) {
            return finder.findObject(monitor, exasolTable, objectName);
        } else {
            return null;
        }
    }

    // ----------------
    // Standard Getters
    // ----------------

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public DBPImage getImage() {
        return image;
    }

    @Override
    public Class<? extends DBSObject> getTypeClass() {
        return typeClass;
    }

    // ----------------
    // Helpers
    // ----------------

    private static class ObjectFinder {
        DBSObject findObject(DBRProgressMonitor monitor, ExasolDataSource exasolDataSource, String objectName) throws DBException {
            return null;
        }

        DBSObject findObject(DBRProgressMonitor monitor, ExasolSchema schema, String objectName) throws DBException {
            return null;
        }

        DBSObject findObject(DBRProgressMonitor monitor, ExasolTable exasolTable, String objectName) throws DBException {
            return null;
        }


    }

    public static ExasolObjectType getByType(String typename) {
        return typeMap.get(typename);
    }

    // ---
    // Init
    // ---
    private static Map<String, ExasolObjectType> typeMap = new HashMap<>();

    static {
        for (ExasolObjectType type : values()) {
            typeMap.put(type.getTypeName(), type);
        }
    }


}
