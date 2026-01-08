/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.model;

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
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBUtils;
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
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

public class YashanDBDataType extends YashanDBObject<DBSObject>
		implements DBSDataType, DBSEntity, DBPQualifiedObject, YashanDBSourceObject, DBPScriptObjectExt {

	private static final Log log = Log.getLog(YashanDBDataType.class);

	public static final String TYPE_CODE_COLLECTION = "COLLECTION";
	public static final String TYPE_CODE_OBJECT = "OBJECT";

	public static class TypeDesc {
		
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
		// Numeric type
		PREDEFINED_TYPES.put("TINYINT", new TypeDesc(DBPDataKind.NUMERIC, Types.TINYINT, 63, 127, -84));
		PREDEFINED_TYPES.put("SMALLINT", new TypeDesc(DBPDataKind.NUMERIC, Types.SMALLINT, 63, 127, -84));
		PREDEFINED_TYPES.put("INTEGER", new TypeDesc(DBPDataKind.NUMERIC, Types.INTEGER, 63, 127, -84));
		PREDEFINED_TYPES.put("BIGINT", new TypeDesc(DBPDataKind.NUMERIC, Types.BIGINT, 63, 127, -84));

		PREDEFINED_TYPES.put("FLOAT", new TypeDesc(DBPDataKind.NUMERIC, Types.FLOAT, 63, 127, -84));
		PREDEFINED_TYPES.put("DOUBLE", new TypeDesc(DBPDataKind.NUMERIC, Types.DOUBLE, 63, 127, -84));

		PREDEFINED_TYPES.put("NUMBER", new TypeDesc(DBPDataKind.NUMERIC, Types.NUMERIC, 18, 0, 127));

		PREDEFINED_TYPES.put("BIT", new TypeDesc(DBPDataKind.NUMERIC, Types.BIT, 64, 0, 0));

		// String type
		PREDEFINED_TYPES.put("CHAR", new TypeDesc(DBPDataKind.STRING, Types.CHAR, 0, 0, 0));
		PREDEFINED_TYPES.put("NCHAR", new TypeDesc(DBPDataKind.STRING, Types.CHAR, 0, 0, 0));
		PREDEFINED_TYPES.put("VARCHAR", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 0, 0));
		PREDEFINED_TYPES.put("NVARCHAR", new TypeDesc(DBPDataKind.STRING, Types.NVARCHAR, 0, 0, 0));

		// Boolean type
		PREDEFINED_TYPES.put("BOOLEAN", new TypeDesc(DBPDataKind.BOOLEAN, Types.BOOLEAN, 0, 0, 0));

		// Date type
		PREDEFINED_TYPES.put("DATE", new TypeDesc(DBPDataKind.DATETIME, Types.DATE, 0, 0, 0));

		PREDEFINED_TYPES.put("TIME", new TypeDesc(DBPDataKind.DATETIME, Types.TIME, 0, 0, 0));

		PREDEFINED_TYPES.put("TIMESTAMP", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 0, 0, 0));

		PREDEFINED_TYPES.put("TIMESTAMP WITH LOCAL TIME ZONE",
				new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP_WITH_TIMEZONE, 0, 0, 0));

		PREDEFINED_TYPES.put("TIMESTAMP WITH TIME ZONE",
				new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP_WITH_TIMEZONE, 0, 0, 0));

		PREDEFINED_TYPES.put("INTERVAL YEAR TO MONTH", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 0, 0));
		PREDEFINED_TYPES.put("INTERVAL DAY TO SECOND", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 0, 0));

		// Big object type
		PREDEFINED_TYPES.put("BLOB", new TypeDesc(DBPDataKind.CONTENT, Types.BLOB, 0, 0, 0));
		PREDEFINED_TYPES.put("CLOB", new TypeDesc(DBPDataKind.CONTENT, Types.CLOB, 0, 0, 0));
		PREDEFINED_TYPES.put("NCLOB", new TypeDesc(DBPDataKind.CONTENT, Types.NCLOB, 0, 0, 0));

		// Others
		PREDEFINED_TYPES.put("RAW", new TypeDesc(DBPDataKind.BINARY, Types.VARBINARY, 0, 0, 0));
		PREDEFINED_TYPES.put("JSON", new TypeDesc(DBPDataKind.CONTENT, Types.BLOB, 0, 0, 0));
		PREDEFINED_TYPES.put("XMLTYPE", new TypeDesc(DBPDataKind.CONTENT, Types.CLOB, 0, 0, 0));
		PREDEFINED_TYPES.put("BFILE", new TypeDesc(DBPDataKind.CONTENT, Types.OTHER, 0, 0, 0));
		PREDEFINED_TYPES.put("ROWID", new TypeDesc(DBPDataKind.ROWID, Types.ROWID, 0, 0, 0));
		PREDEFINED_TYPES.put("UROWID", new TypeDesc(DBPDataKind.BINARY, Types.BINARY, 0, 0, 0));

		PREDEFINED_TYPES.put("UNKNOWN", new TypeDesc(DBPDataKind.UNKNOWN, 0, 0, 0, 0));

		for (TypeDesc type : PREDEFINED_TYPES.values()) {
			PREDEFINED_TYPE_IDS.put(type.valueType, type);
		}
	}

	private String typeCode;
	private byte[] typeOID;
	private YashanDBLazyReference superType;
	private final AttributeCache attributeCache;
	private final MethodCache methodCache;
	private boolean flagPredefined;
	private boolean flagIncomplete;
	private boolean flagFinal;
	private boolean flagInstantiable;
	private TypeDesc typeDesc;
	private int valueType = Types.OTHER;
	private String sourceDeclaration;
	private String sourceDefinition;
	private YashanDBDataType componentType;

	public YashanDBDataType(DBSObject owner, String typeName, boolean persisted) {
		super(owner, typeName, persisted);
		this.attributeCache = new AttributeCache();
		this.methodCache = new MethodCache();
		if (owner instanceof YashanDBDataSource) {
			flagPredefined = true;
			findTypeDesc(typeName);
		}
	}

	protected YashanDBDataType(DBSObject owner, ResultSet dbResult) {
		super(owner, JDBCUtils.safeGetString(dbResult, "TYPE_NAME"), true);
		this.typeCode = JDBCUtils.safeGetString(dbResult, "TYPECODE");

		String superTypeOwner = JDBCUtils.safeGetString(dbResult, "SUPERTYPE_OWNER");
		boolean hasAttributes;
		boolean hasMethods;
		if (!CommonUtils.isEmpty(superTypeOwner)) {
			this.superType = new YashanDBLazyReference(superTypeOwner,
					JDBCUtils.safeGetString(dbResult, "SUPERTYPE_NAME"));
			hasAttributes = JDBCUtils.safeGetInt(dbResult, "LOCAL_ATTRIBUTES") > 0;
			hasMethods = JDBCUtils.safeGetInt(dbResult, "LOCAL_METHODS") > 0;
		} else {
			hasAttributes = JDBCUtils.safeGetInt(dbResult, "ATTRIBUTES") > 0;
			hasMethods = JDBCUtils.safeGetInt(dbResult, "METHODS") > 0;
		}

		this.typeOID = JDBCUtils.safeGetBytes(dbResult, "TYPE_OID");
		this.flagPredefined = JDBCUtils.safeGetBoolean(dbResult, "PREDEFINED", YashanDBConstants.YES);
		this.flagIncomplete = JDBCUtils.safeGetBoolean(dbResult, "INCOMPLETE", YashanDBConstants.YES);
		this.flagFinal = JDBCUtils.safeGetBoolean(dbResult, "FINAL", YashanDBConstants.YES);
		this.flagInstantiable = JDBCUtils.safeGetBoolean(dbResult, "INSTANTIABLE", YashanDBConstants.YES);

		attributeCache = hasAttributes ? new AttributeCache() : null;
		methodCache = hasMethods ? new MethodCache() : null;

		if (owner instanceof YashanDBDataSource && flagPredefined) {
			findTypeDesc(name);
		} else {
			if (TYPE_CODE_COLLECTION.equals(this.typeCode)) {
				this.valueType = Types.ARRAY;
			} else if (TYPE_CODE_OBJECT.equals(this.typeCode)) {
				this.valueType = Types.STRUCT;
			} else {
				if (this.name.equals(YashanDBConstants.TYPE_NAME_XML)
						&& owner.getName().equals(YashanDBConstants.SCHEMA_SYS)) {
					this.valueType = Types.SQLXML;
				}
			}
		}
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
	public YashanDBLazyReference getSuperType(DBRProgressMonitor monitor) {
		return superType;
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

	@Property(viewable = true, order = 8)
	public YashanDBDataType getComponentType(@NotNull DBRProgressMonitor monitor) throws DBException {
		return componentType;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBCException {
		if (flagPredefined) {
			return "-- Source code not available";
		}
		if (sourceDeclaration == null && monitor != null) {
			sourceDeclaration = YashanDBUtils.getSource(monitor, this, false, true);
		}
		return sourceDeclaration;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException {
		if (sourceDefinition == null && monitor != null) {
			sourceDefinition = YashanDBUtils.getSource(monitor, this, true, false);
		}
		return sourceDefinition;
	}

	@Nullable
	@Association
	public Collection<YashanDBDataTypeMethod> getMethods(DBRProgressMonitor monitor) throws DBException {
		return methodCache != null ? methodCache.getAllObjects(monitor, this) : null;
	}

	@Override
	@Association
	public List<YashanDBDataTypeAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
		return attributeCache != null ? attributeCache.getAllObjects(monitor, this) : null;
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

	@Nullable
	public static DBPDataKind getDataKind(String typeName) {
		TypeDesc desc = PREDEFINED_TYPES.get(typeName);
		return desc != null ? desc.dataKind : null;
	}

	@Nullable
	@Override
	public YashanDBSchema getSchema() {
		return parent instanceof YashanDBSchema ? (YashanDBSchema) parent : null;
	}

	@Override
	public YashanDBSourceType getSourceType() {
		return YashanDBSourceType.TYPE;
	}

	public void setObjectDefinitionText(String sourceDeclaration) {
		this.sourceDeclaration = sourceDeclaration;
	}

	public void setExtendedDefinitionText(String source) {
		this.sourceDefinition = source;
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
	public long getTypeModifiers() {
		return 0;
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
		return parent instanceof YashanDBSchema ? parent
				: parent instanceof YashanDBDataSource ? ((YashanDBDataSource) parent).getContainer() : null;
	}

	@NotNull
	@Override
	public DBSEntityType getEntityType() {
		return DBSEntityType.TYPE;
	}

	@Nullable
	@Override
	public Collection<? extends DBSEntityConstraint> getConstraints(@NotNull DBRProgressMonitor monitor)
			throws DBException {
		return null;
	}

	@Override
	public YashanDBDataTypeAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
			throws DBException {
		return attributeCache != null ? attributeCache.getObject(monitor, this, attributeName) : null;
	}

	@Override
	public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor)
			throws DBException {
		return null;
	}

	@Override
	public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor)
			throws DBException {
		return null;
	}

	@Nullable
	@Override
	public Object geTypeExtension() {
		return typeOID;
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return parent instanceof YashanDBSchema ? DBUtils.getFullQualifiedName(getDataSource(), parent, this) : name;
	}

	@Override
	public String toString() {
		return getFullyQualifiedName(DBPEvaluationContext.UI);
	}

	public static YashanDBDataType resolveDataType(DBRProgressMonitor monitor, YashanDBDataSource dataSource,
			String typeOwner, String typeName) {
		typeName = normalizeTypeName(typeName);
		YashanDBSchema typeSchema = null;
		YashanDBDataType type = null;
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
			type = (YashanDBDataType) dataSource.getLocalDataType(typeName);
		}
		if (type == null) {
			log.debug("Data type '" + typeName + "' not found - declare new one");
			type = new YashanDBDataType(typeSchema == null ? dataSource : typeSchema, typeName, true);
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

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return DBSObjectState.NORMAL;
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {

	}

	private class AttributeCache extends JDBCObjectCache<YashanDBDataType, YashanDBDataTypeAttribute> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataType owner)
				throws SQLException {
			final JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT * FROM " + YashanDBUtils.isAdminPriv(getDataSource(), "TYPE_ATTRS")
							+ " WHERE OWNER=? AND TYPE_NAME=? ORDER BY ATTR_NO");
			dbStat.setString(1, YashanDBDataType.this.parent.getName());
			dbStat.setString(2, getName());
			return dbStat;
		}

		@Override
		protected YashanDBDataTypeAttribute fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataType owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBDataTypeAttribute(session.getProgressMonitor(), YashanDBDataType.this, resultSet);
		}
	}

	private class MethodCache extends JDBCObjectCache<YashanDBDataType, YashanDBDataTypeMethod> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataType owner)
				throws SQLException {
			final JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT *" + " FROM " + YashanDBUtils.isAdminPriv(getDataSource(), "TYPE_METHODS")
							+ " m\n" + "WHERE m.OWNER=? AND m.TYPE_NAME=?\n" + "ORDER BY m.METHOD_NO");
			dbStat.setString(1, YashanDBDataType.this.parent.getName());
			dbStat.setString(2, getName());
			return dbStat;
		}

		@Override
		protected YashanDBDataTypeMethod fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataType owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBDataTypeMethod(session.getProgressMonitor(), YashanDBDataType.this, resultSet);
		}
	}
}
