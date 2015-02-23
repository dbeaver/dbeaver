/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.geometry.Point;
import org.jkiss.dbeaver.ext.erd.part.AssociationPart;

public class BendpointMoveCommand extends BendpointCommand {

    private Point location;
    private int bendpointIndex;

    private Bendpoint oldBendpoint;

    public BendpointMoveCommand(AssociationPart association, Point location, int bendpointIndex) {
        super(association);
        this.location = location;
        this.bendpointIndex = bendpointIndex;
    }

    @Override
    public void execute() {
        association.moveBendpoint(bendpointIndex, location);
/*
        WireBendpoint bp = new WireBendpoint();
        bp.setRelativeDimensions(getFirstRelativeDimension(),
            getSecondRelativeDimension());
        setOldBendpoint((Bendpoint) getWire().getBendpoints().get(getIndex()));
        getWire().setBendpoint(getIndex(), bp);
        super.execute();
*/
    }

    @Override
    public void undo() {
/*
        super.undo();
        getWire().setBendpoint(getIndex(), getOldBendpoint());
*/
    }

}


