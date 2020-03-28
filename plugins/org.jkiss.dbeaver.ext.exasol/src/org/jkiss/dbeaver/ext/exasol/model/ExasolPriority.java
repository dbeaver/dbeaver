package org.jkiss.dbeaver.ext.exasol.model;

import java.math.BigDecimal;
import java.sql.Date;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class ExasolPriority implements DBPRefreshableObject, DBPNamedObject2, DBPSaveableObject {

	private ExasolDataSource dataSource;
	private String groupName;
	private Date created;
	private String comment="";
	private Boolean persisted;
	private BigDecimal groupId = new BigDecimal(-1);
	
	public ExasolPriority(ExasolDataSource dataSource, String name, String comment ) {
		this.groupName = name;
		this.comment = comment;
	    this.persisted = false;
	    this.dataSource = dataSource;
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
	
	public void setDescription(String description)
	{
		this.comment = description;
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
	
    @Property(viewable = true, editable= false, updatable=false, order = 40)
    public Date getCreated()
    {
    	return created;
    }
	
    @Property(viewable = true, editable= true, updatable=true, order = 10)
	@Override
	public String getName() {
		return groupName;
	}

    


}
