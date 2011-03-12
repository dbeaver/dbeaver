/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 17, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to delete tables from the schema
 * 
 * @author Serge Rieder
 */
public class EntityDeleteCommand extends Command
{

    private EntityPart entityPart;
	private ERDTable table;
	private EntityDiagram entityDiagram;
	private int index = -1;
	private List<ERDAssociation> foreignKeyRelationships = new ArrayList<ERDAssociation>();
	private List<ERDAssociation> primaryKeyRelationships = new ArrayList<ERDAssociation>();
	private Rectangle bounds;

    public EntityDeleteCommand(EntityDiagram entityDiagram, EntityPart entityPart, Rectangle originalBounds) {
        this.entityDiagram = entityDiagram;
        this.entityPart = entityPart;
        this.table = entityPart.getTable();
        this.bounds = originalBounds;
    }

    private void deleteRelationships(ERDTable t)
	{

		this.foreignKeyRelationships.addAll(t.getForeignKeyRelationships());

		//for all relationships where current table is foreign key
		for (int i = 0; i < foreignKeyRelationships.size(); i++)
		{
			ERDAssociation r = foreignKeyRelationships.get(i);
			r.getPrimaryKeyTable().removePrimaryKeyRelationship(r, true);
			t.removeForeignKeyRelationship(r, true);
		}

		//for all relationships where current table is primary key
		this.primaryKeyRelationships.addAll(t.getPrimaryKeyRelationships());
		for (int i = 0; i < primaryKeyRelationships.size(); i++)
		{
			ERDAssociation r = primaryKeyRelationships.get(i);
			r.getForeignKeyTable().removeForeignKeyRelationship(r, true);
			t.removePrimaryKeyRelationship(r, true);
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
        entityPart.modifyBounds(new Rectangle(0, 0, 0, 0));

		deleteRelationships(table);
		index = entityDiagram.getTables().indexOf(table);
		entityDiagram.removeTable(table, true);
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
			ERDAssociation r = foreignKeyRelationships.get(i);
			r.getForeignKeyTable().addForeignKeyRelationship(r, true);
			r.getPrimaryKeyTable().addPrimaryKeyRelationship(r, true);
		}
		foreignKeyRelationships.clear();
		for (int i = 0; i < primaryKeyRelationships.size(); i++)
		{
			ERDAssociation r = primaryKeyRelationships.get(i);
			r.getForeignKeyTable().addForeignKeyRelationship(r, true);
			r.getPrimaryKeyTable().addPrimaryKeyRelationship(r, true);
		}
		primaryKeyRelationships.clear();
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo()
	{
		entityDiagram.addTable(table, index, true);
		restoreRelationships();
		//entityPart.modifyBounds(bounds);
	}

}

