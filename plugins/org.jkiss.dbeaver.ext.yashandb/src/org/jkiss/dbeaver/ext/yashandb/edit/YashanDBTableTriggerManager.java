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
import java.util.Locale;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableBase;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableTrigger;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

public class YashanDBTableTriggerManager extends SQLTriggerManager<YashanDBTableTrigger, YashanDBTableBase> {
	
	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, YashanDBTableTrigger> getObjectsCache(YashanDBTableTrigger object) {
		return object.getTable().getSchema().tableTriggerCache;
	}

	@Override
	public boolean canCreateObject(Object container) {
		return container instanceof YashanDBTableBase;
	}

	@Override
	protected YashanDBTableTrigger createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object copyFrom, Map<String, Object> options) {
		YashanDBTableBase table = (YashanDBTableBase) container;
		return new YashanDBTableTrigger(table, "NEW_TRIGGER");
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
		actions.add(new SQLDatabasePersistAction("Drop trigger",
				"DROP TRIGGER " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)));
	}

	protected void createOrReplaceTriggerQuery(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, YashanDBTableTrigger trigger, boolean create) {
		String source = YashanDBUtils.normalizeSourceName(trigger, false);
		if (source == null) {
			return;
		}
		String script = source;
		if (!script.toUpperCase(Locale.ENGLISH).trim().contains("CREATE ")) {
			script = "CREATE OR REPLACE " + script;
		}
		actions.add(new SQLDatabasePersistAction("Create trigger", script, true));
		YashanDBUtils.addSchemaChangeActions(executionContext, actions, trigger);
	}
}
