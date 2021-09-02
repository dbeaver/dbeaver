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
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableTrigger;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;

public class OceanbaseOracleTable extends OracleTable {

	public OceanbaseOracleTable(OracleSchema schema, String name) {
		super(schema, name);
	}

	public OceanbaseOracleTable(DBRProgressMonitor monitor, OracleSchema schema, ResultSet dbResult) {
		super(monitor, schema, dbResult);
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		DBPDataSource dataSource = getDataSource();
		DBPNamedObject[] path = new DBPNamedObject[] { getContainer(), this };
		StringBuilder name = new StringBuilder(20 * path.length);
		if (dataSource == null) {
			// It is not SQL identifier, let's just make it simple then
			for (DBPNamedObject namePart : path) {
				if (DBUtils.isVirtualObject(namePart)) {
					continue;
				}
				if (name.length() > 0) {
					name.append('.');
				}
				name.append(namePart.getName());
			}
		} else {
			final SQLDialect sqlDialect = dataSource.getSQLDialect();

			DBPNamedObject parent = null;
			for (DBPNamedObject namePart : path) {
				if (namePart == null || DBUtils.isVirtualObject(namePart)) {
					continue;
				}
				if (namePart instanceof DBSCatalog && ((sqlDialect.getCatalogUsage() & SQLDialect.USAGE_DML) == 0)) {
					continue;
				}
				if (!DBUtils.isValidObjectName(namePart.getName())) {
					continue;
				}
				if (name.length() > 0) {
					if (parent instanceof DBSCatalog) {
						name.append(sqlDialect.getCatalogSeparator());
					} else {
						name.append(sqlDialect.getStructSeparator());
					}
				}
				name.append(DBUtils.getQuotedIdentifier(dataSource, namePart.getName()));
				parent = namePart;
			}
		}
		return name.toString();
	}

	@Nullable
	@Association
	@Override
	public List<OracleTableTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getSchema().oceanbaseTableTriggerCache.getObjects(monitor, getSchema(), this);
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		getSchema().refreshObject(monitor);

		return getSchema().oceanbaseTableCache.refreshObject(monitor, getSchema(), this);
	}

	@Override
	@NotNull
	public OceanbaseOracleSchema getSchema() {
		return (OceanbaseOracleSchema) super.getContainer();
	}

	public static OracleTableBase findTable(DBRProgressMonitor monitor, OceanbaseOracleDataSource dataSource,
			String ownerName, String tableName) throws DBException {
		OracleSchema refSchema = dataSource.getSchema(monitor, ownerName);
		if (refSchema == null) {
			return null;
		} else {
			OracleTableBase refTable = refSchema.tableCache.getObject(monitor, refSchema, tableName);
			return refTable;
		}
	}

}
