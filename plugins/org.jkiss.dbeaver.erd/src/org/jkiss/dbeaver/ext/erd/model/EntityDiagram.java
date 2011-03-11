/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.model.struct.DBSObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Schema in the model. Note that this class also includes
 * diagram specific information (layoutManualDesired and layoutManualAllowed fields)
 * although ideally these should be in a separate model hierarchy
 * @author Serge Rieder
 */
public class EntityDiagram extends ERDObject<DBSObject>
{

	private String name;
	private List<ERDTable> tables = new ArrayList<ERDTable>();
	private boolean layoutManualDesired = true;
	private boolean layoutManualAllowed = false;

	public EntityDiagram(DBSObject container, String name)
	{
		super(container);
		if (name == null)
			throw new NullPointerException("Name cannot be null");
		this.name = name;
	}

	public synchronized void addTable(ERDTable table)
	{
		tables.add(table);
		//firePropertyChange(CHILD, null, table);
	}

	public synchronized void addTable(ERDTable table, int i)
	{
		tables.add(i, table);
		//firePropertyChange(CHILD, null, table);
	}

	public synchronized void removeTable(ERDTable table)
	{
		tables.remove(table);
		//firePropertyChange(CHILD, table, null);
	}

    /**
	 * @return the Tables for the current schema
	 */
	public synchronized List<ERDTable> getTables()
	{
		return tables;
	}

	/**
	 * @return the name of the schema
	 */
	public String getName()
	{
		return name;
	}

    public void setName(String name)
    {
        this.name = name;
    }

	/**
	 * @param layoutManualAllowed
	 *            The layoutManualAllowed to set.
	 */
	public void setLayoutManualAllowed(boolean layoutManualAllowed)
	{
		this.layoutManualAllowed = layoutManualAllowed;
	}

	/**
	 * @return Returns the layoutManualDesired.
	 */
	public boolean isLayoutManualDesired()
	{
		return layoutManualDesired;
	}

	/**
	 * @param layoutManualDesired
	 *            The layoutManualDesired to set.
	 */
	public void setLayoutManualDesired(boolean layoutManualDesired)
	{
		this.layoutManualDesired = layoutManualDesired;
	}

	/**
	 * @return Returns whether we can lay out individual tables manually using the XYLayout
	 */
	public boolean isLayoutManualAllowed()
	{
		return layoutManualAllowed;
	}

    public int getEntityCount() {
        return tables.size();
    }

    public void load(InputStream in)
        throws IOException
    {

    }

    public void save(OutputStream out)
        throws IOException
    {

    }

    public EntityDiagram copy()
    {
        EntityDiagram copy = new EntityDiagram(getObject(), getName());
        copy.tables.addAll(this.tables);
        copy.layoutManualDesired = this.layoutManualDesired;
        copy.layoutManualAllowed = this.layoutManualAllowed;
        return copy;
    }
}