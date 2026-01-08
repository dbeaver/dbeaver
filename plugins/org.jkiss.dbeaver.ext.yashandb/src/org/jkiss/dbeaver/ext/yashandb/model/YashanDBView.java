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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;

public class YashanDBView extends YashanDBTableBase implements YashanDBSourceObject, DBSView {

	private String viewText;

	public YashanDBView(YashanDBSchema schema, String name) {
		super(schema, name, false);
	}

	public YashanDBView(YashanDBSchema schema, ResultSet dbResult) {
		super(schema, dbResult);
	}

	@NotNull
	@Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
	@Override
	public String getName() {
		return super.getName();
	}

	@Override
	public boolean isView() {
		return true;
	}

	@Override
	public YashanDBSourceType getSourceType() {
		return YashanDBSourceType.VIEW;
	}

	public void setObjectDefinitionText(String source) {
		this.viewText = source;
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		this.viewText = null;
		return super.refreshObject(monitor);
	}

	@Override
	protected String getTableTypeName() {
		return "VIEW";
	}

	@Override
	public TableAdditionalInfo getAdditionalInfo() {
		return null;
	}

	public String getViewText() {
		return viewText;
	}

	public void setViewText(String viewText) {
		this.viewText = viewText;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		if (viewText != null) {
			return viewText;
		}
		return YashanDBUtils.getTableOrViewDDL(monitor, getTableTypeName(), this, options);
	}

	@Override
	public DBSObjectState getObjectState() {
		return null;
	}

	@Override
	public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
	}

	@Override
	public String getDescription(DBRProgressMonitor monitor) {
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}
}
