package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.source.DmSourceObject;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

public class DmDataType extends DmObject<DBSObject>
		implements DBSDataType, DBSEntity, DBPQualifiedObject, DmSourceObject, DBPScriptObjectExt {
	private static final Log log = Log.getLog(DmDataType.class);

	public static final String TYPE_CODE_COLLECTION = "COLLECTION";
	public static final String TYPE_CODE_OBJECT = "OBJECT";

	static class TypeDesc {
		final DBPDataKind dataKind;
		final int valueType;
		final int precision;
		final int minScale;
		final int maxScale;

		private TypeDesc(DBPDataKind dataKind, int valueType, int precision, int minScale, int maxScale) {
			this.dataKind = dataKind;
			this.valueType = valueType;
			this.precision = precision;
			this.minScale = minScale;
			this.maxScale = maxScale;
		}
	}

	static final Map<String, TypeDesc> PREDEFINED_TYPES = new HashMap<>();
	static final Map<Integer, TypeDesc> PREDEFINED_TYPE_IDS = new HashMap<>();

	static {
		// 数值类型
		PREDEFINED_TYPES.put("NUMERIC", new TypeDesc(DBPDataKind.NUMERIC, Types.NUMERIC, 63, 127, -84));
		PREDEFINED_TYPES.put("DECIMAL", new TypeDesc(DBPDataKind.NUMERIC, Types.DECIMAL, 63, 127, -84));
		PREDEFINED_TYPES.put("NUMBER", new TypeDesc(DBPDataKind.NUMERIC, Types.NUMERIC, 63, 127, -84));
		PREDEFINED_TYPES.put("DEC", new TypeDesc(DBPDataKind.NUMERIC, Types.NUMERIC, 63, 127, -84));
		PREDEFINED_TYPES.put("INT", new TypeDesc(DBPDataKind.NUMERIC, Types.INTEGER, 63, 127, -84));
		PREDEFINED_TYPES.put("BIGINT", new TypeDesc(DBPDataKind.NUMERIC, Types.BIGINT, 63, 127, -84));
		PREDEFINED_TYPES.put("TINYINT", new TypeDesc(DBPDataKind.NUMERIC, Types.TINYINT, 63, 127, -84));
		PREDEFINED_TYPES.put("BYTE", new TypeDesc(DBPDataKind.NUMERIC, Types.TINYINT, 63, 127, -84));
		PREDEFINED_TYPES.put("SMALLINT", new TypeDesc(DBPDataKind.NUMERIC, Types.SMALLINT, 63, 127, -84));
		PREDEFINED_TYPES.put("FLOAT", new TypeDesc(DBPDataKind.NUMERIC, Types.FLOAT, 63, 127, -84));
		PREDEFINED_TYPES.put("DOUBLE", new TypeDesc(DBPDataKind.NUMERIC, Types.DOUBLE, 63, 127, -84));
		PREDEFINED_TYPES.put("DOUBLE PRECISION", new TypeDesc(DBPDataKind.NUMERIC, Types.DOUBLE, 63, 127, -84));
		PREDEFINED_TYPES.put("REAL", new TypeDesc(DBPDataKind.NUMERIC, Types.REAL, 63, 127, -84));
		PREDEFINED_TYPES.put("BIT", new TypeDesc(DBPDataKind.NUMERIC, Types.BIT, 63, 127, -84));
		// 字符串类型
		PREDEFINED_TYPES.put("CHAR", new TypeDesc(DBPDataKind.STRING, Types.CHAR, 0, 0, 0));
		PREDEFINED_TYPES.put("CHARACTER", new TypeDesc(DBPDataKind.STRING, Types.CHAR, 0, 0, 0));
		PREDEFINED_TYPES.put("VARCHAR", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 0, 0));
		PREDEFINED_TYPES.put("VARCHAR2", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 0, 0));
		PREDEFINED_TYPES.put("BINARY", new TypeDesc(DBPDataKind.STRING, Types.BINARY, 0, 0, 0));
		PREDEFINED_TYPES.put("VARBINARY", new TypeDesc(DBPDataKind.STRING, Types.VARBINARY, 0, 0, 0));
		PREDEFINED_TYPES.put("NVARCHAR2", new TypeDesc(DBPDataKind.STRING, Types.NVARCHAR, 0, 0, 0));
		PREDEFINED_TYPES.put("NVARCHAR", new TypeDesc(DBPDataKind.STRING, Types.NVARCHAR, 0, 0, 0));
		// 日期类型
		PREDEFINED_TYPES.put("TIMESTAMP", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 0, 0, 0));
		PREDEFINED_TYPES.put("DATETIME", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 0, 0, 0));
		PREDEFINED_TYPES.put("TIME", new TypeDesc(DBPDataKind.DATETIME, Types.TIME, 0, 0, 0));
		PREDEFINED_TYPES.put("DATE", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 0, 0, 0));
		// 多媒体类型
		PREDEFINED_TYPES.put("BLOB", new TypeDesc(DBPDataKind.CONTENT, Types.BLOB, 0, 0, 0));
		PREDEFINED_TYPES.put("CLOB", new TypeDesc(DBPDataKind.CONTENT, Types.CLOB, 0, 0, 0));
		PREDEFINED_TYPES.put("BFILE", new TypeDesc(DBPDataKind.CONTENT, Types.OTHER, 0, 0, 0));
		PREDEFINED_TYPES.put("TEXT", new TypeDesc(DBPDataKind.STRING, Types.CLOB, 0, 0, 0)); // 注意这个要换为Types.CLOB 即2005,因为从ResultSet 中获取到的即为CLOB类型
		PREDEFINED_TYPES.put("IMAGE", new TypeDesc(DBPDataKind.CONTENT, Types.OTHER, 0, 0, 0));
		PREDEFINED_TYPES.put("LONGVARCHAR", new TypeDesc(DBPDataKind.CONTENT, Types.OTHER, 0, 0, 0));
		PREDEFINED_TYPES.put("LONGVARBINARY", new TypeDesc(DBPDataKind.CONTENT, Types.OTHER, 0, 0, 0));

		for (TypeDesc type : PREDEFINED_TYPES.values()) {
			PREDEFINED_TYPE_IDS.put(type.valueType, type);
		}
	}

	private String typeCode;
	private byte[] typeOID;
	private Object superType;
	 private final AttributeCache attributeCache;
	 private final MethodCache methodCache; 
	private boolean flagPredefined;
	private boolean flagIncomplete;
	private boolean flagFinal;
	private boolean flagInstantiable;
	private TypeDesc typeDesc;
	private int valueType = java.sql.Types.OTHER;
	private String sourceDeclaration;
	private String sourceDefinition;
	private DmDataType componentType;

	public DmDataType(DBSObject owner, String typeName, boolean persisted) {
		super(owner, typeName, persisted);
		this.attributeCache = new AttributeCache();
		this.methodCache = new MethodCache();
        if (owner instanceof DmDataSource) {
            flagPredefined = true;
            findTypeDesc(typeName);
        }
	}

	public DmDataType(DBSObject owner, ResultSet dbResult) {
		super(owner, JDBCUtils.safeGetString(dbResult, "data_type"), true);
		this.typeCode = JDBCUtils.safeGetString(dbResult, "TYPECODE");
		this.typeOID = JDBCUtils.safeGetBytes(dbResult, "TYPE_OID");
		this.flagPredefined = JDBCUtils.safeGetBoolean(dbResult, "PREDEFINED", DmConstants.YES);
		this.flagIncomplete = JDBCUtils.safeGetBoolean(dbResult, "INCOMPLETE", DmConstants.YES);
		this.flagFinal = JDBCUtils.safeGetBoolean(dbResult, "FINAL", DmConstants.YES);
		this.flagInstantiable = JDBCUtils.safeGetBoolean(dbResult, "INSTANTIABLE", DmConstants.YES);
		String superTypeOwner = JDBCUtils.safeGetString(dbResult, "SUPERTYPE_OWNER");
		boolean hasAttributes;
		boolean hasMethods;
		if (!CommonUtils.isEmpty(superTypeOwner)) {
			this.superType = new DmLazyReference(superTypeOwner, JDBCUtils.safeGetString(dbResult, "SUPERTYPE_NAME"));
			hasAttributes = JDBCUtils.safeGetInt(dbResult, "LOCAL_ATTRIBUTES") > 0;
			hasMethods = JDBCUtils.safeGetInt(dbResult, "LOCAL_METHODS") > 0;
		} else {
			hasAttributes = JDBCUtils.safeGetInt(dbResult, "ATTRIBUTES") > 0;
			hasMethods = JDBCUtils.safeGetInt(dbResult, "METHODS") > 0;
		}
		 attributeCache = hasAttributes ? new AttributeCache() : null;
		 methodCache = hasMethods ? new MethodCache() : null;
         if(name==null) {
        	 name="";
         }
         /**
          *  此处决定了该类中的valueType 属性值为多少
          */
		if (owner instanceof DmDataSource) {
			findTypeDesc(name);
		} else {
			if (TYPE_CODE_COLLECTION.equals(this.typeCode)) {
				this.valueType = java.sql.Types.ARRAY;
			} else if (TYPE_CODE_OBJECT.equals(this.typeCode)) {
				this.valueType = java.sql.Types.STRUCT;
			} else {
				if (this.name.equals(DmConstants.TYPE_NAME_XML) && owner.getName().equals(DmConstants.SCHEMA_SYS)) {
					this.valueType = java.sql.Types.SQLXML;
				}
			}
		}
	}

    // Use by tree navigator thru reflection
    public boolean hasAttributes()
    {
        return attributeCache != null;
    }
    
	public static DmDataType resolveDataType(DBRProgressMonitor monitor, DmDataSource dataSource, String typeOwner,
			String typeName) {
		typeName = normalizeTypeName(typeName);
		DmSchema typeSchema = null;
		DmDataType type = null;
		if (typeOwner != null) {
			try {
				typeSchema = dataSource.getSchema(monitor, typeOwner);
				if (typeSchema == null) {
					log.error("Type attr schema '" + typeOwner + "' not found");
				} else {
					type = typeSchema.getDataType(monitor, typeName);
				}
			} catch (DBException e) {
				log.error(e);
			}
		} else {
			//System.out.println(dataSource.getLocalDataType(typeName));
			type = (DmDataType) dataSource.getLocalDataType(typeName);
		}
		if (type == null) {
			log.debug("Data type '" + typeName + "' not found - declare new one");
			type = new DmDataType(typeSchema == null ? dataSource : typeSchema, typeName, true);
			type.flagPredefined = true;
			if (typeSchema == null) {
				dataSource.dataTypeCache.cacheObject(type);
			} else {
				typeSchema.dataTypeCache.cacheObject(type);
			}
		}
		return type;
	}

	private static String normalizeTypeName(String typeName) {
		if (CommonUtils.isEmpty(typeName)) {
			return "";
		}
		for (;;) {
			int modIndex = typeName.indexOf('(');
			if (modIndex == -1) {
				break;
			}
			int modEnd = typeName.indexOf(')', modIndex);
			if (modEnd == -1) {
				break;
			}
			typeName = typeName.substring(0, modIndex)
					+ (modEnd == typeName.length() - 1 ? "" : typeName.substring(modEnd + 1));
		}
		return typeName;
	}

	private boolean findTypeDesc(String typeName) {
		if (typeName.startsWith("PL/SQL")) {
			return true;
		}
		typeName = normalizeTypeName(typeName);
		this.typeDesc = PREDEFINED_TYPES.get(typeName);
		if (this.typeDesc == null) {
			log.warn("Unknown predefined type: " + typeName);
			return false;
		} else {
			this.valueType = this.typeDesc.valueType;
			return true;
		}
	}

	@Override
	public String getTypeName() {
		return getFullyQualifiedName(DBPEvaluationContext.DDL);
	}

	@Override
	public String getFullTypeName() {
		return DBUtils.getFullTypeName(this);
	}

	@Override
	public int getTypeID() {
		return valueType;
	}

	@Nullable
	public static DBPDataKind getDataKind(String typeName) {
		TypeDesc desc = PREDEFINED_TYPES.get(typeName);
		return desc != null ? desc.dataKind : null;
	}

	@Override
	public DBPDataKind getDataKind() {
		return JDBCUtils.resolveDataKind(getDataSource(), getName(), valueType);
	}

	@Override
	public Integer getScale() {
		return typeDesc == null ? 0 : typeDesc.minScale;
	}

	@Override
	public Integer getPrecision() {
		return typeDesc == null ? 0 : typeDesc.precision;
	}

	@Override
	public long getMaxLength() {
		return CommonUtils.toInt(getPrecision());
	}

	@Override
	public void setObjectDefinitionText(String sourceDeclaration) {
		this.sourceDeclaration = sourceDeclaration;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		if (sourceDeclaration == null && monitor != null) {
			sourceDeclaration = DmUtils.getSource(monitor, this, false, false);
		}
		return sourceDeclaration;
	}

	@Nullable
	@Override
	public DmSchema getSchema() {
		return parent instanceof DmSchema ? (DmSchema) parent : null;
	}

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return DBSObjectState.NORMAL;
	}

	@Override
	public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException {
		if (sourceDeclaration == null && monitor != null) {
			sourceDeclaration = DmUtils.getSource(monitor, this, true, false);
		}
		return sourceDeclaration;
	}

	public void setExtendedDefinitionText(String source) {
		this.sourceDefinition = source;
	}

	@Override
	public DmSourceType getSourceType() {
		return DmSourceType.TYPE;
	}

	@Override
	public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) {
		return new DBEPersistAction[] { new DmObjectPersistAction(DmObjectType.VIEW, "Compile type",
				"ALTER TYPE " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE") };
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return parent instanceof DmSchema ? DBUtils.getFullQualifiedName(getDataSource(), parent, this) : name;
	}

	@Override
	@Association
	public List<? extends DBSEntityAttribute> getAttributes(DBRProgressMonitor monitor) throws DBException {
		 return attributeCache != null ? attributeCache.getAllObjects(monitor, this) : null;
	}

	@Override
	public DBSEntityAttribute getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException {
		 return attributeCache != null ? attributeCache.getObject(monitor, this, attributeName) : null;
	}

    @Nullable
    @Association
    public Collection<DmDataTypeMethod> getMethods(DBRProgressMonitor monitor)
        throws DBException
    {
        return methodCache != null ? methodCache.getAllObjects(monitor, this) : null;
    }
	@Override
	public Collection<? extends DBSEntityConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException {
		return null;
	}

	@Override
	public Collection<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException {
		return null;
	}

	@Override
	public Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException {
		return null;
	}

	@Nullable
	@Override
	public Object geTypeExtension() {
		return typeOID;
	}

	@Override
	public DBSDataType getComponentType(DBRProgressMonitor monitor) throws DBException {
        if (componentType != null) {
            return componentType;
        }
        DmSchema schema = getSchema();
        if (schema == null || !TYPE_CODE_COLLECTION.equals(typeCode) || !getDataSource().isAtLeastV10()) {
            return null;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load collection types")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT ELEM_TYPE_OWNER,ELEM_TYPE_NAME,ELEM_TYPE_MOD FROM " +
                    DmUtils.getSysSchemaPrefix(getDataSource()) + "ALL_COLL_TYPES WHERE OWNER=? AND TYPE_NAME=?"))
            {
                dbStat.setString(1, schema.getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResults = dbStat.executeQuery()) {
                    if (dbResults.next()) {
                        String compTypeSchema = JDBCUtils.safeGetString(dbResults, "ELEM_TYPE_OWNER");
                        String compTypeName = JDBCUtils.safeGetString(dbResults, "ELEM_TYPE_NAME");
                        //String compTypeMod = JDBCUtils.safeGetString(dbResults, "ELEM_TYPE_MOD");
                        componentType = DmDataType.resolveDataType(monitor, getDataSource(), compTypeSchema, compTypeName);
                    } else {
                        log.warn("Can't resolve collection type [" + getName() + "]");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error reading collection types", e);
        }
        return componentType;
	}

	@Override
	public int getMinScale() {
		return typeDesc == null ? 0 : typeDesc.minScale;
	}

	@Override
	public int getMaxScale() {
		return typeDesc == null ? 0 : typeDesc.maxScale;
	}

	@NotNull
	@Override
	public DBCLogicalOperator[] getSupportedOperators(DBSTypedObject attribute) {
		return DBUtils.getDefaultOperators(this);
	}

	@Override
	public DBSObject getParentObject() {
		return parent instanceof DmSchema ? parent
				: parent instanceof DmDataType ? ((DmDataSource) parent).getContainer() : null;
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
	public String getName() {
		return name;
	}

	@Property(viewable = true, editable = true, order = 2)
	public String getTypeCode() {
		return typeCode;
	}

	@Property(hidden = true, viewable = false, editable = false)
	public byte[] getTypeOID() {
		return typeOID;
	}

	@Property(viewable = true, editable = true, order = 3)
	public DmDataType getSuperType(DBRProgressMonitor monitor) {
		if (superType == null) {
			return null;
		} else if (superType instanceof DmDataType) {
			return (DmDataType) superType;
		} else {
			try {
				DmLazyReference olr = (DmLazyReference) superType;
				final DmSchema superSchema = getDataSource().getSchema(monitor, olr.schemaName);
				if (superSchema == null) {
					log.warn("Referenced schema '" + olr.schemaName + "' not found for super type '" + olr.objectName
							+ "'");
				} else {
					superType = superSchema.dataTypeCache.getObject(monitor, superSchema, olr.objectName);
					if (superType == null) {
						log.warn("Referenced type '" + olr.objectName + "' not found in schema '" + olr.schemaName
								+ "'");
					} else {
						return (DmDataType) superType;
					}
				}
			} catch (DBException e) {
				log.error(e);
			}
			superType = null;
			return null;
		}
	}

	@Property(viewable = true, order = 4)
	public boolean isPredefined() {
		return flagPredefined;
	}

	@Property(viewable = true, order = 5)
	public boolean isIncomplete() {
		return flagIncomplete;
	}

	@Property(viewable = true, order = 6)
	public boolean isFinal() {
		return flagFinal;
	}

	@Property(viewable = true, order = 7)
	public boolean isInstantiable() {
		return flagInstantiable;
	}

	@NotNull
	@Override
	public DBSEntityType getEntityType() {
		return DBSEntityType.TYPE;
	}

	@Override
	public String toString() {
		return getFullyQualifiedName(DBPEvaluationContext.UI);
	}
	

    private class AttributeCache extends JDBCObjectCache<DmDataType, DmDataTypeAttribute> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmDataType owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM "+ DmUtils.getSysSchemaPrefix(getDataSource()) + "ALL_TYPE_ATTRS " +
                "WHERE OWNER=? AND TYPE_NAME=? ORDER BY ATTR_NO");
            dbStat.setString(1, DmDataType.this.parent.getName());
            dbStat.setString(2, getName());
            return dbStat;
        }
        @Override
        protected DmDataTypeAttribute fetchObject(@NotNull JDBCSession session, @NotNull DmDataType owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new DmDataTypeAttribute(session.getProgressMonitor(), DmDataType.this, resultSet);
        }
    }
    private class MethodCache extends JDBCObjectCache<DmDataType, DmDataTypeMethod> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmDataType owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT m.*,r.RESULT_TYPE_OWNER,RESULT_TYPE_NAME,RESULT_TYPE_MOD\n" +
                "FROM "+ DmUtils.getSysSchemaPrefix(getDataSource()) + "ALL_TYPE_METHODS m\n" +
                "LEFT OUTER JOIN "+ DmUtils.getSysSchemaPrefix(getDataSource()) + "ALL_METHOD_RESULTS r ON r.OWNER=m.OWNER AND r.TYPE_NAME=m.TYPE_NAME AND r.METHOD_NAME=m.METHOD_NAME AND r.METHOD_NO=m.METHOD_NO\n" +
                "WHERE m.OWNER=? AND m.TYPE_NAME=?\n" +
                "ORDER BY m.METHOD_NO");
            dbStat.setString(1, DmDataType.this.parent.getName());
            dbStat.setString(2, getName());
            return dbStat;
        }

        @Override
        protected DmDataTypeMethod fetchObject(@NotNull JDBCSession session, @NotNull DmDataType owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new DmDataTypeMethod(session.getProgressMonitor(), DmDataType.this, resultSet);
        }
    }
	@Override
	public long getTypeModifiers() {
		// TODO Auto-generated method stub
		return 0;
	}

}
