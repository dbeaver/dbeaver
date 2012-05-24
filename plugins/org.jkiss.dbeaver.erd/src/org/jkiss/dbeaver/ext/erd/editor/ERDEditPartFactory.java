/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.jkiss.dbeaver.ext.erd.model.*;
import org.jkiss.dbeaver.ext.erd.part.*;

/**
 * Edit part factory for creating EditPart instances as delegates for model objects
 *
 * @author Serge Rieder
 */
class ERDEditPartFactory implements EditPartFactory
{
    @Override
    public EditPart createEditPart(EditPart context, Object model) {
        EditPart part = null;
        if (model instanceof EntityDiagram) {
            part = new DiagramPart();
        } else if (model instanceof ERDEntity) {
            part = new EntityPart();
        } else if (model instanceof ERDAssociation) {
            part = new AssociationPart();
        } else if (model instanceof ERDEntityAttribute) {
            part = new AttributePart();
        } else if (model instanceof ERDNote) {
            part = new NotePart();
        }
        if (part != null) {
            part.setModel(model);
        }
        return part;
    }
}