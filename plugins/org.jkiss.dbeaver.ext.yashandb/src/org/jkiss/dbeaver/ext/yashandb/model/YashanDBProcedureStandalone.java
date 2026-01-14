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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class YashanDBProcedureStandalone extends YashanDBProcedureBase<YashanDBSchema>
		implements YashanDBSourceObject, DBPRefreshableObject {

	private boolean valid;
	private String sourceDeclaration;

	private int subprogramId;;
	private List<YashanDBProcedureArgument> params = new ArrayList<>();

	public YashanDBProcedureStandalone(DBRProgressMonitor monitor, YashanDBSchema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"), JDBCUtils.safeGetLong(dbResult, "OBJECT_ID"),
				Objects.equals(JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE"), "UDF") ? DBSProcedureType.FUNCTION
						: DBSProcedureType.PROCEDURE);
		this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
		try {
			subprogramId = JDBCUtils.safeGetInt(dbResult, "SUBPROGRAM_ID");
			this.params = (List<YashanDBProcedureArgument>) this.getParameters(monitor);
		} catch (DBException e) {
			throw new RuntimeException(e);
		}
	}

	public int getSubprogramId() {
		return subprogramId;
	}

	public void setSubprogramId(int subprogramId) {
		this.subprogramId = subprogramId;
	}

	public YashanDBProcedureStandalone(YashanDBSchema yashandbSchema, String name, DBSProcedureType procedureType) {
		super(yashandbSchema, name, 0L, procedureType);
		sourceDeclaration = procedureType.name() + " " + name + GeneralUtils.getDefaultLineSeparator() + "IS"
				+ GeneralUtils.getDefaultLineSeparator() + "BEGIN" + GeneralUtils.getDefaultLineSeparator() + "END "
				+ name + ";" + GeneralUtils.getDefaultLineSeparator();
	}

	@Property(viewable = true, order = 3)
	public boolean isValid() {
		return valid;
	}

	@NotNull
	@Override
	public YashanDBSchema getSchema() {
		return getParentObject();
	}

	@Override
	public YashanDBSourceType getSourceType() {
		return getProcedureType() == DBSProcedureType.PROCEDURE ? YashanDBSourceType.PROCEDURE
				: YashanDBSourceType.FUNCTION;
	}

	@Override
	public Integer getOverloadNumber() {
		return null;
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBCException {
		if (sourceDeclaration == null && monitor != null) {
			sourceDeclaration = YashanDBUtils.getSource(monitor, this, false, true);
		}
		return sourceDeclaration;
	}

	public void setObjectDefinitionText(String sourceDeclaration) {
		this.sourceDeclaration = sourceDeclaration;
	}

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
		this.valid = YashanDBUtils.getObjectStatus(monitor, this,
				getProcedureType() == DBSProcedureType.PROCEDURE ? YashanDBObjectType.PROCEDURE
						: YashanDBObjectType.FUNCTION);
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		this.params = (List<YashanDBProcedureArgument>) super.getParameters(monitor);
		return getSchema().proceduresCache.refreshObject(monitor, getSchema(), this);
	}

	public String getFullQualifiedSignature() {
		return this.getSchema().getName() + "." + this.getName();
	}

	public List<YashanDBProcedureArgument> getInputParams() {
		List<YashanDBProcedureArgument> inputParams = new ArrayList<>();
		for (YashanDBProcedureArgument param : params) {
			if (param.getParameterKind().equals(DBSProcedureParameterKind.IN)
					|| param.getParameterKind().equals(DBSProcedureParameterKind.INOUT))
				inputParams.add(param);
		}
		return inputParams;
	}

}
