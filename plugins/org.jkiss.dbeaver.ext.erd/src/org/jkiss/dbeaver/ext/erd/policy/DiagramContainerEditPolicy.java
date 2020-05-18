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
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.AbstractEditPolicy;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.GroupRequest;
import org.jkiss.dbeaver.ext.erd.command.NoteCreateCommand;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.ext.erd.model.ERDNote;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Handles creation of new tables using drag and drop or point and click from the palette
 *
 * @author Serge Rider
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
            return new NoteCreateCommand(diagramPart, (ERDNote)newObject, location, request.getSize());
        }
        List<ERDEntity> entities = null;
        if (newObject instanceof ERDEntity) {
            entities = Collections.singletonList((ERDEntity) newObject);
        } else if (newObject instanceof Collection) {
            entities = new ArrayList<>((Collection<ERDEntity>)newObject);
        }
        if (CommonUtils.isEmpty(entities)) {
            return null;
        }
        //EditPart host = getTargetEditPart(request);

        Command entityAddCommand = diagramPart.createEntityAddCommand(entities, location);
        if (!entityAddCommand.canExecute()) {
            return null;
        }
        return entityAddCommand;
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