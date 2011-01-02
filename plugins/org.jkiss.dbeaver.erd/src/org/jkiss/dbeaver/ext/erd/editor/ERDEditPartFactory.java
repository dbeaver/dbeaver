/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;
import org.jkiss.dbeaver.ext.erd.model.ERDTableColumn;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.AssociationPart;
import org.jkiss.dbeaver.ext.erd.part.AttributePart;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

/**
 * Edit part factory for creating EditPart instances as delegates for model objects
 *
 * @author Serge Rieder
 */
class ERDEditPartFactory implements EditPartFactory
{
    public EditPart createEditPart(EditPart context, Object model) {
        EditPart part = null;
        if (model instanceof EntityDiagram) {
            part = new DiagramPart();
        } else if (model instanceof ERDTable) {
            part = new EntityPart();
        } else if (model instanceof ERDAssociation) {
            part = new AssociationPart();
        } else if (model instanceof ERDTableColumn) {
            part = new AttributePart();
        }
        if (part != null) {
            part.setModel(model);
        }
        return part;
    }
}