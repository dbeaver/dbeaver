/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.erd.command.AssociationCreateCommand;
import org.jkiss.dbeaver.ext.erd.command.AssociationReconnectSourceCommand;
import org.jkiss.dbeaver.ext.erd.command.AssociationReconnectTargetCommand;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

/**
 * Handles manipulation of relationships between tables
 */
public class EntityConnectionEditPolicy extends GraphicalNodeEditPolicy {

    @Override
    protected Command getConnectionCreateCommand(CreateConnectionRequest request) {
        AssociationCreateCommand cmd = makeCreateCommand();
        EntityPart part = (EntityPart) getHost();
        cmd.setSourceEntity(part.getEntity());
        request.setStartCommand(cmd);
        return cmd;
    }

    @Override
    protected Command getConnectionCompleteCommand(CreateConnectionRequest request) {
        AssociationCreateCommand cmd = (AssociationCreateCommand) request.getStartCommand();
        EntityPart part = (EntityPart) request.getTargetEditPart();
        cmd.setTargetEntity(part.getEntity());
        return cmd;
    }

    @Override
    protected Command getReconnectSourceCommand(ReconnectRequest request) {

        AssociationReconnectSourceCommand cmd = makeReconnectSourceCommand();
        cmd.setAssociation((ERDAssociation) request.getConnectionEditPart().getModel());
        EntityPart entityPart = (EntityPart) getHost();
        cmd.setSourceEntity(entityPart.getEntity());
        return cmd;
    }

    @Override
    protected Command getReconnectTargetCommand(ReconnectRequest request) {
        AssociationReconnectTargetCommand cmd = makeReconnectTargetCommand();
        cmd.setRelationship((ERDAssociation) request.getConnectionEditPart().getModel());
        EntityPart entityPart = (EntityPart) getHost();
        cmd.setTargetEntity(entityPart.getEntity());
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