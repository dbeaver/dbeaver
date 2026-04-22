/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.jdbc.data.handlers;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDComposite;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandlerComposite;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructImpl;
import org.jkiss.dbeaver.model.impl.jdbc.data.*;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.Map;

/**
 * JDBC Struct value handler.
 * Handle STRUCT types.
 *
 * @author Serge Rider
 */
public class JDBCStructValueHandler extends JDBCComplexValueHandler implements DBDValueHandlerComposite {

    private static final Log log = Log.getLog(JDBCStructValueHandler.class);

    public static final JDBCStructValueHandler INSTANCE = new JDBCStructValueHandler();

    /**
     * NumberFormat is not thread safe thus this method is synchronized.
     */
    @NotNull
    @Override
    public synchronized String getValueDisplayString(
        @NotNull DBSTypedObject column,
        @Nullable Object value,
        @NotNull DBDDisplayFormat format
    ) {
        if (value instanceof JDBCComposite composite) {
            if (DBUtils.isNullValue(value)) {
                return DBValueFormatting.getDefaultValueDisplayString(value, format);
            }
            return composite.getStringRepresentation();
        } else {
            return DBValueFormatting.getDefaultValueDisplayString(value, format);
        }
    }

    @NotNull
    @Override
    public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return JDBCComposite.class;
    }

    @Override
    protected void bindParameter(
        @NotNull JDBCSession session,
        @NotNull JDBCPreparedStatement statement,
        @NotNull DBSTypedObject paramType,
        int paramIndex,
        @Nullable Object value
    ) throws DBCException, SQLException {
        if (value == null) {
            statement.setNull(paramIndex, Types.STRUCT);
        } else if (value instanceof DBDComposite struct) {
            if (struct.isNull()) {
                statement.setNull(paramIndex, Types.STRUCT);
            } else if (struct instanceof JDBCComposite composite) {
                statement.setObject(paramIndex, composite.getStructValue(), Types.STRUCT);
            } else {
                statement.setObject(paramIndex, struct.getRawValue());
            }
        } else {
            throw new DBCException("Struct parameter type '" + value.getClass().getName() + "' not supported");
        }
    }

    @Override
    public Object getValueFromObject(
        @NotNull DBCSession session,
        @NotNull DBSTypedObject type,
        @Nullable Object object,
        boolean copy,
        boolean validateValue
    ) throws DBCException {
        if (object instanceof JDBCComposite composite) {
            return copy ? composite.cloneValue(session.getProgressMonitor()) : object;
        }

        String typeName = null;
        try {
            if (object instanceof Struct struct) {
                typeName = struct.getSQLTypeName();
            } else {
                typeName = type.getTypeName();
            }
        } catch (Exception e) {
            log.debug("Error reading SQL type name", e);
        }
        DBSDataType dataType = null;
        if (typeName != null) {
            try {
                dataType = DBUtils.resolveDataType(session.getProgressMonitor(), session.getDataSource(), typeName);
            } catch (DBException e) {
                log.debug("Error resolving data type '" + typeName + "'", e);
            }
        }
        if (dataType == null) {
            if (object instanceof Struct struct) {
                return new JDBCCompositeDynamic(session, struct, null);
            } else {
                return new JDBCCompositeUnknown(session, object);
            }
        }
        return switch (object) {
            case null -> new JDBCCompositeStatic(
                session,
                dataType,
                new JDBCStructImpl(dataType.getTypeName(), null, "")
            );
            case Struct struct -> new JDBCCompositeStatic(session, dataType, struct);
            case Map<?, ?> map -> new JDBCCompositeMap(session, dataType, map);
            default -> new JDBCCompositeUnknown(session, object);
        };
    }

}