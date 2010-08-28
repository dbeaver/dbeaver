/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 18, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;

import org.jkiss.dbeaver.ext.erd.model.Column;

/**
 * Command to change the name and type text field
 * 
 * @author Phil Zoio
 */
public class ResetNameTypeForColumnCommand extends Command
{

	private Column source;
	private String name, oldName;
	private String type, oldType;

	/**
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute()
	{
		source.setName(name);
		source.setType(type);
	}

	/**
	 * @return whether we can apply changes
	 */
	public boolean canExecute()
	{
		if (name != null && type != null)
		{
			return true;
		}
		else
		{
			name = oldName;
			type = oldType;
			return false;
		}
	}

	/**
	 * Sets the new Column name
	 * 
	 * @param string
	 *            the new name
	 */
	public void setNameType(String string)
	{
		String oldName = this.name;
		String oldType = this.type;

		if (string != null)
		{
			int colonIndex = string.indexOf(':');
			if (colonIndex >= 0)
			{
				name = string.substring(0, colonIndex);
				if (string.length() > colonIndex + 1)
				{
					this.type = string.substring(colonIndex + 1);
				}
			}
		}
		if (this.type == null)
		{
			this.name = oldName;
			this.type = oldType;
		}
	}

	/**
	 * Sets the old Column name
	 * 
	 * @param string
	 *            the old name
	 */
	public void setOldName(String string)
	{
		oldName = string;
	}

	/**
	 * Sets the source Column
	 * 
	 * @param column
	 *            the source Column
	 */
	public void setSource(Column column)
	{
		source = column;
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo()
	{
		source.setName(oldName);
		source.setType(oldType);
	}

	/**
	 * @param sets
	 *            the old type
	 */
	public void setOldType(String type)
	{
		this.oldType = type;
	}

}