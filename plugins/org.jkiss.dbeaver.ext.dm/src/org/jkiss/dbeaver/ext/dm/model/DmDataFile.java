package org.jkiss.dbeaver.ext.dm.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;

import org.eclipse.jface.text.templates.GlobalTemplateVariables.User;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

/**
 * Dm tablespace file
 * 
 * @author caosw
 *
 */
public class DmDataFile extends DmObject<DmTablespace> {

	public enum OnlineStatus {
		SYSOFF, SYSTEM, OFFLINE, ONLINE, RECOVER,
	}

	
	private final DmTablespace tablespace;
	private long id;
	private long relativeNo;
	private String file;//文件
	private String filePath = ""; //文件路径
	private String clientPath = "";//客户端路径
	private String oldfile;//旧路径
	private String mirrorPath = ""; //镜像路径
	private long totalSize = 32;//总大小
	private long freeSize = 32; //空闲大小
	private static boolean autoExtensible=true; //是否自动扩容
	private long nextExtSize = 0; //扩充尺寸
	private long maxExtSize = 0;//扩充上限
	private float usedRatio;//使用率
	private String createTime;//创建时间
	private String modifyTime;//修改时间
	
	
	//private BigDecimal bytes;
	//private BigDecimal blocks;
	//private BigDecimal maxBytes;
	//private BigDecimal maxBlocks;
	//private long incrementBy;
	//private BigDecimal userBytes;
	//private BigDecimal userBlocks;
	//private boolean available;
	
	//private OnlineStatus onlineStatus;
	private boolean temporary;

	
	
	public DmDataFile(DmDataSource dataSource, DmTablespace tablespace) { //创建默认表空间
		super(tablespace, "dmxx.dbf", false);
		this.tablespace = tablespace;
		this.name="dmxx.dbf";
		this.filePath=getDataSource().getDefaultPath();
		this.id=-1;
		this.totalSize=32;
		this.nextExtSize=10;
		this.maxExtSize=16777215;
		this.autoExtensible=true;
		this.oldfile=filePath+name;
	}

	protected DmDataFile(DmTablespace tablespace, ResultSet dbResult, boolean temporary) {
		super(tablespace,"", true);//JDBCUtils.safeGetString(dbResult, "PATH")
		this.tablespace = tablespace;
		this.temporary = temporary;
		this.id = JDBCUtils.safeGetLong(dbResult, "ID");
		this.relativeNo = JDBCUtils.safeGetLong(dbResult, "ID");
		this.file = JDBCUtils.safeGetString(dbResult, "PATH");
		String[] files=file.split("/"); 
		this.name=files[files.length-1];
		this.oldfile=file;
		this.filePath=file.substring(0,file.indexOf(name));
		this.clientPath=JDBCUtils.safeGetString(dbResult, "CLIENT_PATH");
		this.mirrorPath=JDBCUtils.safeGetString(dbResult, "MIRROR_PATH");
		this.maxExtSize=JDBCUtils.safeGetLong(dbResult, "MAX_SIZE");
		this.nextExtSize=JDBCUtils.safeGetLong(dbResult, "NEXT_SIZE");
		this.createTime=JDBCUtils.safeGetString(dbResult, "CREATE_TIME");
		this.modifyTime=JDBCUtils.safeGetString(dbResult, "MODIFY_TIME");
	    this.totalSize=JDBCUtils.safeGetLong(dbResult, "TOTAL_SIZE");
	    this.freeSize=JDBCUtils.safeGetLong(dbResult, "FREE_SIZE");
	    int auto=JDBCUtils.safeGetInt(dbResult, "AUTO_EXTEND");
		this.autoExtensible=(auto==1);
		try {
			long usedSize=totalSize-freeSize;
			BigDecimal usedDecimal=new BigDecimal(usedSize);
			BigDecimal totalDecimal=new BigDecimal(totalSize);
			this.usedRatio=(usedDecimal.divide(totalDecimal,5,RoundingMode.HALF_UP).floatValue()*100);
		} catch (Exception e) {
			this.usedRatio=0;
		}

		//this.bytes = JDBCUtils.safeGetBigDecimal(dbResult, "BYTES");
		//this.blocks = JDBCUtils.safeGetBigDecimal(dbResult, "BLOCKS");   
		//this.maxBytes = JDBCUtils.safeGetBigDecimal(dbResult, "MAXBYTES");
		//this.maxBlocks = JDBCUtils.safeGetBigDecimal(dbResult, "MAXBLOCKS");
		//this.incrementBy = JDBCUtils.safeGetLong(dbResult, "INCREMENT_BY");
		//this.userBytes = JDBCUtils.safeGetBigDecimal(dbResult, "USER_BYTES");
		//this.userBlocks = JDBCUtils.safeGetBigDecimal(dbResult, "USER_BLOCKS");	
		//this.autoExtensible = JDBCUtils.safeGetBoolean(dbResult, "AUTOEXTENSIBLE", "Y");
		//this.available = "AVAILABLE".equals(JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS"));
		/**if (!this.temporary) {
			this.onlineStatus = CommonUtils.valueOf(OnlineStatus.class,
					JDBCUtils.safeGetStringTrimmed(dbResult, "ONLINE_STATUS"));    
		}*/
	}

	public DmTablespace getTablespace() {
		return tablespace;
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = true, updatable = true, order = 1)
	public String getName() {
		return name;
	}

	@Property(order = 2)
	public long getId() {
		return id;
	}

	@Property(order = 3)
	public long getRelativeNo() {
		return relativeNo;
	}

	public String getFile() {
		return filePath+name;
	}

	public void setFile(String file) {
		this.file = file;
	}

	 @Property(viewable = true, editable = false, order = 4)
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getClientPath() {
		return clientPath;
	}

	public void setClientPath(String clientPath) {
		this.clientPath = clientPath;
	}

	public String getOldfile() {
		return oldfile;
	}

	public void setOldfile(String oldfile) {
		this.oldfile = oldfile;
	}

	 @Property(viewable = true, editable = true, order = 100)
	public String getMirrorPath() {
		return mirrorPath;
	}

	public void setMirrorPath(String mirrorPath) {
		this.mirrorPath = mirrorPath;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 5)
	public long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}

	@Property(viewable = true, order =6)
	public long getFreeSize() {
		return freeSize;
	}

	public void setFreeSize(long freeSize) {
		this.freeSize = freeSize;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 10)
	public long getNextExtSize() {
		return nextExtSize;
	}

	public void setNextExtSize(long nextExtSize) {
		this.nextExtSize = nextExtSize;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 11)
	public long getMaxExtSize() {
		return maxExtSize;
	}

	public void setMaxExtSize(long maxExtSize) {
		this.maxExtSize = maxExtSize;
	}

	@Property(viewable = true, order =7)
	public float getUsedRatio() {
		return usedRatio;
	}

	public void setUsedRatio(float usedRatio) {
		this.usedRatio = usedRatio;
	}

	@Property(viewable = true, order =12)
	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	@Property(viewable = true, order =14)
	public String getModifyTime() {
		return modifyTime;
	}

	public void setModifyTime(String modifyTime) {
		this.modifyTime = modifyTime;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setRelativeNo(long relativeNo) {
		this.relativeNo = relativeNo;
	}

	public void setAutoExtensible(boolean autoExtensible) {
		this.autoExtensible = autoExtensible;
	}

	/**public void setTemporary(boolean temporary) {
		this.temporary = temporary;
	}*/

	@Property(viewable = true,  editable = true, updatable = true,order = 8)
	public boolean isAutoExtensible() {
		return autoExtensible;
	}

	/**@Property(order = 14)
	public boolean isTemporary() {
		return temporary;
	}*/

}
