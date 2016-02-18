/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.TextFlow;
import org.jkiss.dbeaver.ext.erd.model.ERDNote;

/**
 * Figure used to represent a note
 *
 * @author Serge Rieder
 */
public class NoteFigure extends FlowPage {

    private TextFlow textFlow;

    public NoteFigure(ERDNote note)
    {
        //super(note.getObject());
        textFlow = new TextFlow(note.getObject());
        add(textFlow);

        setBackgroundColor(ColorConstants.tooltipBackground);
        setForegroundColor(ColorConstants.tooltipForeground);
        setOpaque(true);
        setBorder(new CompoundBorder(
            new LineBorder(ColorConstants.black, 1),
            new MarginBorder(5)
        ));
    }

    public void setText(String text)
    {
        textFlow.setText(text);
    }

    @Override
    public Dimension getPreferredSize(int width, int h)
    {
        return textFlow.getPreferredSize(width, h);//super.getPreferredSize(width, h);
    }

    @Override
    public void setPreferredSize(Dimension size)
    {
        textFlow.setSize(size);
        textFlow.setPreferredSize(size);
        super.setPreferredSize(size);
    }
}