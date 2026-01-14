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
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableIndex;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTablePhysical;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;
import org.jkiss.utils.CommonUtils;

public class YashanDBIndexManager extends SQLIndexManager<YashanDBTableIndex, YashanDBTablePhysical> {

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, YashanDBTableIndex> getObjectsCache(YashanDBTableIndex object) {
		return object.getParentObject().getSchema().indexCache;
	}

	@Override
	protected YashanDBTableIndex createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
		YashanDBTablePhysical table = (YashanDBTablePhysical) container;
		return new YashanDBTableIndex(table.getSchema(), table, "INDEX", true, DBSIndexType.UNKNOWN);
	}

	@Override
	protected String getDropIndexPattern(YashanDBTableIndex index) {
		return "DROP INDEX " + PATTERN_ITEM_INDEX;
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
		final YashanDBTablePhysical table = command.getObject().getTable();
		final YashanDBTableIndex index = command.getObject();

		final String indexName = DBUtils.getQuotedIdentifier(index.getDataSource(), index.getName());
		index.setName(indexName);
		final String tableName = DBUtils.getEntityScriptName(table, options);

		StringBuilder decl = new StringBuilder(40);
		decl.append("CREATE");
		appendIndexModifiers(index, decl);
		decl.append(" INDEX ").append(tableName.split("\\.")[0]).append(".").append(indexName);
		appendIndexType(index, decl);
		decl.append(" ON ").append(tableName).append(" (");
		try {
			boolean firstColumn = true;
			for (DBSTableIndexColumn indexColumn : CommonUtils
					.safeCollection(command.getObject().getAttributeReferences(new VoidProgressMonitor()))) {
				if (!firstColumn)
					decl.append(",");
				firstColumn = false;
				decl.append(DBUtils.getQuotedIdentifier(indexColumn));
				appendIndexColumnModifiers(monitor, decl, indexColumn);
			}
		} catch (Exception e) {
			log.error(e);
		}
		decl.append(")");

		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_index, decl.toString()));
	}

}
