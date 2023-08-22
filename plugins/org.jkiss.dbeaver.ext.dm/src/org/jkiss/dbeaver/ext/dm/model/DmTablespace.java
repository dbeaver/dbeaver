package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

public class DmTablespace extends DmGlobalObject implements DBPRefreshableObject {

	public enum Status {
		//枚举类后面添加参数相当于直接进行初始化，初始化后即可使用该参数
		ONLINE("在线"), OFFLINE("离线"), RES_OFFLINE("离线"),ALL("所有"),CORRUPT("损坏"); 
    	private final String value;
		Status(String status) {
			this.value=status;
		}
		private String getValue() {
			return value;
		}
	}
	
	//缓存策略
	public enum Cache{
		KEEP,NORMAL
	}
	

	public enum Contents {
		PERMANENT, TEMPORARY, UNDO
	}

	public enum Logging {
		LOGGING, NOLOGGING
	}

	public enum ExtentManagement {
		DICTIONARY, LOCAL
	}

	public enum AllocationType {
		SYSTEM, UNIFORM, USER,
	}

	public enum SegmentSpaceManagement {
		MANUAL, AUTO
	}

	public enum Retention {
		GUARANTEE, NOGUARANTEE, NOT_APPLY
	}

	private String name;
	//private String usedRadio = ""; //空间使用率
	private Status status; //状态
	private String encryptCipher; //加密算法
	private String pass; //密钥
	private Cache  cache;//缓冲策略
	private int copyNum; //副本数
	private String copyStrategy;// 副本策略
	private long id;
	
	
	
	
	/**private long blockSize;
	private long initialExtent;
	private long nextExtent;
	private long minExtents;
	private long maxExtents;
	private long pctIncrease;
	private long minExtLen;
	
	private Contents contents;
	private Logging logging;
	private boolean forceLogging;
	private ExtentManagement extentManagement;
	private AllocationType allocationType;
	private boolean pluggedIn;
	private SegmentSpaceManagement segmentSpaceManagement;
	private boolean defTableCompression;
	private Retention retention;
	private boolean bigFile;
	private volatile Long availableSize;
	private volatile Long usedSize;*/

	public final FileCache fileCache = new FileCache();
	final SegmentCache segmentCache = new SegmentCache();
    private DmDataFile dataFile;
	
	
    
	
	public DmTablespace(DmDataSource dataSource,String name, long id) {
		super(dataSource, false);
		this.name = name;
		this.id = id;
		this.cache=Cache.NORMAL;
		this.dataFile=new DmDataFile(dataSource, this); 
		this.status=Status.ONLINE;
	}

	protected DmTablespace(DmDataSource dataSource, ResultSet dbResult) {
		super(dataSource, true);
		this.name = JDBCUtils.safeGetString(dbResult, "NAME");
		this.id=JDBCUtils.safeGetInt(dbResult, "ID");
//		this.status = CommonUtils.valueOf(Status.class, JDBCUtils.safeGetString(dbResult, "STATUS"), Status.OFFLINE,
//				true);
		int status_number = JDBCUtils.safeGetInt(dbResult, "STATUS$");
		if(status_number == 0) {
			this.status = Status.ONLINE;
		} else if(status_number == 1) {
			this.status = Status.OFFLINE;
		} else if (status_number == -1) {
			this.status=Status.ALL;
		}else if (status_number == 2) {
			this.status=Status.RES_OFFLINE;
		}else if (status_number==3) {
			this.status=Status.CORRUPT;
		}

		this.encryptCipher = JDBCUtils.safeGetString(dbResult, "ENCRYPT_NAME");
		this.pass=JDBCUtils.safeGetString(dbResult, "ENCRYPTED_KEY");
		this.cache = CommonUtils.valueOf(Cache.class, JDBCUtils.safeGetString(dbResult, "CACHE"), null, true);
		try {
			if(dataSource.getInfo().getDatabaseVersion().getMajor()>7) {
				this.copyNum=JDBCUtils.safeGetInt(dbResult, "COPY_NUM");
				this.copyStrategy=JDBCUtils.safeGetString(dbResult, "SIZE_MODE");
			}
		} catch (Exception e) {
		  
		}
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = true, order = 2)
	public String getName() {
		return name;
	}

	@Property(viewable = true, order = 4)
	public String getStatus() {
 		return status.getValue();
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@Property(viewable = true, order = 5)
	public String getEncryptCipher() {
		return encryptCipher;
	}

	public void setEncryptCipher(String encryptCipher) {
		this.encryptCipher = encryptCipher;
	}
	
	@Property(viewable = true, order = 6)
	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	@Property(viewable = true, order = 3, editable = true, updatable = true)
	public Cache getCache() {
		return cache;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	@Property(viewable = true, order = 90)
	public int getCopyNum() {
		return copyNum;
	}

	public void setCopyNum(int copyNum) {
		this.copyNum = copyNum;
	}

	@Property(viewable = true, order = 100)
	public String getCopyStrategy() {
		return copyStrategy;
	}

	public void setCopyStrategy(String copyStrategy) {
		this.copyStrategy = copyStrategy;
	}

	@Property(viewable = true, order = 1)
	public long getId() {
		return id;
	}
	

	public DmDataFile getDataFile() {
		return dataFile;
	}

	public void setDataFile(DmDataFile dataFile) {
		this.dataFile = dataFile;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Association
	public Collection<DmDataFile> getFiles(DBRProgressMonitor monitor) throws DBException {
		//return fileCache.getAllObjects(monitor, this);
		return this.isPersisted() ? fileCache.getAllObjects(monitor, this): Collections.singletonList(dataFile);
	}

	public DmDataFile getFile(DBRProgressMonitor monitor, long relativeFileNo) throws DBException {
		for (DmDataFile file : fileCache.getAllObjects(monitor, this)) {
			if (file.getRelativeNo() == relativeFileNo) {
				return file;
			}
		}
		return null;
	}

	@Association
	public Collection<DmSegment<DmTablespace>> getSegments(DBRProgressMonitor monitor) throws DBException {
		return segmentCache.getAllObjects(monitor, this);
	}

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		fileCache.clearCache();
		segmentCache.clearCache();
		return this;
	}

	/**private void loadSizes(DBRProgressMonitor monitor) throws DBException {
		try (final JDBCSession session = DBUtils.openMetaSession(monitor, this,
				"Load tablespace '" + getName() + "' statistics")) {
			availableSize = CommonUtils.toLong(JDBCUtils.queryObject(session,
					"SELECT SUM(F.BYTES) AVAILABLE_SPACE FROM " + DmUtils.getSysSchemaPrefix(getDataSource())
							+ "DBA_DATA_FILES F WHERE F.TABLESPACE_NAME=?",
					getName()));
			usedSize = CommonUtils.toLong(JDBCUtils.queryObject(session, "SELECT SUM(S.BYTES) USED_SPACE FROM "
					+ DmUtils.getSysSchemaPrefix(getDataSource()) + "DBA_SEGMENTS S WHERE S.TABLESPACE_NAME=?",
					getName()));
		} catch (SQLException e) {
			throw new DBException("Can't read tablespace statistics", e, getDataSource());
		}
	}**/

	static class FileCache extends JDBCObjectCache<DmTablespace, DmDataFile> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmTablespace owner)
				throws SQLException {
			String sql="SELECT\r\n"
					+ "    ID,\r\n"
					+ "	PATH,\r\n"
					+ "	SF_GET_PAGE_SIZE() PAGE_SIZE,\r\n"
					+ "	(TOTAL_SIZE*(PAGE_SIZE/1024)/1024) TOTAL_SIZE,\r\n"
					+ "	(FREE_SIZE*(PAGE_SIZE/1024)/1024) FREE_SIZE,\r\n"
					+ "	AUTO_EXTEND,\r\n"
					+ "	NEXT_SIZE,\r\n"
					+ "	MAX_SIZE,\r\n"
					+ "	CLIENT_PATH,\r\n"
					+ "	MIRROR_PATH,\r\n"
					+ "	CREATE_TIME,\r\n"
					+ "	MODIFY_TIME\r\n"
					+ "FROM\r\n"
					+ "	SYS.V$DATAFILE\r\n"
					+ "WHERE\r\n"
					+ "GROUP_ID =?";
			final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
					//.prepareStatement("SELECT * FROM " + DmUtils.getSysSchemaPrefix(owner.getDataSource()) + "DBA_DATA_FILES WHERE TABLESPACE_NAME=? ORDER BY FILE_NAME");
			dbStat.setLong(1, owner.getId());		
			return dbStat;
		}

		@Override
		protected DmDataFile fetchObject(@NotNull JDBCSession session, @NotNull DmTablespace owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmDataFile(owner, resultSet, false);
		}

	}

	static class SegmentCache extends JDBCObjectCache<DmTablespace, DmSegment<DmTablespace>> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmTablespace owner)
				throws SQLException {
			final JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM "
					+ DmUtils.getSysUserViewName(session.getProgressMonitor(), owner.getDataSource(), "SEGMENTS")
					+ " WHERE TABLESPACE_NAME=? ORDER BY SEGMENT_NAME");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected DmSegment<DmTablespace> fetchObject(@NotNull JDBCSession session, @NotNull DmTablespace owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmSegment<>(session.getProgressMonitor(), owner, resultSet);
		}
	}

	static Object resolveTablespaceReference(DBRProgressMonitor monitor, DBSObjectLazy<DmDataSource> referrer,
			@Nullable Object propertyId) throws DBException {
		final DmDataSource dataSource = referrer.getDataSource();
		if (!dataSource.isAdmin()) {
			return referrer.getLazyReference(propertyId);
		} else {
			return DmUtils.resolveLazyReference(monitor, dataSource, dataSource.tablespaceCache, referrer, propertyId);
		}
	}

	public static class TablespaceReferenceValidator implements IPropertyCacheValidator<DBSObjectLazy<DmDataSource>> {
		@Override
		public boolean isPropertyCached(DBSObjectLazy<DmDataSource> object, Object propertyId) {
			return object.getLazyReference(propertyId) instanceof DmTablespace
					|| object.getLazyReference(propertyId) == null
					|| object.getDataSource().tablespaceCache.isFullyCached() || !object.getDataSource().isAdmin();
		}
	}
}
