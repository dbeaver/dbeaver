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