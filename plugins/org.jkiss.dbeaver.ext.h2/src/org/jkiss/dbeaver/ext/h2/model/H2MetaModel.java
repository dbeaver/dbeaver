/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.h2.model;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * H2MetaModel
 */
public class H2MetaModel extends GenericMetaModel
{
    public H2MetaModel(IConfigurationElement cfg) {
        super(cfg);
    }

    @Override
    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read H2 view source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT VIEW_DEFINITION FROM INFORMATION_SCHEMA.VIEWS " +
                    "WHERE TABLE_SCHEMA=? AND TABLE_NAME=?"))
            {
                dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return dbResult.getString(1);
                    }
                    return "-- H2 view definition not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<GenericSequence> loadSequences(@NotNull DBRProgressMonitor monitor, @NotNull GenericStructContainer container) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container.getDataSource(), "Read sequences")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.SEQUENCES")) {
                List<GenericSequence> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, "SEQUENCE_NAME");
                        if (name == null) {
                            continue;
                        }
                        String description = JDBCUtils.safeGetString(dbResult, "REMARKS");
                        GenericSequence sequence = new GenericSequence(
                            container,
                            name,
                            description,
                            JDBCUtils.safeGetLong(dbResult, "CURRENT_VALUE"),
                            JDBCUtils.safeGetLong(dbResult, "MIN_VALUE"),
                            JDBCUtils.safeGetLong(dbResult, "MAX_VALUE"),
                            JDBCUtils.safeGetLong(dbResult, "INCREMENT")
                        );
                        result.add(sequence);
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

    @Override
    public String getAutoIncrementClause(GenericTableColumn column) {
        return "AUTO_INCREMENT";
/*
        if (!column.isPersisted()) {
            return "AUTO_INCREMENT";
        } else {
            // For existing columns auto-increment will in DEFAULT clause
            return null;
        }
*/
    }

}
