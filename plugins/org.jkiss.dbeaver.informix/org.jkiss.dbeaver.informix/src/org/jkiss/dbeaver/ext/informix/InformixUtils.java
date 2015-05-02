// Sequences, index source, trigger source, constraint syntax

/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ext.informix;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Informix utils
 */
public class InformixUtils {

	static final Log log = Log.getLog(InformixUtils.class);

	private static String getSource(DBRProgressMonitor monitor,
			String sqlStatement, String dbObjectName,
			GenericDataSource datasource) throws DBException {
		JDBCSession session = datasource.openSession(monitor,
				DBCExecutionPurpose.META, "Load source code");
		try {
			JDBCPreparedStatement dbStat = session
					.prepareStatement(sqlStatement);
			try {
				JDBCResultSet dbResult = dbStat.executeQuery();
				try {
					StringBuilder sbResult = new StringBuilder();
					while (dbResult.nextRow()) {
						sbResult.append(dbResult.getString(1).trim());
					}
					return sbResult.toString();
				} finally {
					dbResult.close();
				}
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

	public static String getProcedureSource(DBRProgressMonitor monitor,
			GenericProcedure procedure) throws DBException {
		String sqlProcedure = String.format("select b.data "
				+ "from sysprocbody b "
				+ "join sysprocedures p on b.procid=p.procid "
				+ "where datakey='T' and p.procname = '%s'"
				+ "order by b.procid, b.seqno", procedure.getName());
		return getSource(monitor, sqlProcedure, procedure.getName(),
				procedure.getDataSource());
	}

	public static String getViewSource(DBRProgressMonitor monitor,
			GenericTable view) throws DBException {
		String sqlView = String.format("select v.viewtext "
				+ "from informix.sysviews v "
				+ "join systables s on s.tabid = v.tabid "
				+ "where s.tabname = '%s'", view.getName());
		return getSource(monitor, sqlView, view.getName(), view.getDataSource());
	}

}
