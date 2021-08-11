/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.policy;

import org.eclipse.draw2dl.geometry.Point;
import org.eclipse.gef3.EditPart;
import org.eclipse.gef3.Request;
import org.eclipse.gef3.commands.Command;
import org.eclipse.gef3.editpolicies.AbstractEditPolicy;
import org.eclipse.gef3.editpolicies.ContainerEditPolicy;
import org.eclipse.gef3.requests.CreateRequest;
import org.eclipse.gef3.requests.GroupRequest;
import org.jkiss.dbeaver.erd.model.ERDEntity;
import org.jkiss.dbeaver.erd.model.ERDNote;
import org.jkiss.dbeaver.erd.ui.command.NoteCreateCommand;
import org.jkiss.dbeaver.erd.ui.part.DiagramPart;
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
     * @see org.eclipse.gef3.editpolicies.ContainerEditPolicy#getAddCommand(org.eclipse.gef3.requests.GroupRequest)
     */
    @Override
    protected Command getAddCommand(GroupRequest request)
    {
        EditPart host = getTargetEditPart(request);
        return null;
    }

    /**
     * @see ContainerEditPolicy#getCreateCommand(org.eclipse.gef3.requests.CreateRequest)
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
     * @see AbstractEditPolicy#getTargetEditPart(org.eclipse.gef3.Request)
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