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
package org.jkiss.dbeaver.ext.yashandb.model;

import java.sql.ResultSet;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;
import org.jkiss.utils.CommonUtils;

public abstract class YashanDBTrigger<PARENT extends DBSObject> extends YashanDBObject<PARENT>
		implements DBSTrigger, DBPQualifiedObject, YashanDBSourceObject {

	public YashanDBTrigger(PARENT parent, String name) {
		super(parent, name, false);
	}

	public YashanDBTrigger(PARENT parent, ResultSet dbResult) {
		super(parent, JDBCUtils.safeGetString(dbResult, "TRIGGER_NAME"), true);
		this.objectType = CommonUtils.valueOf(BaseObjectType.class,
				JDBCUtils.safeGetStringTrimmed(dbResult, "BASE_OBJECT_TYPE"));
		this.triggerType = JDBCUtils.safeGetString(dbResult, "TRIGGER_TYPE");
		this.triggeringEvent = JDBCUtils.safeGetString(dbResult, "TRIGGERING_EVENT");
		this.columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
		this.refNames = JDBCUtils.safeGetString(dbResult, "REFERENCING_NAMES");
		this.whenClause = JDBCUtils.safeGetString(dbResult, "WHEN_CLAUSE");
		this.status = CommonUtils.valueOf(YashanDBObjectStatus.class,
				JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS"));
		this.description = JDBCUtils.safeGetString(dbResult, "DESCRIPTION");
		this.actionType = JDBCUtils.safeGetString(dbResult, "ACTION_TYPE");
	}

	private BaseObjectType objectType;
	private String triggerType;
	private String triggeringEvent;
	private String columnName;
	private String refNames;
	private String whenClause;
	private YashanDBObjectStatus status;
	private String description;
	private String actionType;
	private String objectDefinitionText;

	@NotNull
	@Override
	@Property(viewable = true, editable = true, order = 1)
	public String getName() {
		return super.getName();
	}

	@Property(viewable = true, order = 5)
	public BaseObjectType getObjectType() {
		return objectType;
	}

	@Property(viewable = true, order = 5)
	public String getTriggerType() {
		return triggerType;
	}

	@Property(viewable = true, order = 6)
	public String getTriggeringEvent() {
		return triggeringEvent;
	}

	@Property(viewable = true, order = 7)
	public String getColumnName() {
		return columnName;
	}

	@Property(order = 8)
	public String getRefNames() {
		return refNames;
	}

	@Property(order = 9)
	public String getWhenClause() {
		return whenClause;
	}

	@Property(viewable = true, order = 10)
	public YashanDBObjectStatus getStatus() {
		return status;
	}

	@Nullable
	@Override
	@Property(length = PropertyLength.MULTILINE, order = 11)
	public String getDescription() {
		return description;
	}

	@Property(viewable = true, order = 12)
	public String getActionType() {
		return actionType;
	}

	@Override
	public YashanDBSourceType getSourceType() {
		return YashanDBSourceType.TRIGGER;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		if (objectDefinitionText == null && monitor != null) {
			objectDefinitionText = YashanDBUtils.getSource(monitor, this, false, false);
		}
		return objectDefinitionText;
	}

	public void setObjectDefinitionText(String source) {
		this.objectDefinitionText = source;
	}

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return status == YashanDBObjectStatus.ENABLED ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
		this.status = (YashanDBUtils.getObjectStatus(monitor, this, YashanDBObjectType.TRIGGER)
				? YashanDBObjectStatus.ENABLED
				: YashanDBObjectStatus.ERROR);
	}

	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
	}

	@Override
	public String toString() {
		return getFullyQualifiedName(DBPEvaluationContext.DDL);
	}

	public enum BaseObjectType {
		TABLE, VIEW, SCHEMA, DATABASE
	}
}
