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
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

public class YashanDBMaterializedView extends YashanDBTableBase
		implements YashanDBSourceObject, DBSObjectLazy<YashanDBDataSource> {

	private String viewText;

	private final AdditionalInfo additionalInfo = new AdditionalInfo();

	public YashanDBMaterializedView(YashanDBSchema schema, String name) {
		super(schema, name, false);
	}

	public YashanDBMaterializedView(YashanDBSchema schema, ResultSet dbResult) {
		super(schema, dbResult);
	}

	@NotNull
	@Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
	@Override
	public String getName() {
		return super.getName();
	}

	@Property(hidden = true, viewable = false, editable = false, updatable = false, length = PropertyLength.MULTILINE, order = 100)
	@LazyProperty(cacheValidator = CommentsValidator.class)
	public String getComment(DBRProgressMonitor monitor) {
		return super.getComment();
	}

	@PropertyGroup()
	@LazyProperty(cacheValidator = AdditionalInfoValidator.class)
	public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBCException {
		synchronized (additionalInfo) {
			if (!additionalInfo.loaded && monitor != null) {
				loadAdditionalInfo(monitor);
			}
			return additionalInfo;
		}
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		if (viewText != null) {
			return viewText;
		}
		return YashanDBUtils.getTableOrViewDDL(monitor, getTableTypeName(), this, options);
	}

	public static class AdditionalInfo extends TableAdditionalInfo {

		private volatile boolean loaded = false;

		private String refreshMode;
		private String refreshMethod;
		private String refreshStartDate;
		private String refreshNextDate;
		private String ddlTime;
		private boolean rewriteEnabled;
		private String buildMode;

		@Property(viewable = false, order = 15)
		public String getRefreshMode() {
			return refreshMode;
		}

		@Property(viewable = false, order = 16)
		public String getRefreshMethod() {
			return refreshMethod;
		}

		@Property(viewable = false, order = 17)
		public String getRefreshStartDate() {
			return refreshStartDate;
		}

		@Property(viewable = false, order = 18)
		public String getRefreshNextDate() {
			return refreshNextDate;
		}

		@Property(viewable = false, order = 19)
		public String getDdlTime() {
			return ddlTime;
		}

		@Property(viewable = false, order = 20)
		public boolean isRewriteEnabled() {
			return rewriteEnabled;
		}

		@Property(viewable = false, order = 21)
		public String getBuildMode() {
			return buildMode;
		}
	}

	private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBCException {
		if (!isPersisted()) {
			additionalInfo.loaded = true;
			return;
		}
		try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load view meta")) {
			try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM "
					+ YashanDBUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "MVIEWS")
					+ " WHERE OWNER=? AND MVIEW_NAME=?")) {
				dbStat.setString(1, getSchema().getName());
				dbStat.setString(2, getName());
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					if (dbResult.next()) {
						additionalInfo.refreshMode = JDBCUtils.safeGetString(dbResult, "REFRESH_MODE");
						additionalInfo.refreshMethod = JDBCUtils.safeGetString(dbResult, "REFRESH_METHOD");
						additionalInfo.refreshStartDate = JDBCUtils.safeGetString(dbResult, "REFRESH_START_DATE");
						additionalInfo.refreshNextDate = JDBCUtils.safeGetString(dbResult, "REFRESH_NEXT_DATE");
						additionalInfo.ddlTime = JDBCUtils.safeGetString(dbResult, "DDL_TIME");
						additionalInfo.rewriteEnabled = JDBCUtils.safeGetBoolean(dbResult, "REWRITE_ENABLED", "Y");
						additionalInfo.buildMode = JDBCUtils.safeGetString(dbResult, "BUILD_MODE");
					}
					additionalInfo.loaded = true;
				}
			} catch (SQLException e) {
				throw new DBCException(e, session.getExecutionContext());
			}
		}
	}

	@Override
	public boolean isFeatureSupported(String feature) {
		return super.isFeatureSupported(feature);
	}

	@Override
	public YashanDBSourceType getSourceType() {
		return YashanDBSourceType.MATERIALIZED_VIEW;
	}

	public void setObjectDefinitionText(String source) {
		this.viewText = source;
	}

	public String getMViewText() {
		return viewText;
	}

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
		this.valid = YashanDBUtils.getObjectStatus(monitor, this, YashanDBObjectType.MATERIALIZED_VIEW);
	}

	@Override
	public Object getLazyReference(Object propertyId) {
		return null;
	}

	@Override
	public boolean isView() {
		return true;
	}

	@Override
	public TableAdditionalInfo getAdditionalInfo() {
		return additionalInfo;
	}

	@Override
	protected String getTableTypeName() {
		return "MATERIALIZED VIEW";
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		getContainer().constraintCache.clearObjectCache(this);

		return getContainer().tableCache.refreshObject(monitor, getContainer(), this);
	}

}
