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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSPackage;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

public class YashanDBPackage extends YashanDBSchemaObject implements YashanDBSourceObject, DBPScriptObjectExt,
		DBSObjectContainer, DBSPackage, DBPRefreshableObject, DBSProcedureContainer {

	private Timestamp created;
	private Timestamp lastDDLTime;
	private boolean temporary;
	private boolean valid;
	private String sourceDeclaration;
	private String sourceDefinition;

	private final ProceduresCache proceduresCache = new ProceduresCache();

	public YashanDBPackage(YashanDBSchema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"), true);
		this.created = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
		this.lastDDLTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_DDL_TIME");
		this.temporary = YashanDBConstants.Y.equals(JDBCUtils.safeGetString(dbResult, "TEMPORARY"));
		this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
	}

	public YashanDBPackage(YashanDBSchema schema, String name) {
		super(schema, name, false);
	}

	@Property(viewable = true, order = 2)
	public Timestamp getCreated() {
		return created;
	}

	@Property(viewable = true, order = 3)
	public Timestamp getLastDDLTime() {
		return lastDDLTime;
	}

	@Property(viewable = true, order = 4)
	public boolean isTemporary() {
		return temporary;
	}

	@Property(viewable = true, order = 5)
	public boolean isValid() {
		return valid;
	}

	@Override
	public YashanDBSourceType getSourceType() {
		return YashanDBSourceType.PACKAGE;
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

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException {
		if (sourceDefinition == null && monitor != null) {
			sourceDefinition = YashanDBUtils.getSource(monitor, this, true, true);
		}
		return sourceDefinition;
	}

	public void setExtendedDefinitionText(String source) {
		this.sourceDefinition = source;
	}

	@Association
	public Collection<YashanDBDependencyGroup> getDependencies(DBRProgressMonitor monitor) {
		return YashanDBDependencyGroup.of(this);
	}

	@Association
	public Collection<YashanDBProcedurePackaged> getProceduresOnly(DBRProgressMonitor monitor) throws DBException {
		return getProcedures(monitor).stream().filter(proc -> proc.getProcedureType() == DBSProcedureType.PROCEDURE)
				.collect(Collectors.toList());
	}

	@Association
	public Collection<YashanDBProcedurePackaged> getFunctionsOnly(DBRProgressMonitor monitor) throws DBException {
		return getProcedures(monitor).stream().filter(proc -> proc.getProcedureType() == DBSProcedureType.FUNCTION)
				.collect(Collectors.toList());
	}

	@Association
	public Collection<YashanDBProcedurePackaged> getProcedures(DBRProgressMonitor monitor) throws DBException {
		return proceduresCache.getAllObjects(monitor, this);
	}

	@Override
	public YashanDBProcedurePackaged getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
		return proceduresCache.getObject(monitor, this, uniqueName);
	}

	@Override
	public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
		return proceduresCache.getAllObjects(monitor, this);
	}

	@Override
	public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
		return proceduresCache.getObject(monitor, this, childName);
	}

	@NotNull
	@Override
	public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
		return YashanDBProcedurePackaged.class;
	}

	@Override
	public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
		proceduresCache.getAllObjects(monitor, this);
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		this.proceduresCache.clearCache();
		this.sourceDeclaration = null;
		this.sourceDefinition = null;
		return this;
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
		this.valid = YashanDBUtils.getObjectStatus(monitor, this, YashanDBObjectType.PACKAGE)
				&& YashanDBUtils.getObjectStatus(monitor, this, YashanDBObjectType.PACKAGE_BODY);
	}

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	static class ProceduresCache extends JDBCObjectCache<YashanDBPackage, YashanDBProcedurePackaged> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBPackage owner)
				throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement(
					"SELECT P.*,CASE WHEN A.DATA_TYPE IS NULL THEN 'PROCEDURE' ELSE 'UDF' END as PROCEDURE_TYPE FROM ALL_PROCEDURES P\n"
							+ "LEFT OUTER JOIN ALL_ARGUMENTS A ON A.OWNER=P.OWNER  AND A.OBJECT_NAME=P.PROCEDURE_NAME AND A.ARGUMENT_NAME IS NULL AND A.DATA_LEVEL=0\n"
							+ "WHERE P.OWNER=? AND P.OBJECT_NAME=?\n" + "ORDER BY P.PROCEDURE_NAME");
			dbStat.setString(1, owner.getSchema().getName());
			dbStat.setString(2, owner.getName());
			return dbStat;
		}

		@Override
		protected YashanDBProcedurePackaged fetchObject(@NotNull JDBCSession session, @NotNull YashanDBPackage owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new YashanDBProcedurePackaged(owner, dbResult);
		}

		@Override
		protected void invalidateObjects(DBRProgressMonitor monitor, YashanDBPackage owner,
				Iterator<YashanDBProcedurePackaged> objectIter) {
			Map<String, YashanDBProcedurePackaged> overloads = new HashMap<>();
			while (objectIter.hasNext()) {
				final YashanDBProcedurePackaged proc = objectIter.next();
				if (CommonUtils.isEmpty(proc.getName())) {
					objectIter.remove();
					continue;
				}
				final YashanDBProcedurePackaged overload = overloads.get(proc.getName());
				if (overload == null) {
					overloads.put(proc.getName(), proc);
				} else {
					if (overload.getOverloadNumber() == null) {
						overload.setOverload(1);
					}
					proc.setOverload(overload.getOverloadNumber() + 1);
					overloads.put(proc.getName(), proc);
				}
			}
		}
	}

}
