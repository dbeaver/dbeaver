/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.yashandb.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBObjectPersistAction;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBObjectType;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBProcedureStandalone;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

public class YashanDBProcedureManager extends SQLObjectEditor<YashanDBProcedureStandalone, YashanDBSchema> {

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, YashanDBProcedureStandalone> getObjectsCache(
			YashanDBProcedureStandalone object) {
		return object.getSchema().proceduresCache;
	}

	@Override
	protected YashanDBProcedureStandalone createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object copyFrom, Map<String, Object> options) {
		return new YashanDBProcedureStandalone((YashanDBSchema) container, "NEW_PROCEDURE", DBSProcedureType.PROCEDURE);
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectCreateCommand objectCreateCommand, Map<String, Object> options) {
		createOrReplaceProcedureQuery(executionContext, actions, objectCreateCommand.getObject());
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectDeleteCommand objectDeleteCommand, Map<String, Object> options) {
		final YashanDBProcedureStandalone object = objectDeleteCommand.getObject();
		actions.add(new SQLDatabasePersistAction("Drop procedure", "DROP " + object.getProcedureType().name() + " " //$NON-NLS-2$ //$NON-NLS-3$
				+ object.getFullyQualifiedName(DBPEvaluationContext.DDL)));
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, ObjectChangeCommand objectChangeCommand, Map<String, Object> options) {
		createOrReplaceProcedureQuery(executionContext, actionList, objectChangeCommand.getObject());
	}

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	private void createOrReplaceProcedureQuery(DBCExecutionContext executionContext, List<DBEPersistAction> actionList,
			YashanDBProcedureStandalone procedure) {
		String source = YashanDBUtils.normalizeSourceName(procedure, false);
		if (source != null) {
			actionList.add(new YashanDBObjectPersistAction(YashanDBObjectType.PROCEDURE, "Create procedure", source));
			YashanDBUtils.addSchemaChangeActions(executionContext, actionList, procedure);
		}
	}

}
