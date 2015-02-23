/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.editors;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Alias;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2DataType;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2MaterializedQueryTable;
import org.jkiss.dbeaver.ext.db2.model.DB2Package;
import org.jkiss.dbeaver.ext.db2.model.DB2Routine;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Sequence;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableCheckConstraint;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableForeignKey;
import org.jkiss.dbeaver.ext.db2.model.DB2TableReference;
import org.jkiss.dbeaver.ext.db2.model.DB2TableUniqueKey;
import org.jkiss.dbeaver.ext.db2.model.DB2Tablespace;
import org.jkiss.dbeaver.ext.db2.model.DB2Trigger;
import org.jkiss.dbeaver.ext.db2.model.DB2Variable;
import org.jkiss.dbeaver.ext.db2.model.DB2View;
import org.jkiss.dbeaver.ext.db2.model.DB2XMLSchema;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2Nickname;
import org.jkiss.dbeaver.ext.db2.model.module.DB2Module;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.HashMap;
import java.util.Map;

/**
 * DB2 Object type used by Search, Content Assist and object dependency resolution
 * 
 * @author Denis Forveille
 */
public enum DB2ObjectType implements DBSObjectType {

    ALIAS(DBIcon.TREE_SYNONYM.getImage(), DB2Alias.class, new ObjectFinder() {
        @Override
        public DB2Alias findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getAliasCache().getObject(monitor, schema, objectName);
        }
    }),

    CHECK(DBIcon.TREE_CONSTRAINT.getImage(), DB2TableCheckConstraint.class, new ObjectFinder() {
        @Override
        public DB2TableCheckConstraint findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName)
            throws DBException
        {
            return schema.getCheckCache().getObject(monitor, schema, objectName);
        }
    }),

    COLUMN(DBIcon.TREE_COLUMN.getImage(), DB2TableColumn.class, new ObjectFinder() {
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

    FOREIGN_KEY(DBIcon.TREE_FOREIGN_KEY.getImage(), DB2TableForeignKey.class, new ObjectFinder() {
        @Override
        public DB2TableForeignKey findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getAssociationCache().getObject(monitor, schema, objectName);
        }
    }),

    MODULE(DBIcon.TREE_PACKAGE.getImage(), DB2Module.class, new ObjectFinder() {

        @Override
        public DB2Module findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getModuleCache().getObject(monitor, schema, objectName);
        }
    }),

    MQT(DBIcon.TREE_TABLE.getImage(), DB2MaterializedQueryTable.class, new ObjectFinder() {

        @Override
        public DB2MaterializedQueryTable findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName)
            throws DBException
        {
            return schema.getMaterializedQueryTableCache().getObject(monitor, schema, objectName);
        }
    }),

    INDEX(DBIcon.TREE_INDEX.getImage(), DB2Index.class, new ObjectFinder() {
        @Override
        public DB2Index findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getIndexCache().getObject(monitor, schema, objectName);
        }
    }),

    NICKNAME(DBIcon.TREE_SYNONYM.getImage(), DB2View.class, new ObjectFinder() {
        @Override
        public DB2Nickname findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getNicknameCache().getObject(monitor, schema, objectName, DB2Nickname.class);
        }
    }),

    PACKAGE(DBIcon.TREE_PACKAGE.getImage(), DB2Package.class, new ObjectFinder() {
        @Override
        public DB2Package findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getPackageCache().getObject(monitor, schema, objectName);
        }
    }),

    PROCEDURE(DBIcon.TREE_PROCEDURE.getImage(), DB2Routine.class, new ObjectFinder() {
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

    REFERENCE(DBIcon.TREE_REFERENCE.getImage(), DB2TableReference.class, new ObjectFinder() {
        @Override
        public DB2TableReference findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getReferenceCache().getObject(monitor, schema, objectName);
        }
    }),

    ROUTINE(DBIcon.TREE_PROCEDURE.getImage(), DB2Routine.class, new ObjectFinder() {
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

    SCHEMA(DBIcon.TREE_SCHEMA.getImage(), DB2Schema.class, null),

    SEQUENCE(DBIcon.TREE_SEQUENCE.getImage(), DB2Sequence.class, new ObjectFinder() {
        @Override
        public DB2Sequence findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getSequenceCache().getObject(monitor, schema, objectName);
        }
    }),

    TABLE(DBIcon.TREE_TABLE.getImage(), DB2Table.class, new ObjectFinder() {
        @Override
        public DB2Table findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getTableCache().getObject(monitor, schema, objectName);
        }
    }),

    TABLESPACE(DBIcon.TREE_TABLESPACE.getImage(), DB2Tablespace.class, new ObjectFinder() {
        @Override
        public DB2Tablespace findObject(DBRProgressMonitor monitor, DB2DataSource db2DataSource, String objectName)
            throws DBException
        {
            return db2DataSource.getTablespace(monitor, objectName);
        }
    }),

    TRIGGER(DBIcon.TREE_TABLE.getImage(), DB2Trigger.class, new ObjectFinder() {
        @Override
        public DB2Trigger findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getTriggerCache().getObject(monitor, schema, objectName);
        }
    }),

    UDF(DBIcon.TREE_PROCEDURE.getImage(), DB2Routine.class, new ObjectFinder() {
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

    UDT(DBIcon.TREE_DATA_TYPE.getImage(), DB2DataType.class, new ObjectFinder() {
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

    UNIQUE_KEY(DBIcon.TREE_UNIQUE_KEY.getImage(), DB2TableUniqueKey.class, new ObjectFinder() {
        @Override
        public DB2TableUniqueKey findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getConstraintCache().getObject(monitor, schema, objectName);
        }
    }),

    VIEW(DBIcon.TREE_VIEW.getImage(), DB2View.class, new ObjectFinder() {
        @Override
        public DB2View findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getViewCache().getObject(monitor, schema, objectName, DB2View.class);
        }
    }),

    VARIABLE(DBIcon.TREE_ATTRIBUTE.getImage(), DB2Variable.class, new ObjectFinder() {
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

    XML_SCHEMA(DBIcon.TREE_DATA_TYPE.getImage(), DB2XMLSchema.class, new ObjectFinder() {
        @Override
        public DB2XMLSchema findObject(DBRProgressMonitor monitor, DB2Schema schema, String objectName) throws DBException
        {
            return schema.getXmlSchemaCache().getObject(monitor, schema, objectName);
        }
    });

    private final static Log LOG = Log.getLog(DB2ObjectType.class);
    private final static String SPLIT_DOT = "\\.";

    private final Image image;
    private final Class<? extends DBSObject> typeClass;
    private final ObjectFinder finder;

    // -----------
    // Constructor
    // -----------
    <OBJECT_TYPE extends DBSObject> DB2ObjectType(Image image, Class<OBJECT_TYPE> typeClass, ObjectFinder finder)
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
    public Image getImage()
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
    private static Map<String, DB2ObjectType> typeMap = new HashMap<String, DB2ObjectType>();

    static {
        for (DB2ObjectType type : values()) {
            typeMap.put(type.getTypeName(), type);
        }
    }

}
