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

package org.jkiss.dbeaver.ext.erd.part;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Abstract node part
 */
public abstract class NodePart extends PropertyAwarePart implements NodeEditPart, ICustomizablePart {

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
            figure.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.getColorRegistry().get(ERDConstants.COLOR_ERD_ATTR_FOREGROUND), borderWidth),
                new MarginBorder(5)
            ));
        }
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
                    UIUtils.getColorRegistry().get(ERDConstants.COLOR_ERD_NOTE_BACKGROUND) :
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
                    UIUtils.getColorRegistry().get(ERDConstants.COLOR_ERD_NOTE_FOREGROUND) :
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
