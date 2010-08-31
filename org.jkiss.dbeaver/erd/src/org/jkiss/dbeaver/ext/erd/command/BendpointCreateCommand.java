/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.draw2d.geometry.Point;
import org.jkiss.dbeaver.ext.erd.part.AssociationPart;


public class BendpointCreateCommand extends BendpointCommand {

    private Point location;
    private int bendpointIndex;

    public BendpointCreateCommand(AssociationPart association, Point location, int bendpointIndex) {
        super(association);
        this.location = location;
        this.bendpointIndex = bendpointIndex;
    }

    public void execute() {
        association.addBendpoint(bendpointIndex, location);
    }

    public void undo() {
        association.removeBendpoint(bendpointIndex);
    }

}


