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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.*;
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
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * PostgreTypeType
 */
public class PostgreDataType extends JDBCDataType<PostgreSchema> implements PostgreClass, DBPQualifiedObject, DBPImageProvider
{
    private static final Log log = Log.getLog(PostgreDataType.class);

    private static final String CAT_MAIN = "Main";
    private static final String CAT_MISC = "Miscellaneous";
    private static final String CAT_MODIFIERS = "Modifiers";
    private static final String CAT_FUNCTIONS = "Functions";
    private static final String CAT_ARRAY = "Array";

    private static String[] OID_TYPES = new String[] {
        "regproc",
        "regprocedure",
        "regoper",
        "regoperator",
        "regclass",
        "regtype",
        "regconfig",
        "regdictionary",
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
    private int arrayDim;
    private long collationId;
    private String defaultValue;

    private final AttributeCache attributeCache;
    private Object[] enumValues;

    public PostgreDataType(@NotNull JDBCSession session, @NotNull PostgreSchema owner, long typeId, int valueType, String name, int length, JDBCResultSet dbResult) throws DBException {
        super(owner, valueType, name, null, false, true, length, -1, -1);
        this.alias = false;
        this.typeId = typeId;
        this.typeType = PostgreTypeType.b;
        String typTypeStr = JDBCUtils.safeGetString(dbResult, "typtype");
        try {
            if (typTypeStr != null && !typTypeStr.isEmpty()) {
                this.typeType = PostgreTypeType.valueOf(typTypeStr.toLowerCase(Locale.ENGLISH));
            }
        } catch (Exception e) {
            log.debug("Invalid type type [" + typTypeStr + "] - " + e.getMessage());
        }
        this.typeCategory = PostgreTypeCategory.X;
        String typCategoryStr = JDBCUtils.safeGetString(dbResult, "typcategory");
        try {
            if (typCategoryStr != null && !typCategoryStr.isEmpty()) {
                this.typeCategory = PostgreTypeCategory.valueOf(typCategoryStr.toUpperCase(Locale.ENGLISH));
            }
        } catch (Exception e) {
            log.debug("Invalid type category [" + typCategoryStr + "] - " + e.getMessage());
        }

        this.dataKind = JDBCDataSource.getDataKind(getName(), valueType);
        if (this.dataKind == DBPDataKind.OBJECT) {
            if (PostgreConstants.TYPE_JSONB.equals(name) || PostgreConstants.TYPE_JSON.equals(name)) {
                this.dataKind = DBPDataKind.CONTENT;
            }
        }

        this.ownerId = JDBCUtils.safeGetLong(dbResult, "typowner");
        this.isByValue = JDBCUtils.safeGetBoolean(dbResult, "typbyval");
        this.isPreferred = JDBCUtils.safeGetBoolean(dbResult, "typispreferred");
        this.arrayDelimiter = JDBCUtils.safeGetString(dbResult, "typdelim");
        this.classId = JDBCUtils.safeGetLong(dbResult, "typrelid");
        this.elementTypeId = JDBCUtils.safeGetLong(dbResult, "typelem");
        this.arrayItemTypeId = JDBCUtils.safeGetLong(dbResult, "typarray");
        this.inputFunc = JDBCUtils.safeGetString(dbResult, "typinput");
        this.outputFunc = JDBCUtils.safeGetString(dbResult, "typoutput");
        this.receiveFunc = JDBCUtils.safeGetString(dbResult, "typreceive");
        this.sendFunc = JDBCUtils.safeGetString(dbResult, "typsend");
        this.modInFunc = JDBCUtils.safeGetString(dbResult, "typmodin");
        this.modOutFunc = JDBCUtils.safeGetString(dbResult, "typmodout");
        this.analyzeFunc = JDBCUtils.safeGetString(dbResult, "typanalyze");
        String typAlignStr = JDBCUtils.safeGetString(dbResult, "typalign");
        try {
            this.align = PostgreTypeAlign.valueOf(typAlignStr);
        } catch (Exception e) {
            log.debug("Invalid type align [" + typAlignStr + "] - " + e.getMessage());
        }
        String typStorageStr = JDBCUtils.safeGetString(dbResult, "typstorage");
        try {
            this.storage = PostgreTypeStorage.valueOf(typStorageStr);
        } catch (Exception e) {
            log.debug("Invalid type storage [" + typStorageStr + "] - " + e.getMessage());
        }
        this.isNotNull = JDBCUtils.safeGetBoolean(dbResult, "typnotnull");
        this.baseTypeId = JDBCUtils.safeGetLong(dbResult, "typbasetype");
        this.typeMod = JDBCUtils.safeGetInt(dbResult, "typtypmod");
        this.arrayDim = JDBCUtils.safeGetInt(dbResult, "typndims");
        this.collationId = JDBCUtils.safeGetLong(dbResult, "typcollation");
        this.defaultValue = JDBCUtils.safeGetString(dbResult, "typdefault");

        this.attributeCache = hasAttributes() ? new AttributeCache() : null;

        if (typeCategory == PostgreTypeCategory.E) {
            readEnumValues(session);
        }
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

    public boolean isAlias() {
        return alias;
    }

    private void readEnumValues(JDBCSession session) throws DBException {
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT e.enumlabel \n" +
                "FROM pg_catalog.pg_enum e\n" +
                "WHERE e.enumtypid=?")) {
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
    public DBPDataKind getDataKind()
    {
        if (dataKind != null) {
            return dataKind;
        }
        return super.getDataKind();
    }

    @Nullable
    @Override
    public DBSDataType getComponentType(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getElementType();
    }

    @Nullable
    @Override
    public Object geTypeExtension() {
        return typeCategory;
    }

    @Override
    public long getObjectId() {
        return typeId;
    }

    @Property(category = CAT_MAIN, viewable = true, order = 10)
    public PostgreTypeType getTypeType() {
        return typeType;
    }

    @Property(category = CAT_MAIN, viewable = true, order = 11)
    public PostgreTypeCategory getTypeCategory() {
        return typeCategory;
    }

    @Property(category = CAT_MAIN, viewable = true, order = 12)
    public PostgreDataType getBaseType() {
        return resolveType(baseTypeId);
    }

    @Property(category = CAT_MAIN, viewable = true, order = 13)
    public PostgreDataType getElementType() {
        return elementTypeId == 0 ? null : resolveType(elementTypeId);
    }

    @Property(category = CAT_MAIN, order = 15)
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, getDatabase().roleCache, getDatabase(), ownerId);
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
    public long getCollationId() {
        return collationId;
    }

    @Property(category = CAT_ARRAY)
    public String getArrayDelimiter() {
        return arrayDelimiter;
    }

    @Property(category = CAT_ARRAY)
    public PostgreDataType getArrayItemType() {
        return arrayItemTypeId == 0 ? null : resolveType(arrayItemTypeId);
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

    private PostgreDataType resolveType(long typeId) {
        return getDatabase().getDataType(typeId);
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return DBSEntityType.TYPE;
    }

    @Override
    public Collection<PostgreDataTypeAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
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
            if (typeCategory == PostgreTypeCategory.S) {
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
            try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Refresh enum values")) {
                readEnumValues(session);
            }
        }
        return this;
    }

    public Object[] getEnumValues() {
        return enumValues;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        final PostgreSchema owner = getParentObject();
        if (owner == null) {
            return getName();
        } else {
            return DBUtils.getQuotedIdentifier(owner) + "." + getName();
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

    class AttributeCache extends JDBCObjectCache<PostgreDataType, PostgreDataTypeAttribute> {

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
            return new PostgreDataTypeAttribute(postgreDataType, resultSet);
        }
    }

    public static PostgreDataType readDataType(@NotNull JDBCSession session, @NotNull PostgreSchema schema, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        //long schemaId = JDBCUtils.safeGetLong(dbResult, "typnamespace");
        long typeId = JDBCUtils.safeGetLong(dbResult, "oid");
        String name = JDBCUtils.safeGetString(dbResult, "typname");
        if (CommonUtils.isEmpty(name)) {
            return null;
        }
        int typeLength = JDBCUtils.safeGetInt(dbResult, "typlen");
        PostgreTypeCategory typeCategory;
        final String catString = JDBCUtils.safeGetString(dbResult, "typcategory");
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
        if (ArrayUtils.contains(OID_TYPES, name) || name.equals("hstore")) {
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
                    case PostgreOid.CHAR_ARRAY:
                        valueType = Types.CHAR;
                        break;
                    case PostgreOid.BPCHAR:
                        valueType = Types.CHAR;
                        break;
                    case PostgreOid.XML:
                        valueType = Types.SQLXML;
                        break;
                    default:
                        if (typElem > 0) {
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
                        if (name.startsWith("timestamp")) {
                            valueType = Types.TIMESTAMP;
                        } else if (name.startsWith("date")) {
                            valueType = Types.DATE;
                        } else {
                            valueType = Types.TIME;
                        }
                        break;
                    case N:
                        valueType = Types.NUMERIC;
                        if (name.equals("numeric")) {
                            valueType = Types.NUMERIC;
                        } else if (name.startsWith("float")) {
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
