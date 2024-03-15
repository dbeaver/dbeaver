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
package org.jkiss.dbeaver.ext.altibase.data;

import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStringValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/*
 * Though NIBBLE data type is one of binary data  type, it's unable to handle binary values in hex editor.
 * So, this data type is handled like string data type. 
 */
public class AltibaseNibbleValueHandler extends JDBCStringValueHandler {

    public static final AltibaseNibbleValueHandler INSTANCE = new AltibaseNibbleValueHandler();

    @Override
    protected Object fetchColumnValue(
        DBCSession session,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index) throws SQLException {
        
        String value = resultSet.getString(index);
        
        if (CommonUtils.isNotEmpty(value)) {
            value = value.toUpperCase();
        }
        
        return value;
    }
    
    @Override
    public void bindParameter(
            JDBCSession session, 
            JDBCPreparedStatement statement, 
            DBSTypedObject paramType,
            int paramIndex, 
            Object value) throws SQLException {
        
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            try {
                statement.setObject(paramIndex, value.toString(), AltibaseConstants.TYPE_NIBBLE);
            } catch (SQLException e) {
                statement.setObject(paramIndex, value);
            }
        }
    }
}
