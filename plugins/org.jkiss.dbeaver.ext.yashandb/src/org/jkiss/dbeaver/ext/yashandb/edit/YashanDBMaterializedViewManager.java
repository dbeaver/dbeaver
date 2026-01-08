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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBMaterializedView;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

public class YashanDBMaterializedViewManager extends SQLObjectEditor<YashanDBMaterializedView, YashanDBSchema> {

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	@Override
	protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		if (CommonUtils.isEmpty(command.getObject().getName())) {
			throw new DBException("Materialized view name cannot be empty");
		}
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, YashanDBMaterializedView> getObjectsCache(
			YashanDBMaterializedView object) {
		return (DBSObjectCache) object.getSchema().tableCache;
	}

	@Override
	protected YashanDBMaterializedView createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			Object container, Object copyFrom, Map<String, Object> options) {
		YashanDBSchema schema = (YashanDBSchema) container;
		YashanDBMaterializedView newView = new YashanDBMaterializedView(schema, "NEW_MVIEW");
		setNewObjectName(monitor, schema, newView);
		newView.setObjectDefinitionText("SELECT 1 AS A FROM DUAL");
		return newView;
	}

	@Override
	protected String getBaseObjectName() {
		return SQLTableManager.BASE_MATERIALIZED_VIEW_NAME;
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
			throws DBException {
		createOrReplaceViewQuery(monitor, actions, command, options);
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
			throws DBException {
		createOrReplaceViewQuery(monitor, actionList, command, options);
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
		actions.add(new SQLDatabasePersistAction("Drop materialized view",
				"DROP MATERIALIZED VIEW " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)));
	}

	private void createOrReplaceViewQuery(DBRProgressMonitor monitor, List<DBEPersistAction> actions,
			DBECommandComposite<YashanDBMaterializedView, PropertyHandler> command, Map<String, Object> options)
			throws DBException {
		YashanDBMaterializedView view = command.getObject();
		StringBuilder decl = new StringBuilder(200);
		final String lineSeparator = GeneralUtils.getDefaultLineSeparator();
		String mViewDefinition = view.getMViewText().trim();
		if (mViewDefinition.contains("CREATE MATERIALIZED VIEW")) {
			if (mViewDefinition.endsWith(";"))
				mViewDefinition = mViewDefinition.substring(0, mViewDefinition.length() - 1);
			decl.append(mViewDefinition);
		} else {
			decl.append("CREATE MATERIALIZED VIEW ").append(view.getFullyQualifiedName(DBPEvaluationContext.DDL))
					.append(lineSeparator).append("AS ").append(mViewDefinition);
		}
		if (view.isPersisted()) {
			actions.add(new SQLDatabasePersistAction("Drop materialized view",
					"DROP MATERIALIZED VIEW " + view.getFullyQualifiedName(DBPEvaluationContext.DDL)));
		}
		List<SQLScriptElement> sqlScriptElements = SQLScriptParser.parseScript(view.getDataSource(), mViewDefinition);
		if (sqlScriptElements.size() > 1) {
			for (SQLScriptElement scriptElement : sqlScriptElements) {
				actions.add(new SQLDatabasePersistAction("Create materialized view part", scriptElement.getText()));
			}
			return;
		}
		actions.add(new SQLDatabasePersistAction("Create materialized view", decl.toString()));
	}

}
