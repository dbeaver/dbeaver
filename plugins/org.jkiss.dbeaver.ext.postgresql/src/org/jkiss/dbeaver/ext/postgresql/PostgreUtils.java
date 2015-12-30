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

package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreAttribute;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * postgresql utils
 */
public class PostgreUtils {

    static final Log log = Log.getLog(PostgreUtils.class);

    public static String getObjectComment(DBRProgressMonitor monitor, DBPDataSource dataSource, String schema, String object)
        throws DBException
    {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Load PostgreSQL description")) {
            return JDBCUtils.queryString(
                session,
                "select description from pg_description\n" +
                "join pg_class on pg_description.objoid = pg_class.oid\n" +
                "join pg_namespace on pg_class.relnamespace = pg_namespace.oid\n" +
                "where pg_class.relname = ? and pg_namespace.nspname=?", object, schema);
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    public static String getDefaultDataTypeName(@NotNull DBPDataKind dataKind) {
        switch (dataKind) {
            case BOOLEAN: return "bool";
            case NUMERIC: return "int";
            case STRING: return "varchar";
            case DATETIME: return "timestamp";
            case BINARY: return "bytea";
            case CONTENT: return "bytea";
            case ROWID: return "oid";
            default: return "varchar";
        }
    }

    private static Method getValueMethod;

    public static PostgreAttribute getAttributeByNum(Collection<PostgreAttribute> attrs, int attNum) {
        for (PostgreAttribute attr : attrs) {
            if (attr.getOrdinalPosition() == attNum) {
                return attr;
            }
        }
        return null;
    }

    public static <T> T extractValue(Object pgObject) {
        if (pgObject == null) {
            return null;
        }
        if (getValueMethod == null) {
            try {
                getValueMethod = pgObject.getClass().getMethod("getValue");
            } catch (NoSuchMethodException e) {
                log.debug(e);
            }
        }
        if (getValueMethod != null) {
            try {
                return (T)getValueMethod.invoke(pgObject);
            } catch (Exception e) {
                log.debug(e);
            }
        }
        return null;
    }

}
