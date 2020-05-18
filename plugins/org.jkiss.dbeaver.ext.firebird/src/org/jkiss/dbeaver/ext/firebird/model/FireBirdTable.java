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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.*;

public class FireBirdTable extends GenericTable implements DBPNamedObject2 {

    private int keyLength;
    private String externalFile;
    private String ownerName;
    private Map<String, String> columnDomainTypes;

    public FireBirdTable(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        super(container, tableName, tableType, null);

        if (dbResult != null) {
            keyLength = JDBCUtils.safeGetInt(dbResult, "RDB$DBKEY_LENGTH");
            externalFile = JDBCUtils.safeGetStringTrimmed(dbResult, "RDB$EXTERNAL_FILE");
            ownerName = JDBCUtils.safeGetStringTrimmed(dbResult, "RDB$OWNER_NAME");
        }
    }

    @Property(viewable = true, order = 20)
    public String getOwnerName() {
        return ownerName;
    }

    @Property(viewable = true, order = 21)
    public int getKeyLength() {
        return keyLength;
    }

    @Property(viewable = true, order = 22)
    public String getExternalFile() {
        return externalFile;
    }

    @Override
    protected boolean isTruncateSupported() {
        return false;
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

    String getColumnDomainType(DBRProgressMonitor monitor, FireBirdTableColumn column) throws DBException {
        if (columnDomainTypes == null) {
            columnDomainTypes = readColumnDomainTypes(monitor);
        }
        return columnDomainTypes.get(column.getName());
    }

    private Map<String, String> readColumnDomainTypes(DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read column domain type")) {
            // Read metadata
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT RF.RDB$FIELD_NAME,RF.RDB$FIELD_SOURCE FROM RDB$RELATION_FIELDS RF WHERE RF.RDB$RELATION_NAME=?")) {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    Map<String, String> dtMap = new HashMap<>();
                    while (dbResult.next()) {
                        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, 1);
                        String domainTypeName = JDBCUtils.safeGetStringTrimmed(dbResult, 2);
                        if (!CommonUtils.isEmpty(columnName) && !CommonUtils.isEmpty(domainTypeName)) {
                            dtMap.put(columnName, domainTypeName);
                        }
                    }
                    return dtMap;
                }
            }

        } catch (SQLException ex) {
            throw new DBException("Error reading column domain types for " + getName(), ex);
        }

    }

}
