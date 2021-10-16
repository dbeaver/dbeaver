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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericObjectContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

/**
 * VerticaObjectType
 */
public enum VerticaObjectType implements DBSObjectType {

    TABLE("Table", "Vertica table", VerticaTable.class, DBIcon.TREE_TABLE),
    SYSTEM_TABLE("System table", "Vertica system table", VerticaSystemTable.class, DBIcon.TREE_TABLE),
    VIEW("View", "Vertica View", VerticaView.class, DBIcon.TREE_VIEW),
    PROJECTION("Projection", "Vertica Projection", VerticaProjection.class, DBIcon.TREE_TABLE_INDEX),
    NODE("Node", "Vertica Node", VerticaNode.class, DBIcon.TREE_SERVERS),
    SEQUENCE("Sequence", "Vertica Sequence", VerticaSequence.class, DBIcon.TREE_SEQUENCE);

    private final String typeName;
    private final String description;
    private final Class<? extends DBSObject> theClass;
    private final DBPImage icon;
    private static final Log log = Log.getLog(VerticaObjectType.class);

    VerticaObjectType(String type, String description, Class<? extends DBSObject> theClass, DBPImage icon) {
        this.typeName = type;
        this.description = description;
        this.theClass = theClass;
        this.icon = icon;
    }

    @Override
    public String getTypeName() {
        return typeName;
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

    public DBSObject findObject(DBRProgressMonitor monitor, GenericObjectContainer schema, String objectName) throws DBException {
        if (schema == null) {
            log.debug("Null schema in table " + objectName + " search (" + objectName + ")");
            return null;
        }

        if (GenericTable.class.isAssignableFrom(theClass) || VerticaView.class.isAssignableFrom(theClass)) {
            return schema.getChild(monitor, objectName);
        }
        if (schema instanceof VerticaSchema && VerticaProjection.class.isAssignableFrom(theClass)) {
            return ((VerticaSchema) schema).getProjection(monitor, objectName);
        }
        GenericDataSource dataSource = schema.getDataSource();
        if (dataSource instanceof VerticaDataSource && VerticaNode.class.isAssignableFrom(theClass)) {
            return (((VerticaDataSource) dataSource).getClusterNode(monitor, objectName));
        }

        log.debug("Unsupported object for Vertica search: " + name());
        return null;
    }
}