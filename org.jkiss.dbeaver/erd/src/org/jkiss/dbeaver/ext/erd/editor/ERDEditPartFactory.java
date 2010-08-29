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
import org.jkiss.dbeaver.ext.erd.model.Schema;
import org.jkiss.dbeaver.ext.erd.model.Table;
import org.jkiss.dbeaver.ext.erd.part.ColumnPart;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.part.RelationshipPart;
import org.jkiss.dbeaver.ext.erd.part.TablePart;

/**
 * Edit part factory for creating EditPart instances as delegates for model objects
 *
 * @author Phil Zoio
 */
class ERDEditPartFactory implements EditPartFactory
{
    public EditPart createEditPart(EditPart context, Object model) {
        EditPart part = null;
        if (model instanceof Schema) {
            part = new DiagramPart();
        } else if (model instanceof Table) {
            part = new TablePart();
        } else if (model instanceof Relationship) {
            part = new RelationshipPart();
        } else if (model instanceof Column) {
            part = new ColumnPart();
        }
        if (part != null) {
            part.setModel(model);
        }
        return part;
    }
}