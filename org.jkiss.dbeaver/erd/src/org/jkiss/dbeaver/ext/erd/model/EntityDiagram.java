/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Schema in the model. Note that this class also includes
 * diagram specific information (layoutManualDesired and layoutManualAllowed fields)
 * although ideally these should be in a separate model hiearchy 
 * @author Phil Zoio
 */
public class EntityDiagram extends PropertyAwareObject
{

	private String name;
	private List<Table> tables = new ArrayList<Table>();
	private boolean layoutManualDesired = true;
	private boolean layoutManualAllowed = false;

	public EntityDiagram(String name)
	{
		super();
		if (name == null)
			throw new NullPointerException("Name cannot be null");
		this.name = name;
	}

	public void addTable(Table table)
	{
		tables.add(table);
		firePropertyChange(CHILD, null, table);
	}

	public void addTable(Table table, int i)
	{
		tables.add(i, table);
		firePropertyChange(CHILD, null, table);
	}

	public void removeTable(Table table)
	{
		tables.remove(table);
		firePropertyChange(CHILD, table, null);
	}

    /**
	 * @return the Tables for the current schema
	 */
	public List getTables()
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
		firePropertyChange(LAYOUT, null, layoutManualDesired);
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
}