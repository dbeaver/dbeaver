/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.oceanbase.oracle.model;

import java.sql.ResultSet;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataType;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class OceanbaseOracleViewColumn extends OracleTableColumn {

	public OceanbaseOracleViewColumn(OracleTableBase table) {
		super(table);
	}

	public OceanbaseOracleViewColumn(DBRProgressMonitor monitor, OracleTableBase table, ResultSet dbResult)
			throws DBException {
		super(monitor, table, dbResult);
		// Read default value first because it is of LONG type and has to be read before
		// others
		setDefaultValue(JDBCUtils.safeGetString(dbResult, "DEFAULT"));

		setName(JDBCUtils.safeGetString(dbResult, "FIELD"));
		this.typeName = JDBCUtils.safeGetString(dbResult, "TYPE");
		setDataType(OracleDataType.resolveDataType(monitor, getDataSource(),
				JDBCUtils.safeGetString(dbResult, "DATA_TYPE_OWNER"), this.typeName));
		OracleDataType type = getDataType();
		if (type != null) {
			this.typeName = type.getFullyQualifiedName(DBPEvaluationContext.DDL);
			this.valueType = type.getTypeID();
		}
		String charUsed = JDBCUtils.safeGetString(dbResult, "CHAR_USED");
		setMaxLength(JDBCUtils.safeGetLong(dbResult, "C".equals(charUsed) ? "CHAR_LENGTH" : "DATA_LENGTH"));
		setRequired(!"YES".equals(JDBCUtils.safeGetString(dbResult, "NULL")));
		setComment(JDBCUtils.safeGetString(dbResult, "EXTRA"));
	}
}
