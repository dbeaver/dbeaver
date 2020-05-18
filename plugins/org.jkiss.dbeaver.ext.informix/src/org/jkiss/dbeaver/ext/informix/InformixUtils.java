// Sequences, index source, trigger source, constraint syntax

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

package org.jkiss.dbeaver.ext.informix;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Informix utils
 */
public class InformixUtils {

    static final Log log = Log.getLog(InformixUtils.class);

    private static List<String> getSource(DBRProgressMonitor monitor,
                                          String sqlStatement, String dbObjectName,
                                          GenericDataSource datasource) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, datasource, "Load source code")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sqlStatement)) {
                List<String> result = new ArrayList<>();
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.nextRow()) {
                        result.add(dbResult.getString(1));
                    }
                }
                return result;
            }
        } catch (Exception e) {
            throw new DBException("Can't read source code of '" + dbObjectName + "'", e);
        }
    }

    private static String listToString(List<String> value, String delimiter) {
        StringBuilder sbResult = new StringBuilder();
        for (String o : value) {
            //NOT APPLY .TRIM IN 'O' VARIABLE, PROBLEM TO RENDERIZE PROCEDURE BECAUSE LINE DELIMITED CRLF and LF generate  'Sintax error'
            sbResult.append(o);
            if (delimiter != null && !delimiter.isEmpty())
                sbResult.append(delimiter);
        }
        return sbResult.toString();
    }

    public static String getProcedureSource(DBRProgressMonitor monitor, GenericProcedure procedure) throws DBException {
        String sqlProcedure = String.format("select b.data "
            + "from sysprocbody b "
            + "join sysprocedures p on b.procid=p.procid "
            + "where datakey='T' and p.procname = '%s'"
            + "order by b.procid, b.seqno", procedure.getName());
        return listToString(
            getSource(monitor, sqlProcedure, procedure.getName(),
                procedure.getDataSource()), null);
    }

    public static String getViewSource(DBRProgressMonitor monitor,
                                       GenericTableBase view) throws DBException {
        String sqlView = String.format("select v.viewtext "
            + "from informix.sysviews v "
            + "join systables s on s.tabid = v.tabid "
            + "where s.tabname = '%s'", view.getName());
        return listToString(
            getSource(monitor, sqlView, view.getName(),
                view.getDataSource()), null);
    }

    // Triggers, Sequences?
    public static String getTriggerDDL(DBRProgressMonitor monitor,
                                       GenericTableBase table) throws DBException {
        String sqlTrigger = String
            .format("select tb.data " + "from systables ta "
                    + "join systriggers tr on tr.tabid = ta.tabid "
                    + "join systrigbody tb on tb.trigid = tr.trigid "
                    + "where ta.tabname = '%s' and ta.tabtype='T' "
                    + "and tb.datakey IN ('A', 'D') "
                    + "order by tr.trigname, datakey desc, seqno ",
                table.getName());
        return listToString(
            getSource(monitor, sqlTrigger, table.getName(),
                table.getDataSource()), "\n");
        // systriggers.event:
        // D = Delete trigger, I = Insert, U = Update trigger,S = Select,
        // d = INSTEAD OF Delete, i = INSTEAD OF Insert,u = INSTEAD OF Update
    }

    public static String getTriggerDDL(DBRProgressMonitor monitor, GenericTrigger trigger) throws DBException {
        assert trigger.getTable() != null;
        String sqlTrigger = String
            .format("select tb.data from systables ta "
                    + "join systriggers tr on tr.tabid = ta.tabid "
                    + "join systrigbody tb on tb.trigid = tr.trigid "
                    + "where ta.tabname = '%s' and ta.tabtype='T' "
                    + "and tb.datakey IN ('A', 'D') "
                    + "and tr.trigname = '%s'"
                    + "order by tr.trigname, datakey desc, seqno ",
                trigger.getTable().getName(),
                trigger.getName());
        return listToString(
            getSource(monitor, sqlTrigger, trigger.getName(), trigger.getDataSource()), "\n");
    }

}
