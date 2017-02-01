/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.erd.part;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.NodeEditPart;

/**
 * Abstract node part
 */
public abstract class NodePart extends PropertyAwarePart implements NodeEditPart {

    private Rectangle bounds;


    /**
     * @return Returns the bounds.
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Sets bounds without firing off any event notifications
     *
     * @param bounds The bounds to set.
     */
    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    /**
     * If modified, sets bounds and fires off event notification
     *
     * @param bounds The bounds to set.
     */
    public void modifyBounds(Rectangle bounds) {
        Rectangle oldBounds = this.bounds;
        if (!bounds.equals(oldBounds)) {
            this.bounds = bounds;

            Figure entityFigure = (Figure) getFigure();
            DiagramPart parent = (DiagramPart) getParent();
            parent.setLayoutConstraint(this, entityFigure, bounds);
        }
    }

}
