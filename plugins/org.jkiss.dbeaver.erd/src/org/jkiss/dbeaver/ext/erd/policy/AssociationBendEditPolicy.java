/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.BendpointEditPolicy;
import org.eclipse.gef.requests.BendpointRequest;
import org.jkiss.dbeaver.ext.erd.command.BendpointCreateCommand;
import org.jkiss.dbeaver.ext.erd.command.BendpointDeleteCommand;
import org.jkiss.dbeaver.ext.erd.command.BendpointMoveCommand;
import org.jkiss.dbeaver.ext.erd.part.AssociationPart;

/**
 * EditPolicy to handle deletion of relationships
 * @author Serge Rieder
 */
public class AssociationBendEditPolicy extends BendpointEditPolicy
{


    @Override
    protected Command getCreateBendpointCommand(BendpointRequest request) {
        return new BendpointCreateCommand(
            (AssociationPart) getHost(),
            getRelativeLocation(request),
            request.getIndex());
    }

    @Override
    protected Command getDeleteBendpointCommand(BendpointRequest request) {
        return new BendpointDeleteCommand((AssociationPart) getHost(), request.getIndex());
    }

    @Override
    protected Command getMoveBendpointCommand(BendpointRequest request) {
        return new BendpointMoveCommand(
            (AssociationPart) getHost(),
            getRelativeLocation(request),
            request.getIndex());
    }

    private Point getRelativeLocation(BendpointRequest request)
    {
        Point p = request.getLocation();
        getConnection().translateToRelative(p);
        return p;
    }

}