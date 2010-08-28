/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

/**
 * Column entry in model Table
 * @author Phil Zoio
 */
public class Column extends PropertyAwareObject
{

	private String name;
	private String type;

	
	public Column()
	{
		super();
	}	
	
	public Column(String name, String type)
	{
		super();
		this.name = name;
		this.type = type;
	}


	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name)
	{
		String oldName = this.name;
		this.name = name;
	}

	/**
	 * @return Returns the type.
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * @param type
	 *            The type to set.
	 */
	public void setType(String type)
	{
		String oldType = this.type;
		this.type = type;
		firePropertyChange(NAME, null, type);
	}
	
	
	public String getLabelText()
	{
		String labelText = getName() + ":" + getType();
		return labelText;
	}
	
}