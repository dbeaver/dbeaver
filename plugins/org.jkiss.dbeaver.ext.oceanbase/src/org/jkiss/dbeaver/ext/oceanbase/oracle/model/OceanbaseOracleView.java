/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.oceanbase.oracle.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDDLFormat;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableTrigger;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.ext.oracle.model.OracleView;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;

public class OceanbaseOracleView extends OracleView {
	private final OceanbaseAdditionalInfo additionalInfo = new OceanbaseAdditionalInfo();
	private String viewText;
	// Generated from ALL_VIEWS
	private String viewSourceText;
	private OracleDDLFormat currentDDLFormat;

	public OceanbaseOracleView(OracleSchema schema, ResultSet dbResult) {
		super(schema, dbResult);
	}

	public OceanbaseOracleView(OracleSchema schema, String name) {
		super(schema, name);
	}

	public class OceanbaseAdditionalInfo extends AdditionalInfo {
		private String typeText;
		private String oidText;
		private String typeOwner;
		private String typeName;
		private OracleView superView;

		boolean loaded;

		@Property(viewable = false, order = 10)
		public Object getType(DBRProgressMonitor monitor) throws DBException {
			if (typeOwner == null) {
				return null;
			}
			OracleSchema owner = getDataSource().getSchema(monitor, typeOwner);
			return owner == null ? null : owner.getDataType(monitor, typeName);
		}

		@Property(viewable = false, order = 11)
		public String getTypeText() {
			return typeText;
		}

		public void setTypeText(String typeText) {
			this.typeText = typeText;
		}

		@Property(viewable = false, order = 12)
		public String getOidText() {
			return oidText;
		}

		public void setOidText(String oidText) {
			this.oidText = oidText;
		}

		@Property(viewable = false, editable = true, order = 5)
		public OracleView getSuperView() {
			return superView;
		}

		public void setSuperView(OracleView superView) {
			this.superView = superView;
		}
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		DBPDataSource dataSource = getDataSource();
		DBPNamedObject[] path = new DBPNamedObject[] { getContainer(), this };
		StringBuilder name = new StringBuilder(20 * path.length);
		if (dataSource == null) {
			// It is not SQL identifier, let's just make it simple then
			for (DBPNamedObject namePart : path) {
				if (DBUtils.isVirtualObject(namePart)) {
					continue;
				}
				if (name.length() > 0) {
					name.append('.');
				}
				name.append(namePart.getName());
			}
		} else {
			final SQLDialect sqlDialect = dataSource.getSQLDialect();

			DBPNamedObject parent = null;
			for (DBPNamedObject namePart : path) {
				if (namePart == null || DBUtils.isVirtualObject(namePart)) {
					continue;
				}
				if (namePart instanceof DBSCatalog && ((sqlDialect.getCatalogUsage() & SQLDialect.USAGE_DML) == 0)) {
					continue;
				}
				if (!DBUtils.isValidObjectName(namePart.getName())) {
					continue;
				}
				if (name.length() > 0) {
					if (parent instanceof DBSCatalog) {
						name.append(sqlDialect.getCatalogSeparator());
					} else {
						name.append(sqlDialect.getStructSeparator());
					}
				}
				name.append(DBUtils.getQuotedIdentifier(dataSource, namePart.getName()));
				parent = namePart;
			}
		}
		return name.toString();
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		if (viewText == null) {
			currentDDLFormat = OracleDDLFormat.getCurrentFormat(getDataSource());
		}
		OracleDDLFormat newFormat = OracleDDLFormat.FULL;
		boolean isFormatInOptions = options.containsKey(OracleConstants.PREF_KEY_DDL_FORMAT);
		if (isFormatInOptions) {
			newFormat = (OracleDDLFormat) options.get(OracleConstants.PREF_KEY_DDL_FORMAT);
		}

		if (viewText == null || (currentDDLFormat != newFormat && isPersisted())) {
			try {
				if (viewText == null || !isFormatInOptions) {
					viewText = OracleUtils.getDDL(monitor, getTableTypeName(), this, currentDDLFormat, options);
				} else {
					viewText = OracleUtils.getDDL(monitor, getTableTypeName(), this, newFormat, options);
					currentDDLFormat = newFormat;
				}
			} catch (DBException e) {
			}
		}
		if (CommonUtils.isEmpty(viewText)) {
			loadAdditionalInfo(monitor);
			if (CommonUtils.isEmpty(viewSourceText)) {
				return "-- Oracle view definition is not available";
			}
			return viewSourceText;
		}
		return viewText;
	}

	@PropertyGroup()
	@LazyProperty(cacheValidator = AdditionalInfoValidator.class)
	public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBException {
		synchronized (additionalInfo) {
			if (!additionalInfo.loaded && monitor != null) {
				loadAdditionalInfo(monitor);
			}
			return additionalInfo;
		}
	}

	private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBException {
		if (!isPersisted()) {
			additionalInfo.loaded = true;
			return;
		}
		try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
			boolean isOracle9 = getDataSource().isAtLeastV9();
			try (JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT TEXT,OID_TEXT,VIEW_TYPE" + (isOracle9 ? ",SUPERVIEW_NAME" : "") + "\n"
							+ "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "VIEWS")
							+ " WHERE OWNER=? AND VIEW_NAME=?")) {
				dbStat.setString(1, getContainer().getName());
				dbStat.setString(2, getName());
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					if (dbResult.next()) {
						additionalInfo.setTypeText(JDBCUtils.safeGetStringTrimmed(dbResult, "TYPE_TEXT"));
						additionalInfo.setOidText(JDBCUtils.safeGetStringTrimmed(dbResult, "OID_TEXT"));
						additionalInfo.typeOwner = JDBCUtils.safeGetStringTrimmed(dbResult, "VIEW_TYPE_OWNER");
						additionalInfo.typeName = JDBCUtils.safeGetStringTrimmed(dbResult, "VIEW_TYPE");
						if (isOracle9) {
							String superViewName = JDBCUtils.safeGetString(dbResult, "SUPERVIEW_NAME");
							if (!CommonUtils.isEmpty(superViewName)) {
								additionalInfo.setSuperView(getContainer().getView(monitor, superViewName));
							}
						}
					}
					additionalInfo.loaded = true;
				}
			} catch (SQLException e) {
				throw new DBCException(e, session.getExecutionContext());
			}
		}
	}

	public void setObjectDefinitionText(String source) {
		this.viewText = source;
	}

	@Override
	public AdditionalInfo getAdditionalInfo() {
		return additionalInfo;
	}

	@Override
	protected String getTableTypeName() {
		return "VIEW";
	}

	@Nullable
	@Association
	@Override
	public List<OracleTableTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getSchema().oceanbaseTableTriggerCache.getObjects(monitor, getSchema(), this);
	}

	@Override
	@NotNull
	public OceanbaseOracleSchema getSchema() {
		return (OceanbaseOracleSchema) super.getContainer();
	}

	@Override
	public OracleTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
			throws DBException {
		return ((OceanbaseOracleSchema) getContainer()).oceanbaseTableCache.getChild(monitor,
				(OceanbaseOracleSchema) getContainer(), this, attributeName);
	}

	@Override
	public List<OracleTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
		return ((OceanbaseOracleSchema) getContainer()).oceanbaseTableCache.getChildren(monitor,
				(OceanbaseOracleSchema) getContainer(), this);
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		this.additionalInfo.loaded = false;
		this.viewText = null;
		this.viewSourceText = null;
		getSchema().refreshObject(monitor);

		return ((OceanbaseOracleSchema) getContainer()).oceanbaseTableCache.refreshObject(monitor,
				(OceanbaseOracleSchema) getContainer(), this);
	}

}
