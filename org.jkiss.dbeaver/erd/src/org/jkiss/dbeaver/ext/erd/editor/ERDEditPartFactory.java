/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

import org.jkiss.dbeaver.ext.erd.model.Column;
import org.jkiss.dbeaver.ext.erd.model.Relationship;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.model.Table;
import org.jkiss.dbeaver.ext.erd.part.AttributePart;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.part.AssociationPart;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

/**
 * Edit part factory for creating EditPart instances as delegates for model objects
 *
 * @author Phil Zoio
 */
class ERDEditPartFactory implements EditPartFactory
{
    public EditPart createEditPart(EditPart context, Object model) {
        EditPart part = null;
        if (model instanceof EntityDiagram) {
            part = new DiagramPart();
        } else if (model instanceof Table) {
            part = new EntityPart();
        } else if (model instanceof Relationship) {
            part = new AssociationPart();
        } else if (model instanceof Column) {
            part = new AttributePart();
        }
        if (part != null) {
            part.setModel(model);
        }
        return part;
    }
}