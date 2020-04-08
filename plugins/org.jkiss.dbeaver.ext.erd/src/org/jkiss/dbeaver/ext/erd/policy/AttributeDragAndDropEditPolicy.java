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

import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.GraphicalEditPolicy;
import org.jkiss.dbeaver.ext.erd.part.AttributePart;

/**
 * EditPolicy for the direct editing of Column names
 *
 * @author Serge Rider
 */
public class AttributeDragAndDropEditPolicy extends GraphicalEditPolicy {
    private AttributePart part;

    public AttributeDragAndDropEditPolicy(AttributePart part) {
        this.part = part;
    }

    @Override
    public Command getCommand(Request req) {
        if (RequestConstants.REQ_MOVE.equals(req.getType()) ||
            RequestConstants.REQ_CLONE.equals(req.getType()) ||
            RequestConstants.REQ_ORPHAN.equals(req.getType()))
        {
            // We come here if attribute target part is entity
            return null;//new AttributeCheckCommand(part, false);
        }
        return super.getCommand(req);
    }

    @Override
    public boolean understandsRequest(Request req) {
        if (RequestConstants.REQ_MOVE.equals(req.getType()) ||
            RequestConstants.REQ_CLONE.equals(req.getType()) ||
            RequestConstants.REQ_ORPHAN.equals(req.getType())) {
            return true;
        }
        return super.understandsRequest(req);
    }
}