package org.jkiss.dbeaver.ext.dm.model;

import java.util.HashMap;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

public enum DmObjectType implements DBSObjectType {

	CLUSTER("CLUSTER", null, DBSObject.class, null),
	MATERIALIZED_VIEW("MATERIALIZED VIEW", DBIcon.TREE_VIEW, DBSObject.class, null),
	TRIGGER("TRIGGER", DBIcon.TREE_TRIGGER, DmTrigger.class, new ObjectFinder() {
		@Override
		public DmTrigger findObject(DBRProgressMonitor monitor, DmSchema schema, String objectName) throws DBException {
			return schema.triggerCache.getObject(monitor, schema, objectName);
		}
	}), TYPE("TYPE", DBIcon.TREE_DATA_TYPE, DmDataType.class, new ObjectFinder() {
		@Override
		public DmDataType findObject(DBRProgressMonitor monitor, DmSchema schema, String objectName)
				throws DBException {
			return schema.dataTypeCache.getObject(monitor, schema, objectName);
		}
	}), TYPE_BODY("TYPE BODY", DBIcon.TREE_DATA_TYPE, DmDataType.class, new ObjectFinder() {
		@Override
		public DmDataType findObject(DBRProgressMonitor monitor, DmSchema schema, String objectName)
				throws DBException {
			return schema.dataTypeCache.getObject(monitor, schema, objectName);
		}
	}), TABLE("TABLE", DBIcon.TREE_TABLE, DmTable.class, new ObjectFinder() {
		@Override
		public DmTableBase findObject(DBRProgressMonitor monitor, DmSchema schema, String objectName)
				throws DBException {
			return schema.tableCache.getObject(monitor, schema, objectName);
		}
	}), INDEX("INDEX", DBIcon.TREE_INDEX, DmTableIndex.class, new ObjectFinder() {
        @Override
        public DmTableIndex findObject(DBRProgressMonitor monitor, DmSchema schema, String objectName) throws DBException
        {
            return schema.indexCache.getObject(monitor, schema, objectName);
        }
    }),
	VIEW("VIEW", DBIcon.TREE_VIEW, DmView.class, new ObjectFinder() {
		@Override
		public DmView findObject(DBRProgressMonitor monitor, DmSchema schema, String objectName) throws DBException {
			return schema.tableCache.getObject(monitor, schema, objectName, DmView.class);
		}
	}), PROCEDURE("PROCEDURE", DBIcon.TREE_PROCEDURE, DmProcedureStandalone.class, new ObjectFinder() {
		@Override
		public DmProcedureStandalone findObject(DBRProgressMonitor monitor, DmSchema schema, String objectName)
				throws DBException {
			return schema.proceduresCache.getObject(monitor, schema, objectName);
		}
	}), FUNCTION("FUNCTION", DBIcon.TREE_PROCEDURE, DmProcedureStandalone.class, new ObjectFinder() {
		@Override
		public DmProcedureStandalone findObject(DBRProgressMonitor monitor, DmSchema schema, String objectName)
				throws DBException {
			return schema.proceduresCache.getObject(monitor, schema, objectName);
		}
	}), PACKAGE("PACKAGE", DBIcon.TREE_PACKAGE, DmPackage.class, new ObjectFinder() {
		@Override
		public DmPackage findObject(DBRProgressMonitor monitor, DmSchema schema, String objectName) throws DBException {
			return schema.packageCache.getObject(monitor, schema, objectName);
		}
	}), PACKAGE_BODY("PACKAGE BODY", DBIcon.TREE_PACKAGE, DmPackage.class, new ObjectFinder() {
		@Override
		public DmPackage findObject(DBRProgressMonitor monitor, DmSchema schema, String objectName)
				throws DBException {
			return schema.packageCache.getObject(monitor, schema, objectName);
		}
	});

	private static final Log log = Log.getLog(DmObjectType.class);

	private static Map<String, DmObjectType> typeMap = new HashMap<>();

	private static interface ObjectFinder {
		DBSObject findObject(DBRProgressMonitor monitor, DmSchema schema, String objectName) throws DBException;
	}

	private final String objectType;
	private final DBPImage image;
	private final Class<? extends DBSObject> typeClass;
	private final ObjectFinder finder;

	<OBJECT_TYPE extends DBSObject> DmObjectType(String objectType, DBPImage image, Class<OBJECT_TYPE> typeClass,
			ObjectFinder finder) {
		this.objectType = objectType;
		this.image = image;
		this.typeClass = typeClass;
		this.finder = finder;
	}

	public static DmObjectType getByType(String typeName) {
		return typeMap.get(typeName);
	}

	public static Object resolveObject(DBRProgressMonitor monitor, DmDataSource dataSource, String dbLink,
			String objectTypeName, String objectOwner, String objectName) throws DBException {
		if (dbLink != null) {
			return objectName;
		}
		DmObjectType objectType = DmObjectType.getByType(objectTypeName);
		if (objectType == null) {
			log.debug("Unrecognized Dm object type: " + objectTypeName);
			return objectName;
		}
		if (!objectType.isBrowsable()) {
			log.debug("Unsupported Dm object type: " + objectTypeName);
			return objectName;
		}
		final DmSchema schema = dataSource.getSchema(monitor, objectOwner);
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

	public DBSObject findObject(DBRProgressMonitor monitor, DmSchema schema, String objectName) throws DBException {
		if (finder != null) {
			return finder.findObject(monitor, schema, objectName);
		} else {
			return null;
		}
	}

	public String toString() {
		return objectType;
	}
}
