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
package org.jkiss.dbeaver.ext.sqlite.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;

import java.util.Collection;
import java.util.List;

public class SQLiteTable extends GenericTable implements DBDPseudoAttributeContainer,DBPNamedObject2 {

    private static final DBDPseudoAttribute PSEUDO_ATTR_ROWID = new DBDPseudoAttribute(
        DBDPseudoAttributeType.ROWID,
        "rowid",
        "$alias.rowid",
        null,
        "Unique row identifier",
        true);


    public SQLiteTable(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
    }

    @Override
    protected boolean isTruncateSupported() {
        return false;
    }

    // We use ROWID only if we don't have primary key. Looks like it is the only way to determine ROWID column presence.
    @Override
    public DBDPseudoAttribute[] getPseudoAttributes() throws DBException {
        if (hasPrimaryKey()) {
            return null;
        }
        return new DBDPseudoAttribute[] { PSEUDO_ATTR_ROWID };
    }

    private boolean hasPrimaryKey() throws DBException {
        List<GenericUniqueKey> constraints = getConstraints(new VoidProgressMonitor());
        if (constraints != null) {
            for (DBSTableConstraint cons : constraints) {
                if (cons.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public List<SQLiteTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return (List<SQLiteTableColumn>) super.getAttributes(monitor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<SQLiteTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return (Collection<SQLiteTableForeignKey>) super.getAssociations(monitor);
    }

    @Override
    public SQLiteTableForeignKey getAssociation(@NotNull DBRProgressMonitor monitor, String name) throws DBException {
        return (SQLiteTableForeignKey) super.getAssociation(monitor, name);
    }
}
