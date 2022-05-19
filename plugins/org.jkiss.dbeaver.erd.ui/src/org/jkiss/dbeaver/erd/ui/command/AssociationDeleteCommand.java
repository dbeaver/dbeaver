/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.command;

import java.util.function.Supplier;

import org.eclipse.gef3.commands.Command;
import org.eclipse.swt.SWT;
import org.jkiss.dbeaver.erd.model.ERDAssociation;
import org.jkiss.dbeaver.erd.model.ERDElement;
import org.jkiss.dbeaver.erd.model.ERDEntity;
import org.jkiss.dbeaver.erd.ui.part.AssociationPart;
import org.jkiss.dbeaver.erd.ui.part.DiagramPart;
import org.jkiss.dbeaver.erd.ui.part.EntityPart;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityForeignKey;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;

/**
 * Command to delete relationship
 *
 * @author Serge Rider
 */
public class AssociationDeleteCommand extends Command {

    protected final DiagramPart diagramPart;
    protected final ERDElement sourceEntity;
    protected final ERDElement targetEntity;
    protected final ERDAssociation association;
    protected final DBVEntity vEntity;
    protected final Supplier<DBVEntityForeignKey> virtualFkSupplier;

    public AssociationDeleteCommand(AssociationPart part) {
        super();
        diagramPart = part.getDiagramPart();
        association = part.getAssociation();
        sourceEntity = association.getSourceEntity();
        targetEntity = association.getTargetEntity();
        
        DBSEntityAssociation entityAssociation = association.getObject();
        if (entityAssociation instanceof DBVEntityForeignKey) {
            DBVEntityForeignKey vfk = (DBVEntityForeignKey) entityAssociation;
            vEntity = DBVUtils.getVirtualEntity(entityAssociation.getParentObject(), false);
            virtualFkSupplier = EditForeignKeyPage.makeVirtualForeignKeySupplier(vEntity, vfk.getReferencedConstraint(), vfk.getAttributes());
        } else {
            vEntity = null;
            virtualFkSupplier = null;
        }
    }

    /**
     * Removes the relationship
     */
    @Override
    public void execute() {
        DBSEntityAssociation entityAssociation = association.getObject();
        if (entityAssociation instanceof DBVEntityForeignKey) {
            if (!UIUtils.confirmAction("Delete logical key", "Are you sure you want to delete logical key '" + association.getName() + "'?")) {
                return;
            }
            if (vEntity == null) {
                UIUtils.showMessageBox(UIUtils.getActiveWorkbenchShell(), "No virtual entity", "Can't find association owner virtual entity", SWT.ICON_ERROR);
                return;
            }
            vEntity.removeForeignKey((DBVEntityForeignKey) entityAssociation);
        }
        removeAssociationFromDiagram();
    }

    protected void removeAssociationFromDiagram() {
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
        if (association.getSourceEntity() != null) {
            return;
        }
        if (virtualFkSupplier != null) { 
            association.setObject(virtualFkSupplier.get());
        }
        association.setSourceEntity(sourceEntity);
        association.setTargetEntity(targetEntity);
        sourceEntity.addAssociation(association, true);
        targetEntity.addReferenceAssociation(association, true);
        
        if (sourceEntity instanceof ERDEntity) {
            EntityPart sourcePart = diagramPart.getEntityPart((ERDEntity) sourceEntity);
            sourcePart.getConnectionPart(association, true).activate();
        }
    }
    
    @Override
    public void redo() {
        this.execute();
    }

}

