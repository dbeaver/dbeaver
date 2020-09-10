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

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.jkiss.dbeaver.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.command.AssociationCreateCommand;
import org.jkiss.dbeaver.ext.erd.command.AssociationReconnectSourceCommand;
import org.jkiss.dbeaver.ext.erd.command.AssociationReconnectTargetCommand;
import org.jkiss.dbeaver.ext.erd.part.NodePart;

/**
 * Handles manipulation of relationships between tables
 */
public class EntityConnectionEditPolicy extends GraphicalNodeEditPolicy {

    @Override
    protected Command getConnectionCreateCommand(CreateConnectionRequest request) {
        AssociationCreateCommand cmd = makeCreateCommand();
        NodePart part = (NodePart) getHost();
        cmd.setSourceEntity(part.getElement());
        request.setStartCommand(cmd);
        return cmd;
    }

    @Override
    protected Command getConnectionCompleteCommand(CreateConnectionRequest request) {
        AssociationCreateCommand cmd = (AssociationCreateCommand) request.getStartCommand();
        NodePart part = (NodePart) request.getTargetEditPart();
        cmd.setTargetEntity(part.getElement());
        return cmd;
    }

    @Override
    protected Command getReconnectSourceCommand(ReconnectRequest request) {

        AssociationReconnectSourceCommand cmd = makeReconnectSourceCommand();
        cmd.setAssociation((ERDAssociation) request.getConnectionEditPart().getModel());
        NodePart entityPart = (NodePart) getHost();
        cmd.setSourceEntity(entityPart.getElement());
        return cmd;
    }

    @Override
    protected Command getReconnectTargetCommand(ReconnectRequest request) {
        AssociationReconnectTargetCommand cmd = makeReconnectTargetCommand();
        cmd.setRelationship((ERDAssociation) request.getConnectionEditPart().getModel());
        NodePart entityPart = (NodePart) getHost();
        cmd.setTargetEntity(entityPart.getElement());
        return cmd;
    }

    protected AssociationCreateCommand makeCreateCommand() {
        return new AssociationCreateCommand();
    }

    protected AssociationReconnectSourceCommand makeReconnectSourceCommand() {
        return new AssociationReconnectSourceCommand();
    }

    protected AssociationReconnectTargetCommand makeReconnectTargetCommand() {
        return new AssociationReconnectTargetCommand();
    }


}