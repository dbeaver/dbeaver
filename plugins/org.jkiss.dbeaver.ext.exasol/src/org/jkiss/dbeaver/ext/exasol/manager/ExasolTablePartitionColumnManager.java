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
package org.jkiss.dbeaver.ext.exasol.manager;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTablePartitionColumn;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolTablePartitionColumnCache;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;
import java.util.Map;

public class ExasolTablePartitionColumnManager extends SQLObjectEditor<ExasolTablePartitionColumn, ExasolTable> implements DBEObjectEditor<ExasolTablePartitionColumn>, DBEObjectMaker<ExasolTablePartitionColumn, ExasolTable>  {

    private static final Log LOG = Log.getLog(ExasolTablePartitionColumnManager.class);

	@Override
	public ExasolTablePartitionColumnCache getObjectsCache(
			ExasolTablePartitionColumn object) {
		return object.getTable().getPartitionCache();
	}

	@Override
	public boolean canCreateObject(@NotNull Object container) {
		return false;
	}

	@Override
	public long getMakerOptions(@NotNull DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}
	
	@Override
	protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList,
										  @NotNull ObjectChangeCommand command,
										  @NotNull Map<String, Object> options) throws DBException {
		ExasolTable table = command.getObject().getTable();
		try {
			actionList.add(new SQLDatabasePersistAction(generateAction(monitor, table)));
		} catch (DBException e) {
			LOG.error("Failed to create Partition Action", e);
		}
	}

	@Override
	protected ExasolTablePartitionColumn createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context,
															  Object container, Object copyFrom, @NotNull Map<String, Object> options) throws DBException {
		return new ExasolTablePartitionColumn((ExasolTable) container);
	}

	@Override
	protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions,
										  @NotNull ObjectCreateCommand command,
										  @NotNull Map<String, Object> options) {
		ExasolTable table = command.getObject().getTable();
		try {
			actions.add(new SQLDatabasePersistAction(generateAction(monitor, table)));
		} catch (DBException e) {
			LOG.error("Failed to create Partition Action", e);
		}
	}

	@Override
	protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions,
										  @NotNull ObjectDeleteCommand command,
										  @NotNull Map<String, Object> options) {
		ExasolTablePartitionColumn col = command.getObject();
		ExasolTablePartitionColumnCache cache = getObjectsCache(col);
		cache.removeObject(col, false);
		ExasolTable table = command.getObject().getTable();
		try {
			actions.add(new SQLDatabasePersistAction(generateAction(monitor, table)));
		} catch (DBException e) {
			LOG.error("Failed to create Partition Action", e);
		}
	}
	
	private String generateAction(DBRProgressMonitor monitor, ExasolTable table) throws DBException
	{
		if (table.getAdditionalInfo(monitor).getHasPartitionKey(monitor) & table.getPartitions(monitor).size() == 0)
		{
			return "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " DROP PARTITION KEYS";
		} 
		if (table.getPartitions(monitor).size() > 0)
		{
			if (! table.getAdditionalInfo(monitor).getHasPartitionKey(monitor))
					table.setHasPartitionKey(true, true);
			return ExasolUtils.getPartitionDdl(table, monitor);
		}
			
		return null;
	}



}
