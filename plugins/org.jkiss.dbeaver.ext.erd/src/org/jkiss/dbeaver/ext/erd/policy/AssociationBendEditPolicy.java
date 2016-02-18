/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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