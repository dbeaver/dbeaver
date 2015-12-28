/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;

/**
 * PostgreTypeType
 */
public class PostgreDataType extends JDBCDataType implements PostgreObject
{
    static final Log log = Log.getLog(PostgreDataType.class);

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

    private int typeId;
    private PostgreTypeType typeType;
    private PostgreTypeCategory typeCategory;

    private final int ownerId;
    private boolean isByValue;
    private boolean isPreferred;
    private String arrayDelimiter;
    private int classId;
    private int elementTypeId;
    private int arrayItemTypeId;
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
    private int baseTypeId;
    private int typeMod;
    private int arrayDim;
    private int collationId;
    private String defaultValue;

    public PostgreDataType(DBSObject owner, int valueType, String name, int length, JDBCResultSet dbResult) {
        super(owner, valueType, name, null, false, true, length, -1, -1);

        this.typeId = JDBCUtils.safeGetInt(dbResult, "oid");
        this.typeType = PostgreTypeType.b;
        try {
            this.typeType = PostgreTypeType.valueOf(JDBCUtils.safeGetString(dbResult, "typtype"));
        } catch (IllegalArgumentException e) {
            log.debug(e);
        }
        this.typeCategory = PostgreTypeCategory.X;
        try {
            this.typeCategory = PostgreTypeCategory.valueOf(JDBCUtils.safeGetString(dbResult, "typcategory"));
        } catch (IllegalArgumentException e) {
            log.debug(e);
        }

        this.ownerId = JDBCUtils.safeGetInt(dbResult, "typowner");
        this.isByValue = JDBCUtils.safeGetBoolean(dbResult, "typbyval");
        this.isPreferred = JDBCUtils.safeGetBoolean(dbResult, "typispreferred");
        this.arrayDelimiter = JDBCUtils.safeGetString(dbResult, "typdelim");
        this.classId = JDBCUtils.safeGetInt(dbResult, "typrelid");
        this.elementTypeId = JDBCUtils.safeGetInt(dbResult, "typelem");
        this.arrayItemTypeId = JDBCUtils.safeGetInt(dbResult, "typarray");
        this.inputFunc = JDBCUtils.safeGetString(dbResult, "typinput");
        this.outputFunc = JDBCUtils.safeGetString(dbResult, "typoutput");
        this.receiveFunc = JDBCUtils.safeGetString(dbResult, "typreceive");
        this.sendFunc = JDBCUtils.safeGetString(dbResult, "typsend");
        this.modInFunc = JDBCUtils.safeGetString(dbResult, "typmodin");
        this.modOutFunc = JDBCUtils.safeGetString(dbResult, "typmodout");
        this.analyzeFunc = JDBCUtils.safeGetString(dbResult, "typanalyze");
        this.align = PostgreTypeAlign.valueOf(JDBCUtils.safeGetString(dbResult, "typalign"));
        this.storage = PostgreTypeStorage.valueOf(JDBCUtils.safeGetString(dbResult, "typstorage"));
        this.isNotNull = JDBCUtils.safeGetBoolean(dbResult, "typnotnull");
        this.baseTypeId = JDBCUtils.safeGetInt(dbResult, "typbasetype");
        this.typeMod = JDBCUtils.safeGetInt(dbResult, "typtypmod");
        this.arrayDim = JDBCUtils.safeGetInt(dbResult, "typndims");
        this.collationId = JDBCUtils.safeGetInt(dbResult, "typcollation");
        this.defaultValue = JDBCUtils.safeGetString(dbResult, "typdefault");
    }

    @Override
    @Property(category = CAT_MAIN, viewable = true)
    public int getObjectId() {
        return typeId;
    }

    @Property(category = CAT_MAIN, viewable = true)
    public PostgreTypeType getTypeType() {
        return typeType;
    }

    @Property(category = CAT_MAIN, viewable = true)
    public PostgreTypeCategory getTypeCategory() {
        return typeCategory;
    }

    @Property(category = CAT_MAIN, viewable = true)
    public int getBaseTypeId() {
        return baseTypeId;
    }

    @Property(category = CAT_MAIN, viewable = true)
    public int getClassId() {
        return classId;
    }

    @Property(category = CAT_MAIN, viewable = true)
    public int getElementTypeId() {
        return elementTypeId;
    }

    @Property(category = CAT_MISC)
    public int getOwnerId() {
        return ownerId;
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
    public int getCollationId() {
        return collationId;
    }

    @Property(category = CAT_ARRAY)
    public String getArrayDelimiter() {
        return arrayDelimiter;
    }

    @Property(category = CAT_ARRAY)
    public int getArrayItemTypeId() {
        return arrayItemTypeId;
    }

    @Property(category = CAT_ARRAY)
    public int getArrayDim() {
        return arrayDim;
    }

    public static PostgreDataType readDataType(@NotNull DBSObject owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        String name = JDBCUtils.safeGetString(dbResult, "typname");
        if (CommonUtils.isEmpty(name)) {
            return null;
        }
        int typeLength = JDBCUtils.safeGetInt(dbResult, "typlen");
        PostgreTypeCategory typeCategory = PostgreTypeCategory.X;
        try {
            typeCategory = PostgreTypeCategory.valueOf(JDBCUtils.safeGetString(dbResult, "typcategory"));
        } catch (IllegalArgumentException e) {
            log.debug(e);
        }
        int valueType;
        if (ArrayUtils.contains(OID_TYPES, name) || name.equals("hstore")) {
            valueType = Types.VARCHAR;
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
                    if (name.startsWith("float")) {
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
                default:
                    valueType = Types.OTHER;
                    break;
            }
        }

        return new PostgreDataType(
            owner,
            valueType,
            name,
            typeLength,
            dbResult);
    }
}
