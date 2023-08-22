package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

/**
 * Dm Table
 * 
 * @author caosw
 *
 */
public class DmTable extends DmTablePhysical implements DBPScriptObject, DBDPseudoAttributeContainer, DBPImageProvider {
	private static final Log log = Log.getLog(DmTable.class);

	private DmDataType tableType;
	private String iotType;
	private String iotName;
	private boolean temporary;
	private boolean secondary;
	private boolean nested;
	private int id;//表ID
	private List<String> autoCloumnNames=new ArrayList<>();


	public class AdditionalInfo extends TableAdditionalInfo {
		private int pctFree;
		private int pctUsed;
		private int iniTrans;
		private int maxTrans;
		private int initialExtent;
		private int nextExtent;
		private int minExtents;
		private int maxExtents;
		private int pctIncrease;
		private int freelists;
		private int freelistGroups;
		private int blocks;
		private int emptyBlocks;
		private int avgSpace;
		private int chainCount;
		private int avgRowLen;
		private int avgSpaceFreelistBlocks;
		private int numFreelistBlocks;

		@Property(category = CAT_STATISTICS,hidden = true,order = 31) //隐藏该属性
		public int getPctFree() {
			return pctFree;
		}

		@Property(category = CAT_STATISTICS,hidden = true, order = 32)
		public int getPctUsed() {
			return pctUsed;
		}

		@Property(category = CAT_STATISTICS, hidden = true, order = 33)
		public int getIniTrans() {
			return iniTrans;
		}

		@Property(category = CAT_STATISTICS, hidden = true, order = 34)
		public int getMaxTrans() {
			return maxTrans;
		}

		@Property(category = CAT_STATISTICS, hidden = true, order = 35)
		public int getInitialExtent() {
			return initialExtent;
		}

		@Property(category = CAT_STATISTICS, order = 36)
		public int getNextExtent() {
			return nextExtent;
		}

		@Property(category = CAT_STATISTICS, order = 37)
		public int getMinExtents() {
			return minExtents;
		}

		@Property(category = CAT_STATISTICS, order = 38)
		public int getMaxExtents() {
			return maxExtents;
		}

		@Property(category = CAT_STATISTICS, order = 39)
		public int getPctIncrease() {
			return pctIncrease;
		}

		@Property(category = CAT_STATISTICS, order = 40)
		public int getFreelists() {
			return freelists;
		}

		@Property(category = CAT_STATISTICS, order = 41)
		public int getFreelistGroups() {
			return freelistGroups;
		}

		@Property(category = CAT_STATISTICS, order = 42)
		public int getBlocks() {
			return blocks;
		}

		@Property(category = CAT_STATISTICS, order = 43)
		public int getEmptyBlocks() {
			return emptyBlocks;
		}

		@Property(category = CAT_STATISTICS, order = 44)
		public int getAvgSpace() {
			return avgSpace;
		}

		@Property(category = CAT_STATISTICS, order = 45)
		public int getChainCount() {
			return chainCount;
		}

		@Property(category = CAT_STATISTICS, order = 46)
		public int getAvgRowLen() {
			return avgRowLen;
		}

		@Property(category = CAT_STATISTICS, order = 47)
		public int getAvgSpaceFreelistBlocks() {
			return avgSpaceFreelistBlocks;
		}

		@Property(category = CAT_STATISTICS, order = 48)
		public int getNumFreelistBlocks() {
			return numFreelistBlocks;
		}
	}

	private final AdditionalInfo additionalInfo = new AdditionalInfo();

	public DmTable(DmSchema schema, String name) {
		super(schema, name);
	}

	public DmTable(DBRProgressMonitor monitor, DmSchema schema, ResultSet dbResult) {
		super(schema, dbResult);
		String typeOwner = JDBCUtils.safeGetString(dbResult, "TABLE_TYPE_OWNER");
		if (!CommonUtils.isEmpty(typeOwner)) {
			tableType = DmDataType.resolveDataType(monitor, schema.getDataSource(), typeOwner,
					JDBCUtils.safeGetString(dbResult, "TABLE_TYPE"));
		}
		//this.iotType = JDBCUtils.safeGetString(dbResult, "IOT_TYPE");
		//this.iotName = JDBCUtils.safeGetString(dbResult, "IOT_NAME");
		this.temporary = JDBCUtils.safeGetBoolean(dbResult, "TEMPORARY", "Y");
		this.secondary = JDBCUtils.safeGetBoolean(dbResult, "SECONDARY", "Y");
		this.nested = JDBCUtils.safeGetBoolean(dbResult, "NESTED", "Y");
		this.id=JDBCUtils.safeGetInt(dbResult, "ID");//获取表ID
		loadAutoCloumns(monitor);//获取表自增列
	}

	public TableAdditionalInfo getAdditionalInfo() {
		return additionalInfo;
	}

	@PropertyGroup()
	@LazyProperty(cacheValidator = AdditionalInfoValidator.class)
	public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBException {
		synchronized (additionalInfo) {
			if (!additionalInfo.loaded && monitor != null) {
				//loadAdditionalInfo(monitor); DBeaver1.3.5 版本开始不加载表其他属性
			}
			return additionalInfo;
		}
	}

	@Override
	public String getTableTypeName() {
		return "TABLE";
	}

	@Override
	public boolean isView() {
		return false;
	}

	@Property(viewable = false, order = 5)
	public DmDataType getTableType() {
		return tableType;
	}

	@Property(viewable = false, order = 6)
	public String getIotType() {
		return iotType;
	}

	@Property(viewable = false, order = 7)
	public String getIotName() {
		return iotName;
	}

	@Property(viewable = false, order = 10)
	public boolean isTemporary() {
		return temporary;
	}

	@Property(viewable = false, order = 11)
	public boolean isSecondary() {
		return secondary;
	}

	@Property(viewable = false, order = 12)
	public boolean isNested() {
		return nested;
	}

	@Override
	public DmTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
			throws DBException {
		return super.getAttribute(monitor, attributeName);
	}

	@Nullable
	private DmTableColumn getXMLColumn(DBRProgressMonitor monitor) throws DBException {
		for (DmTableColumn col : CommonUtils.safeCollection(getAttributes(monitor))) {
			if (col.getDataType() == tableType) {
				return col;
			}
		}
		return null;
	}

	@Override
	public Collection<DmTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
		List<DmTableForeignKey> refs = new ArrayList<>();
		final Collection<DmTableForeignKey> allForeignKeys = getContainer().foreignKeyCache.getObjects(monitor,
				getContainer(), null);
		for (DmTableForeignKey constraint : allForeignKeys) {
			if (constraint.getReferencedTable() == this) {
				refs.add(constraint);
			}
		}
		return refs;
	}

	@Override
	@Association
	public Collection<DmTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getContainer().foreignKeyCache.getObjects(monitor, getContainer(), this);
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		getContainer().foreignKeyCache.clearObjectCache(this);
		return super.refreshObject(monitor);
	}

	@Override
	public DBDPseudoAttribute[] getPseudoAttributes() throws DBException {
		if (CommonUtils.isEmpty(this.iotType)
				&& getDataSource().getContainer().getPreferenceStore().getBoolean(DmConstants.PREF_SUPPORT_ROWID)) {
			return new DBDPseudoAttribute[] { DmConstants.PSEUDO_ATTR_ROWID };
		} else {
			return null;
		}
	}

	@Override
	protected void appendSelectSource(DBRProgressMonitor monitor, StringBuilder query, String tableAlias,
			DBDPseudoAttribute rowIdAttribute) {
		if (tableType != null && tableType.getName().equals(DmConstants.TYPE_NAME_XML)) {
			try {
				DmTableColumn xmlColumn = getXMLColumn(monitor);
				if (xmlColumn != null) {
					query.append("XMLType(").append(tableAlias).append(".").append(xmlColumn.getName())
							.append(".getClobval()) as ").append(xmlColumn.getName());
					if (rowIdAttribute != null) {
						query.append(",").append(rowIdAttribute.translateExpression(tableAlias));
					}
					return;
				}
			} catch (DBException e) {
				log.warn(e);
			}
		}
		super.appendSelectSource(monitor, query, tableAlias, rowIdAttribute);
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		return getDDL(monitor, DmDDLFormat.getCurrentFormat(getDataSource()), options);
	}

	private void loadAutoCloumns(DBRProgressMonitor monitor) {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table autoGenerate")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT NAME FROM SYS.SYSCOLUMNS WHERE ID=" +id+ " AND INFO2>0")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        autoCloumnNames.add(JDBCUtils.safeGetString(dbResult, "NAME").toUpperCase());
                    }
                }
            } catch (Exception e) {
                log.warn("获取DM表自增列失败");
            }
        }catch (Exception e) {
        	log.warn("获取DM表自增列失败");
		}
	}
	
	
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	
	public List<String> getAutoCloumnNames() {
		return autoCloumnNames;
	}

	public void setAutoCloumnNames(List<String> autoCloumnNames) {
		this.autoCloumnNames = autoCloumnNames;
	}

	@Nullable
	@Override
	public DBPImage getObjectImage() {
		if (CommonUtils.isEmpty(iotType)) {
			return DBIcon.TREE_TABLE;
		} else {
			return DBIcon.TREE_TABLE_INDEX;
		}
	}

	private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBException {
		if (!isPersisted()) {
			additionalInfo.loaded = true;
			return;
		}
		try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
			try (JDBCPreparedStatement dbStat = session.prepareStatement(
					"SELECT * FROM " + DmUtils.getAdminAllViewPrefix(monitor, getDataSource(), "TABLES")
							+ " WHERE OWNER=? AND TABLE_NAME=?")) {
				dbStat.setString(1, getContainer().getName());
				dbStat.setString(2, getName());
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					if (dbResult.next()) {
						additionalInfo.pctFree = JDBCUtils.safeGetInt(dbResult, "PCT_FREE");
						additionalInfo.pctUsed = JDBCUtils.safeGetInt(dbResult, "PCT_USED");
						additionalInfo.iniTrans = JDBCUtils.safeGetInt(dbResult, "INI_TRANS");
						additionalInfo.maxTrans = JDBCUtils.safeGetInt(dbResult, "MAX_TRANS");
						additionalInfo.initialExtent = JDBCUtils.safeGetInt(dbResult, "INITIAL_EXTENT");
						additionalInfo.nextExtent = JDBCUtils.safeGetInt(dbResult, "NEXT_EXTENT");
						additionalInfo.minExtents = JDBCUtils.safeGetInt(dbResult, "MIN_EXTENTS");
						additionalInfo.maxExtents = JDBCUtils.safeGetInt(dbResult, "MAX_EXTENTS");
						additionalInfo.pctIncrease = JDBCUtils.safeGetInt(dbResult, "PCT_INCREASE");
						additionalInfo.freelists = JDBCUtils.safeGetInt(dbResult, "FREELISTS");
						additionalInfo.freelistGroups = JDBCUtils.safeGetInt(dbResult, "FREELIST_GROUPS");
						additionalInfo.blocks = JDBCUtils.safeGetInt(dbResult, "BLOCKS");
						additionalInfo.emptyBlocks = JDBCUtils.safeGetInt(dbResult, "EMPTY_BLOCKS");
						additionalInfo.avgSpace = JDBCUtils.safeGetInt(dbResult, "AVG_SPACE");
						additionalInfo.chainCount = JDBCUtils.safeGetInt(dbResult, "CHAIN_CNT");
						additionalInfo.avgRowLen = JDBCUtils.safeGetInt(dbResult, "AVG_ROW_LEN");
						additionalInfo.avgSpaceFreelistBlocks = JDBCUtils.safeGetInt(dbResult,
								"AVG_SPACE_FREELIST_BLOCKS");
						additionalInfo.numFreelistBlocks = JDBCUtils.safeGetInt(dbResult, "NUM_FREELIST_BLOCKS");
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
}
