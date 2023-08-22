/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBConstants;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.time.ExtendedDateFormat;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.text.Format;
import java.text.SimpleDateFormat;

/**
 * Object type support
 */
public class YashanDBTimestampValueHandler extends JDBCDateTimeValueHandler {

    private static final SimpleDateFormat DEFAULT_DATETIME_FORMAT = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.ffffff");
    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSSSSS");

    public YashanDBTimestampValueHandler(DBDFormatSettings formatSettings) {
        super(formatSettings);
    }

    @Override
    public Object fetchValueObject(DBCSession session, DBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException {
        if (resultSet instanceof JDBCResultSet) {
            JDBCResultSet dbResults = (JDBCResultSet) resultSet;
            switch (type.getTypeID()) {
                case Types.TIME:
                    try {
                        Object object = dbResults.getObject(index + 1);
                        return getValueFromObject(session, type, object, false, false);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);//TODO
                    }
            }
        }
        return super.fetchValueObject(session, resultSet, type, index);
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy,
                                     boolean validateValue) throws DBCException {
        if (object != null) { //类型
            String className = object.getClass().getName();
            if (className.equalsIgnoreCase(YashanDBConstants.TIME_CLASS_NAME)) {
                try {
                    return getTimestampReadMethod(object.getClass(), ((JDBCSession) session).getOriginal(), object);
                } catch (Exception e) {
                    throw new DBCException("Error extracting YashanDB YasTIME value", e);
                }
            }
            switch (type.getTypeID()) { //字符串
                case Types.TIME:
                    try {
                        String path = session.getDataSource().getContainer().getDriver().getDriverLibraries().get(0).getPath();
                        Class<?> aClass = new URLClassLoader(new URL[]{new URL(path)}).loadClass(YashanDBConstants.TIME_CLASS_NAME);
                        return getTimestampReadMethod(aClass, ((JDBCSession) session).getOriginal(), object);
//                        return getTimestampReadMethod(Class.forName(YashanDBConstants.TIME_CLASS_NAME), ((JDBCSession) session).getOriginal(), object);
                    } catch (Exception e) {
//                        throw new RuntimeException(e);
                        //忽略粘贴板错误格式
                        log.debug("YashanDB Copy from clipboard error, inogre..."+ object.toString());
                    }
            }
        }
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (value != null && value.getClass().getName().equalsIgnoreCase(YashanDBConstants.TIME_CLASS_NAME)) {
            return value.toString();
        }else {
        	return super.getValueDisplayString(column, value, format);
		}
    }

    private static Object getTimestampReadMethod(Class<?> aClass, Connection connection, Object object) throws Exception {
        switch (aClass.getName()) {
            case YashanDBConstants.TIME_CLASS_NAME:
                return getNativeMethod(aClass, "valueOf", String.class)
                        .invoke(null, object.toString());
        }
        throw new DBException("Unsupported Yashandb TIME type: " + aClass.getName());
    }

    private static Method getNativeMethod(Class<?> aClass, String name, Class<?>... args) throws NoSuchMethodException {
        Method method = aClass.getMethod(name, args);
        method.setAccessible(true);
        return method;
    }

    @Nullable
    @Override
    public Format getNativeValueFormat(DBSTypedObject type) {
        switch (type.getTypeID()) {
            case Types.TIMESTAMP:
                return DEFAULT_DATETIME_FORMAT;
            case Types.TIME:
                return DEFAULT_TIME_FORMAT;
            case Types.DATE:
                return DEFAULT_DATE_FORMAT;
        }
        return super.getNativeValueFormat(type);
    }

    @Override
    public void bindValueObject(@NotNull DBCSession session, @NotNull DBCStatement statement, @NotNull DBSTypedObject type, int index,
                                @Nullable Object value) throws DBCException {
        try {
            JDBCPreparedStatement dbStat = (JDBCPreparedStatement) statement;
            if (value == null) {
                dbStat.setNull(index + 1, type.getTypeID());
                return;
            }
            if (value.getClass().getName().equals(YashanDBConstants.TIME_CLASS_NAME)) {
                //YS 的time如果使用setTime会丢失精度 TODO, 先用setString
                dbStat.setString(index + 1, value.toString());
            } else {
                super.bindValueObject(session, statement, type, index, value);
            }
        } catch (SQLException e) {
            throw new DBCException(ModelMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
        }
    }

    @NotNull
    protected String getFormatterId(DBSTypedObject column) {
        if (column.getFullTypeName().equalsIgnoreCase("TIME")) {
            return DBDDataFormatter.TYPE_NAME_TIME;
        }
        return super.getFormatterId(column);
    }

}
