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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.directedit;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Text;

/**
 * A CellEditorLocator for a specified text flow
 *
 * @author Serge Rider
 */
public class FigureEditorLocator implements CellEditorLocator {

    private IFigure figure;

    public FigureEditorLocator(IFigure figure) {
        this.figure = figure;
    }

    /**
     * expands the size of the control by 1 pixel in each direction
     */
    @Override
    public void relocate(CellEditor celleditor) {
        Border border = figure.getBorder();
        //Insets insets = border.getInsets(figure);
        Dimension borderSize = border.getPreferredSize(figure);
        Text text = (Text) celleditor.getControl();

        Rectangle rect = figure.getBounds().getCopy();
        figure.translateToAbsolute(rect);
        text.setBackground(figure.getBackgroundColor());
        text.setBounds(
            rect.x + borderSize.width,
            rect.y + borderSize.height,
            rect.width - borderSize.width * 2,
            rect.height - borderSize.height * 2);

    }

    protected IFigure getFigure() {
        return figure;
    }

}