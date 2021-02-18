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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.firebird.FireBirdUtils;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;

import java.util.*;

public class FireBirdView extends GenericView implements FireBirdTableBase, DBSObjectWithScript, DBSView {

    private String ownerName;
    private Map<String, String> columnDomainTypes;

    public FireBirdView(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
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
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public synchronized List<FireBirdTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        Collection<? extends GenericTableColumn> childColumns = super.getAttributes(monitor);
        if (childColumns == null) {
            return Collections.emptyList();
        }
        List<FireBirdTableColumn> columns = new ArrayList<>();
        for (GenericTableColumn gtc : childColumns) {
            columns.add((FireBirdTableColumn) gtc);
        }
        columns.sort(DBUtils.orderComparator());
        return columns;
    }

    public String getColumnDomainType(DBRProgressMonitor monitor, FireBirdTableColumn column) throws DBException {
        if (columnDomainTypes == null) {
            columnDomainTypes = FireBirdUtils.readColumnDomainTypes(monitor, this);
        }
        return columnDomainTypes.get(column.getName());
    }

}
