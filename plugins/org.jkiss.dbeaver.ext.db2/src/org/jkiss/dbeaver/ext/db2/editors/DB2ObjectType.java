/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.db2.model.*;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2Nickname;
import org.jkiss.dbeaver.ext.db2.model.module.DB2Module;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.util.HashMap;
import java.util.Map;

/**
 * DB2 Object type used by Search, Content Assist and object dependency resolution
 * 
 * @author Denis Forveille
 */
public enum DB2ObjectType implements DBSObjectType {

    ALIAS(DBIcon.TREE_SYNONYM, DB2Alias.class, new ObjectFinder() {
        @Override
        public DB2Alias findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getAliasCache().getObject(monitor, schema, objectName);
        }
    }),

    CHECK(DBIcon.TREE_CONSTRAINT, DB2TableCheckConstraint.class, new ObjectFinder() {
        @Override
        public DB2TableCheckConstraint findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName)
            throws DBException
        {
            return schema.getCheckCache().getObject(monitor, schema, objectName);
        }
    }),

    COLUMN(DBIcon.TREE_COLUMN, DB2TableColumn.class, new ObjectFinder() {
        @Override
        public DB2TableColumn findObject(DBRProgressMonitor monitor, DB2Table db2Table, String objectName) throws DBException
        {
            return db2Table.getAttribute(monitor, objectName);
        }

        @Override
        public DB2TableColumn findObject(DBRProgressMonitor monitor, DB2View db2View, String objectName) throws DBException
        {
            return db2View.getAttribute(monitor, objectName);
        }
    }),

    FOREIGN_KEY(DBIcon.TREE_FOREIGN_KEY, DB2TableForeignKey.class, new ObjectFinder() {
        @Override
        public DB2TableForeignKey findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getAssociationCache().getObject(monitor, schema, objectName);
        }
    }),

    MODULE(DBIcon.TREE_PACKAGE, DB2Module.class, new ObjectFinder() {

        @Override
        public DB2Module findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getModuleCache().getObject(monitor, schema, objectName);
        }
    }),

    MQT(DBIcon.TREE_TABLE, DB2MaterializedQueryTable.class, new ObjectFinder() {

        @Override
        public DB2MaterializedQueryTable findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName)
            throws DBException
        {
            return schema.getMaterializedQueryTableCache().getObject(monitor, schema, objectName);
        }
    }),

    INDEX(DBIcon.TREE_INDEX, DB2Index.class, new ObjectFinder() {
        @Override
        public DB2Index findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getIndexCache().getObject(monitor, schema, objectName);
        }
    }),

    NICKNAME(DBIcon.TREE_SYNONYM, DB2View.class, new ObjectFinder() {
        @Override
        public DB2Nickname findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getNicknameCache().getObject(monitor, schema, objectName, DB2Nickname.class);
        }
    }),

    PACKAGE(DBIcon.TREE_PACKAGE, DB2Package.class, new ObjectFinder() {
        @Override
        public DB2Package findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getPackageCache().getObject(monitor, schema, objectName);
        }
    }),

    PROCEDURE(DBIcon.TREE_PROCEDURE, DB2Routine.class, new ObjectFinder() {
        @Override
        public DB2Routine findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            // Procedure may be global or from a Module
            DB2Routine procedure;
            String[] tokens = objectName.split(SPLIT_DOT);
            if (tokens.length == 1) {
                procedure = schema.getProcedureCache().getObject(monitor, schema, tokens[0]);
            } else {
                DB2Module module = schema.getModuleCache().getObject(monitor, schema, tokens[0]);
                procedure = module.getProcedureCache().getObject(monitor, module, tokens[1]);
            }
            return procedure;
        }
    }),

    REFERENCE(DBIcon.TREE_REFERENCE, DB2TableReference.class, new ObjectFinder() {
        @Override
        public DB2TableReference findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getReferenceCache().getObject(monitor, schema, objectName);
        }
    }),

    ROUTINE(DBIcon.TREE_PROCEDURE, DB2Routine.class, new ObjectFinder() {
        @Override
        public DB2Routine findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            // Routines may be an UDF, a Method or a Procedure
            // Routines may be global or from a Module

            DB2Routine routine;
            String[] tokens = objectName.split(SPLIT_DOT);
            if (tokens.length == 1) {
                routine = schema.getUdfCache().getObject(monitor, schema, tokens[0]);
                if (routine == null) {
                    routine = schema.getProcedureCache().getObject(monitor, schema, tokens[0]);
                    if (routine == null) {
                        routine = schema.getMethodCache().getObject(monitor, schema, tokens[0]);
                    }
                }
            } else {
                DB2Module module = schema.getModuleCache().getObject(monitor, schema, tokens[0]);
                routine = module.getFunctionCache().getObject(monitor, module, tokens[1]);
                if (routine == null) {
                    routine = module.getProcedureCache().getObject(monitor, module, tokens[1]);
                    if (routine == null) {
                        routine = module.getMethodCache().getObject(monitor, module, tokens[1]);
                    }
                }
            }
            return routine;
        }
    }),

    SCHEMA(DBIcon.TREE_SCHEMA, DB2Schema.class, null),

    SEQUENCE(DBIcon.TREE_SEQUENCE, DB2Sequence.class, new ObjectFinder() {
        @Override
        public DB2Sequence findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getSequenceCache().getObject(monitor, schema, objectName);
        }
    }),

    TABLE(DBIcon.TREE_TABLE, DB2Table.class, new ObjectFinder() {
        @Override
        public DB2Table findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getTableCache().getObject(monitor, schema, objectName);
        }
    }),

    TABLESPACE(DBIcon.TREE_TABLESPACE, DB2Tablespace.class, new ObjectFinder() {
        @Override
        public DB2Tablespace findObject(DBRProgressMonitor monitor, DB2DataSource db2DataSource, String objectName)
            throws DBException
        {
            return db2DataSource.getTablespace(monitor, objectName);
        }
    }),

    TRIGGER(DBIcon.TREE_TABLE, DB2Trigger.class, new ObjectFinder() {
        @Override
        public DB2Trigger findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getTriggerCache().getObject(monitor, schema, objectName);
        }
    }),

    UDF(DBIcon.TREE_PROCEDURE, DB2Routine.class, new ObjectFinder() {
        @Override
        public DB2Routine findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            // Function may be global or from a Module
            DB2Routine udf;
            String[] tokens = objectName.split(SPLIT_DOT);
            if (tokens.length == 1) {
                udf = schema.getUdfCache().getObject(monitor, schema, tokens[0]);
            } else {
                DB2Module module = schema.getModuleCache().getObject(monitor, schema, tokens[0]);
                udf = module.getFunctionCache().getObject(monitor, module, tokens[1]);
            }
            return udf;
        }
    }),

    UDT(DBIcon.TREE_DATA_TYPE, DB2DataType.class, new ObjectFinder() {
        @Override
        public DB2DataType findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            // Type may be global or from a Module
            String[] tokens = objectName.split(SPLIT_DOT);
            if (tokens.length == 1) {
                return schema.getUdtCache().getObject(monitor, schema, tokens[0]);
            } else {
                DB2Module module = schema.getModuleCache().getObject(monitor, schema, tokens[0]);
                return module.getTypeCache().getObject(monitor, module, tokens[1]);
            }
        }
    }),

    UNIQUE_KEY(DBIcon.TREE_UNIQUE_KEY, DB2TableUniqueKey.class, new ObjectFinder() {
        @Override
        public DB2TableUniqueKey findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getConstraintCache().getObject(monitor, schema, objectName);
        }
    }),

    VIEW(DBIcon.TREE_VIEW, DB2View.class, new ObjectFinder() {
        @Override
        public DB2View findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getViewCache().getObject(monitor, schema, objectName, DB2View.class);
        }
    }),

    VARIABLE(DBIcon.TREE_ATTRIBUTE, DB2Variable.class, new ObjectFinder() {
        @Override
        public DB2Variable findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            // Variable may be global or from a Module
            DB2Variable variable;
            DB2DataSource db2DataSource = schema.getDataSource();
            String[] tokens = objectName.split(SPLIT_DOT);
            if (tokens.length == 1) {
                variable = db2DataSource.getVariableCache().getObject(monitor, db2DataSource, tokens[0]);
            } else {
                DB2Module module = schema.getModuleCache().getObject(monitor, schema, tokens[0]);
                variable = module.getVariableCache().getObject(monitor, module, tokens[1]);
            }
            return variable;
        }
    }),

    XML_SCHEMA(DBIcon.TREE_DATA_TYPE, DB2XMLSchema.class, new ObjectFinder() {
        @Override
        public DB2XMLSchema findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getXmlSchemaCache().getObject(monitor, schema, objectName);
        }
    });

    private final static Log LOG = Log.getLog(DB2ObjectType.class);
    private final static String SPLIT_DOT = "\\.";

    private final DBPImage image;
    private final Class<? extends DBSObject> typeClass;
    private final ObjectFinder finder;

    // -----------
    // Constructor
    // -----------
    <OBJECT_TYPE extends DBSObject> DB2ObjectType(DBPImage image, Class<OBJECT_TYPE> typeClass, ObjectFinder finder)
    {
        this.image = image;
        this.typeClass = typeClass;
        this.finder = finder;
    }

    @Override
    public String getTypeName()
    {
        return this.name();
    }

    public boolean isBrowsable()
    {
        return finder != null;
    }

    public DBSObject findObject(DBRProgressMonitor monitor, DB2DataSource db2DataSource, String objectName) throws DBException
    {
        if (finder != null) {
            return finder.findObject(monitor, db2DataSource, objectName);
        } else {
            return null;
        }
    }

    public DBSObject findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
    {
        if (finder != null) {
            return finder.findObject(monitor, schema, objectName);
        } else {
            return null;
        }
    }

    public DBSObject findObject(DBRProgressMonitor monitor, DB2Table db2Table, String objectName) throws DBException
    {
        if (finder != null) {
            return finder.findObject(monitor, db2Table, objectName);
        } else {
            return null;
        }
    }

    public DBSObject findObject(DBRProgressMonitor monitor, DB2View db2View, String objectName) throws DBException
    {
        if (finder != null) {
            return finder.findObject(monitor, db2View, objectName);
        } else {
            return null;
        }
    }

    // ----------------
    // Standard Getters
    // ----------------

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBPImage getImage()
    {
        return image;
    }

    @Override
    public Class<? extends DBSObject> getTypeClass()
    {
        return typeClass;
    }

    // ----------------
    // Helpers
    // ----------------

    private static class ObjectFinder {
        DBSObject findObject(DBRProgressMonitor monitor, DB2DataSource db2DataSource, String objectName) throws DBException
        {
            return null;
        }

        DBSObject findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return null;
        }

        DBSObject findObject(DBRProgressMonitor monitor, DB2Table db2Table, String objectName) throws DBException
        {
            return null;
        }

        DBSObject findObject(DBRProgressMonitor monitor, DB2View db2View, String objectName) throws DBException
        {
            return null;
        }

    }

    public static DB2ObjectType getByType(String typeName)
    {
        return typeMap.get(typeName);
    }

    // TODO DF: to be factorised
    public static Object resolveObject(DBRProgressMonitor monitor, DB2DataSource dataSource, String objectTypeName,
        String objectOwner, String objectName) throws DBException
    {
        DB2ObjectType objectType = DB2ObjectType.getByType(objectTypeName);
        if (objectType == null) {
            LOG.debug("Unrecognized object type: " + objectTypeName);
            return objectName;
        }
        if (!objectType.isBrowsable()) {
            LOG.debug("Unsupported object type: " + objectTypeName);
            return objectName;
        }
        final DB2Schema schema = dataSource.getSchema(monitor, objectOwner);
        if (schema == null) {
            LOG.debug("Schema '" + objectOwner + "' not found");
            return objectName;
        }
        final DBSObject object = objectType.findObject(monitor, schema, objectName);
        if (object == null) {
            LOG.debug(objectTypeName + " '" + objectName + "' not found in '" + schema.getName() + "'");
            return objectName;
        }
        return object;
    }

    // ---
    // Init
    // ---
    private static Map<String, DB2ObjectType> typeMap = new HashMap<>();

    static {
        for (DB2ObjectType type : values()) {
            typeMap.put(type.getTypeName(), type);
        }
    }

}
