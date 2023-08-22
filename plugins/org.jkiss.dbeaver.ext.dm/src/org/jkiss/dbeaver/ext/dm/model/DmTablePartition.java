package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.util.Collection;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class DmTablePartition extends DmPartitionBase<DmTablePhysical> {

	protected DmTablePartition(
	        DmTablePhysical DmTable,
	        boolean subpartition,
	        ResultSet dbResult)
	    {
	        super(DmTable, subpartition, dbResult);
	    }

	@Association
	public Collection<DmTablePartition> getSubPartitions(DBRProgressMonitor monitor) throws DBException {
		return getParentObject().getSubPartitions(monitor, this);
	}
}
