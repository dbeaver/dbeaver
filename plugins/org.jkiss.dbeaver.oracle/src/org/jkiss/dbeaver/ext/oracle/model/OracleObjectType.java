/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Object type
 */
public enum OracleObjectType {
	CLUSTER("CLUSTER", DBSObject.class, null),
	CONSUMER_GROUP("CONSUMER GROUP", DBSObject.class, null),
	CONTEXT("CONTEXT", DBSObject.class, null),
	DIRECTORY("DIRECTORY", DBSObject.class, null),
	EVALUATION_CONTEXT("EVALUATION CONTEXT", DBSObject.class, null),
	FUNCTION("FUNCTION", DBSObject.class, null),
	INDEX("INDEX", DBSObject.class, null),
	INDEX_PARTITION("INDEX PARTITION", DBSObject.class, null),
	INDEXTYPE("INDEXTYPE", DBSObject.class, null),
	JAVA_CLASS("JAVA CLASS", DBSObject.class, null),
	JAVA_DATA("JAVA DATA", DBSObject.class, null),
	JAVA_RESOURCE("JAVA RESOURCE", DBSObject.class, null),
	JOB("JOB", DBSObject.class, null),
	JOB_CLASS("JOB CLASS", DBSObject.class, null),
	LIBRARY("LIBRARY", DBSObject.class, null),
	LOB("LOB", DBSObject.class, null),
	MATERIALIZED_VIEW("MATERIALIZED VIEW", DBSObject.class, null),
	OPERATOR("OPERATOR", DBSObject.class, null),
	PACKAGE("PACKAGE", OraclePackage.class, new ObjectFinder() {
        public OraclePackage findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.packageCache.getObject(monitor, schema, objectName);
        }
    }),
	PACKAGE_BODY("PACKAGE BODY", OraclePackage.class, new ObjectFinder() {
        public OraclePackage findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.packageCache.getObject(monitor, schema, objectName);
        }
    }),
	PROCEDURE("PROCEDURE", OracleProcedure.class, new ObjectFinder() {
        public OracleProcedure findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.proceduresCache.getObject(monitor, schema, objectName);
        }
    }),
	PROGRAM("PROGRAM", DBSObject.class, null),
	QUEUE("QUEUE", DBSObject.class, null),
	RULE("RULE", DBSObject.class, null),
	RULE_SET("RULE SET", DBSObject.class, null),
	SCHEDULE("SCHEDULE", DBSObject.class, null),
	SEQUENCE("SEQUENCE", OracleSequence.class, new ObjectFinder() {
        public OracleSequence findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.sequenceCache.getObject(monitor, schema, objectName);
        }
    }),
	SYNONYM("SYNONYM", OracleSynonym.class, new ObjectFinder() {
        public OracleSynonym findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.synonymCache.getObject(monitor, schema, objectName);
        }
    }),
	TABLE("TABLE", OracleTable.class, new ObjectFinder() {
        public OracleTable findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.tableCache.getObject(monitor, schema, objectName, OracleTable.class);
        }
    }),
	TABLE_PARTITION("TABLE PARTITION", DBSObject.class, null),
	TRIGGER("TRIGGER", OracleTrigger.class, new ObjectFinder() {
        public OracleTrigger findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.triggerCache.getObject(monitor, schema, objectName);
        }
    }),
	TYPE("TYPE", OracleDataType.class, new ObjectFinder() {
        public OracleDataType findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.dataTypeCache.getObject(monitor, schema, objectName);
        }
    }),
	TYPE_BODY("TYPE BODY", OracleDataType.class, new ObjectFinder() {
        public OracleDataType findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.dataTypeCache.getObject(monitor, schema, objectName);
        }
    }),
	VIEW("VIEW", OracleView.class, new ObjectFinder() {
        public OracleView findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.tableCache.getObject(monitor, schema, objectName, OracleView.class);
        }
    }),
	WINDOW("WINDOW", DBSObject.class, null),
	WINDOW_GROUP("WINDOW GROUP", DBSObject.class, null),
	XML_SCHEMA("XML SCHEMA", DBSObject.class, null);
    
    private static Map<String, OracleObjectType> typeMap = new HashMap<String, OracleObjectType>();
    
    static {
        for (OracleObjectType type : values()) {
            typeMap.put(type.getObjectType(), type);
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
    private final Class<?> typeClass;
    private final ObjectFinder finder;

    <OBJECT_TYPE extends DBSObject> OracleObjectType(String objectType, Class<OBJECT_TYPE> typeClass, ObjectFinder<OBJECT_TYPE> finder)
    {
        this.objectType = objectType;
        this.typeClass = typeClass;
        this.finder = finder;
    }

    public String getObjectType()
    {
        return objectType;
    }

    public boolean isBrowsable()
    {
        return finder != null;
    }

    public Class<?> getTypeClass()
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
