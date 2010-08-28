/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;

import org.jkiss.dbeaver.ext.erd.model.Column;
import org.jkiss.dbeaver.ext.erd.model.Schema;
import org.jkiss.dbeaver.ext.erd.model.Table;

/**
 * Command to create a new table table
 * 
 * @author Phil Zoio
 */
public class TableAddCommand extends Command
{

	private Schema schema;
	private Table table;
	private int index = -1;

	/**
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute()
	{
		this.table.setName("TABLE " + (schema.getTables().size() + 1));
		this.table.setSchema(schema);
		if (table.getColumns().size() < 2)
		{
			Column column1 = new Column("VARCHAR_FIELD", "VARCHAR");
			Column column2 = new Column("NUMBER_FIELD", "INTEGER");
			table.addColumn(column1);
			table.addColumn(column2);
		}
		schema.addTable(table);
	}

	/**
	 * Sets the index to the passed value
	 * 
	 * @param i
	 *            the index
	 */
	public void setIndex(int i)
	{
		index = i;
	}

	/**
	 * Sets the parent ActivityDiagram
	 */
	public void setSchema(Schema schema)
	{
		this.schema = schema;
	}

	/**
	 * Sets the Activity to create
	 * 
	 * @param table
	 *            the Activity to create
	 */
	public void setTable(Table table)
	{
		this.table = table;
	}

	public void undo()
	{
		schema.removeTable(table);
	}

}