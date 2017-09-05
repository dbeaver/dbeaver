/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @author Serge Rider
 */
public class EntityDeleteCommand extends Command
{

    private EntityPart entityPart;
	private ERDEntity entity;
	private EntityDiagram entityDiagram;
	private int index = -1;
	private List<ERDAssociation> foreignKeyRelationships = new ArrayList<>();
	private List<ERDAssociation> primaryKeyRelationships = new ArrayList<>();
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
        entityDiagram.getVisualInfo(entity, true).initBounds = entityPart.getBounds();

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

