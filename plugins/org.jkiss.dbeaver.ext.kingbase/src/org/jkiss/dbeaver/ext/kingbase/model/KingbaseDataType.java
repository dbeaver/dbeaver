/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
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
import org.jkiss.dbeaver.model.meta.ForTest;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSBindableDataType;
import org.jkiss.dbeaver.model.struct.DBSContextBoundAttribute;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

/**
 * KingbaseTypeType
 */
public class KingbaseDataType extends JDBCDataType<KingbaseSchema> 
    implements KingbaseClass, KingbaseScriptObject, DBPQualifiedObject, DBPImageProvider, DBSBindableDataType, DBPNamedObject2 {

    private static final Log log = Log.getLog(KingbaseDataType.class);

    //private static final String CAT_MAIN = "Main";
    private static final String CAT_MISC = "Miscellaneous";
    private static final String CAT_MODIFIERS = "Modifiers";
    private static final String CAT_FUNCTIONS = "Functions";
    private static final String CAT_ARRAY = "Array";

    private static final String[] OID_TYPES = new String[] {
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

    private static final String[] VECTOR_TYPES = {
        "int2vector",
        "oidvector"
    };

    private final boolean alias;

    private long typeId;
    private KingbaseTypeType typeType;
    private KingbaseTypeCategory typeCategory;
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
    private KingbaseTypeAlign align = KingbaseTypeAlign.c;
    private KingbaseTypeStorage storage = KingbaseTypeStorage.p;
    private boolean isNotNull;
    private long baseTypeId;
    private int typeMod;
    private String baseTypeName;
    private int arrayDim;
    private long collationId;
    private String defaultValue;
    private String canonicalName;
    private List<String> constraintsText;
    private String description;
    private boolean extraDataType;

    private final AttributeCache attributeCache;
    private Object[] enumValues;

    public KingbaseDataType(@NotNull JDBCSession session, @NotNull KingbaseSchema schema, long typeId, int valueType, String name, int length, JDBCResultSet dbResult) throws DBException {
        super(schema, valueType, name, null, false, true, length, -1, -1);
        this.alias = false;
        if (schema.isCatalogSchema() || schema.isSysCatalogSchema()) {
            this.canonicalName = KingbaseConstants.DATA_TYPE_CANONICAL_NAMES.get(name);
        }
        this.typeId = typeId;
        this.typeType = KingbaseTypeType.b;
        String typTypeStr = JDBCUtils.safeGetString(dbResult, "typtype"); //$NON-NLS-1$
        try {
            if (typTypeStr != null && !typTypeStr.isEmpty()) {
                this.typeType = KingbaseTypeType.valueOf(typTypeStr.toLowerCase(Locale.ENGLISH));
            }
        } catch (Throwable e) {
            log.debug("Invalid type type [" + typTypeStr + "] - " + e.getMessage());
        }
        this.typeCategory = KingbaseTypeCategory.X;
        boolean supportsCategory = true;
        if (supportsCategory) {
            String typCategoryStr = JDBCUtils.safeGetString(dbResult, "typcategory"); //$NON-NLS-1$
            try {
                if (typCategoryStr != null && !typCategoryStr.isEmpty()) {
                    this.typeCategory = KingbaseTypeCategory.valueOf(typCategoryStr.toUpperCase(Locale.ENGLISH));
                }
            } catch (Throwable e) {
                log.debug("Invalid type category [" + typCategoryStr + "] - " + e.getMessage());
            }
        }

        if (typeType == KingbaseTypeType.e) {
            this.dataKind = DBPDataKind.STRING;
        } else {
            this.dataKind = JDBCDataSource.getDataKind(getName(), valueType);
            if (this.dataKind == DBPDataKind.OBJECT) {
                if (KingbaseConstants.TYPE_JSONB.equals(name) || KingbaseConstants.TYPE_JSON.equals(name)) {
                    this.dataKind = DBPDataKind.CONTENT;
                } else if (KingbaseConstants.TYPE_INTERVAL.equals(name)) {
                    this.dataKind = DBPDataKind.DATETIME;
                }
            }
        }

        this.ownerId = JDBCUtils.safeGetLong(dbResult, "typowner"); 
        this.isByValue = JDBCUtils.safeGetBoolean(dbResult, "typbyval"); 
        
        this.isPreferred = JDBCUtils.safeGetBoolean(dbResult, "typispreferred");
        
        this.arrayDelimiter = JDBCUtils.safeGetString(dbResult, "typdelim"); 
        this.classId = JDBCUtils.safeGetLong(dbResult, "typrelid"); 
        this.elementTypeId = JDBCUtils.safeGetLong(dbResult, "typelem"); 
        if (getDataSource().getServerType().supportsCustomDataTypes()) {
            this.inputFunc = JDBCUtils.safeGetString(dbResult, "typinput"); 
            this.outputFunc = JDBCUtils.safeGetString(dbResult, "typoutput"); 
            this.receiveFunc = JDBCUtils.safeGetString(dbResult, "typreceive"); 
            this.sendFunc = JDBCUtils.safeGetString(dbResult, "typsend"); 
        }
        
        this.arrayItemTypeId = JDBCUtils.safeGetLong(dbResult, "typarray"); 
        this.modInFunc = JDBCUtils.safeGetString(dbResult, "typmodin"); 
        this.modOutFunc = JDBCUtils.safeGetString(dbResult, "typmodout"); 
      
        this.analyzeFunc = JDBCUtils.safeGetString(dbResult, "typanalyze"); 
        String typAlignStr = JDBCUtils.safeGetString(dbResult, "typalign"); 
        if (!CommonUtils.isEmpty(typAlignStr)) {
            try {
                this.align = KingbaseTypeAlign.valueOf(typAlignStr);
            } catch (Exception e) {
                log.debug("Invalid type align [" + typAlignStr + "] - " + e.getMessage());
            }
        }
        String typStorageStr = JDBCUtils.safeGetString(dbResult, "typstorage"); 
        if (!CommonUtils.isEmpty(typStorageStr)) {
            try {
                this.storage = KingbaseTypeStorage.valueOf(typStorageStr);
            } catch (Exception e) {
                log.debug("Invalid type storage [" + typStorageStr + "] - " + e.getMessage());
            }
        }
        this.isNotNull = JDBCUtils.safeGetBoolean(dbResult, "typnotnull"); 
        this.baseTypeId = JDBCUtils.safeGetLong(dbResult, "typbasetype"); 
        this.typeMod = JDBCUtils.safeGetInt(dbResult, "typtypmod"); 
        this.baseTypeName = JDBCUtils.safeGetString(dbResult, "base_type_name"); 
        this.arrayDim = JDBCUtils.safeGetInt(dbResult, "typndims"); 
        if (getDataSource().getServerType().supportsCollations()) {
            this.collationId = JDBCUtils.safeGetLong(dbResult, "typcollation");
        }
        this.defaultValue = JDBCUtils.safeGetString(dbResult, "typdefault"); 

        this.attributeCache = hasAttributes() ? new AttributeCache() : null;

        if (typeCategory == KingbaseTypeCategory.E) {
            readEnumValues(session.getProgressMonitor());
        }
        description = JDBCUtils.safeGetString(dbResult, "description"); 
    }

    KingbaseDataType(KingbaseDataType realType, String aliasName) {
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

    @ForTest
    public KingbaseDataType(KingbaseSchema schema, int valueType, String name) {
        super(schema, valueType, name, null, false, false, -1, -1, -1);
        alias = false;
        ownerId = 0;
        attributeCache = null;
    }

    void resolveValueTypeFromBaseType(DBRProgressMonitor monitor) {
        if (baseTypeId > 0) {
            KingbaseDataType baseType = getBaseType(monitor);
            if (baseType == null) {
                log.debug("Can't find type '" + getFullTypeName() + "' base type " + baseTypeId);
            } else {
                if (getTypeID() != baseType.getTypeID()) {
                    setTypeID(baseType.getTypeID());
                }
            }
        }
    }

    @Nullable
    String getConditionTypeCasting(boolean isInCondition, boolean castColumnName) {
        final String typeName = getTypeName();
        if (isInCondition && typeCategory == KingbaseTypeCategory.E) {
            return "::text";
        }
        if (isInCondition && (KingbaseConstants.TYPE_JSON.equals(typeName) || KingbaseConstants.TYPE_XML.equals(typeName))) {

            return "::text";
        }
        if (!castColumnName && (ArrayUtils.contains(KingbaseDataType.getOidTypes(), typeName) || getTypeID() == Types.OTHER)) {

            return "::" + getFullyQualifiedName(DBPEvaluationContext.DDL);
        }
        return null;
    }

    public boolean isAlias() {
        return alias;
    }

    public void setTypeId(long typeId) {
        this.typeId = typeId;
    }

    public boolean isExtraDataType() {
        return extraDataType;
    }

    public void setExtraDataType(boolean extraDataType) {
        this.extraDataType = extraDataType;
    }

    private void readEnumValues(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (getDataSource().isSupportsEnumTable()) {
            List<KingbaseEnumValue> cachedObjects = getDatabase().getEnumValueCache()
                .getAllObjects(monitor, getDatabase());
            enumValues = cachedObjects.stream()
                .filter(e -> e.getEnumTypId() == getObjectId())
                .sorted(Comparator.comparing(KingbaseEnumValue::getEnumSortOrder))
                .map(KingbaseEnumValue::getEnumLabel)
                .toArray();
        }
    }

    private void readNewEnumValues(DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Refresh enum values")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT e.enumlabel \n" +
                    "FROM sys_catalog.sys_enum e\n" +
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
                throw new DBDatabaseException("Error reading enum values", e, getDataSource());
            }
        }
    }

    public static String[] getOidTypes() {
      return OID_TYPES;
    }

    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return super.getName();
    }

    @NotNull
    @Override
    public String getFullTypeName() {
        return super.getFullTypeName();
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    @NotNull
    @Override
    public KingbaseDataSource getDataSource() {
        return (KingbaseDataSource) super.getDataSource();
    }

    @NotNull
    @Override
    public KingbaseDatabase getDatabase() {
        return getParentObject().getDatabase();
    }

    @NotNull
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
    public boolean isStructurallyConsistentTypeWith(@NotNull DBCAttributeMetaData metaData) {
        return super.isStructurallyConsistentTypeWith(metaData) || typeCategory == KingbaseTypeCategory.E;
    }

    @Override
    @Property(viewable = false, order = 9)
    public long getObjectId() {
        return typeId;
    }

    @Property(viewable = true, order = 10)
    public KingbaseTypeType getTypeType() {
        return typeType;
    }

    @Property(viewable = true, order = 11)
    public KingbaseTypeCategory getTypeCategory() {
        return typeCategory;
    }

    @Property(viewable = true, optional = true, order = 12)
    public KingbaseDataType getBaseType(DBRProgressMonitor monitor) {
        return getDatabase().getDataType(monitor, baseTypeId);
    }

    public boolean isArray() {
        return elementTypeId != 0;
    }

    @Property(viewable = true, optional = true, order = 13)
    public KingbaseDataType getElementType(DBRProgressMonitor monitor) {
        return elementTypeId == 0 ? null : getDatabase().getDataType(monitor, elementTypeId);
    }

    @Property(order = 15)
    public KingbaseRole getOwner(DBRProgressMonitor monitor) throws DBException {
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
    public KingbaseTypeAlign getAlign() {
        return align;
    }

    @Property(category = CAT_MODIFIERS)
    public KingbaseTypeStorage getStorage() {
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
    public KingbaseCollation getCollationId(DBRProgressMonitor monitor) throws DBException {
        if (collationId != 0) {
            return getDatabase().getCollation(monitor, collationId);
        }
        return null;
    }

    @Property(name = "Constraints", length = PropertyLength.MULTILINE)
    public List<String> getConstraintsDefinition(DBRProgressMonitor monitor) throws DBException {
        if (typeType != KingbaseTypeType.d) {
            return null;
        }
        if (constraintsText != null) {
            return constraintsText;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read domain constraint value")) {
            try {
                this.constraintsText = JDBCUtils.queryStrings(
                    session, 
                    "SELECT concat(c.conname, ' ', sys_catalog.sys_get_constraintdef(oid, true))\r\n"
                    + "FROM sys_catalog.sys_constraint c\r\n"
                    + "WHERE contypid = " + getObjectId());
            } catch (SQLException e) {
                throw new DBCException("Error reading domain constraint value", e, session.getExecutionContext());
            }
        }
        return this.constraintsText;
    }

    @Property(category = CAT_ARRAY)
    public String getArrayDelimiter() {
        return arrayDelimiter;
    }

    @Property(category = CAT_ARRAY)
    public KingbaseDataType getArrayItemType(DBRProgressMonitor monitor) {
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
        return typeType == KingbaseTypeType.c && classId >= 0;
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return DBSEntityType.TYPE;
    }

    @Nullable
    @Override
    public List<? extends DBSContextBoundAttribute> bindAttributesToContext(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntity dataContainer,
        @NotNull DBSEntityAttribute memberContext
    ) throws DBException {
        List<KingbaseDataTypeAttribute> attrs = this.getAttributes(monitor);
        if (attrs == null) {
            return null;
        }
    
        List<KingbaseDataBoundTypeAttribute> boundAttrs = new ArrayList<>(attrs.size());
        for (KingbaseDataTypeAttribute attr : attrs) {
            boundAttrs.add(new KingbaseDataBoundTypeAttribute(monitor, (KingbaseTableBase) dataContainer, memberContext, attr));
        }
        return boundAttrs;
    }

    @Override
    public List<KingbaseDataTypeAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return attributeCache == null ? null : attributeCache.getAllObjects(monitor, this);
    }

    @Override
    public KingbaseDataTypeAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
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
            if (typeCategory == KingbaseTypeCategory.S || typeCategory == KingbaseTypeCategory.E || typeCategory == KingbaseTypeCategory.X) {
                return new DBCLogicalOperator[]{
                    DBCLogicalOperator.IS_NULL,
                    DBCLogicalOperator.IS_NOT_NULL,
                    DBCLogicalOperator.EQUALS,
                    DBCLogicalOperator.NOT_EQUALS,
                    DBCLogicalOperator.GREATER,
                    DBCLogicalOperator.LESS,
                    DBCLogicalOperator.LIKE,
                    DBCLogicalOperator.ILIKE,
                    DBCLogicalOperator.IN,
                };
            } else {
                return new DBCLogicalOperator[] {
                    DBCLogicalOperator.IS_NULL,
                    DBCLogicalOperator.IS_NOT_NULL
                };
            }
        } else if (dataKind == DBPDataKind.OBJECT && (typeCategory == KingbaseTypeCategory.G || typeCategory == KingbaseTypeCategory.U)) {
            List<DBCLogicalOperator> operators = new ArrayList<DBCLogicalOperator>();
            if (attribute instanceof DBSAttributeBase && !((DBSAttributeBase) attribute).isRequired()) {
                operators.add(DBCLogicalOperator.IS_NULL);
                operators.add(DBCLogicalOperator.IS_NOT_NULL);
            }
            operators.add(DBCLogicalOperator.EQUALS);
            return operators.toArray(new DBCLogicalOperator[0]);
        }
        return super.getSupportedOperators(attribute);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (attributeCache != null) {
            attributeCache.clearCache();
        }
        if (enumValues != null) {
            getDatabase().getEnumValueCache().clearCache();
            enumValues = null;
        }
        return this;
    }

    @Property(viewable = true, order = 16, visibleIf = EnumTypeValidator.class)
    public Object[] getEnumValues(DBRProgressMonitor monitor) {
        if (typeCategory == KingbaseTypeCategory.E && ArrayUtils.isEmpty(enumValues)) {
            try {
                readEnumValues(monitor);
                if (ArrayUtils.isEmpty(enumValues)) {
                    readNewEnumValues(monitor);
                }
            } catch (DBException e) {
                log.error("Can't read enum values of type " + getFullTypeName());
                enumValues = new Object[]{0};
            }
        }
        return enumValues;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        final KingbaseSchema owner = getParentObject();
        if (owner == null || owner.getName().equals(KingbaseConstants.CATALOG_SCHEMA_NAME) || owner.getName().equals(KingbaseConstants.SYS_CATALOG_SCHEMA_NAME)) {
            return getName();
        } else {
            return DBUtils.getQuotedIdentifier(owner) + "." + DBUtils.getQuotedIdentifier(this);
        }
    }

    @Nullable
    @Override
    public DBPImage getObjectImage() {
        if (KingbaseConstants.TYPE_JSONB.equals(getName()) || KingbaseConstants.TYPE_JSON.equals(getName())) {
            return DBIcon.TYPE_JSON;
        }
        return null;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        StringBuilder sql = new StringBuilder();

        if (typeType == KingbaseTypeType.d) {
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
                sql.append("CREATE DOMAIN ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" AS "); 
                if (baseTypeName != null) {
                    sql.append(baseTypeName);
                } else {
                    sql.append(getBaseType(monitor).getFullyQualifiedName(DBPEvaluationContext.DDL));
                }
                KingbaseCollation collation = getCollationId(monitor);
                if (collation != null) {
                    sql.append("\n\tCOLLATE ").append(DBUtils.getQuotedIdentifier(collation)); 
                }
                if (!CommonUtils.isEmpty(defaultValue)) {
                    sql.append("\n\tDEFAULT ").append(defaultValue); 
                }
                List<String> constraints = getConstraintsDefinition(monitor);
                for (String constraint : constraints) {
                    if (!CommonUtils.isEmpty(constraint)) {
                        sql.append("\n\tCONSTRAINT ").append(constraint);
                    }
                }

                sql.append(";");
                break;
            }
            case e: {
                sql.append("CREATE TYPE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" AS ENUM (\n"); 
                if (enumValues != null) {
                    for (int i = 0; i < enumValues.length; i++) {
                        Object item = enumValues[i];
                        sql.append("\t").append(SQLUtils.quoteString(this, CommonUtils.toString(item)));
                        if (i < enumValues.length - 1) sql.append(",\n"); 
                    }
                }
                sql.append(");\n"); //$NON-NLS-1$
                break;
            }
            case r: {
                KingbaseCollation collation = getCollationId(monitor);
                if (collation != null) {
                    sql.append("CREATE TYPE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" AS RANGE (\n"); //$NON-NLS-1$ //$NON-NLS-2$
                    appendCreateTypeParameter(sql, "COLLATION ", collation.getName());
                    appendCreateTypeParameter(sql, "CANONICAL", canonicalName);
                    sql.append(");\n"); 
                }
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

                KingbaseDataType elementType = getElementType(monitor);
                if (elementType != null) {
                    appendCreateTypeParameter(sql, "ELEMENT", elementType.getFullyQualifiedName(DBPEvaluationContext.DDL));
                }
                if (!CommonUtils.isEmpty(arrayDelimiter)) appendCreateTypeParameter(sql, "DELIMITER", SQLUtils.quoteString(getDataSource(), arrayDelimiter));
                if (collationId != 0) appendCreateTypeParameter(sql, "COLLATABLE", true);

                sql.append(");\n"); 
                break;
            }
            case c: {
                sql.append("CREATE TYPE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" AS ("); 
                Collection<KingbaseDataTypeAttribute> attributes = getAttributes(monitor);
                if (!CommonUtils.isEmpty(attributes)) {
                    boolean first = true;
                    for (KingbaseDataTypeAttribute attr : attributes) {
                        if (!first) sql.append(","); 
                        first = false;

                        sql.append("\n\t") 
                            .append(DBUtils.getQuotedIdentifier(attr)).append(" ").append(attr.getTypeName()); //$NON-NLS-1$
                        String modifiers = SQLUtils.getColumnTypeModifiers(getDataSource(), attr, attr.getTypeName(), attr.getDataKind());
                        if (modifiers != null) sql.append(modifiers);
                    }
                }
                sql.append(");\n"); 
                break;
            }
            default: {
                sql.append("-- Data type ").append(getFullyQualifiedName(DBPEvaluationContext.UI)).append(" (").append(typeType.getName()).append(") DDL is not supported\n");
                break;
            }
        }

        String description = getDescription();
        if (!CommonUtils.isEmpty(description)) {
            sql.append("\nCOMMENT ON TYPE ") 
                    .append(getFullyQualifiedName(DBPEvaluationContext.DDL))
                    .append(" IS ") 
                    .append(SQLUtils.quoteString(this, description))
                    .append(";"); 
        }

        return sql.toString();
    }

    private boolean isValidFuncRef(String func) {
        return !CommonUtils.isEmpty(func) && !func.equals("-");
    }

    private void appendCreateTypeParameter(@NotNull StringBuilder sql, @NotNull String name, @Nullable Object value) {
        if (value == null) {
            return;
        }
        if (sql.charAt(sql.length() - 1)!= '(') {
            sql.append(","); //$NON-NLS-1$
        }
        sql.append("\n\t").append(name).append(" = ").append(value); 
    }

    private void appendCreateTypeParameter(@NotNull StringBuilder sql, @NotNull String name) {
        if (Character.isLetterOrDigit(sql.charAt(sql.length() - 1))) {
            sql.append(",");//$NON-NLS-1$
        }
        sql.append("\n\t").append(name); 
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    class AttributeCache extends JDBCObjectCache<KingbaseDataType, KingbaseDataTypeAttribute> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull KingbaseDataType kingbaseDataType) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT c.relname,row_number() OVER (ORDER BY a.attnum) as attnum,a.*,sys_catalog.sys_get_expr(ad.adbin, ad.adrelid, true) as def_value,dsc.description" +
                "\nFROM sys_catalog.sys_attribute a" +
                "\nINNER JOIN sys_catalog.sys_class c ON (a.attrelid=c.oid)" +
                "\nLEFT OUTER JOIN sys_catalog.sys_attrdef ad ON (a.attrelid=ad.adrelid AND a.attnum = ad.adnum)" +
                "\nLEFT OUTER JOIN sys_catalog.sys_description dsc ON (c.oid=dsc.objoid AND a.attnum = dsc.objsubid)" +
                "\nWHERE a.attnum > 0 AND NOT a.attisdropped AND c.oid=?" +
                "\nORDER BY a.attnum");
            dbStat.setLong(1, kingbaseDataType.classId);
            return dbStat;
        }

        @Override
        protected KingbaseDataTypeAttribute fetchObject(@NotNull JDBCSession session, @NotNull KingbaseDataType kingbaseDataType, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new KingbaseDataTypeAttribute(session.getProgressMonitor(), kingbaseDataType, resultSet);
        }
    }

    @Property(order = 100, length = PropertyLength.MULTILINE, editable = true, viewable = true, updatable = true)
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static KingbaseDataType readDataType(@NotNull JDBCSession session, @NotNull KingbaseDatabase database, @NotNull JDBCResultSet dbResult, boolean skipTables) throws SQLException, DBException
    {
        long schemaId = JDBCUtils.safeGetLong(dbResult, "typnamespace");
        KingbaseSchema dataTypeSchema = database.getSchema(session.getProgressMonitor(), schemaId);
        if (dataTypeSchema == null) {
            return null;
        }
        long typeId = JDBCUtils.safeGetLong(dbResult, "oid"); 
        String name = JDBCUtils.safeGetString(dbResult, "typname"); 
        if (CommonUtils.isEmpty(name)) {
            log.debug("Empty name for data type " + typeId);
            return null;
        }
        boolean readAllTypes = database.getDataSource().supportReadingAllDataTypes();
        if (!readAllTypes && skipTables) {
            String relKind = JDBCUtils.safeGetString(dbResult, "relkind"); 
            if (relKind != null) {
                try {
                    final RelKind tableType = RelKind.valueOf(relKind);
                    if (tableType != RelKind.c) {
                        return null;
                    }
                } catch (Exception e) {
                    log.debug(e.getMessage());
                }
            }
        }
        int typeLength = JDBCUtils.safeGetInt(dbResult, "typlen"); 
        KingbaseTypeCategory typeCategory;
        final String catString =
            KingbaseUtils.supportsTypeCategory(session.getDataSource()) ? JDBCUtils.safeGetString(dbResult, "typcategory") : null; //$NON-NLS-1$
        if (catString == null) {
            typeCategory = null;
        } else {
            try {
                typeCategory = KingbaseTypeCategory.valueOf(catString.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.debug(e);
                typeCategory = null;
            }
        }

        int valueType;
        if (ArrayUtils.contains(OID_TYPES, name) || ArrayUtils.contains(VECTOR_TYPES, name) || name.equals(KingbaseConstants.TYPE_HSTORE)) {
            valueType = Types.VARCHAR;
        } else {
            if (typeCategory == null) {
                final long typElem = JDBCUtils.safeGetLong(dbResult, "typelem");
               
                switch ((int) typeId) {
                    case KingbaseOid.BIT:
                        valueType = Types.BIT;
                        break;
                    case KingbaseOid.BOOL:
                        valueType = Types.BOOLEAN;
                        break;
                    case KingbaseOid.INT2:
                        valueType = Types.SMALLINT;
                        break;
                    case KingbaseOid.INT4:
                        valueType = Types.INTEGER;
                        break;
                    case KingbaseOid.INT8:
                        valueType = Types.BIGINT;
                        break;
                    case KingbaseOid.FLOAT4:
                        valueType = Types.FLOAT;
                        break;
                    case KingbaseOid.FLOAT8:
                        valueType = Types.DOUBLE;
                        break;
                    case KingbaseOid.NUMERIC:
                        valueType = Types.NUMERIC;
                        break;
                    case KingbaseOid.CHAR:
                        valueType = Types.CHAR;
                        break;
                    case KingbaseOid.VARCHAR:
                        valueType = Types.VARCHAR;
                        break;
                    case KingbaseOid.DATE:
                        valueType = Types.DATE;
                        break;
                    case KingbaseOid.TIME:
                    case KingbaseOid.TIMETZ:
                        valueType = Types.TIME;
                        break;
                    case KingbaseOid.TIMESTAMP:
                    case KingbaseOid.TIMESTAMPTZ:
                        valueType = Types.TIMESTAMP;
                        break;
                    case KingbaseOid.BYTEA:
                        valueType = Types.BINARY;
                        break;
                    case KingbaseOid.BPCHAR:
                        valueType = Types.CHAR;
                        break;
                    case KingbaseOid.XML:
                        valueType = Types.SQLXML;
                        break;
                    case KingbaseOid.NAME:
                        valueType = Types.VARCHAR;
                        break;
                    case KingbaseOid.OID:
                    case KingbaseOid.BOX:
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
                                case KingbaseOid.DATE:
                                    valueType = Types.DATE;
                                    break;
                                case KingbaseOid.TIME:
                                case KingbaseOid.TIMETZ:
                                    valueType = Types.TIME;
                                    break;
                                case KingbaseOid.TIMESTAMP:
                                case KingbaseOid.TIMESTAMPTZ:
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
                        valueType = Types.VARCHAR;
                        break;
                    case U:
                        switch (name) {
                            case "bytea":
                                valueType = Types.BINARY;
                                break;
                            case KingbaseConstants.TYPE_XML:
                                valueType = Types.SQLXML;
                                break;
                            case "int1":
                            case "uint1":
                            case "uint2":
                                valueType = Types.SMALLINT;
                                break;
                            case "uint4":
                                valueType = Types.INTEGER;
                                break;
                            case "uint8":
                                valueType = Types.BIGINT;
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

        return new KingbaseDataType(
            session,
            dataTypeSchema,
            typeId,
            valueType,
            name,
            typeLength,
            dbResult);
    }

    public static class EnumTypeValidator implements IPropertyValueValidator<KingbaseDataType, Object> {
        @Override
        public boolean isValidValue(KingbaseDataType object, Object value) throws IllegalArgumentException {
            return object.getTypeCategory() == KingbaseTypeCategory.E;
        }
    }

}
