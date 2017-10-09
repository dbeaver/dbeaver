/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
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
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Oracle data type
 */
public class OracleDataType extends OracleObject<DBSObject>
    implements DBSDataType, DBSEntity, DBPQualifiedObject, OracleSourceObject, DBPScriptObjectExt {

    private static final Log log = Log.getLog(OracleTableForeignKey.class);
    public static final String TYPE_CODE_COLLECTION = "COLLECTION";
    public static final String TYPE_CODE_OBJECT = "OBJECT";

    static class TypeDesc {
        final DBPDataKind dataKind;
        final int valueType;
        final int precision;
        final int minScale;
        final int maxScale;
        private TypeDesc(DBPDataKind dataKind, int valueType, int precision, int minScale, int maxScale)
        {
            this.dataKind = dataKind;
            this.valueType = valueType;
            this.precision = precision;
            this.minScale = minScale;
            this.maxScale = maxScale;
        }
    }

    static final Map<String, TypeDesc> PREDEFINED_TYPES = new HashMap<>();
    static final Map<Integer, TypeDesc> PREDEFINED_TYPE_IDS = new HashMap<>();
    static  {
        PREDEFINED_TYPES.put("BFILE", new TypeDesc(DBPDataKind.CONTENT, Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("BINARY ROWID", new TypeDesc(DBPDataKind.ROWID, Types.ROWID, 0, 0, 0));
        PREDEFINED_TYPES.put("BINARY_DOUBLE", new TypeDesc(DBPDataKind.NUMERIC, Types.DOUBLE, 63, 127, -84));
        PREDEFINED_TYPES.put("BINARY_FLOAT", new TypeDesc(DBPDataKind.NUMERIC, Types.FLOAT, 63, 127, -84));
        PREDEFINED_TYPES.put("BLOB", new TypeDesc(DBPDataKind.CONTENT, Types.BLOB, 0, 0, 0));
        PREDEFINED_TYPES.put("CANONICAL", new TypeDesc(DBPDataKind.UNKNOWN, Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("CFILE", new TypeDesc(DBPDataKind.CONTENT, Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("CHAR", new TypeDesc(DBPDataKind.STRING, Types.CHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("CLOB", new TypeDesc(DBPDataKind.CONTENT, Types.CLOB, 0, 0, 0));
        PREDEFINED_TYPES.put("CONTIGUOUS ARRAY", new TypeDesc(DBPDataKind.ARRAY, Types.ARRAY, 0, 0, 0));
        PREDEFINED_TYPES.put("DATE", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 0, 0, 0));
        PREDEFINED_TYPES.put("DECIMAL", new TypeDesc(DBPDataKind.NUMERIC, Types.DECIMAL, 63, 127, -84));
        PREDEFINED_TYPES.put("DOUBLE PRECISION", new TypeDesc(DBPDataKind.NUMERIC, Types.DOUBLE, 63, 127, -84));
        PREDEFINED_TYPES.put("FLOAT", new TypeDesc(DBPDataKind.NUMERIC, Types.FLOAT, 63, 127, -84));
        PREDEFINED_TYPES.put("INTEGER", new TypeDesc(DBPDataKind.NUMERIC, Types.INTEGER, 63, 127, -84));
        PREDEFINED_TYPES.put("INTERVAL DAY TO SECOND", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("INTERVAL YEAR TO MONTH", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("CONTENT POINTER", new TypeDesc(DBPDataKind.CONTENT, Types.BLOB, 0, 0, 0));
        PREDEFINED_TYPES.put("NAMED COLLECTION", new TypeDesc(DBPDataKind.ARRAY, Types.ARRAY, 0, 0, 0));
        PREDEFINED_TYPES.put("NAMED OBJECT", new TypeDesc(DBPDataKind.OBJECT, Types.STRUCT, 0, 0, 0));
        PREDEFINED_TYPES.put("NUMBER", new TypeDesc(DBPDataKind.NUMERIC, Types.NUMERIC, 63, 127, -84));
        PREDEFINED_TYPES.put("OCTET", new TypeDesc(DBPDataKind.BINARY, Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("OID", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("POINTER", new TypeDesc(DBPDataKind.UNKNOWN, Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("RAW", new TypeDesc(DBPDataKind.BINARY, Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("REAL", new TypeDesc(DBPDataKind.NUMERIC, Types.REAL, 63, 127, -84));
        PREDEFINED_TYPES.put("REF", new TypeDesc(DBPDataKind.REFERENCE, Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("SIGNED BINARY INTEGER", new TypeDesc(DBPDataKind.NUMERIC, Types.INTEGER, 63, 127, -84));
        PREDEFINED_TYPES.put("SMALLINT", new TypeDesc(DBPDataKind.NUMERIC, Types.SMALLINT, 63, 127, -84));
        PREDEFINED_TYPES.put("TABLE", new TypeDesc(DBPDataKind.OBJECT, Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("TIME", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 0, 0, 0));
        PREDEFINED_TYPES.put("TIME WITH TZ", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP WITH LOCAL TZ", new TypeDesc(DBPDataKind.DATETIME, OracleConstants.DATA_TYPE_TIMESTAMP_WITH_LOCAL_TIMEZONE, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP WITH TZ", new TypeDesc(DBPDataKind.DATETIME, OracleConstants.DATA_TYPE_TIMESTAMP_WITH_TIMEZONE, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP WITH LOCAL TIME ZONE", new TypeDesc(DBPDataKind.DATETIME, OracleConstants.DATA_TYPE_TIMESTAMP_WITH_LOCAL_TIMEZONE, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP WITH TIME ZONE", new TypeDesc(DBPDataKind.DATETIME, OracleConstants.DATA_TYPE_TIMESTAMP_WITH_TIMEZONE, 0, 0, 0));
        PREDEFINED_TYPES.put("UNSIGNED BINARY INTEGER", new TypeDesc(DBPDataKind.NUMERIC, Types.BIGINT, 63, 127, -84));
        PREDEFINED_TYPES.put("UROWID", new TypeDesc(DBPDataKind.ROWID, Types.ROWID, 0, 0, 0));
        PREDEFINED_TYPES.put("VARCHAR", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("VARCHAR2", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("VARYING ARRAY", new TypeDesc(DBPDataKind.ARRAY, Types.ARRAY, 0, 0, 0));

        PREDEFINED_TYPES.put("VARRAY", new TypeDesc(DBPDataKind.ARRAY, Types.ARRAY, 0, 0, 0));
        PREDEFINED_TYPES.put("ROWID", new TypeDesc(DBPDataKind.ROWID, Types.ROWID, 0, 0, 0));
        PREDEFINED_TYPES.put("LONG", new TypeDesc(DBPDataKind.BINARY, Types.LONGVARBINARY, 0, 0, 0));
        PREDEFINED_TYPES.put("RAW", new TypeDesc(DBPDataKind.BINARY, Types.LONGVARBINARY, 0, 0, 0));
        PREDEFINED_TYPES.put("LONG RAW", new TypeDesc(DBPDataKind.BINARY, Types.LONGVARBINARY, 0, 0, 0));
        PREDEFINED_TYPES.put("NVARCHAR2", new TypeDesc(DBPDataKind.STRING, Types.NVARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("NCHAR", new TypeDesc(DBPDataKind.STRING, Types.NCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("NCLOB", new TypeDesc(DBPDataKind.CONTENT, Types.NCLOB, 0, 0, 0));
        PREDEFINED_TYPES.put("LOB POINTER", new TypeDesc(DBPDataKind.CONTENT, Types.BLOB, 0, 0, 0));

        PREDEFINED_TYPES.put("REF CURSOR", new TypeDesc(DBPDataKind.REFERENCE, -10, 0, 0, 0));

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
    private OracleDataType componentType;

    public OracleDataType(DBSObject owner, String typeName, boolean persisted)
    {
        super(owner, typeName, persisted);
        this.attributeCache = new AttributeCache();
        this.methodCache = new MethodCache();
        if (owner instanceof OracleDataSource) {
            flagPredefined = true;
            findTypeDesc(typeName);
        }
    }

    public OracleDataType(DBSObject owner, ResultSet dbResult)
    {
        super(owner, JDBCUtils.safeGetString(dbResult, "TYPE_NAME"), true);
        this.typeCode = JDBCUtils.safeGetString(dbResult, "TYPECODE");
        this.typeOID = JDBCUtils.safeGetBytes(dbResult, "TYPE_OID");
        this.flagPredefined = JDBCUtils.safeGetBoolean(dbResult, "PREDEFINED", OracleConstants.YES);
        this.flagIncomplete = JDBCUtils.safeGetBoolean(dbResult, "INCOMPLETE", OracleConstants.YES);
        this.flagFinal = JDBCUtils.safeGetBoolean(dbResult, "FINAL", OracleConstants.YES);
        this.flagInstantiable = JDBCUtils.safeGetBoolean(dbResult, "INSTANTIABLE", OracleConstants.YES);
        String superTypeOwner = JDBCUtils.safeGetString(dbResult, "SUPERTYPE_OWNER");
        boolean hasAttributes;
        boolean hasMethods;
        if (!CommonUtils.isEmpty(superTypeOwner)) {
            this.superType = new OracleLazyReference(
                superTypeOwner,
                JDBCUtils.safeGetString(dbResult, "SUPERTYPE_NAME"));
            hasAttributes = JDBCUtils.safeGetInt(dbResult, "LOCAL_ATTRIBUTES") > 0;
            hasMethods = JDBCUtils.safeGetInt(dbResult, "LOCAL_METHODS") > 0;
        } else {
            hasAttributes = JDBCUtils.safeGetInt(dbResult, "ATTRIBUTES") > 0;
            hasMethods = JDBCUtils.safeGetInt(dbResult, "METHODS") > 0;
        }
        attributeCache = hasAttributes ? new AttributeCache() : null;
        methodCache = hasMethods ? new MethodCache() : null;
        
        if (owner instanceof OracleDataSource && flagPredefined) {
            // Determine value type for predefined types
            findTypeDesc(name);
        } else {
            if (TYPE_CODE_COLLECTION.equals(this.typeCode)) {
                this.valueType = java.sql.Types.ARRAY;
            } else if (TYPE_CODE_OBJECT.equals(this.typeCode)) {
                this.valueType = java.sql.Types.STRUCT;
            } else {
                if (this.name.equals(OracleConstants.TYPE_NAME_XML) && owner.getName().equals(OracleConstants.SCHEMA_SYS)) {
                    this.valueType = java.sql.Types.SQLXML;
                }
            }
        }
    }

    // Use by tree navigator thru reflection
    public boolean hasMethods()
    {
        return methodCache != null;
    }
    // Use by tree navigator thru reflection
    public boolean hasAttributes()
    {
        return attributeCache != null;
    }

    private boolean findTypeDesc(String typeName)
    {
        if (typeName.startsWith("PL/SQL")) {
            // Don't care about PL/SQL types
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
    public static DBPDataKind getDataKind(String typeName)
    {
        TypeDesc desc = PREDEFINED_TYPES.get(typeName);
        return desc != null ? desc.dataKind : null;
    }

    @Nullable
    @Override
    public OracleSchema getSchema()
    {
        return parent instanceof OracleSchema ? (OracleSchema)parent : null;
    }

    @Override
    public OracleSourceType getSourceType()
    {
        return OracleSourceType.TYPE;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBCException
    {
        if (sourceDeclaration == null && monitor != null) {
            sourceDeclaration = OracleUtils.getSource(monitor, this, false, false);
        }
        return sourceDeclaration;
    }

    public void setObjectDefinitionText(String sourceDeclaration)
    {
        this.sourceDeclaration = sourceDeclaration;
    }

    @Override
    public DBEPersistAction[] getCompileActions()
    {
        return new DBEPersistAction[] {
            new OracleObjectPersistAction(
                OracleObjectType.VIEW,
                "Compile type",
                "ALTER TYPE " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
            )};
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        if (sourceDefinition == null && monitor != null) {
            sourceDefinition = OracleUtils.getSource(monitor, this, true, false);
        }
        return sourceDefinition;
    }

    public void setExtendedDefinitionText(String source)
    {
        this.sourceDefinition = source;
    }

    @Override
    public String getTypeName()
    {
        return getFullyQualifiedName(DBPEvaluationContext.DDL);
    }

    @Override
    public String getFullTypeName() {
        return DBUtils.getFullTypeName(this);
    }

    @Override
    public int getTypeID()
    {
        return valueType;
    }

    @Override
    public DBPDataKind getDataKind()
    {
        return JDBCUtils.resolveDataKind(getDataSource(), getName(), valueType);
    }

    @Override
    public Integer getScale()
    {
        return typeDesc == null ? 0 : typeDesc.minScale;
    }

    @Override
    public Integer getPrecision()
    {
        return typeDesc == null ? 0 : typeDesc.precision;
    }

    @Override
    public long getMaxLength()
    {
        return CommonUtils.toInt(getPrecision());
    }

    @Override
    public int getMinScale()
    {
        return typeDesc == null ? 0 : typeDesc.minScale;
    }

    @Override
    public int getMaxScale()
    {
        return typeDesc == null ? 0 : typeDesc.maxScale;
    }

    @NotNull
    @Override
    public DBCLogicalOperator[] getSupportedOperators(DBSTypedObject attribute) {
        return DBUtils.getDefaultOperators(this);
    }

    @Override
    public DBSObject getParentObject()
    {
        return parent instanceof OracleSchema ?
            parent :
            parent instanceof OracleDataSource ? ((OracleDataSource) parent).getContainer() : null;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, editable = true, order = 2)
    public String getTypeCode()
    {
        return typeCode;
    }

    @Property(hidden = true, viewable = false, editable = false)
    public byte[] getTypeOID()
    {
        return typeOID;
    }

    @Property(viewable = true, editable = true, order = 3)
    public OracleDataType getSuperType(DBRProgressMonitor monitor)
    {
        if (superType  == null) {
            return null;
        } else if (superType instanceof OracleDataType) {
            return (OracleDataType)superType;
        } else {
            try {
                OracleLazyReference olr = (OracleLazyReference) superType;
                final OracleSchema superSchema = getDataSource().getSchema(monitor, olr.schemaName);
                if (superSchema == null) {
                    log.warn("Referenced schema '" + olr.schemaName + "' not found for super type '" + olr.objectName + "'");
                } else {
                    superType = superSchema.dataTypeCache.getObject(monitor, superSchema, olr.objectName);
                    if (superType == null) {
                        log.warn("Referenced type '" + olr.objectName + "' not found in schema '" + olr.schemaName + "'");
                    } else {
                        return (OracleDataType)superType;
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
    public boolean isPredefined()
    {
        return flagPredefined;
    }

    @Property(viewable = true, order = 5)
    public boolean isIncomplete()
    {
        return flagIncomplete;
    }

    @Property(viewable = true, order = 6)
    public boolean isFinal()
    {
        return flagFinal;
    }

    @Property(viewable = true, order = 7)
    public boolean isInstantiable()
    {
        return flagInstantiable;
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType()
    {
        return DBSEntityType.TYPE;
    }

    @Override
    @Association
    public Collection<OracleDataTypeAttribute> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return attributeCache != null ? attributeCache.getAllObjects(monitor, this) : null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public OracleDataTypeAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException
    {
        return attributeCache != null ? attributeCache.getObject(monitor, this, attributeName) : null;
    }

    @Nullable
    @Association
    public Collection<OracleDataTypeMethod> getMethods(DBRProgressMonitor monitor)
        throws DBException
    {
        return methodCache != null ? methodCache.getAllObjects(monitor, this) : null;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Nullable
    @Override
    public Object geTypeExtension() {
        return typeOID;
    }

    @Property(viewable = true, order = 8)
    public OracleDataType getComponentType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (componentType != null) {
            return componentType;
        }
        OracleSchema schema = getSchema();
        if (schema == null || !TYPE_CODE_COLLECTION.equals(typeCode) || !getDataSource().isAtLeastV10()) {
            return null;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Load collection types")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT ELEM_TYPE_OWNER,ELEM_TYPE_NAME,ELEM_TYPE_MOD FROM SYS.ALL_COLL_TYPES WHERE OWNER=? AND TYPE_NAME=?")) {
                dbStat.setString(1, schema.getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResults = dbStat.executeQuery()) {
                    if (dbResults.next()) {
                        String compTypeSchema = JDBCUtils.safeGetString(dbResults, "ELEM_TYPE_OWNER");
                        String compTypeName = JDBCUtils.safeGetString(dbResults, "ELEM_TYPE_NAME");
                        //String compTypeMod = JDBCUtils.safeGetString(dbResults, "ELEM_TYPE_MOD");
                        componentType = OracleDataType.resolveDataType(monitor, getDataSource(), compTypeSchema, compTypeName);
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

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return parent instanceof OracleSchema ?
            DBUtils.getFullQualifiedName(getDataSource(), parent, this) :
            name;
    }

    @Override
    public String toString()
    {
        return getFullyQualifiedName(DBPEvaluationContext.UI);
    }

    public static OracleDataType resolveDataType(DBRProgressMonitor monitor, OracleDataSource dataSource, String typeOwner, String typeName)
    {
        typeName = normalizeTypeName(typeName);
        OracleSchema typeSchema = null;
        OracleDataType type = null;
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
            type = (OracleDataType)dataSource.getLocalDataType(typeName);
        }
        if (type == null) {
            log.debug("Data type '" + typeName + "' not found - declare new one");
            type = new OracleDataType(typeSchema == null ? dataSource : typeSchema, typeName, true);
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
            typeName = typeName.substring(0, modIndex) +
                (modEnd == typeName.length() - 1 ? "" : typeName.substring(modEnd + 1));
        }
        return typeName;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return DBSObjectState.NORMAL;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {

    }

    private class AttributeCache extends JDBCObjectCache<OracleDataType, OracleDataTypeAttribute> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataType owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM SYS.ALL_TYPE_ATTRS " +
                "WHERE OWNER=? AND TYPE_NAME=? ORDER BY ATTR_NO");
            dbStat.setString(1, OracleDataType.this.parent.getName());
            dbStat.setString(2, getName());
            return dbStat;
        }
        @Override
        protected OracleDataTypeAttribute fetchObject(@NotNull JDBCSession session, @NotNull OracleDataType owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataTypeAttribute(session.getProgressMonitor(), OracleDataType.this, resultSet);
        }
    }

    private class MethodCache extends JDBCObjectCache<OracleDataType, OracleDataTypeMethod> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataType owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT m.*,r.RESULT_TYPE_OWNER,RESULT_TYPE_NAME,RESULT_TYPE_MOD\n" +
                "FROM SYS.ALL_TYPE_METHODS m\n" +
                "LEFT OUTER JOIN SYS.ALL_METHOD_RESULTS r ON r.OWNER=m.OWNER AND r.TYPE_NAME=m.TYPE_NAME AND r.METHOD_NAME=m.METHOD_NAME AND r.METHOD_NO=m.METHOD_NO\n" +
                "WHERE m.OWNER=? AND m.TYPE_NAME=?\n" +
                "ORDER BY m.METHOD_NO");
            dbStat.setString(1, OracleDataType.this.parent.getName());
            dbStat.setString(2, getName());
            return dbStat;
        }

        @Override
        protected OracleDataTypeMethod fetchObject(@NotNull JDBCSession session, @NotNull OracleDataType owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataTypeMethod(session.getProgressMonitor(), OracleDataType.this, resultSet);
        }
    }

}
