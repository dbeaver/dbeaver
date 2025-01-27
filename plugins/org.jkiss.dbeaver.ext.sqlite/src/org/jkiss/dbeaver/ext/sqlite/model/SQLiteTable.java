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
package org.jkiss.dbeaver.ext.sqlite.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.ext.sqlite.internal.SQLiteMessages;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLiteTable extends GenericTable implements DBDPseudoAttributeContainer,DBPNamedObject2 {

    private static final DBDPseudoAttribute PSEUDO_ATTR_ROWID = new DBDPseudoAttribute(
        DBDPseudoAttributeType.ROWID,
        "rowid",
        "$alias.rowid",
        null,
        SQLiteMessages.pseudo_column_rowid_description,
        true,
        DBDPseudoAttribute.PropagationPolicy.TABLE_LOCAL
    );

    private static final List<DBDPseudoAttribute> ALL_KNOWN_PSEUDO_ATTRS = Stream.of("rowid", "oid", "_rowid_")
        .map(name -> new DBDPseudoAttribute(
            DBDPseudoAttributeType.ROWID,
            name,
            null,
            null,
            SQLiteMessages.pseudo_column_rowid_description,
            true,
            DBDPseudoAttribute.PropagationPolicy.TABLE_LOCAL
        )).toList();

    private DBDPseudoAttribute[] allPseudoAttributes = null;

    private boolean hasStrictTyping;

    public SQLiteTable(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
        hasStrictTyping = dbResult != null && JDBCUtils.safeGetBoolean(dbResult, "STRICT"); //$NON-NLS-1$
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

    @Override
    public DBDPseudoAttribute[] getAllPseudoAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (this.allPseudoAttributes == null) {

            boolean isWithoutRowId = this.obtainIsWithoutRowId(monitor);
            if (isWithoutRowId) {
                this.allPseudoAttributes = DBDPseudoAttribute.EMPTY_ARRAY;
            } else {
                // see https://www.sqlite.org/lang_createtable.html#rowid (5. ROWIDs and the INTEGER PRIMARY KEY):
                //     If a table contains a user defined column named "rowid", "oid" or "_rowid_",
                //     then that name always refers the explicitly declared column and cannot be used to retrieve the integer rowid value.

                Set<String> columnNames = this.getAttributes(monitor).stream()
                    .map(a -> a.getName().toLowerCase())
                    .collect(Collectors.toSet());
                this.allPseudoAttributes = ALL_KNOWN_PSEUDO_ATTRS.stream()
                    .filter(a -> !columnNames.contains(a.getName())) // all names are lowercased here
                    .toArray(DBDPseudoAttribute[]::new);
            }
        }
        return this.allPseudoAttributes;
    }

    private boolean obtainIsWithoutRowId(@NotNull DBRProgressMonitor monitor) throws DBException {
        // https://www.sqlite.org/releaselog/3_8_2.html - Added support for WITHOUT ROWID tables.
        // https://www.sqlite.org/releaselog/3_30_0.html - The index_info and index_xinfo pragmas are enhanced
        //                      to provide information about the on-disk representation of WITHOUT ROWID tables.

        if (this.getDataSource().isServerVersionAtLeast(3, 30)) { // obtain metainfo in a normal way
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Obtaining table's extra metadata")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT EXISTS(SELECT 1 FROM pragma_index_info(?)) as isWithoutRowId")) {
                    dbStat.setString(1, this.getName());
                    try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                        return resultSet.next() && resultSet.getBoolean("isWithoutRowId");
                    }
                } catch (SQLException e) {
                    throw new DBException("Failed to obtain isWithoutRowId flag for table", e);
                }
            }
        } else if (this.getDataSource().isServerVersionAtLeast(3, 8)) { // try to execute query with all the possible rowid names
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Obtaining table's extra metadata")) {
                String tableName = this.getFullyQualifiedName(DBPEvaluationContext.DML);
                String sql = "SELECT EXISTS(SELECT rowid, oid, _rowid_ FROM " + tableName + ") as test";
                try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
                    try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                        if (resultSet.next()) {
                            resultSet.getBoolean("test");
                            // All known rowid names resolved, so two possible situations:
                            // 1. rowid presented --> isWithoutRowId is false
                            // 2. user-defined columns for all known rowid names presented --> pseudo-columns will be excluded anyway
                            return false;
                        } else { // should never happen
                            throw new DBException("Failed to obtain isWithoutRowId flag for table due to unexpected investigation result");
                        }
                    }
                } catch (SQLException e) {
                    String msg = e.getMessage();
                    if (msg.contains("rowid") || msg.contains("oid") || msg.contains("_rowid_")) { // "no such column: rowid"
                        return true;
                    } else {
                        throw new DBException("Failed to obtain isWithoutRowId flag for table", e);
                    }
                }
            }
        } else {
            return false;
        }
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

    @Property(viewable = true, editable = true, order = 40)
    public boolean isHasStrictTyping() {
        return hasStrictTyping;
    }

    public void setHasStrictTyping(boolean hasStrictTyping) {
        this.hasStrictTyping = hasStrictTyping;
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
