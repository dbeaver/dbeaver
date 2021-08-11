/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.erd.ui.part;

import org.eclipse.draw2dl.*;
import org.eclipse.draw2dl.geometry.Rectangle;
import org.eclipse.gef3.NodeEditPart;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.jkiss.dbeaver.erd.model.ERDAssociation;
import org.jkiss.dbeaver.erd.model.ERDElement;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;

/**
 * Abstract node part
 */
public abstract class NodePart extends PropertyAwarePart implements NodeEditPart, ICustomizablePart {

    private Rectangle bounds;


    public abstract ERDElement getElement();


    @Override
    protected List<ERDAssociation> getModelSourceConnections() {
        return getElement().getAssociations();
    }

    @Override
    protected List<ERDAssociation> getModelTargetConnections() {
        return getElement().getReferences();
    }

    public AssociationPart getConnectionPart(ERDAssociation rel, boolean source) {
        for (Object conn : source ? getSourceConnections() : getTargetConnections()) {
            if (conn instanceof AssociationPart && ((AssociationPart) conn).getAssociation() == rel) {
                return (AssociationPart) conn;
            }
        }
        return null;
    }

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
        this.modifyBounds(bounds);
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
            getDiagramPart().setLayoutConstraint(this, entityFigure, bounds);
        }
    }

    @Override
    public boolean getCustomTransparency() {
        IFigure figure = getFigure();
        return figure != null && !figure.isOpaque();
    }

    @Override
    public void setCustomTransparency(boolean transparency) {
        IFigure figure = getFigure();
        if (figure != null) {
            figure.setOpaque(!transparency);
        }
    }

    @Override
    public int getCustomBorderWidth() {
        IFigure figure = getFigure();
        if (figure != null) {
            Border border = figure.getBorder();
            if (border instanceof LineBorder) {
                return ((LineBorder) border).getWidth();
            } else if (border instanceof CompoundBorder) {
                if (((CompoundBorder) border).getOuterBorder() instanceof LineBorder) {
                    return ((LineBorder) ((CompoundBorder) border).getOuterBorder()).getWidth();
                }
            }
        }
        return 0;
    }

    @Override
    public void setCustomBorderWidth(int borderWidth) {
        IFigure figure = getFigure();
        if (figure != null) {
            figure.setBorder(createBorder(borderWidth));
        }
    }

    protected Border createBorder(int borderWidth) {
        Border newBorder;
        if (borderWidth == 0) {
            newBorder = new MarginBorder(5);
        } else {
            newBorder = new CompoundBorder(
                new LineBorder(UIUtils.getColorRegistry().get(ERDUIConstants.COLOR_ERD_ATTR_FOREGROUND), borderWidth),
                new MarginBorder(5)
            );
        }
        return newBorder;
    }

    @Override
    public Color getCustomBackgroundColor() {
        IFigure figure = getFigure();
        return figure == null ? null : figure.getBackgroundColor();
    }

    @Override
    public void setCustomBackgroundColor(Color color) {
        IFigure figure = getFigure();
        if (figure != null) {
            figure.setBackgroundColor(
                color == null ?
                    UIUtils.getColorRegistry().get(ERDUIConstants.COLOR_ERD_NOTE_BACKGROUND) :
                    color);
        }
    }

    @Override
    public Color getCustomForegroundColor() {
        IFigure figure = getFigure();
        return figure == null ? null : figure.getForegroundColor();
    }

    @Override
    public void setCustomForegroundColor(Color color) {
        IFigure figure = getFigure();
        if (figure != null) {
            figure.setForegroundColor(
                color == null ?
                    UIUtils.getColorRegistry().get(ERDUIConstants.COLOR_ERD_NOTE_FOREGROUND) :
                    color);
        }
    }

    @Override
    public Font getCustomFont() {
        IFigure figure = getFigure();
        return figure == null ? null : figure.getFont();
    }

    @Override
    public void setCustomFont(Font font) {
        IFigure figure = getFigure();
        if (figure != null) {
            figure.setFont(font);
        }
    }
}
