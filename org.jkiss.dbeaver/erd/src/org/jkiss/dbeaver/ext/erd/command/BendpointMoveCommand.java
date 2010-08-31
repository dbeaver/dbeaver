/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
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

    public void undo() {
/*
        super.undo();
        getWire().setBendpoint(getIndex(), getOldBendpoint());
*/
    }

}


