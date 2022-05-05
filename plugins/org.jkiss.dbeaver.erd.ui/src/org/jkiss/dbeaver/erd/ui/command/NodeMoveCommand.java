/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.command;

import org.eclipse.draw2dl.geometry.Rectangle;
import org.eclipse.gef3.commands.Command;
import org.jkiss.dbeaver.erd.ui.part.NodePart;

/**
 * Command to move the bounds of an existing table. Only used with
 * XYLayoutEditPolicy (manual layout)
 * 
 * @author Serge Rider
 */
public class NodeMoveCommand extends Command
{

	private NodePart customisablePart;
	private Rectangle oldBounds;
	private Rectangle newBounds;

	public NodeMoveCommand(NodePart customisablePart, Rectangle oldBounds, Rectangle newBounds)
	{
		super();
		this.customisablePart = customisablePart;
		this.oldBounds = oldBounds;
		this.newBounds = newBounds;
	}

	@Override
    public void execute()
	{
/*
        List tcList = nodePart.getTargetConnections();
        for (Object tc : tcList) {
            AssociationPart as = (AssociationPart)tc ;
            PolylineConnection pc = (PolylineConnection) as.getFigure();
            pc.getConnectionRouter().route(pc);
        }
*/
        customisablePart.modifyBounds(newBounds);
	}

	@Override
    public void undo()
	{
		customisablePart.modifyBounds(oldBounds);
	}

}