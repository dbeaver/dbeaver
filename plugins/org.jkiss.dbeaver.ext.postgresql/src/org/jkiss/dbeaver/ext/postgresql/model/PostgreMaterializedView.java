/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;

/**
 * PostgreMaterializedView
 */
public class PostgreMaterializedView extends PostgreViewBase
{
    private boolean withData;
    private long tablespaceId;

    public PostgreMaterializedView(PostgreSchema catalog) {
        super(catalog);
    }

    public PostgreMaterializedView(
        PostgreSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);
        this.withData = JDBCUtils.safeGetBoolean(dbResult, "relispopulated");
        this.tablespaceId = JDBCUtils.safeGetLong(dbResult, "reltablespace");
    }

    public boolean isWithData() {
        return withData;
    }

    public void setWithData(boolean withData) {
        this.withData = withData;
    }

    @Override
    public Collection<PostgreIndex> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return getSchema().indexCache.getObjects(monitor, getSchema(), this);
    }

    @Override
    protected String readExtraDefinition(JDBCSession session, Map<String, Object> options) throws DBException {
        Collection<PostgreIndex> indexes = getIndexes(session.getProgressMonitor());
        if (!CommonUtils.isEmpty(indexes)) {
            StringBuilder indexDefs = new StringBuilder("\n-- View indexes:\n");
            for (PostgreIndex index : indexes) {
                String indexDefinition = index.getObjectDefinitionText(session.getProgressMonitor(), options);
                indexDefs.append(indexDefinition).append(";\n");
            }
            return indexDefs.toString();
        }
        return null;
    }

    public String getViewType() {
        return "MATERIALIZED VIEW";
    }

    @Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = PostgreTableBase.TablespaceListProvider.class)
    public PostgreTablespace getTablespace(DBRProgressMonitor monitor) throws DBException {
        if (tablespaceId == 0) {
            return getDatabase().getDefaultTablespace(monitor);
        }
        return PostgreUtils.getObjectById(monitor, getDatabase().tablespaceCache, getDatabase(), tablespaceId);
    }

    public void setTablespace(PostgreTablespace tablespace) {
        this.tablespaceId = tablespace.getObjectId();
    }


}
