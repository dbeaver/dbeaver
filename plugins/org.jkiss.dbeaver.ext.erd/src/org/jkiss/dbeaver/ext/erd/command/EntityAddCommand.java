/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.ext.erd.model.ERDUtils;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.List;

/**
 * Add entity to diagram
 */
public class EntityAddCommand extends Command
{

    protected DiagramPart diagramPart;
	protected List<ERDEntity> entities;
    protected Point location;

    public EntityAddCommand(DiagramPart diagram, List<ERDEntity> entities, Point location)
    {
        this.diagramPart = diagram;
        this.entities = entities;
        this.location = location;
    }

    public DiagramPart getDiagram() {
        return diagramPart;
    }

    @Override
    public void execute()
	{
        VoidProgressMonitor monitor = new VoidProgressMonitor();

        Point curLocation = location == null ? null : new Point(location);
        for (ERDEntity entity : entities) {
            boolean resolveRelations = false;
            if (entity.getObject() == null) {
                // Entity is not initialized
                if (entity.getDataSource() != null) {
                    DBSObject selectedObject = DBUtils.getSelectedObject(entity.getDataSource(), true);
                    DBNDatabaseNode dsNode = NavigatorUtils.getNodeByObject(selectedObject != null ? selectedObject : entity.getDataSource().getContainer());
                    if (dsNode != null) {
                        DBNNode tableNode = DBUserInterface.getInstance().selectObject(
                                UIUtils.getActiveWorkbenchShell(),
                                "Select a table",
                                dsNode,
                                null,
                                new Class[]{DBSTable.class},
                                new Class[]{DBSTable.class});
                        if (tableNode instanceof DBNDatabaseNode && ((DBNDatabaseNode) tableNode).getObject() instanceof DBSEntity) {
                            entity = ERDUtils.makeEntityFromObject(
                                    monitor,
                                    diagramPart.getDiagram(),
                                    (DBSEntity)((DBNDatabaseNode) tableNode).getObject(),
                                null);
                            // This actually only loads unresolved relations.
                            // This happens only with entities added on diagram during editing
                            entity.addModelRelations(monitor, diagramPart.getDiagram(), false, false);
                        }
                    }
                }
            }
            if (entity.getObject() == null) {
                continue;
            }
		    diagramPart.getDiagram().addEntity(entity, true);

            if (curLocation != null) {
                // Put new entities in specified location
                for (Object diagramChild : diagramPart.getChildren()) {
                    if (diagramChild instanceof EntityPart) {
                        EntityPart entityPart = (EntityPart) diagramChild;
                        if (entityPart.getEntity() == entity) {
                            final Rectangle newBounds = new Rectangle();
                            final Dimension size = entityPart.getFigure().getPreferredSize();
                            newBounds.x = curLocation.x;
                            newBounds.y = curLocation.y;
                            newBounds.width = size.width;
                            newBounds.height = size.height;
                            entityPart.modifyBounds(newBounds);

                            curLocation.x += size.width + (size.width / 2);
                            break;
                        }
                    }
                }
            }

            handleEntityChange(entity, false);
        }
	}

    @Override
    public void undo()
    {
        for (ERDEntity entity : entities) {
            diagramPart.getDiagram().removeEntity(entity, true);
            handleEntityChange(entity, true);
        }
    }

    protected void handleEntityChange(ERDEntity entity, boolean remove) {
        // Nothing special
    }

}