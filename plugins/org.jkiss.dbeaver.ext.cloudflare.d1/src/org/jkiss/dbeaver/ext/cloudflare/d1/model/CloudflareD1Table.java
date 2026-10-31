/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cloudflare.d1.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteTable;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteTableForeignKey;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CloudflareD1Table extends SQLiteTable {
    private final boolean withoutRowid;

    public CloudflareD1Table(
        @NotNull GenericStructContainer container, @NotNull String tableName, @Nullable String tableType, @Nullable JDBCResultSet resultSet
    ) {
        super(container, tableName, tableType, resultSet);
        this.withoutRowid = resultSet != null && JDBCUtils.safeGetInt(resultSet, "WITHOUT_ROWID") != 0;
    }

    @Nullable
    @Override
    public DBDPseudoAttribute[] getPseudoAttributes() throws DBException {
        return this.withoutRowid ? null : super.getPseudoAttributes();
    }

    @NotNull
    @Override
    public DBDPseudoAttribute[] getAllPseudoAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return this.withoutRowid ? DBDPseudoAttribute.EMPTY_ARRAY : super.getAllPseudoAttributes(monitor);
    }

    @NotNull
    @Override
    public Collection<SQLiteTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!isPersisted() || !getDataSource().getInfo().supportsReferentialIntegrity() || monitor.isForceCacheUsage()) {
            return List.of();
        }
        List<SQLiteTableForeignKey> references = new ArrayList<>();
        var tables = getContainer().getTables(monitor);
        if (tables != null) {
            for (var table : tables) {
                var associations = table.getAssociations(monitor);
                if (associations == null) {
                    continue;
                }
                for (var association : associations) {
                    if (association instanceof SQLiteTableForeignKey foreignKey && foreignKey.refersToTable(this)) {
                        references.add(foreignKey);
                    }
                }
            }
        }
        return references;
    }
}
