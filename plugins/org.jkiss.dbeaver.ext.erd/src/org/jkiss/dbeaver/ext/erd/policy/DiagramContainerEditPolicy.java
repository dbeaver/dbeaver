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
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.AbstractEditPolicy;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.GroupRequest;
import org.jkiss.dbeaver.ext.erd.command.EntityAddCommand;
import org.jkiss.dbeaver.ext.erd.command.NoteCreateCommand;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.ext.erd.model.ERDNote;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * Handles creation of new tables using drag and drop or point and click from the palette
 *
 * @author Serge Rieder
 */
public class DiagramContainerEditPolicy extends ContainerEditPolicy {

    /**
     * @see org.eclipse.gef.editpolicies.ContainerEditPolicy#getAddCommand(org.eclipse.gef.requests.GroupRequest)
     */
    @Override
    protected Command getAddCommand(GroupRequest request)
    {
        EditPart host = getTargetEditPart(request);
        return null;
    }

    /**
     * @see ContainerEditPolicy#getCreateCommand(org.eclipse.gef.requests.CreateRequest)
     */
    @Override
    protected Command getCreateCommand(CreateRequest request)
    {
        DiagramPart diagramPart = (DiagramPart) getHost();
        Point location = request.getLocation();
        diagramPart.getFigure().translateToRelative(location);

        Object newObject = request.getNewObject();
        if (newObject instanceof ERDNote) {
            return new NoteCreateCommand(diagramPart, (ERDNote)newObject, location);
        }
        Collection<ERDEntity> entities = null;
        if (newObject instanceof ERDEntity) {
            entities = Collections.singletonList((ERDEntity) newObject);
        } else if (newObject instanceof Collection) {
            entities = (Collection<ERDEntity>) newObject;
        }
        if (CommonUtils.isEmpty(entities)) {
            return null;
        }
        //EditPart host = getTargetEditPart(request);

        return new EntityAddCommand(diagramPart, entities, location);
    }

    /**
     * @see AbstractEditPolicy#getTargetEditPart(org.eclipse.gef.Request)
     */
    @Override
    public EditPart getTargetEditPart(Request request)
    {
        if (REQ_CREATE.equals(request.getType())) {
            return getHost();
        }
        if (REQ_ADD.equals(request.getType())) {
            return getHost();
        }
        if (REQ_MOVE.equals(request.getType())) {
            return getHost();
        }
        return super.getTargetEditPart(request);
    }

}