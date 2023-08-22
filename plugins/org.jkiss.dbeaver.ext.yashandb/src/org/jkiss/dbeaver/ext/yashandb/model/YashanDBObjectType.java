package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.util.HashMap;
import java.util.Map;

public enum YashanDBObjectType implements DBSObjectType {

    CONSTRAINT("CONSTRAINT", DBIcon.TREE_CONSTRAINT, YashanDBTableConstraint.class, null),
    EVALUATION_CONTEXT("EVALUATION CONTEXT", null, DBSObject.class, null),
    FOREIGN_KEY("FOREIGN KEY", DBIcon.TREE_FOREIGN_KEY, YashanDBTableForeignKey.class, null), // fake object
    INDEX("INDEX", DBIcon.TREE_INDEX, YashanDBTableIndex.class, new ObjectFinder() {
        @Override
        public YashanDBTableIndex findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException {
            return schema.indexCache.getObject(monitor, schema, objectName);
        }
    }),
    FUNCTION("FUNCTION", DBIcon.TREE_PROCEDURE, YashanDBProcedureStandalone.class, new ObjectFinder() {
        @Override
        public YashanDBProcedureStandalone findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException {
            return schema.proceduresCache.getObject(monitor, schema, objectName);
        }
    }),
    JOB("JOB", null, DBSObject.class, null),
    LOB("CONTENT", null, DBSObject.class, null),
    MATERIALIZED_VIEW("MATERIALIZED VIEW", DBIcon.TREE_VIEW, DBSObject.class, null),
    PACKAGE("PACKAGE", DBIcon.TREE_PACKAGE, YashanDBPackage.class, new ObjectFinder() {
        @Override
        public YashanDBPackage findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException {
            return schema.packageCache.getObject(monitor, schema, objectName);
        }
    }),
    PACKAGE_BODY("PACKAGE BODY", DBIcon.TREE_PACKAGE, YashanDBPackage.class, new ObjectFinder() {
        @Override
        public YashanDBPackage findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException {
            return schema.packageCache.getObject(monitor, schema, objectName);
        }
    }),
    PROCEDURE("PROCEDURE", DBIcon.TREE_PROCEDURE, YashanDBProcedureStandalone.class, new ObjectFinder() {
        @Override
        public YashanDBProcedureStandalone findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException {
            return schema.proceduresCache.getObject(monitor, schema, objectName);
        }
    }),
    SYNONYM("SYNONYM", DBIcon.TREE_SYNONYM, YashanDBSynonym.class, new ObjectFinder() {
        @Override
        public YashanDBSynonym findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException {
            return schema.synonymCache.getObject(monitor, schema, objectName);
        }
    }),
    SCHEDULE("SCHEDULE", null, DBSObject.class, null),
    SEQUENCE("SEQUENCE", DBIcon.TREE_SEQUENCE, YashanDBSequence.class, new ObjectFinder() {
        @Override
        public YashanDBSequence findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException {
            return schema.sequenceCache.getObject(monitor, schema, objectName);
        }
    }),
    TABLE("TABLE", DBIcon.TREE_TABLE, YashanDBTable.class, new ObjectFinder() {
        @Override
        public YashanDBTableBase findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException {
            return schema.tableCache.getObject(monitor, schema, objectName);
        }
    }),
    TABLE_PARTITION("TABLE PARTITION", null, DBSObject.class, null),
    TRIGGER("TRIGGER", DBIcon.TREE_TRIGGER, YashanDBTrigger.class, new ObjectFinder() {
        @Override
        public YashanDBTrigger findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException {
            // First we will try to find a trigger at the tables level.
            YashanDBTableTrigger trigger = schema.tableTriggerCache.getObject(monitor, schema, objectName);
            if (trigger != null) {
                return trigger;
            }
            // Nope. Now we will try to find a trigger at the schemas level.
            return schema.triggerCache.getObject(monitor, schema, objectName);
        }
    }),
    TYPE("TYPE", DBIcon.TREE_DATA_TYPE, YashanDBDataType.class, new ObjectFinder() {
        @Override
        public YashanDBDataType findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException {
            return schema.dataTypeCache.getObject(monitor, schema, objectName);
        }
    }),
    TYPE_BODY("TYPE BODY", DBIcon.TREE_DATA_TYPE, YashanDBDataType.class, new ObjectFinder() {
        @Override
        public YashanDBDataType findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException {
            return schema.dataTypeCache.getObject(monitor, schema, objectName);
        }
    }),
    VIEW("VIEW", DBIcon.TREE_VIEW, YashanDBView.class, new ObjectFinder() {
        @Override
        public YashanDBView findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException {
            return schema.tableCache.getObject(monitor, schema, objectName, YashanDBView.class);
        }
    });

    private static final Log log = Log.getLog(YashanDBObjectType.class);

    private final ObjectFinder finder;
    private final String objectType;
    private final DBPImage image;
    private final Class<? extends DBSObject> typeClass;

    <OBJECT_TYPE extends DBSObject> YashanDBObjectType(String objectType, DBPImage image, Class<OBJECT_TYPE> typeClass, ObjectFinder finder) {
        this.objectType = objectType;
        this.image = image;
        this.typeClass = typeClass;
        this.finder = finder;
    }

    private static Map<String, YashanDBObjectType> typeMap = new HashMap<>();

    public static YashanDBObjectType getByType(String typeName) {
        return typeMap.get(typeName);
    }

    static {
        for (YashanDBObjectType type : values()) {
            typeMap.put(type.getTypeName(), type);
        }
    }

    @Override
    public String getTypeName() {
        return objectType;
    }

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

    @Override
    public boolean isCompatibleWith(DBSObjectType other) {
        return false;
    }

    public static Object resolveObject(
            DBRProgressMonitor monitor,
            YashanDBDataSource dataSource,
            String dbLink,
            String objectTypeName,
            String objectOwner,
            String objectName) throws DBException {
        if (dbLink != null) {
            return objectName;
        }
        YashanDBObjectType objectType = YashanDBObjectType.getByType(objectTypeName);
        if (objectType == null) {
            log.debug("Unrecognized yashanDB object type: " + objectTypeName);
            return objectName;
        }
        if (!objectType.isBrowsable()) {
            log.debug("Unsupported yashanDB object type: " + objectTypeName);
            return objectName;
        }
        final YashanDBSchema schema = dataSource.getSchema(monitor, objectOwner);
        if (schema == null) {
            log.debug("Schema '" + objectOwner + "' not found");
            return objectName;
        }
        final DBSObject object = objectType.findObject(monitor, schema, objectName);
        if (object == null) {
            log.debug(objectTypeName + " '" + objectName + "' not found in '" + schema.getName() + "'");
            return objectName;
        }
        return object;
    }

    public boolean isBrowsable() {
        return finder != null;
    }

    public DBSObject findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException {
        if (finder != null) {
            return finder.findObject(monitor, schema, objectName);
        } else {
            return null;
        }
    }

    private static interface ObjectFinder {
        DBSObject findObject(DBRProgressMonitor monitor, YashanDBSchema schema, String objectName) throws DBException;
    }

    @Override
    public String toString() {
        return objectType;
    }

}
