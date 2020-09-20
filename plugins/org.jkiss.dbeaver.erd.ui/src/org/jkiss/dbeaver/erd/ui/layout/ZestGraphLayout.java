/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.erd.ui.layout;

/*
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.Log;
import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

import org.eclipse.zest.layouts.*;
import org.eclipse.zest.layouts.algorithms.*;
import org.eclipse.zest.layouts.exampleStructures.SimpleNode;
import org.eclipse.zest.layouts.exampleStructures.SimpleRelationship;

import org.jkiss.dbeaver.erd.ui.part.EntityPart;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
*/

/**
 * ZestGraphLayout
 * @author Serge Rider
 */
public class ZestGraphLayout
{

/*
    private static final Log log = Log.getLog(ZestGraphLayout.class);

    List<LayoutEntity> entities = new ArrayList<LayoutEntity>();
    List<LayoutRelationship> relationships = new ArrayList<LayoutRelationship>();

	Map<EntityPart, LayoutEntity> partToNodesMap;
    Map<ConnectionEditPart, LayoutRelationship> connectionToRelationshipsMap;

	public void layoutDiagram(AbstractGraphicalEditPart diagram)
	{
        //algorithm = new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
        //algorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
        //algorithm = new DirectedGraphLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
        //algorithm = new RadialLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);

        LayoutAlgorithm algorithm = new
            CompositeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING,
            new LayoutAlgorithm[] {
                new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING),
                new HorizontalShift(LayoutStyles.NO_LAYOUT_NODE_RESIZING)
            });

		partToNodesMap = new IdentityHashMap<EntityPart, LayoutEntity>();
        connectionToRelationshipsMap = new IdentityHashMap<ConnectionEditPart, LayoutRelationship>();
		
		addDiagramNodes(diagram);
		if (!entities.isEmpty())
		{	
			addDiagramEdges(diagram);
		}
        final Rectangle diagramBounds = diagram.getContentPane().getParent().getBounds();
        try {
            algorithm.applyLayout(
                entities.toArray(new LayoutEntity[entities.size()]),
                relationships.toArray(new LayoutRelationship[relationships.size()]),
                0,
                0,
                400,
                400,
                false,
                false);
        } catch (InvalidLayoutConfiguration e) {
            e.printStackTrace();
        }

        for (LayoutEntity entity : entities) {
            EntityPart part = getPartByEntity(entity);
            final Rectangle rect = new Rectangle((int) entity.getXInLayout(), (int) entity.getYInLayout(), (int) entity.getWidthInLayout(), (int) entity.getHeightInLayout());
            part.getFigure().setBounds(rect);
        }
        for (LayoutRelationship relationship : relationships) {
            AbstractConnectionEditPart part = (AbstractConnectionEditPart)getConnectionPartByRelationship(relationship);
            PolylineConnection conn = (PolylineConnection) part.getConnectionFigure();
            final LayoutBendPoint[] bendPoints = ((SimpleRelationship) relationship).getBendPoints();
            if (!CommonUtils.isEmpty(bendPoints)) {
                List<AbsoluteBendpoint> bends = new ArrayList<AbsoluteBendpoint>();
                for (LayoutBendPoint bend : bendPoints) {
                    bends.add(new AbsoluteBendpoint((int) bend.getX(), (int) bend.getY()));
                }
                conn.setRoutingConstraint(bends);
            }
        }
    }


	protected void addDiagramNodes(AbstractGraphicalEditPart diagram)
	{
		GraphAnimation.recordInitialState(diagram.getFigure());
		//IFigure fig = diagram.getFigure();
		for (Object child : diagram.getChildren())
		{
			addEntityNode((EntityPart) child);
		}
	}

	protected void addEntityNode(EntityPart entityPart)
	{
        final Dimension dimension = entityPart.getFigure().getPreferredSize();
        final SimpleNode layoutEntity = new SimpleNode(entityPart, -1, -1, dimension.width, dimension.height);
        //algorithm.addEntity(layoutEntity);

        entities.add(layoutEntity);
        partToNodesMap.put(entityPart, layoutEntity);
	}

	protected void addDiagramEdges(AbstractGraphicalEditPart diagram)
	{
		for (Object child : diagram.getChildren())
		{
			addEntityEdges((GraphicalEditPart) child);
		}
	}

	protected void addEntityEdges(GraphicalEditPart entityPart)
	{
		List<?> outgoing = entityPart.getSourceConnections();
		for (int i = 0; i < outgoing.size(); i++)
		{
			ConnectionEditPart connectionPart = (AbstractConnectionEditPart) entityPart.getSourceConnections().get(i);
			addConnectionEdges(connectionPart);
		}
	}

	protected void addConnectionEdges(ConnectionEditPart connectionPart)
	{
		GraphAnimation.recordInitialState((Connection) connectionPart.getFigure());
		LayoutEntity source = partToNodesMap.get(connectionPart.getSource());
		LayoutEntity target = partToNodesMap.get(connectionPart.getTarget());
        if (source == null || target == null) {
            log.warn("Source or target node not found");
            return;
        }

        final SimpleRelationship relationship = new SimpleRelationship(source, target, false);
        //algorithm.addRelationship(relationship);
        relationships.add(relationship);
        connectionToRelationshipsMap.put(connectionPart, relationship);
	}

    private EntityPart getPartByEntity(LayoutEntity entity)
    {
        for (Map.Entry<EntityPart, LayoutEntity> entry : partToNodesMap.entrySet()) {
            if (entry.getValue() == entity) {
                return entry.getKey();
            }
        }
        return null;
    }

    private ConnectionEditPart getConnectionPartByRelationship(LayoutRelationship relationship)
    {
        for (Map.Entry<ConnectionEditPart, LayoutRelationship> entry : connectionToRelationshipsMap.entrySet()) {
            if (entry.getValue() == relationship) {
                return entry.getKey();
            }
        }
        return null;
    }
*/


}