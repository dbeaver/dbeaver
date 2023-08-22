package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;

import org.jkiss.dbeaver.model.struct.rdb.DBSTable;

/**
 * DM Schema Trigger
 * 
 * @author caosw
 *
 */
public class DmSchemaTrigger extends DmTrigger<DmSchema> {

	public DmSchemaTrigger(DmSchema schema, String name) {
		super(schema, name);
	}

	public DmSchemaTrigger(DmSchema schema, ResultSet dbResult) {
		super(schema, dbResult);
	}

	@Override
	public DBSTable getTable() {
		return null;
	}
	
	@Override
	public DmSchema getSchema() {
		return parent;
	}
}
