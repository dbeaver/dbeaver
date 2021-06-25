/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.util.Date;

/**
 * VerticaView
 */
public class VerticaView extends GenericView implements DBPSystemObject {

    private Date createTime;
    private boolean isTempTable;
    private boolean isSystemTable;

    public VerticaView(VerticaSchema container, String tableName, String tableType, JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
        if (dbResult != null) {
            this.createTime = JDBCUtils.safeGetDate(dbResult, "create_time");
            this.isTempTable = JDBCUtils.safeGetBoolean(dbResult, "is_temp_table");
            this.isSystemTable = JDBCUtils.safeGetBoolean(dbResult, "is_system_table");
        }
    }

    @Property(viewable = true, order = 3)
    public Date getCreateTime() {
        return createTime;
    }

    @Property(viewable = true, order = 4)
    public boolean isTempTable() {
        return isTempTable;
    }

    public boolean isSystem() {
        return isSystemTable;
    }
}
