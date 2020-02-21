/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Struct;

/**
 * Dynamic struct. Self contained entity.
 */
public class JDBCCompositeDynamic extends JDBCComposite {

    private static final Log log = Log.getLog(JDBCCompositeDynamic.class);
        //public static final int MAX_ITEMS_IN_STRING = 100;


    public JDBCCompositeDynamic(@NotNull JDBCComposite struct, @NotNull DBRProgressMonitor monitor) throws DBCException {
        super(struct, monitor);
    }

    public JDBCCompositeDynamic(@NotNull DBCSession session, @Nullable Struct contents, @Nullable ResultSetMetaData metaData) throws DBCException
    {
        super(contents);

        this.type = new StructType(session.getDataSource());

        // Extract structure data
        try {
            Object[] attrValues = contents == null ? null : contents.getAttributes();
            if (attrValues != null) {
                attributes = new DBSEntityAttribute[attrValues.length];
                values = new Object[attrValues.length];
                if (metaData != null) {
                    // Use meta data to read struct information
                    int attrCount = metaData.getColumnCount();
                    if (attrCount != attrValues.length) {
                        log.warn("Meta column count (" + attrCount + ") differs from value count (" + attrValues.length + ")");
                        attrCount = Math.min(attrCount, attrValues.length);
                    }
                    for (int i = 0; i < attrCount; i++) {
                        Object value = attrValues[i];
                        StructAttribute attr = new StructAttribute(this.type, metaData, i);
                        value = DBUtils.findValueHandler(session, attr).getValueFromObject(session, attr, value, false, modified);
                        attributes[i] = attr;
                        values[i] = value;
                    }
                } else {
                    log.warn("Data type '" + contents.getSQLTypeName() + "' isn't resolved as structured type. Use synthetic attributes.");
                    for (int i = 0, attrValuesLength = attrValues.length; i < attrValuesLength; i++) {
                        Object value = attrValues[i];
                        StructAttribute attr = new StructAttribute(this.type, i, value);
                        value = DBUtils.findValueHandler(session, attr).getValueFromObject(session, attr, value, false, modified);
                        attributes[i] = attr;
                        values[i] = value;
                    }
                }
            } else {
                this.attributes = EMPTY_ATTRIBUTE;
                this.values = EMPTY_VALUES;
            }
        } catch (DBException e) {
            throw new DBCException("Can't obtain attributes meta information", e);
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Override
    public DBSDataType getDataType()
    {
        return type;
    }

    @Override
    public JDBCCompositeDynamic cloneValue(DBRProgressMonitor monitor) throws DBCException
    {
        return new JDBCCompositeDynamic(this, monitor);
    }

}
