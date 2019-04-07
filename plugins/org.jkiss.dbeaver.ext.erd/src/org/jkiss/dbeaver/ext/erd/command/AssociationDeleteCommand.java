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
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDElement;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.ext.erd.part.AssociationPart;

/**
 * Command to delete relationship
 *
 * @author Serge Rider
 */
public class AssociationDeleteCommand extends Command {

    protected final AssociationPart part;
    protected final ERDElement sourceEntity;
    protected final ERDElement targetEntity;
    protected final ERDAssociation association;

    public AssociationDeleteCommand(AssociationPart part) {
        super();
        this.part = part;
        association = part.getAssociation();
        sourceEntity = association.getSourceEntity();
        targetEntity = association.getTargetEntity();
    }

    /**
     * Removes the relationship
     */
    @Override
    public void execute() {
        part.markAssociatedAttributes(EditPart.SELECTED_NONE);

        targetEntity.removeReferenceAssociation(association, true);
        sourceEntity.removeAssociation(association, true);
        association.setSourceEntity(null);
        association.setTargetEntity(null);
    }

    /**
     * Restores the relationship
     */
    @Override
    public void undo() {
        association.setSourceEntity(sourceEntity);
        association.setTargetEntity(targetEntity);
        sourceEntity.addAssociation(association, true);
        targetEntity.addReferenceAssociation(association, true);
    }

}

