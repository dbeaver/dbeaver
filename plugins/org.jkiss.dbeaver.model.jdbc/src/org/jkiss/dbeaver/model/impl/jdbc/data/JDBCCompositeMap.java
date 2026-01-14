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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * Dynamic struct. Self contained entity.
 */
public class JDBCCompositeMap extends JDBCComposite {

    private DBSDataType dataType;
    private Map<?,?> map;
    public JDBCCompositeMap(@NotNull DBCSession session, DBSDataType dataType, @NotNull Map<?,?> contents) throws DBCException
    {
        super(null);
        this.dataType = dataType;
        this.map = contents;
        this.type = new StructType(session.getDataSource());

        // Extract structure data
        try {
            attributes = new DBSEntityAttribute[contents.size()];
            values = contents.values().toArray();
            int index = 0;
            for (Map.Entry<?,?> entry : contents.entrySet()) {
                Object value = entry.getValue();
                StructAttribute attr = new StructAttribute(CommonUtils.toString(entry.getKey()), this.type, index, value);
                value = DBUtils.findValueHandler(session, attr).getValueFromObject(session, attr, value, false, modified);
                attributes[index] = attr;
                values[index] = value;
                index++;
            }
        } catch (DBException e) {
            throw new DBCException("Can't obtain attributes meta information", e);
        }
    }

    public JDBCCompositeMap(@NotNull DBRProgressMonitor monitor, @NotNull JDBCCompositeMap struct) throws DBCException {
        super(monitor, struct);
        this.dataType = struct.dataType;
        this.map = struct.map;
    }

    @Override
    public JDBCCompositeMap cloneValue(DBRProgressMonitor monitor) throws DBCException
    {
        return new JDBCCompositeMap(monitor, this);
    }

}
