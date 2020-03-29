package org.jkiss.dbeaver.ext.exasol.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class ExasolConsumerGroup extends ExasolPriority implements DBPRefreshableObject, DBPNamedObject2, DBPSaveableObject {

	private ExasolDataSource dataSource;
	private String groupName;
	private Integer cpuWeight;
	private Integer precedence;

	

	private BigDecimal groupRamLimit; 
	private BigDecimal userRamLimit; 
	private BigDecimal sessionRamLimit; 
	private Date created;
	private String comment="";
	private Boolean persisted;
	private BigDecimal groupId = new BigDecimal(-1);
	
	public ExasolConsumerGroup(ExasolDataSource dataSource, String name, Integer precedence, Integer cpuWeight, BigDecimal groupRamLimit, 
			BigDecimal userRamLimit, BigDecimal sessionRamLimit, String comment) {
		super(dataSource, name, comment);
		this.cpuWeight = cpuWeight;
		this.groupRamLimit = groupRamLimit;
		this.sessionRamLimit = sessionRamLimit;
		this.userRamLimit = userRamLimit;
		this.precedence = precedence;
		this.groupName = name;
		this.comment = comment;
	    this.persisted = false;
	    this.dataSource = dataSource;
	}
	
	public ExasolConsumerGroup(ExasolDataSource dataSource, ResultSet dbResult) {
		super(dataSource, "", "");
		this.dataSource = dataSource;
		if (dbResult != null) {
			this.persisted = true;
			this.cpuWeight = JDBCUtils.safeGetInteger(dbResult, "CPU_WEIGHT");
			this.precedence = JDBCUtils.safeGetInteger(dbResult, "PRECEDENCE");
			this.groupRamLimit = JDBCUtils.safeGetBigDecimal(dbResult, "GROUP_TEMP_DB_RAM_LIMIT");
			this.userRamLimit = JDBCUtils.safeGetBigDecimal(dbResult, "USER_TEMP_DB_RAM_LIMIT");
			this.sessionRamLimit = JDBCUtils.safeGetBigDecimal(dbResult, "SESSION_TEMP_DB_RAM_LIMIT");
			
			this.groupName = JDBCUtils.safeGetString(dbResult, "CONSUMER_GROUP_NAME");
			this.created = JDBCUtils.safeGetDate(dbResult, "CREATED");
			this.comment = JDBCUtils.safeGetString(dbResult, "CONSUMER_GROUP_COMMENT");
			this.cpuWeight = JDBCUtils.safeGetInt(dbResult, "CPU_WEIGHT");
			this.groupId = JDBCUtils.safeGetBigDecimal(dbResult, "CONSUMER_GROUP_ID");
			super.setName(groupName);
			super.setDescription(comment);
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

    @Property(viewable = true, editable= true, updatable=true, order = 10)
	@Override
	public String getName() {
		return groupName;
	}

    public void setName(String groupName) {
		this.groupName = groupName;
	}

    @Property(viewable = true, editable= true, updatable=true, order = 20)
	public Integer getPrecedence() {
		return precedence;
	}

	public void setPrecedence(Integer precedence) {
		this.precedence = precedence;
	}

	@Property(viewable = true, editable= false, updatable=false, order = 25)
	public BigDecimal getGroupId()
	{
		return groupId;
	}
    
    @Property(viewable = true, editable= true, updatable=true, order = 30)
	public int getCpuWeight() {
		return cpuWeight;
	}

	public void setCpuWeight(int weight) {
		this.cpuWeight = weight;
	}

    @Property(viewable = true, editable= true, updatable=true, order = 40)
	public BigDecimal getGroupRamLimit() {
		return groupRamLimit;
	}

	public void setGroupRamLimit(BigDecimal groupRamLimit) {
		this.groupRamLimit = groupRamLimit;
	}

    @Property(viewable = true, editable= true, updatable=true, order = 50)
	public BigDecimal getUserRamLimit() {
		return userRamLimit;
	}

	public void setUserRamLimit(BigDecimal userRamLimit) {
		this.userRamLimit = userRamLimit;
	}

    @Property(viewable = true, editable= true, updatable=true, order = 55)
	public BigDecimal getSessionRamLimit() {
		return sessionRamLimit;
	}

	public void setSessionRamLimit(BigDecimal sessionRamLimit) {
		this.sessionRamLimit = sessionRamLimit;
	}
    

	
	@Property(viewable = true, editable= false, updatable=false, order = 60)
    public Date getCreated()
    {
    	return created;
    }
	
	@Override
    @Property(viewable = true, editable= true, updatable=true, order = 70)
	public String getDescription() {
		return comment;
	}

	public void setDescription(String comment) {
		this.comment = comment;
	}

}
