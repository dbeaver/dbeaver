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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;

public class AltibaseView extends GenericView implements AltibaseTableBase, DBSObjectWithScript, DBSView {

    public AltibaseView(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
    }

    /*
    private String ownerName;
    private Map<String, String> columnDomainTypes;

    public AltibaseView(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);

        if (dbResult != null) {
            ownerName = JDBCUtils.safeGetStringTrimmed(dbResult, "RDB$OWNER_NAME");
        }
    }

    @Override
    public boolean isView() {
        return true;
    }

    @Property(viewable = true, order = 20)
    public String getSchemaName() {
        return ownerName;
    }

    @Override
    public synchronized List<AltibaseTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        Collection<? extends GenericTableColumn> childColumns = super.getAttributes(monitor);
        if (childColumns == null) {
            return Collections.emptyList();
        }
        List<AltibaseTableColumn> columns = new ArrayList<>();
        for (GenericTableColumn gtc : childColumns) {
            columns.add((AltibaseTableColumn) gtc);
        }
        columns.sort(DBUtils.orderComparator());
        return columns;
    }

    public String getColumnDomainType(DBRProgressMonitor monitor, AltibaseTableColumn column) throws DBException {
        if (columnDomainTypes == null) {
            columnDomainTypes = AltibaseUtils.readColumnDomainTypes(monitor, this);
        }
        return columnDomainTypes.get(column.getName());
    }
     */
}
