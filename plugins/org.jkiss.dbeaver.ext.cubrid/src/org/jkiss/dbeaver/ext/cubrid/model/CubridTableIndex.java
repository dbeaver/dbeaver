package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

public class CubridTableIndex extends GenericTableIndex {

	public CubridTableIndex(CubridTable table, boolean nonUnique, String qualifier, long cardinality,
		String indexName, DBSIndexType indexType, boolean persisted) {
		super(table, nonUnique, qualifier, cardinality, indexName, indexType, persisted);
	}

}
