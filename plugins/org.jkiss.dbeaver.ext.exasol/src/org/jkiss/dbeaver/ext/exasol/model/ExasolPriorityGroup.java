package org.jkiss.dbeaver.ext.exasol.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class ExasolPriorityGroup implements DBPRefreshableObject, DBPNamedObject2, DBPSaveableObject {

	private ExasolDataSource dataSource;
	private String groupName;
	private Date created;
	private String comment="";
	private Boolean persisted;
	private int weight;
	private BigDecimal groupId = new BigDecimal(-1);
	
	public ExasolPriorityGroup(ExasolDataSource dataSource, String name, String comment, int weight ) {
		this.groupName = name;
		this.comment = comment;
		this.weight = weight;
	    this.persisted = false;
	    this.dataSource = dataSource;
	}
	
	public ExasolPriorityGroup(ExasolDataSource dataSource, ResultSet dbResult) {
		this.dataSource = dataSource;
		if (dbResult != null) {
			this.persisted = true;
			this.groupName = JDBCUtils.safeGetString(dbResult, "PRIORITY_GROUP_NAME");
			this.created = JDBCUtils.safeGetDate(dbResult, "CREATED");
			this.comment = JDBCUtils.safeGetString(dbResult, "PRIORITY_GROUP_COMMENT");
			this.weight = JDBCUtils.safeGetInt(dbResult, "PRIORITY_GROUP_WEIGHT");
			this.groupId = JDBCUtils.safeGetBigDecimal(dbResult, "PRIORITY_GROUP_ID");
		}
	}
	
	@Override
	public DBSObject getParentObject()
	{
		return this.dataSource.getContainer();
	}

	@Override
	public ExasolDataSource getDataSource()
	{
		return this.dataSource;
	}

	@Override
	public boolean isPersisted()
	{
		return this.persisted;
	}

	@Override
	public void setPersisted(boolean persisted)
	{
		this.persisted = persisted;

	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
			throws DBException
	{
        ((ExasolDataSource) getDataSource()).refreshObject(monitor);
		return this;
	}

	public void setName(String groupName) {
		this.groupName = groupName;
	}

    @Property(viewable = true, editable= true, updatable=true, order = 20)
	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	@Override
    @Property(viewable = true, editable= true, updatable=true, order = 30)
	public String getDescription() {
		return comment;
	}

    @Property(viewable = true, editable= false, updatable=false, order = 40)
	public BigDecimal getGroupId()
	{
		return groupId;
	}
	
	
    @Property(viewable = true, editable= true, updatable=true, order = 10)
	@Override
	public String getName() {
		return groupName;
	}

    


}
