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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableIndex;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.List;
import java.util.Map;

public class ExasolTableIndexManager extends SQLIndexManager<ExasolTableIndex, ExasolTable>  {

	@Override
	public long getMakerOptions(@NotNull DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}
	
	@Nullable
	@Override
	public DBSObjectCache<ExasolSchema, ExasolTableIndex> getObjectsCache(ExasolTableIndex object) {
		return object.getTable().getContainer().getIndexCache();
	}
	
	@Override
	public boolean canEditObject(ExasolTableIndex object) {
		return false;
	}
	
	@Override
	protected ExasolTableIndex createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context,
													Object container, Object from, @NotNull Map<String, Object> options) throws DBException {
		return new ExasolTableIndex((ExasolTable) container, null,  DBSIndexType.OTHER, false );
	}
	
	
	
	@Override
	protected String getDropIndexPattern(ExasolTableIndex index) {
		return "DROP " + index.getType().getName() + " INDEX ON " + index.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " " + index.getColumnString();
	}
	
	@Override
	protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
										  @NotNull List<DBEPersistAction> actions, @NotNull SQLObjectEditor<ExasolTableIndex, ExasolTable>.ObjectCreateCommand command,
										  @NotNull Map<String, Object> options) {
		ExasolTableIndex index = command.getObject();
		String SQL = String.format(
				"ENFORCE %s INDEX ON %s %s",
				index.getType().getName(),
				index.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL),
				index.getColumnString()
				);
		
		actions.add(
				new SQLDatabasePersistAction(
						"Create Index",
						SQL
						)
				); 
		
	}
	
	

}
