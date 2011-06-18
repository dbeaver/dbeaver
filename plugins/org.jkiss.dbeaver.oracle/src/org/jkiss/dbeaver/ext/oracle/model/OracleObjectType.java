/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.HashMap;
import java.util.Map;

/**
 * Object type
 */
public enum OracleObjectType implements DBSObjectType {
	CLUSTER("CLUSTER", null, DBSObject.class, null),
	CONSUMER_GROUP("CONSUMER GROUP", null, DBSObject.class, null),
	CONTEXT("CONTEXT", null, DBSObject.class, null),
	DIRECTORY("DIRECTORY", null, DBSObject.class, null),
	EVALUATION_CONTEXT("EVALUATION CONTEXT", null, DBSObject.class, null),
	FUNCTION("FUNCTION", DBIcon.TREE_PROCEDURE.getImage(), OracleProcedure.class, new ObjectFinder() {
        public OracleProcedure findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.proceduresCache.getObject(monitor, schema, objectName);
        }
    }),
	INDEX("INDEX", null, DBSObject.class, null),
	INDEX_PARTITION("INDEX PARTITION", null, DBSObject.class, null),
	INDEXTYPE("INDEXTYPE", null, DBSObject.class, null),
	JAVA_CLASS("JAVA CLASS", null, DBSObject.class, null),
	JAVA_DATA("JAVA DATA", null, DBSObject.class, null),
	JAVA_RESOURCE("JAVA RESOURCE", null, DBSObject.class, null),
	JOB("JOB", null, DBSObject.class, null),
	JOB_CLASS("JOB CLASS", null, DBSObject.class, null),
	LIBRARY("LIBRARY", null, DBSObject.class, null),
	LOB("LOB", null, DBSObject.class, null),
	MATERIALIZED_VIEW("MATERIALIZED VIEW", null, DBSObject.class, null),
	OPERATOR("OPERATOR", null, DBSObject.class, null),
	PACKAGE("PACKAGE", DBIcon.TREE_PACKAGE.getImage(), OraclePackage.class, new ObjectFinder() {
        public OraclePackage findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.packageCache.getObject(monitor, schema, objectName);
        }
    }),
	PACKAGE_BODY("PACKAGE BODY", DBIcon.TREE_PACKAGE.getImage(), OraclePackage.class, new ObjectFinder() {
        public OraclePackage findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.packageCache.getObject(monitor, schema, objectName);
        }
    }),
	PROCEDURE("PROCEDURE", DBIcon.TREE_PROCEDURE.getImage(), OracleProcedure.class, new ObjectFinder() {
        public OracleProcedure findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.proceduresCache.getObject(monitor, schema, objectName);
        }
    }),
	PROGRAM("PROGRAM", null, DBSObject.class, null),
	QUEUE("QUEUE", null, DBSObject.class, null),
	RULE("RULE", null, DBSObject.class, null),
	RULE_SET("RULE SET", null, DBSObject.class, null),
	SCHEDULE("SCHEDULE", null, DBSObject.class, null),
	SEQUENCE("SEQUENCE", DBIcon.TREE_SEQUENCE.getImage(), OracleSequence.class, new ObjectFinder() {
        public OracleSequence findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.sequenceCache.getObject(monitor, schema, objectName);
        }
    }),
	SYNONYM("SYNONYM", DBIcon.TREE_SYNONYM.getImage(), OracleSynonym.class, new ObjectFinder() {
        public OracleSynonym findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.synonymCache.getObject(monitor, schema, objectName);
        }
    }),
	TABLE("TABLE", DBIcon.TREE_TABLE.getImage(), OracleTable.class, new ObjectFinder() {
        public OracleTable findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.tableCache.getObject(monitor, schema, objectName, OracleTable.class);
        }
    }),
	TABLE_PARTITION("TABLE PARTITION", null, DBSObject.class, null),
	TRIGGER("TRIGGER", DBIcon.TREE_TRIGGER.getImage(), OracleTrigger.class, new ObjectFinder() {
        public OracleTrigger findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.triggerCache.getObject(monitor, schema, objectName);
        }
    }),
	TYPE("TYPE", DBIcon.TREE_DATA_TYPE.getImage(), OracleDataType.class, new ObjectFinder() {
        public OracleDataType findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.dataTypeCache.getObject(monitor, schema, objectName);
        }
    }),
	TYPE_BODY("TYPE BODY", DBIcon.TREE_DATA_TYPE.getImage(), OracleDataType.class, new ObjectFinder() {
        public OracleDataType findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.dataTypeCache.getObject(monitor, schema, objectName);
        }
    }),
	VIEW("VIEW", DBIcon.TREE_VIEW.getImage(), OracleView.class, new ObjectFinder() {
        public OracleView findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.tableCache.getObject(monitor, schema, objectName, OracleView.class);
        }
    }),
	WINDOW("WINDOW", null, DBSObject.class, null),
	WINDOW_GROUP("WINDOW GROUP", null, DBSObject.class, null),
	XML_SCHEMA("XML SCHEMA", null, DBSObject.class, null);
    
    private static Map<String, OracleObjectType> typeMap = new HashMap<String, OracleObjectType>();
    
    static {
        for (OracleObjectType type : values()) {
            typeMap.put(type.getTypeName(), type);
        }
    }
    
    public static OracleObjectType getByType(String typeName)
    {
        return typeMap.get(typeName);
    }
    
    private static interface ObjectFinder<OBJECT_TYPE extends DBSObject> {
        OBJECT_TYPE findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException;
    }
    
    private final String objectType;
    private final Image image;
    private final Class<? extends DBSObject> typeClass;
    private final ObjectFinder finder;

    <OBJECT_TYPE extends DBSObject> OracleObjectType(String objectType, Image image, Class<OBJECT_TYPE> typeClass, ObjectFinder<OBJECT_TYPE> finder)
    {
        this.objectType = objectType;
        this.image = image;
        this.typeClass = typeClass;
        this.finder = finder;
    }

    public boolean isBrowsable()
    {
        return finder != null;
    }

    public String getTypeName()
    {
        return objectType;
    }

    public String getDescription()
    {
        return null;
    }

    public Image getImage()
    {
        return image;
    }

    public Class<? extends DBSObject> getTypeClass()
    {
        return typeClass;
    }

    public DBSObject findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
    {
        if (finder != null) {
            return finder.findObject(monitor, schema, objectName);
        } else {
            return null;
        }
    }

}
