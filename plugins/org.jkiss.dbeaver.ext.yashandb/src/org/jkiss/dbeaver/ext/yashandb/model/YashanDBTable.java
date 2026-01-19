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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBPReferentialIntegrityController;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

public class YashanDBTable extends YashanDBTablePhysical implements DBPScriptObject, DBDPseudoAttributeContainer,
		DBPObjectStatistics, DBPImageProvider, DBPReferentialIntegrityController, DBPScriptObjectExt2 {

	private static final Log log = Log.getLog(YashanDBTable.class);

	public YashanDBTable(YashanDBSchema schema, String name) {
		super(schema, name);
	}

	public YashanDBTable(DBRProgressMonitor monitor, YashanDBSchema schema, ResultSet dbResult) {
		super(schema, dbResult);

		this.tableType = JDBCUtils.safeGetString(dbResult, "TABLE_TYPE");
		this.temporary = JDBCUtils.safeGetBoolean(dbResult, "TEMPORARY", "Y");
		this.secondary = JDBCUtils.safeGetBoolean(dbResult, "SECONDARY", "Y");
	}

	private static final CharSequence TABLE_NAME_PLACEHOLDER = "%table_name%";
	private static final CharSequence FOREIGN_KEY_NAME_PLACEHOLDER = "%foreign_key_name%";
	private static final String DISABLE_REFERENTIAL_INTEGRITY_STATEMENT = "ALTER TABLE " + TABLE_NAME_PLACEHOLDER
			+ " MODIFY CONSTRAINT " + FOREIGN_KEY_NAME_PLACEHOLDER + " DISABLE";
	private static final String ENABLE_REFERENTIAL_INTEGRITY_STATEMENT = "ALTER TABLE " + TABLE_NAME_PLACEHOLDER
			+ " MODIFY CONSTRAINT " + FOREIGN_KEY_NAME_PLACEHOLDER + " ENABLE";

	private static final String[] supportedOptions = new String[] { DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS,
			DBPScriptObject.OPTION_DDL_ONLY_FOREIGN_KEYS };

	private String tableType;
	private boolean temporary;
	private boolean secondary;
	private transient volatile Long tableSize;

	private final AdditionalInfo additionalInfo = new AdditionalInfo();

	public class AdditionalInfo extends TableAdditionalInfo {

		private int pctFree;
		private int iniTrans;
		private int maxTrans;
		private int blocks;
		private int emptyBlocks;

		@Property(category = DBConstants.CAT_STATISTICS, order = 31)
		public int getPctFree() {
			return pctFree;
		}

		@Property(category = DBConstants.CAT_STATISTICS, order = 33)
		public int getIniTrans() {
			return iniTrans;
		}

		@Property(category = DBConstants.CAT_STATISTICS, order = 34)
		public int getMaxTrans() {
			return maxTrans;
		}

		@Property(category = DBConstants.CAT_STATISTICS, order = 42)
		public int getBlocks() {
			return blocks;
		}

		@Property(category = DBConstants.CAT_STATISTICS, order = 43)
		public int getEmptyBlocks() {
			return emptyBlocks;
		}
	}

	@Property(viewable = false, order = 5)
	public String getTableType() {
		return tableType;
	}

	@Property(viewable = false, order = 10)
	public boolean isTemporary() {
		return temporary;
	}

	@Property(viewable = false, order = 11)
	public boolean isSecondary() {
		return secondary;
	}

	@Property(viewable = false, category = DBConstants.CAT_STATISTICS, formatter = ByteNumberFormat.class)
	public Long getTableSize(DBRProgressMonitor monitor) throws DBCException {
		if (tableSize == null) {
			loadSize(monitor);
		}
		return tableSize;
	}

	void fetchTableSize(JDBCResultSet dbResult) throws SQLException {
		tableSize = dbResult.getLong("TABLE_SIZE");
	}

	@Override
	protected String getTableTypeName() {
		return "TABLE";
	}

	@Override
	public boolean isView() {
		return false;
	}

	@Override
	public DBPImage getObjectImage() {
		return null;
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		return getDDL(monitor, options);
	}

	@Override
	public DBDPseudoAttribute[] getPseudoAttributes() throws DBException {
		return new DBDPseudoAttribute[0];
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		getContainer().foreignKeyCache.clearObjectCache(this);
		if (tableSize != null) {
			tableSize = null;
			getTableSize(monitor);
		}
		return super.refreshObject(monitor);
	}

	@Override
	public YashanDBTableColumn getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException {
		return super.getAttribute(monitor, attributeName);
	}

	@Override
	public Collection<YashanDBTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
		List<YashanDBTableForeignKey> refs = new ArrayList<>();
		final Collection<YashanDBTableForeignKey> allForeignKeys = getContainer().foreignKeyCache.getObjects(monitor,
				getContainer(), null);
		for (YashanDBTableForeignKey constraint : allForeignKeys) {
			if (constraint.getReferencedTable() == this) {
				refs.add(constraint);
			}
		}
		return refs;
	}

	@Override
	@Association
	public Collection<YashanDBTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getContainer().foreignKeyCache.getObjects(monitor, getContainer(), this);
	}

	@Override
	public TableAdditionalInfo getAdditionalInfo() {
		return additionalInfo;
	}

	@PropertyGroup
	@LazyProperty(cacheValidator = AdditionalInfoValidator.class)
	public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBException {
		synchronized (additionalInfo) {
			if (!additionalInfo.loaded && monitor != null) {
				loadAdditionalInfo(monitor);
			}
			return additionalInfo;
		}
	}

	public void setTableSize(Long tableSize) {
		this.tableSize = tableSize;
	}

	private void loadSize(DBRProgressMonitor monitor) throws DBCException {
		tableSize = null;
		try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
			boolean hasDBA = getDataSource().isViewAvailable(monitor, YashanDBConstants.SCHEMA_SYS, "DBA_SEGMENTS");
			try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT SUM(bytes) TABLE_SIZE\n" + "FROM "
					+ YashanDBUtils.getSysSchemaPrefix(getDataSource()) + (hasDBA ? "DBA_SEGMENTS" : "USER_SEGMENTS")
					+ " s\n" + "WHERE S.SEGMENT_TYPE='TABLE' AND s.SEGMENT_NAME = ?"
					+ (hasDBA ? " AND s.OWNER = ?" : ""))) {
				dbStat.setString(1, getName());
				if (hasDBA) {
					dbStat.setString(2, getSchema().getName());
				}
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					if (dbResult.next()) {

						fetchTableSize(dbResult);
					}
				}
			}
		} catch (Exception e) {
			log.error("Error reading table statistics", e);
		} finally {
			if (tableSize == null) {
				tableSize = 0L;
			}
		}
	}

	private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBException {
		if (!isPersisted()) {
			additionalInfo.loaded = true;
			return;
		}
		try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
			try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM "
					+ YashanDBUtils.isAdminPriv(getDataSource(), "TABLES") + " WHERE OWNER=? AND TABLE_NAME=?")) {
				dbStat.setString(1, getContainer().getName());
				dbStat.setString(2, getName());
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					if (dbResult.next()) {
						additionalInfo.pctFree = JDBCUtils.safeGetInt(dbResult, "PCT_FREE");
						additionalInfo.iniTrans = JDBCUtils.safeGetInt(dbResult, "INI_TRANS");
						additionalInfo.maxTrans = JDBCUtils.safeGetInt(dbResult, "MAX_TRANS");
						additionalInfo.blocks = JDBCUtils.safeGetInt(dbResult, "BLOCKS");
						additionalInfo.emptyBlocks = JDBCUtils.safeGetInt(dbResult, "EMPTY_BLOCKS");
					} else {
						log.warn("Cannot find table '" + getFullyQualifiedName(DBPEvaluationContext.UI) + "' metadata");
					}
					additionalInfo.loaded = true;
				}
			} catch (SQLException e) {
				throw new DBCException(e, session.getExecutionContext());
			}
		}

	}

	@Override
	public boolean hasStatistics() {
		return tableSize != null;
	}

	@Override
	public long getStatObjectSize() {
		return tableSize == null ? 0 : tableSize;
	}
	
	@Override
	public boolean supportsObjectDefinitionOption(String option) {
		return ArrayUtils.contains(supportedOptions, option);
	}

	@Override
	public void enableReferentialIntegrity(@NotNull DBRProgressMonitor monitor, boolean enable) throws DBException {
		Collection<YashanDBTableForeignKey> foreignKeys = getAssociations(monitor);
		if (CommonUtils.isEmpty(foreignKeys)) {
			return;
		}

		String template;
		if (enable) {
			template = ENABLE_REFERENTIAL_INTEGRITY_STATEMENT;
		} else {
			template = DISABLE_REFERENTIAL_INTEGRITY_STATEMENT;
		}
		template = template.replace(TABLE_NAME_PLACEHOLDER, getFullyQualifiedName(DBPEvaluationContext.DDL));

		try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Changing referential integrity")) {
			try (JDBCStatement statement = session.createStatement()) {
				for (DBPNamedObject fk : foreignKeys) {
					String sql = template.replace(FOREIGN_KEY_NAME_PLACEHOLDER, fk.getName());
					statement.executeUpdate(sql);
				}
			} catch (SQLException e) {
				throw new DBException("Unable to change referential integrity", e);
			}
		}
	}

	@Override
	public boolean supportsChangingReferentialIntegrity(DBRProgressMonitor monitor) throws DBException {
		return !CommonUtils.isEmpty(getAssociations(monitor));
	}

	@Override
	public String getChangeReferentialIntegrityStatement(DBRProgressMonitor monitor, boolean enable)
			throws DBException {
		if (!supportsChangingReferentialIntegrity(monitor)) {
			return null;
		}
		if (enable) {
			return ENABLE_REFERENTIAL_INTEGRITY_STATEMENT;
		}
		return DISABLE_REFERENTIAL_INTEGRITY_STATEMENT;
	}

	@Override
	public DBDPseudoAttribute[] getAllPseudoAttributes(DBRProgressMonitor monitor) throws DBException {
		return null;
	}
}
