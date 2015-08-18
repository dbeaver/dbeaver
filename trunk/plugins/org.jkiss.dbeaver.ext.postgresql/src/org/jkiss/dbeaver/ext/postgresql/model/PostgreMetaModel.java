/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.plan.PostgreQueryPlaner;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformProvider;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreMetaModel
 */
public class PostgreMetaModel extends GenericMetaModel implements DBCQueryTransformProvider
{
    private Pattern ERROR_POSITION_PATTERN = Pattern.compile("\\n\\s*Position: ([0-9]+)");

    public PostgreMetaModel(IConfigurationElement cfg) {
        super(cfg);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
        JDBCSession session = sourceObject.getDataSource().getDefaultContext(true).openSession(monitor, DBCExecutionPurpose.META, "Read view definition");
        try {
            return JDBCUtils.queryString(session, "SELECT definition FROM PG_CATALOG.PG_VIEWS WHERE SchemaName=? and ViewName=?", sourceObject.getContainer().getName(), sourceObject.getName());
        } catch (SQLException e) {
            throw new DBException(e, sourceObject.getDataSource());
        } finally {
            session.close();
        }
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        JDBCSession session = sourceObject.getDataSource().getDefaultContext(true).openSession(monitor, DBCExecutionPurpose.META, "Read procedure definition");
        try {
            return JDBCUtils.queryString(session, "SELECT pg_get_functiondef(p.oid) FROM PG_CATALOG.PG_PROC P, PG_CATALOG.PG_NAMESPACE NS\n" +
                "WHERE ns.oid=p.pronamespace and ns.nspname=? AND p.proname=?", sourceObject.getContainer().getName(), sourceObject.getName());
        } catch (SQLException e) {
            throw new DBException(e, sourceObject.getDataSource());
        } finally {
            session.close();
        }
    }

    @Override
    public boolean supportsSequences(GenericDataSource dataSource) {
        Version databaseVersion = dataSource.getInfo().getDatabaseVersion();
        return databaseVersion.getMajor() >= 9 || databaseVersion.getMajor() == 8 && databaseVersion.getMinor() >= 4;
    }

    @Override
    public List<GenericSequence> loadSequences(DBRProgressMonitor monitor, GenericObjectContainer container) throws DBException {
        JDBCSession session = container.getDataSource().getDefaultContext(true).openSession(monitor, DBCExecutionPurpose.META, "Read procedure definition");
        try {
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema=?");
            try {
                dbStat.setString(1, container.getName());
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    List<GenericSequence> result = new ArrayList<GenericSequence>();
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        JDBCPreparedStatement dbSeqStat = session.prepareStatement("SELECT last_value,min_value,max_value,increment_by from " + container.getName() + "." + name);
                        try {
                            JDBCResultSet seqResults = dbSeqStat.executeQuery();
                            try {
                                seqResults.next();
                                GenericSequence sequence = new GenericSequence(
                                    container,
                                    name,
                                    PostgreUtils.getObjectComment(monitor, container.getDataSource(), container.getName(), name),
                                    JDBCUtils.safeGetLong(seqResults, 1),
                                    JDBCUtils.safeGetLong(seqResults, 2),
                                    JDBCUtils.safeGetLong(seqResults, 3),
                                    JDBCUtils.safeGetLong(seqResults, 4));
                                result.add(sequence);
                            } finally {
                                seqResults.close();
                            }
                        } finally {
                            dbSeqStat.close();
                        }
                    }
                    return result;
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        } finally {
            session.close();
        }
    }

    @Override
    public DBCQueryPlanner getQueryPlanner(GenericDataSource dataSource) {
        return new PostgreQueryPlaner(dataSource);
    }

    @Override
    public DBPErrorAssistant.ErrorPosition getErrorPosition(Throwable error) {
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            if (matcher.find()) {
                DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                pos.position = Integer.parseInt(matcher.group(1)) - 1;
                return pos;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit(false);
        }
        return null;
    }

}
