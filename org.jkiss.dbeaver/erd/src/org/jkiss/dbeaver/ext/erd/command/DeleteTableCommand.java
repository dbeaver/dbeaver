/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 17, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;

import org.jkiss.dbeaver.ext.erd.model.Relationship;
import org.jkiss.dbeaver.ext.erd.model.Schema;
import org.jkiss.dbeaver.ext.erd.model.Table;

/**
 * Command to delete tables from the schema
 * 
 * @author Phil Zoio
 */
public class DeleteTableCommand extends Command
{

	private Table table;
	private Schema schema;
	private int index = -1;
	private List foreignKeyRelationships = new ArrayList();
	private List primaryKeyRelationships = new ArrayList();
	private Rectangle bounds;

	private void deleteRelationships(Table t)
	{

		this.foreignKeyRelationships.addAll(t.getForeignKeyRelationships());

		//for all relationships where current table is foreign key
		for (int i = 0; i < foreignKeyRelationships.size(); i++)
		{
			Relationship r = (Relationship) foreignKeyRelationships.get(i);
			r.getPrimaryKeyTable().removePrimaryKeyRelationship(r);
			t.removeForeignKeyRelationship(r);
		}

		//for all relationships where current table is primary key
		this.primaryKeyRelationships.addAll(t.getPrimaryKeyRelationships());
		for (int i = 0; i < primaryKeyRelationships.size(); i++)
		{
			Relationship r = (Relationship) primaryKeyRelationships.get(i);
			r.getForeignKeyTable().removeForeignKeyRelationship(r);
			t.removePrimaryKeyRelationship(r);
		}
	}

	/**
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute()
	{
		primExecute();
	}

	/**
	 * Invokes the execution of this command.
	 */
	protected void primExecute()
	{
		deleteRelationships(table);
		index = schema.getTables().indexOf(table);
		schema.removeTable(table);
	}

	/**
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo()
	{
		primExecute();
	}

	private void restoreRelationships()
	{
		for (int i = 0; i < foreignKeyRelationships.size(); i++)
		{
			Relationship r = (Relationship) foreignKeyRelationships.get(i);
			r.getForeignKeyTable().addForeignKeyRelationship(r);
			r.getPrimaryKeyTable().addPrimaryKeyRelationship(r);
		}
		foreignKeyRelationships.clear();
		for (int i = 0; i < primaryKeyRelationships.size(); i++)
		{
			Relationship r = (Relationship) primaryKeyRelationships.get(i);
			r.getForeignKeyTable().addForeignKeyRelationship(r);
			r.getPrimaryKeyTable().addPrimaryKeyRelationship(r);
		}
		primaryKeyRelationships.clear();
	}

	/**
	 * Sets the child to the passed Table
	 * 
	 * @param a
	 *            the child
	 */
	public void setTable(Table a)
	{
		table = a;
	}

	/**
	 * Sets the parent to the passed Schema
	 * 
	 * @param sa
	 *            the parent
	 */
	public void setSchema(Schema sa)
	{
		schema = sa;
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo()
	{
		schema.addTable(table, index);
		restoreRelationships();
		table.modifyBounds(bounds);
	}

	/**
	 * Sets the original bounds for the table so that these can be restored
	 */
	public void setOriginalBounds(Rectangle bounds)
	{
		this.bounds = bounds;
	}

}

