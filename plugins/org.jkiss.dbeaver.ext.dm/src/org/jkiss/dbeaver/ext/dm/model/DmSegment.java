package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

/**
 * Dm Segments
 * 
 * @author caosw
 */
public class DmSegment<PARENT extends DBSObject> extends DmObject<PARENT> {

	private String segmentType;
	private String partitionName;
	private long bytes;
	private long blocks;
	private DmSchema schema;
	private DmDataFile file;

	protected DmSegment(DBRProgressMonitor monitor, PARENT parent, ResultSet dbResult) throws DBException {
		super(parent, JDBCUtils.safeGetStringTrimmed(dbResult, "SEGMENT_NAME"), true);
		this.segmentType = JDBCUtils.safeGetStringTrimmed(dbResult, "SEGMENT_TYPE");
		this.partitionName = JDBCUtils.safeGetStringTrimmed(dbResult, "PARTITION_NAME");
		this.bytes = JDBCUtils.safeGetLong(dbResult, "BYTES");
		this.blocks = JDBCUtils.safeGetLong(dbResult, "BLOCKS");
		final long fileNo = JDBCUtils.safeGetInt(dbResult, "RELATIVE_FNO");
		final Object tablespace = getTablespace(monitor);
		if (tablespace instanceof DmTablespace) {
			this.file = ((DmTablespace) tablespace).getFile(monitor, fileNo);
		}
		if (getDataSource().isAdmin()) {
			String ownerName = JDBCUtils.safeGetStringTrimmed(dbResult, "OWNER");
			if (!CommonUtils.isEmpty(ownerName)) {
				schema = getDataSource().getSchema(monitor, ownerName);
			}
		}
	}

	public Object getTablespace(DBRProgressMonitor monitor) throws DBException {
		if (parent instanceof DmTablespace) {
			return parent;
		} else if (parent instanceof DmPartitionBase) {
			return ((DmPartitionBase) parent).getTablespace(monitor);
		} else {
			return null;
		}
	}

	@Property(viewable = true, editable = true, order = 2)
	public DmSchema getSchema() {
		return schema;
	}

	@Property(viewable = true, editable = true, order = 3)
	public String getSegmentType() {
		return segmentType;
	}

	@Property(viewable = true, editable = true, order = 4)
	public String getPartitionName() {
		return partitionName;
	}

	@Property(viewable = true, editable = true, order = 5)
	public long getBytes() {
		return bytes;
	}

	@Property(viewable = true, editable = true, order = 6)
	public long getBlocks() {
		return blocks;
	}

	@Property(order = 7)
	public DmDataFile getFile() {
		return file;
	}
}
