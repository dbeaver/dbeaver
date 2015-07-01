// Sequences, index source, trigger source, constraint syntax

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

package org.jkiss.dbeaver.ext.informix;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
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
		JDBCSession session = datasource.getDefaultContext(true).openSession(monitor,
				DBCExecutionPurpose.META, "Load source code");
		List<String> result = new ArrayList<String>();
		try {
			JDBCPreparedStatement dbStat = session
					.prepareStatement(sqlStatement);
			try {
				JDBCResultSet dbResult = dbStat.executeQuery();
				while (dbResult.nextRow())
					result.add(dbResult.getString(1));
				return result;
			} finally {
				dbStat.close();
			}

		} catch (SQLException e) {
			throw new DBException("Can't read source code of '" + dbObjectName
					+ "'", e);
		} catch (Exception e) {
			log.debug(e);
			return null;
		}
	}

	private static String ListToString(List<String> value, String delimiter) {
		StringBuilder sbResult = new StringBuilder();
		for (String o : value) {
			sbResult.append(o.trim());
			if (delimiter != null && !delimiter.isEmpty())
			sbResult.append(delimiter);
		}
		return sbResult.toString();
	}

	public static String getProcedureSource(DBRProgressMonitor monitor,
			GenericProcedure procedure) throws DBException {
		String sqlProcedure = String.format("select b.data "
				+ "from sysprocbody b "
				+ "join sysprocedures p on b.procid=p.procid "
				+ "where datakey='T' and p.procname = '%s'"
				+ "order by b.procid, b.seqno", procedure.getName());
		return ListToString(
				getSource(monitor, sqlProcedure, procedure.getName(),
						procedure.getDataSource()), null);
	}

	public static String getViewSource(DBRProgressMonitor monitor,
			GenericTable view) throws DBException {
		String sqlView = String.format("select v.viewtext "
				+ "from informix.sysviews v "
				+ "join systables s on s.tabid = v.tabid "
				+ "where s.tabname = '%s'", view.getName());
		return ListToString(
				getSource(monitor, sqlView, view.getName(),
						view.getDataSource()), null);
	}

	// Triggers, Sequences?
	public static String getTriggerDDL(DBRProgressMonitor monitor,
			GenericTable table) throws DBException {
		String sqlTrigger = String
				.format("select tb.data " + "from systables ta "
						+ "join systriggers tr on tr.tabid = ta.tabid "
						+ "join systrigbody tb on tb.trigid = tr.trigid "
						+ "where ta.tabname = '%s' and ta.tabtype='T' "
						+ "and tb.datakey IN ('A', 'D') "
						+ "order by tr.trigname, datakey desc, seqno ",
						table.getName());
		return ListToString(
				getSource(monitor, sqlTrigger, table.getName(),
						table.getDataSource()), "\n");
		// systriggers.event:
		// D = Delete trigger, I = Insert, U = Update trigger,S = Select,
		// d = INSTEAD OF Delete, i = INSTEAD OF Insert,u = INSTEAD OF Update
	}

}
