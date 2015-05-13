/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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