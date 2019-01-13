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
package org.jkiss.dbeaver.ext.postgresql.model.impls.redshift;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * RedshiftTableColumn base
 */
public class RedshiftTableColumn extends PostgreTableColumn
{

    private String columnEncoding;
    private boolean distKey;
    private int sortKey;

    public RedshiftTableColumn(RedshiftTable table) {
        super(table);
    }

    public RedshiftTableColumn(DBRProgressMonitor monitor, RedshiftTable table, JDBCResultSet dbResult) throws DBException {
        super(monitor, table, dbResult);

        columnEncoding = JDBCUtils.safeGetString(dbResult, "encoding");
        distKey = JDBCUtils.safeGetBoolean(dbResult, "attisdistkey");
        sortKey = JDBCUtils.safeGetInt(dbResult, "attsortkeyord");
    }

    @Property(viewable = true, order = 21)
    public String getColumnEncoding() {
        return columnEncoding;
    }

    @Property(viewable = false, order = 22)
    public boolean isDistKey() {
        return distKey;
    }

    @Property(viewable = false, order = 23)
    public int getSortKey() {
        return sortKey;
    }
}
