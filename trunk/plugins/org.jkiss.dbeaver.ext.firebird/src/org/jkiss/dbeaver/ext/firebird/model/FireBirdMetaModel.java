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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.firebird.FireBirdUtils;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FireBirdDataSource
 */
public class FireBirdMetaModel extends GenericMetaModel
{
    static final Log log = Log.getLog(FireBirdMetaModel.class);

    private Pattern ERROR_POSITION_PATTERN = Pattern.compile(" line ([0-9]+), column ([0-9]+)");

    public FireBirdMetaModel(IConfigurationElement cfg) {
        super(cfg);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
        return FireBirdUtils.getViewSource(monitor, sourceObject);
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        return FireBirdUtils.getProcedureSource(monitor, sourceObject);
    }

    @Override
    public boolean supportsSequences(GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<GenericSequence> loadSequences(DBRProgressMonitor monitor, GenericObjectContainer container) throws DBException {
        JDBCSession session = container.getDataSource().getDefaultContext(true).openSession(monitor, DBCExecutionPurpose.META, "Read procedure definition");
        try {
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT RDB$GENERATOR_NAME,RDB$DESCRIPTION FROM RDB$GENERATORS");
            try {
                List<GenericSequence> result = new ArrayList<GenericSequence>();

                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        if (name == null) {
                            continue;
                        }
                        name = name.trim();
                        String description = JDBCUtils.safeGetString(dbResult, 2);
                        GenericSequence sequence = new GenericSequence(
                            container,
                            name,
                            description,
                            -1,
                            0,
                            -1,
                            1
                        );
                        result.add(sequence);
                    }
                } finally {
                    dbResult.close();
                }

                // Obtain sequence values
                for (GenericSequence sequence : result) {
                    JDBCPreparedStatement dbSeqStat = session.prepareStatement("SELECT GEN_ID(" + sequence.getName() + ", 0) from RDB$DATABASE");
                    try {
                        JDBCResultSet seqResults = dbSeqStat.executeQuery();
                        try {
                            seqResults.next();
                            sequence.setLastValue(JDBCUtils.safeGetLong(seqResults, 1));
                        } finally {
                            seqResults.close();
                        }
                    } finally {
                        dbSeqStat.close();
                    }
                }

                return result;

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
    public DBPErrorAssistant.ErrorPosition getErrorPosition(Throwable error) {
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            if (matcher.find()) {
                DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                pos.line = Integer.parseInt(matcher.group(1)) - 1;
                pos.position = Integer.parseInt(matcher.group(2)) - 1;
                return pos;
            }
        }
        return null;
    }
}
