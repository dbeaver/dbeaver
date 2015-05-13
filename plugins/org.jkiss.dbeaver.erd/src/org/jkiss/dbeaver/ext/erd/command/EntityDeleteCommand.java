/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
/*
 * Created on Jul 17, 2004
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
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
	private ERDEntity entity;
	private EntityDiagram entityDiagram;
	private int index = -1;
	private List<ERDAssociation> foreignKeyRelationships = new ArrayList<ERDAssociation>();
	private List<ERDAssociation> primaryKeyRelationships = new ArrayList<ERDAssociation>();
	private Rectangle bounds;

    public EntityDeleteCommand(EntityDiagram entityDiagram, EntityPart entityPart, Rectangle originalBounds) {
        this.entityDiagram = entityDiagram;
        this.entityPart = entityPart;
        this.entity = entityPart.getTable();
        this.bounds = originalBounds;
    }

    private void deleteRelationships(ERDEntity t)
	{

		this.foreignKeyRelationships.addAll(t.getForeignKeyRelationships());

		//for all relationships where current entity is foreign key
		for (int i = 0; i < foreignKeyRelationships.size(); i++)
		{
			ERDAssociation r = foreignKeyRelationships.get(i);
			r.getPrimaryKeyEntity().removePrimaryKeyRelationship(r, true);
			t.removeForeignKeyRelationship(r, true);
		}

		//for all relationships where current entity is primary key
		this.primaryKeyRelationships.addAll(t.getPrimaryKeyRelationships());
		for (int i = 0; i < primaryKeyRelationships.size(); i++)
		{
			ERDAssociation r = primaryKeyRelationships.get(i);
			r.getForeignKeyEntity().removeForeignKeyRelationship(r, true);
			t.removePrimaryKeyRelationship(r, true);
		}
	}

	/**
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	@Override
    public void execute()
	{
		primExecute();
	}

	/**
	 * Invokes the execution of this command.
	 */
	protected void primExecute()
	{
        // Put entity's bound in init map - it could be used by EntityPart on undo
        entityDiagram.addInitBounds(entity, entityPart.getBounds());

        // Zero bounds - to let modifyBounds reflect on undo
        //entityPart.modifyBounds(new Rectangle(0, 0, 0, 0));

        // Delete entity
		deleteRelationships(entity);
		index = entityDiagram.getEntities().indexOf(entity);
		entityDiagram.removeTable(entity, true);
	}

	/**
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	@Override
    public void redo()
	{
		primExecute();
	}

	private void restoreRelationships()
	{
		for (int i = 0; i < foreignKeyRelationships.size(); i++)
		{
			ERDAssociation r = foreignKeyRelationships.get(i);
			r.getForeignKeyEntity().addForeignKeyRelationship(r, true);
			r.getPrimaryKeyEntity().addPrimaryKeyRelationship(r, true);
		}
		foreignKeyRelationships.clear();
		for (int i = 0; i < primaryKeyRelationships.size(); i++)
		{
			ERDAssociation r = primaryKeyRelationships.get(i);
			r.getForeignKeyEntity().addForeignKeyRelationship(r, true);
			r.getPrimaryKeyEntity().addPrimaryKeyRelationship(r, true);
		}
		primaryKeyRelationships.clear();
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	@Override
    public void undo()
	{
		entityDiagram.addTable(entity, index, true);
		restoreRelationships();
		//entityPart.modifyBounds(bounds);
	}

}

