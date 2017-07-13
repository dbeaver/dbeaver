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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;

/**
 * PostgreGenericTypeCache
 */
@Deprecated
public class PostgreGenericTypeCache extends JDBCBasicDataTypeCache<GenericStructContainer, JDBCDataType>
{
    private static final Log log = Log.getLog(PostgreGenericTypeCache.class);

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

    public PostgreGenericTypeCache(GenericStructContainer owner) {
        super(owner);
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner) throws SQLException
    {
        return session.prepareStatement(
            "SELECT t.oid as typid,tn.nspname typnsname,t.* \n" +
            "FROM pg_catalog.pg_type t , pg_catalog.pg_namespace tn\n" +
            "WHERE tn.oid=t.typnamespace \n" +
            "AND t.typtype<>'c' AND t.typcategory not in ('A','P')\n" +
            "ORDER by t.oid");
    }

    @Override
    protected JDBCDataType fetchObject(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
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

        return new JDBCDataType<>(owner, valueType, name,null,false,true,typeLength,-1,-1);
    }


}
