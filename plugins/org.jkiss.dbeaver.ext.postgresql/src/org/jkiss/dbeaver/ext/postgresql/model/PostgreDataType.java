/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * PostgreTypeType
 */
public class PostgreDataType extends JDBCDataType<PostgreSchema> implements PostgreClass, PostgreScriptObject, DBPQualifiedObject, DBPImageProvider
{
    private static final Log log = Log.getLog(PostgreDataType.class);

    //private static final String CAT_MAIN = "Main";
    private static final String CAT_MISC = "Miscellaneous";
    private static final String CAT_MODIFIERS = "Modifiers";
    private static final String CAT_FUNCTIONS = "Functions";
    private static final String CAT_ARRAY = "Array";

    private static String[] OID_TYPES = new String[] {
        "regproc",
        "regprocedure",
        "regoper",
        "regoperator",
        "regnamespace",
        "regclass",
        "regtype",
        "regconfig",
        "regdictionary",
        "regrole",
    };

    private final boolean alias;

    private long typeId;
    private PostgreTypeType typeType;
    private PostgreTypeCategory typeCategory;
    private DBPDataKind dataKind;

    private final long ownerId;
    private boolean isByValue;
    private boolean isPreferred;
    private String arrayDelimiter;
    private long classId;
    private long elementTypeId;
    private long arrayItemTypeId;
    private String inputFunc;
    private String outputFunc;
    private String receiveFunc;
    private String sendFunc;
    private String modInFunc;
    private String modOutFunc;
    private String analyzeFunc;
    private PostgreTypeAlign align = PostgreTypeAlign.c;
    private PostgreTypeStorage storage = PostgreTypeStorage.p;
    private boolean isNotNull;
    private long baseTypeId;
    private int typeMod;
    private String baseTypeName;
    private int arrayDim;
    private long collationId;
    private String defaultValue;
    private String canonicalName;
    private String constraintText;
    private String description;

    private final AttributeCache attributeCache;
    private Object[] enumValues;

    public PostgreDataType(@NotNull JDBCSession session, @NotNull PostgreSchema owner, long typeId, int valueType, String name, int length, JDBCResultSet dbResult) throws DBException {
        super(owner, valueType, name, null, false, true, length, -1, -1);
        this.alias = false;
        if (owner.isCatalogSchema()) {
            this.canonicalName = PostgreConstants.DATA_TYPE_CANONICAL_NAMES.get(name);
        }
        this.typeId = typeId;
        this.typeType = PostgreTypeType.b;
        String typTypeStr = JDBCUtils.safeGetString(dbResult, "typtype"); //$NON-NLS-1$
        try {
            if (typTypeStr != null && !typTypeStr.isEmpty()) {
                this.typeType = PostgreTypeType.valueOf(typTypeStr.toLowerCase(Locale.ENGLISH));
            }
        } catch (Throwable e) {
            log.debug("Invalid type type [" + typTypeStr + "] - " + e.getMessage());
        }
        this.typeCategory = PostgreTypeCategory.X;
        boolean supportsCategory = session.getDataSource().isServerVersionAtLeast(8, 4);
        if (supportsCategory) {
            String typCategoryStr = JDBCUtils.safeGetString(dbResult, "typcategory"); //$NON-NLS-1$
            try {
                if (typCategoryStr != null && !typCategoryStr.isEmpty()) {
                    this.typeCategory = PostgreTypeCategory.valueOf(typCategoryStr.toUpperCase(Locale.ENGLISH));
                }
            } catch (Throwable e) {
                log.debug("Invalid type category [" + typCategoryStr + "] - " + e.getMessage());
            }
        }

        if (typeType == PostgreTypeType.e) {
            // Enums are strings
            this.dataKind = DBPDataKind.STRING;
        } else {
            this.dataKind = JDBCDataSource.getDataKind(getName(), valueType);
            if (this.dataKind == DBPDataKind.OBJECT) {
                if (PostgreConstants.TYPE_JSONB.equals(name) || PostgreConstants.TYPE_JSON.equals(name)) {
                    this.dataKind = DBPDataKind.CONTENT;
                } else if (PostgreConstants.TYPE_INTERVAL.equals(name)) {
                    this.dataKind = DBPDataKind.DATETIME;
                }
            }
        }

        this.ownerId = JDBCUtils.safeGetLong(dbResult, "typowner"); //$NON-NLS-1$
        this.isByValue = JDBCUtils.safeGetBoolean(dbResult, "typbyval"); //$NON-NLS-1$
        if (getDataSource().isServerVersionAtLeast(8, 4)) {
            this.isPreferred = JDBCUtils.safeGetBoolean(dbResult, "typispreferred"); //$NON-NLS-1$
        }
        this.arrayDelimiter = JDBCUtils.safeGetString(dbResult, "typdelim"); //$NON-NLS-1$
        this.classId = JDBCUtils.safeGetLong(dbResult, "typrelid"); //$NON-NLS-1$
        this.elementTypeId = JDBCUtils.safeGetLong(dbResult, "typelem"); //$NON-NLS-1$
        this.arrayItemTypeId = JDBCUtils.safeGetLong(dbResult, "typarray"); //$NON-NLS-1$
        this.inputFunc = JDBCUtils.safeGetString(dbResult, "typinput"); //$NON-NLS-1$
        this.outputFunc = JDBCUtils.safeGetString(dbResult, "typoutput"); //$NON-NLS-1$
        this.receiveFunc = JDBCUtils.safeGetString(dbResult, "typreceive"); //$NON-NLS-1$
        this.sendFunc = JDBCUtils.safeGetString(dbResult, "typsend"); //$NON-NLS-1$
        this.modInFunc = JDBCUtils.safeGetString(dbResult, "typmodin"); //$NON-NLS-1$
        this.modOutFunc = JDBCUtils.safeGetString(dbResult, "typmodout"); //$NON-NLS-1$
        this.analyzeFunc = JDBCUtils.safeGetString(dbResult, "typanalyze"); //$NON-NLS-1$
        String typAlignStr = JDBCUtils.safeGetString(dbResult, "typalign"); //$NON-NLS-1$
        if (!CommonUtils.isEmpty(typAlignStr)) {
            try {
                this.align = PostgreTypeAlign.valueOf(typAlignStr);
            } catch (Exception e) {
                log.debug("Invalid type align [" + typAlignStr + "] - " + e.getMessage());
            }
        }
        String typStorageStr = JDBCUtils.safeGetString(dbResult, "typstorage"); //$NON-NLS-1$
        if (!CommonUtils.isEmpty(typStorageStr)) {
            try {
                this.storage = PostgreTypeStorage.valueOf(typStorageStr);
            } catch (Exception e) {
                log.debug("Invalid type storage [" + typStorageStr + "] - " + e.getMessage());
            }
        }
        this.isNotNull = JDBCUtils.safeGetBoolean(dbResult, "typnotnull"); //$NON-NLS-1$
        this.baseTypeId = JDBCUtils.safeGetLong(dbResult, "typbasetype"); //$NON-NLS-1$
        this.typeMod = JDBCUtils.safeGetInt(dbResult, "typtypmod"); //$NON-NLS-1$
        this.baseTypeName = JDBCUtils.safeGetString(dbResult, "base_type_name"); //$NON-NLS-1$
        this.arrayDim = JDBCUtils.safeGetInt(dbResult, "typndims"); //$NON-NLS-1$
        if (getDataSource().getServerType().supportsCollations()) {
            this.collationId = JDBCUtils.safeGetLong(dbResult, "typcollation"); //$NON-NLS-1$
        }
        this.defaultValue = JDBCUtils.safeGetString(dbResult, "typdefault"); //$NON-NLS-1$

        this.attributeCache = hasAttributes() ? new AttributeCache() : null;

        if (typeCategory == PostgreTypeCategory.E) {
            readEnumValues(session);
        }
        description = JDBCUtils.safeGetString(dbResult, "description"); //$NON-NLS-1$
    }

    PostgreDataType(PostgreDataType realType, String aliasName) {
        super(realType.getParentObject(), realType);
        setName(aliasName);
        this.alias = true;

        this.typeId = realType.typeId;
        this.typeType = realType.typeType;
        this.typeCategory = realType.typeCategory;
        this.dataKind = realType.dataKind;

        this.ownerId = realType.ownerId;
        this.isByValue = realType.isByValue;
        this.isPreferred = realType.isPreferred;
        this.arrayDelimiter = realType.arrayDelimiter;
        this.classId = realType.classId;
        this.elementTypeId = realType.elementTypeId;
        this.arrayItemTypeId = realType.arrayItemTypeId;
        this.inputFunc = realType.inputFunc;
        this.outputFunc = realType.outputFunc;
        this.receiveFunc = realType.receiveFunc;
        this.sendFunc = realType.sendFunc;
        this.modInFunc = realType.modInFunc;
        this.modOutFunc = realType.modOutFunc;
        this.analyzeFunc = realType.analyzeFunc;
        this.align = realType.align;
        this.storage = realType.storage;
        this.isNotNull = realType.isNotNull;
        this.baseTypeId = realType.baseTypeId;
        this.typeMod = realType.typeMod;
        this.arrayDim = realType.arrayDim;
        this.collationId = realType.collationId;
        this.defaultValue = realType.defaultValue;

        this.attributeCache = null;
        this.enumValues = null;
    }

    void resolveValueTypeFromBaseType(DBRProgressMonitor monitor) {
        if (baseTypeId > 0) {
            PostgreDataType baseType = getBaseType(monitor);
            if (baseType == null) {
                log.debug("Can't find type '" + getFullTypeName() + "' base type " + baseTypeId);
            } else {
                if (getTypeID() != baseType.getTypeID()) {
                    //log.debug(getFullTypeName() + " type ID resolved to " + baseType.getTypeID());
                    setTypeID(baseType.getTypeID());
                }
            }
        }
    }

    public boolean isAlias() {
        return alias;
    }

    private void readEnumValues(JDBCSession session) throws DBException {
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT e.enumlabel \n" +
                "FROM pg_catalog.pg_enum e\n" +
                "WHERE e.enumtypid=?\n" +
                "ORDER BY e.enumsortorder")) {
            dbStat.setLong(1, getObjectId());
            try (JDBCResultSet rs = dbStat.executeQuery()) {
                List<String> values = new ArrayList<>();
                while (rs.nextRow()) {
                    values.add(JDBCUtils.safeGetString(rs, 1));
                }
                enumValues = values.toArray();
            }
        } catch (SQLException e) {
            throw new DBException("Error reading enum values", e, getDataSource());
        }
    }

    public static String[] getOidTypes() {
      return OID_TYPES;
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return super.getName();
    }

    @Override
    public String getFullTypeName() {
        return super.getFullTypeName();
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return (PostgreDataSource) super.getDataSource();
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return getParentObject().getDatabase();
    }

    @Override
    public DBPDataKind getDataKind() {
        if (dataKind != null) {
            return dataKind;
        }
        return super.getDataKind();
    }

    @Nullable
    @Override
    public DBSDataType getComponentType(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getElementType(monitor);
    }

    @Nullable
    @Override
    public Object geTypeExtension() {
        return typeCategory;
    }

    @Override
    @Property(viewable = false, order = 9)
    public long getObjectId() {
        return typeId;
    }

    @Property(viewable = true, order = 10)
    public PostgreTypeType getTypeType() {
        return typeType;
    }

    @Property(viewable = true, order = 11)
    public PostgreTypeCategory getTypeCategory() {
        return typeCategory;
    }

    @Property(viewable = true, optional = true, order = 12)
    public PostgreDataType getBaseType(DBRProgressMonitor monitor) {
        return getDatabase().getDataType(monitor, baseTypeId);
    }

    @Property(viewable = true, optional = true, order = 13)
    public PostgreDataType getElementType(DBRProgressMonitor monitor) {
        return elementTypeId == 0 ? null : getDatabase().getDataType(monitor, elementTypeId);
    }

    @Property(order = 15)
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return getDatabase().getRoleById(monitor, ownerId);
    }

    @Property(category = CAT_MISC)
    public boolean isByValue() {
        return isByValue;
    }

    @Property(category = CAT_MISC)
    public boolean isPreferred() {
        return isPreferred;
    }

    @Property(category = CAT_MISC)
    public String getDefaultValue() {
        return defaultValue;
    }

    @Property(category = CAT_FUNCTIONS)
    public String getInputFunc() {
        return inputFunc;
    }

    @Property(category = CAT_FUNCTIONS)
    public String getOutputFunc() {
        return outputFunc;
    }

    @Property(category = CAT_FUNCTIONS)
    public String getReceiveFunc() {
        return receiveFunc;
    }

    @Property(category = CAT_FUNCTIONS)
    public String getSendFunc() {
        return sendFunc;
    }

    @Property(category = CAT_FUNCTIONS)
    public String getModInFunc() {
        return modInFunc;
    }

    @Property(category = CAT_FUNCTIONS)
    public String getModOutFunc() {
        return modOutFunc;
    }

    @Property(category = CAT_FUNCTIONS)
    public String getAnalyzeFunc() {
        return analyzeFunc;
    }

    @Property(category = CAT_MODIFIERS)
    public PostgreTypeAlign getAlign() {
        return align;
    }

    @Property(category = CAT_MODIFIERS)
    public PostgreTypeStorage getStorage() {
        return storage;
    }

    @Property(category = CAT_MODIFIERS)
    public boolean isNotNull() {
        return isNotNull;
    }

    @Property(category = CAT_MODIFIERS)
    public int getTypeMod() {
        return typeMod;
    }

    @Property(category = CAT_MODIFIERS)
    public PostgreCollation getCollationId(DBRProgressMonitor monitor) throws DBException {
        if (collationId != 0) {
            return getDatabase().getCollation(monitor, collationId);
        }
        return null;
    }

    @Property(category = CAT_MODIFIERS)
    public String getConstraint(DBRProgressMonitor monitor) throws DBException {
        if (typeType != PostgreTypeType.d) {
            return null;
        }
        if (constraintText != null) {
            return constraintText;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read domain constraint value")) {
            try {
            this.constraintText = JDBCUtils.queryString(
                session,
                "SELECT pg_catalog.pg_get_constraintdef((SELECT oid FROM pg_catalog.pg_constraint WHERE contypid = " + getObjectId() + "), true)");
            } catch (SQLException e) {
                throw new DBCException("Error reading domain constraint value", e, session.getExecutionContext());
            }
        }
        return this.constraintText;
    }

    @Property(category = CAT_ARRAY)
    public String getArrayDelimiter() {
        return arrayDelimiter;
    }

    @Property(category = CAT_ARRAY)
    public PostgreDataType getArrayItemType(DBRProgressMonitor monitor) {
        return arrayItemTypeId == 0 ? null : getDatabase().getDataType(monitor, arrayItemTypeId);
    }

    // Plain type
    public boolean isPlainType() {
        return arrayItemTypeId != 0;
    }

    @Property(category = CAT_ARRAY)
    public int getArrayDim() {
        return arrayDim;
    }

    public boolean hasAttributes() {
        return typeType == PostgreTypeType.c && classId >= 0;
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return DBSEntityType.TYPE;
    }

    @Override
    public List<PostgreDataTypeAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return attributeCache == null ? null : attributeCache.getAllObjects(monitor, this);
    }

    @Override
    public PostgreDataTypeAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return attributeCache == null ? null : attributeCache.getObject(monitor, this, attributeName);
    }

    @Override
    public Collection<? extends DBSEntityConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @NotNull
    @Override
    public DBCLogicalOperator[] getSupportedOperators(DBSTypedObject attribute) {
        if (dataKind == DBPDataKind.STRING) {
            if (typeCategory == PostgreTypeCategory.S || typeCategory == PostgreTypeCategory.E || typeCategory == PostgreTypeCategory.X) {
                return new DBCLogicalOperator[]{
                    DBCLogicalOperator.IS_NULL,
                    DBCLogicalOperator.IS_NOT_NULL,
                    DBCLogicalOperator.EQUALS,
                    DBCLogicalOperator.NOT_EQUALS,
                    DBCLogicalOperator.GREATER,
                    DBCLogicalOperator.LESS,
                    DBCLogicalOperator.LIKE,
                    DBCLogicalOperator.IN,
                };
            } else {
                return new DBCLogicalOperator[] {
                    DBCLogicalOperator.IS_NULL,
                    DBCLogicalOperator.IS_NOT_NULL
                };
            }
        }
        return super.getSupportedOperators(attribute);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (attributeCache != null) {
            attributeCache.clearCache();
        }
        if (typeCategory == PostgreTypeCategory.E) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Refresh enum values")) {
                readEnumValues(session);
            }
        }
        return this;
    }

    @Property(viewable = true, optional = true, order = 16)
    public Object[] getEnumValues() {
        return enumValues;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        final PostgreSchema owner = getParentObject();
        if (owner == null || owner.getName().equals(PostgreConstants.PUBLIC_SCHEMA_NAME) || owner.getName().equals(PostgreConstants.CATALOG_SCHEMA_NAME)) {
            return getName();
        } else {
            return DBUtils.getQuotedIdentifier(owner) + "." + DBUtils.getQuotedIdentifier(this);
        }
    }

    @Nullable
    @Override
    public DBPImage getObjectImage() {
        if (PostgreConstants.TYPE_JSONB.equals(getName()) || PostgreConstants.TYPE_JSON.equals(getName())) {
            return DBIcon.TYPE_JSON;
        }
        return null;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        StringBuilder sql = new StringBuilder();

        if (typeType == PostgreTypeType.d) {
            sql.append("-- DROP DOMAIN ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(";\n\n"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            sql.append("-- DROP TYPE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(";\n\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        switch (typeType) {
            case p: {
                sql.append("CREATE TYPE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
                break;
            }
            case d: {
                sql.append("CREATE DOMAIN ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" AS "); //$NON-NLS-1$ //$NON-NLS-2$
                if (baseTypeName != null) {
                    sql.append(baseTypeName);
                } else {
                    sql.append(getBaseType(monitor).getFullyQualifiedName(DBPEvaluationContext.DDL));
                }
                PostgreCollation collation = getCollationId(monitor);
                if (collation != null) {
                    sql.append("\n\tCOLLATE ").append(collation.getName()); //$NON-NLS-1$
                }
                if (!CommonUtils.isEmpty(defaultValue)) {
                    sql.append("\n\tDEFAULT ").append(defaultValue); //$NON-NLS-1$
                }
                String constraint = getConstraint(monitor);
                if (!CommonUtils.isEmpty(constraint)) {
                    sql.append("\n\tCONSTRAINT ").append(constraint); //$NON-NLS-1$
                }

                sql.append(";"); //$NON-NLS-1$
                break;
            }
            case e: {
                sql.append("CREATE TYPE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" AS ENUM (\n"); //$NON-NLS-1$ //$NON-NLS-2$
                if (enumValues != null) {
                    for (int i = 0; i < enumValues.length; i++) {
                        Object item = enumValues[i];
                        sql.append("\t").append(SQLUtils.quoteString(this, CommonUtils.toString(item)));
                        if (i < enumValues.length - 1) sql.append(",\n"); //$NON-NLS-1$
                    }
                }
                sql.append(");\n"); //$NON-NLS-1$
                break;
            }
            case r: {
                sql.append("CREATE TYPE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" AS RANGE (\n"); //$NON-NLS-1$ //$NON-NLS-2$
                PostgreCollation collation = getCollationId(monitor);
                appendCreateTypeParameter(sql, "COLLATION ", collation.getName());
                appendCreateTypeParameter(sql, "CANONICAL", canonicalName);
                // TODO: read data from pg_range
//                if (!CommonUtils.isEmpty(su)) {
//                    sql.append("\n\tCOLLATION ").append(canonicalName);
//                }
                sql.append(");\n"); //$NON-NLS-1$
                break;
            }
            case b: {
                sql.append("CREATE TYPE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" ("); //$NON-NLS-1$ //$NON-NLS-2$

                if (isValidFuncRef(inputFunc)) appendCreateTypeParameter(sql, "INPUT", inputFunc);
                if (isValidFuncRef(outputFunc)) appendCreateTypeParameter(sql, "OUTPUT", outputFunc);
                if (isValidFuncRef(receiveFunc)) appendCreateTypeParameter(sql, "RECEIVE", receiveFunc);
                if (isValidFuncRef(sendFunc)) appendCreateTypeParameter(sql, "SEND", sendFunc);
                if (isValidFuncRef(modInFunc)) appendCreateTypeParameter(sql, "TYPMOD_IN", modInFunc);
                if (isValidFuncRef(modOutFunc)) appendCreateTypeParameter(sql, "TYPMOD_OUT", modOutFunc);
                if (isValidFuncRef(analyzeFunc)) appendCreateTypeParameter(sql, "ANALYZE", analyzeFunc);
                if (getMaxLength() > 0) appendCreateTypeParameter(sql, "INTERNALLENGTH", getMaxLength());
                if (isByValue) appendCreateTypeParameter(sql, "PASSEDBYVALUE");
                if (align != null && align.getBytes() > 1) appendCreateTypeParameter(sql, "ALIGNMENT", align.getBytes());
                if (storage != null) appendCreateTypeParameter(sql, "STORAGE", storage.getName());
                if (typeCategory != null) appendCreateTypeParameter(sql, "CATEGORY", typeCategory.name());
                if (isPreferred) appendCreateTypeParameter(sql, "PREFERRED", isPreferred);
                appendCreateTypeParameter(sql, "DEFAULT", defaultValue);

                PostgreDataType elementType = getElementType(monitor);
                if (elementType != null) {
                    appendCreateTypeParameter(sql, "ELEMENT", elementType.getFullyQualifiedName(DBPEvaluationContext.DDL));
                }
                if (!CommonUtils.isEmpty(arrayDelimiter)) appendCreateTypeParameter(sql, "DELIMITER", SQLUtils.quoteString(getDataSource(), arrayDelimiter));
                if (collationId != 0) appendCreateTypeParameter(sql, "COLLATABLE", true);

                sql.append(");\n"); //$NON-NLS-1$
                break;
            }
            case c: {
                sql.append("CREATE TYPE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" AS ("); //$NON-NLS-1$ //$NON-NLS-2$
                Collection<PostgreDataTypeAttribute> attributes = getAttributes(monitor);
                if (!CommonUtils.isEmpty(attributes)) {
                    boolean first = true;
                    for (PostgreDataTypeAttribute attr : attributes) {
                        if (!first) sql.append(","); //$NON-NLS-1$
                        first = false;

                        sql.append("\n\t") //$NON-NLS-1$
                            .append(DBUtils.getQuotedIdentifier(attr)).append(" ").append(attr.getTypeName()); //$NON-NLS-1$
                        String modifiers = SQLUtils.getColumnTypeModifiers(getDataSource(), attr, attr.getTypeName(), attr.getDataKind());
                        if (modifiers != null) sql.append(modifiers);
                    }
                }
                sql.append(");\n"); //$NON-NLS-1$
                break;
            }
            default: {
                sql.append("-- Data type ").append(getFullyQualifiedName(DBPEvaluationContext.UI)).append(" (").append(typeType.getName()).append(") DDL is not supported\n");
                break;
            }
        }

        String description = getDescription();
        if (!CommonUtils.isEmpty(description)) {
            sql.append("\nCOMMENT ON TYPE ") //$NON-NLS-1$
                    .append(getFullyQualifiedName(DBPEvaluationContext.DDL))
                    .append(" IS ") //$NON-NLS-1$
                    .append(SQLUtils.quoteString(this, description))
                    .append(";"); //$NON-NLS-1$
        }

        return sql.toString();
    }

    private boolean isValidFuncRef(String func) {
        return !CommonUtils.isEmpty(func) && !func.equals("-"); //$NON-NLS-1$
    }

    private void appendCreateTypeParameter(@NotNull StringBuilder sql, @NotNull String name, @Nullable Object value) {
        if (value == null) {
            return;
        }
        if (sql.charAt(sql.length() - 1)!= '(') {
            sql.append(","); //$NON-NLS-1$
        }
        sql.append("\n\t").append(name).append(" = ").append(value); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void appendCreateTypeParameter(@NotNull StringBuilder sql, @NotNull String name) {
        if (Character.isLetterOrDigit(sql.charAt(sql.length() - 1))) {
            sql.append(",");//$NON-NLS-1$
        }
        sql.append("\n\t").append(name); //$NON-NLS-1$
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    class AttributeCache extends JDBCObjectCache<PostgreDataType, PostgreDataTypeAttribute> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDataType postgreDataType) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT c.relname,a.*,pg_catalog.pg_get_expr(ad.adbin, ad.adrelid, true) as def_value,dsc.description" +
                "\nFROM pg_catalog.pg_attribute a" +
                "\nINNER JOIN pg_catalog.pg_class c ON (a.attrelid=c.oid)" +
                "\nLEFT OUTER JOIN pg_catalog.pg_attrdef ad ON (a.attrelid=ad.adrelid AND a.attnum = ad.adnum)" +
                "\nLEFT OUTER JOIN pg_catalog.pg_description dsc ON (c.oid=dsc.objoid AND a.attnum = dsc.objsubid)" +
                "\nWHERE a.attnum > 0 AND NOT a.attisdropped AND c.oid=?" +
                "\nORDER BY a.attnum");
            dbStat.setLong(1, postgreDataType.classId);
            return dbStat;
        }

        @Override
        protected PostgreDataTypeAttribute fetchObject(@NotNull JDBCSession session, @NotNull PostgreDataType postgreDataType, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new PostgreDataTypeAttribute(session.getProgressMonitor(), postgreDataType, resultSet);
        }
    }

    @Property(order = 100, editable = true, viewable = true, updatable = true)
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static PostgreDataType readDataType(@NotNull JDBCSession session, @NotNull PostgreSchema schema, @NotNull JDBCResultSet dbResult, boolean skipTables) throws SQLException, DBException
    {
        //long schemaId = JDBCUtils.safeGetLong(dbResult, "typnamespace");
        long typeId = JDBCUtils.safeGetLong(dbResult, "oid"); //$NON-NLS-1$
        String name = JDBCUtils.safeGetString(dbResult, "typname"); //$NON-NLS-1$
        if (CommonUtils.isEmpty(name)) {
            log.debug("Empty name for data type " + typeId);
            return null;
        }
        if (skipTables) {
            String relKind = JDBCUtils.safeGetString(dbResult, "relkind"); //$NON-NLS-1$
            if (relKind != null) {
                try {
                    final RelKind tableType = RelKind.valueOf(relKind);
                    if (tableType != RelKind.c) {
                        // No a composite data type - skip it
                        return null;
                    }
                } catch (Exception e) {
                    log.debug(e.getMessage());
                }
            }
        }
        int typeLength = JDBCUtils.safeGetInt(dbResult, "typlen"); //$NON-NLS-1$
        PostgreTypeCategory typeCategory;
        final String catString =
            PostgreUtils.supportsTypeCategory(session.getDataSource()) ? JDBCUtils.safeGetString(dbResult, "typcategory") : null; //$NON-NLS-1$
        if (catString == null) {
            typeCategory = null;
        } else {
            try {
                typeCategory = PostgreTypeCategory.valueOf(catString.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.debug(e);
                typeCategory = null;
            }
        }

        int valueType;
        if (ArrayUtils.contains(OID_TYPES, name) || name.equals(PostgreConstants.TYPE_HSTORE)) {
            valueType = Types.VARCHAR;
        } else {
            if (typeCategory == null) {
                final long typElem = JDBCUtils.safeGetLong(dbResult, "typelem");
                // In old PostgreSQL versions
                switch ((int) typeId) {
                    case PostgreOid.BIT:
                        valueType = Types.BIT;
                        break;
                    case PostgreOid.BOOL:
                        valueType = Types.BOOLEAN;
                        break;
                    case PostgreOid.INT2:
                        valueType = Types.SMALLINT;
                        break;
                    case PostgreOid.INT4:
                        valueType = Types.INTEGER;
                        break;
                    case PostgreOid.INT8:
                        valueType = Types.BIGINT;
                        break;
                    case PostgreOid.FLOAT4:
                        valueType = Types.FLOAT;
                        break;
                    case PostgreOid.FLOAT8:
                        valueType = Types.DOUBLE;
                        break;
                    case PostgreOid.NUMERIC:
                        valueType = Types.NUMERIC;
                        break;
                    case PostgreOid.CHAR:
                        valueType = Types.CHAR;
                        break;
                    case PostgreOid.VARCHAR:
                        valueType = Types.VARCHAR;
                        break;
                    case PostgreOid.DATE:
                        valueType = Types.DATE;
                        break;
                    case PostgreOid.TIME:
                    case PostgreOid.TIMETZ:
                        valueType = Types.TIME;
                        break;
                    case PostgreOid.TIMESTAMP:
                    case PostgreOid.TIMESTAMPTZ:
                        valueType = Types.TIMESTAMP;
                        break;
                    case PostgreOid.BYTEA:
                        valueType = Types.BINARY;
                        break;
                    case PostgreOid.BPCHAR:
                        valueType = Types.CHAR;
                        break;
                    case PostgreOid.XML:
                        valueType = Types.SQLXML;
                        break;
                    case PostgreOid.NAME:
                        valueType = Types.VARCHAR;
                        break;
                    case PostgreOid.OID:
                    case PostgreOid.BOX:
                        valueType = Types.OTHER;
                        break;
                    default:
                        if (typElem > 0 && typeLength < 0) {
                            valueType = Types.ARRAY;
                        } else {
                            valueType = Types.OTHER;
                        }
                        break;
                }
            } else {
                switch (typeCategory) {
                    case A:
                        valueType = Types.ARRAY;
                        break;
                    case P:
                        valueType = Types.OTHER;
                        break;
                    case B:
                        valueType = Types.BOOLEAN;
                        break;
                    case C:
                        valueType = Types.STRUCT;
                        break;
                    case D:
                        if (typeLength == 4) {
                            valueType = Types.DATE;
                        } else {
                            switch ((int) typeId) {
                                case PostgreOid.DATE:
                                    valueType = Types.DATE;
                                    break;
                                case PostgreOid.TIME:
                                case PostgreOid.TIMETZ:
                                    valueType = Types.TIME;
                                    break;
                                case PostgreOid.TIMESTAMP:
                                case PostgreOid.TIMESTAMPTZ:
                                    valueType = Types.TIMESTAMP;
                                    break;
                                default:
                                    valueType = Types.TIMESTAMP;
                                    break;
                            }
                        }
                        break;
                    case N:
                        valueType = Types.NUMERIC;
                        // Kind of a hack (#7459). Don't know any better way to distinguish floats from integers
                        String outputF = JDBCUtils.safeGetString(dbResult, "typoutput");
                        if (name.equals("numeric")) {
                            valueType = Types.NUMERIC;
                        } else if (outputF != null && outputF.startsWith("float")) {
                            switch (typeLength) {
                                case 4:
                                    valueType = Types.FLOAT;
                                    break;
                                case 8:
                                    valueType = Types.DOUBLE;
                                    break;
                            }
                        } else {
                            switch (typeLength) {
                                case 2:
                                    valueType = Types.SMALLINT;
                                    break;
                                case 4:
                                    valueType = Types.INTEGER;
                                    break;
                                case 8:
                                    valueType = Types.BIGINT;
                                    break;
                            }
                        }
                        break;
                    case S:
                        //                if (name.equals("text")) {
                        //                    valueType = Types.CLOB;
                        //                } else {
                        valueType = Types.VARCHAR;
                        //                }
                        break;
                    case U:
                        switch (name) {
                            case "bytea":
                                valueType = Types.BINARY;
                                break;
                            case "xml":
                                valueType = Types.SQLXML;
                                break;
                            default:
                                valueType = Types.OTHER;
                                break;
                        }
                        break;
                    case V:
                        valueType = Types.NUMERIC;
                        break;
                    default:
                        valueType = Types.OTHER;
                        break;
                }
            }
        }

        return new PostgreDataType(
            session,
            schema,
            typeId,
            valueType,
            name,
            typeLength,
            dbResult);
    }

}
