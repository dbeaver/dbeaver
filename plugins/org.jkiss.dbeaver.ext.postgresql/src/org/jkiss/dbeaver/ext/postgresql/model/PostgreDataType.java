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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * PostgreTypeType
 */
public class PostgreDataType extends JDBCDataType implements PostgreObject
{
    static final Log log = Log.getLog(PostgreDataType.class);

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


    private final int typeId;
    private final String ownerSchema;
    private final PostgreTypeType typeType;
    private final PostgreTypeCategory typeCategory;

    public PostgreDataType(DBSObject owner, int valueType, String name, int length, int typeId, String ownerSchema, PostgreTypeType typeType, PostgreTypeCategory typeCategory) {
        super(owner, valueType, name, null, false, true, length, -1, -1);
        this.typeId = typeId;
        this.ownerSchema = ownerSchema;
        this.typeType = typeType;
        this.typeCategory = typeCategory;
    }

    @Override
    @Property
    public int getObjectId() {
        return typeId;
    }

    @Property
    public String getOwnerSchema() {
        return ownerSchema;
    }

    @Property
    public PostgreTypeType getTypeType() {
        return typeType;
    }

    @Property
    public PostgreTypeCategory getTypeCategory() {
        return typeCategory;
    }

    public static PostgreDataType readDataType(@NotNull DBSObject owner, @NotNull ResultSet dbResult) throws SQLException, DBException
    {
        int typeId = JDBCUtils.safeGetInt(dbResult, "oid");
        String ownerSchema = JDBCUtils.safeGetString(dbResult, "typnsname");
        String name = JDBCUtils.safeGetString(dbResult, "typname");
        if (CommonUtils.isEmpty(name)) {
            return null;
        }
        int typeLength = JDBCUtils.safeGetInt(dbResult, "typlen");
        PostgreTypeType typeType = PostgreTypeType.b;
        try {
            typeType = PostgreTypeType.valueOf(JDBCUtils.safeGetString(dbResult, "typtype"));
        } catch (IllegalArgumentException e) {
            log.debug(e);
        }
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
                case P:
                    return null;
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
            typeId,
            ownerSchema,
            typeType,
            typeCategory);
    }
}
