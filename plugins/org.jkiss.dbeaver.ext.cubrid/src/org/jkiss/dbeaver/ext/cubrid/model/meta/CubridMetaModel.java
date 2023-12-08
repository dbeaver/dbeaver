/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.model.meta;

import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.CubridObjectContainer;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableBase;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableIndex;
import org.jkiss.dbeaver.ext.cubrid.model.CubridView;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

public class CubridMetaModel extends GenericMetaModel {
	private static final Log log = Log.getLog(CubridMetaModel.class);

	public CubridMetaModel()
	{
	}

	public boolean isSupportMultiSchema(JDBCSession session) {
		try {
			int major = session.getMetaData().getDatabaseMajorVersion();
			int minor = session.getMetaData().getDatabaseMinorVersion();
			if(major == 11) {
				if(minor >= 2) {
					return true;
				}
			}else if(major > 11) {
				return true;
			}
		} catch (SQLException e) {
			log.error("can't get database version");
		}
		return false;
	}

	@Override
	public JDBCStatement prepareTableColumnLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forTable) throws SQLException {
		if(isSupportMultiSchema(session)) {
			if(forTable instanceof CubridTable) {
				CubridTableBase tablebase = (CubridTableBase) forTable;
				return session.getMetaData().getColumns(null, null, tablebase!=null?tablebase.getUniqueName():null, null).getSourceStatement();
			}
		}
		return super.prepareTableColumnLoadStatement(session, owner, forTable);
	}

	@Override
	public JDBCStatement prepareUniqueConstraintsLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forTable) throws SQLException, DBException {
		if(isSupportMultiSchema(session)) {
			if(forTable instanceof CubridTable) {
				CubridTableBase tablebase = (CubridTableBase) forTable;
				return session.getMetaData().getPrimaryKeys(null, null, tablebase!=null?tablebase.getUniqueName():null).getSourceStatement();
			}
		}
		return super.prepareUniqueConstraintsLoadStatement(session, owner, forTable);
    }

	@Override
	public JDBCStatement prepareForeignKeysLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forTable) throws SQLException {
		if(isSupportMultiSchema(session)) {
			if(forTable instanceof CubridTable) {
				CubridTableBase tablebase = (CubridTableBase) forTable;
				return session.getMetaData().getImportedKeys(null, null, tablebase!=null?tablebase.getUniqueName():null).getSourceStatement();
			}
		}
		return super.prepareForeignKeysLoadStatement(session, owner, forTable);
	}

	public JDBCStatement prepareIndexLoadStatement(@NotNull JDBCSession session, CubridTableBase tableBase) throws SQLException {
		String tableName = isSupportMultiSchema(session)? tableBase.getUniqueName():tableBase.getName();
		return session.getMetaData().getIndexInfo(null, null, tableName, false, true).getSourceStatement();
	}

	public CubridTableBase createTableImpl(@NotNull JDBCSession session, @NotNull CubridObjectContainer owner, @NotNull GenericMetaObject tableObject, @NotNull JDBCResultSet dbResult) {

		String tableName = JDBCUtils.safeGetString( dbResult, CubridConstants.CLASS_NAME);
		String tableType = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_TYPE);

		CubridTableBase table = this.createTableImpl(owner, tableName, tableType, dbResult);
		if (table == null) {
			return null;
		}
		return table;
	}

	public CubridTableBase createTableImpl(CubridObjectContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
		if (tableType != null && isView(tableType)) {
			return new CubridView(container, tableName, tableType, dbResult);
		}
		return new CubridTable(container, tableName, tableType, dbResult);
	}

	public CubridTableIndex createIndexImpl(
		CubridTable table,
		boolean nonUnique,
		String qualifier,
		long cardinality,
		String indexName,
		DBSIndexType indexType,
		boolean persisted) {
		return new CubridTableIndex(
			table,
			nonUnique,
			qualifier,
			cardinality,
			indexName,
			indexType,
			persisted);
	}

}
