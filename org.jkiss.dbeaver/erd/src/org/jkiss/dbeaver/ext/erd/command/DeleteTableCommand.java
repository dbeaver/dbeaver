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
import org.jkiss.dbeaver.ext.erd.part.TablePart;

/**
 * Command to delete tables from the schema
 * 
 * @author Phil Zoio
 */
public class DeleteTableCommand extends Command
{

    private TablePart tablePart;
	private Table table;
	private Schema schema;
	private int index = -1;
	private List<Relationship> foreignKeyRelationships = new ArrayList<Relationship>();
	private List<Relationship> primaryKeyRelationships = new ArrayList<Relationship>();
	private Rectangle bounds;

    public DeleteTableCommand(Schema schema, TablePart tablePart, Rectangle originalBounds) {
        this.schema = schema;
        this.tablePart = tablePart;
        this.table = tablePart.getTable();
        this.bounds = originalBounds;
    }

    private void deleteRelationships(Table t)
	{

		this.foreignKeyRelationships.addAll(t.getForeignKeyRelationships());

		//for all relationships where current table is foreign key
		for (int i = 0; i < foreignKeyRelationships.size(); i++)
		{
			Relationship r = foreignKeyRelationships.get(i);
			r.getPrimaryKeyTable().removePrimaryKeyRelationship(r);
			t.removeForeignKeyRelationship(r);
		}

		//for all relationships where current table is primary key
		this.primaryKeyRelationships.addAll(t.getPrimaryKeyRelationships());
		for (int i = 0; i < primaryKeyRelationships.size(); i++)
		{
			Relationship r = primaryKeyRelationships.get(i);
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
        tablePart.modifyBounds(new Rectangle(0, 0, 0, 0));

		deleteRelationships(table);
		index = schema.getTables().indexOf(table);
		schema.removeTable(table);
        // Zero bounds - to let modifyBounds reflect on undo
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
			Relationship r = foreignKeyRelationships.get(i);
			r.getForeignKeyTable().addForeignKeyRelationship(r);
			r.getPrimaryKeyTable().addPrimaryKeyRelationship(r);
		}
		foreignKeyRelationships.clear();
		for (int i = 0; i < primaryKeyRelationships.size(); i++)
		{
			Relationship r = primaryKeyRelationships.get(i);
			r.getForeignKeyTable().addForeignKeyRelationship(r);
			r.getPrimaryKeyTable().addPrimaryKeyRelationship(r);
		}
		primaryKeyRelationships.clear();
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo()
	{
		schema.addTable(table, index);
		restoreRelationships();
		//tablePart.modifyBounds(bounds);
	}

}

