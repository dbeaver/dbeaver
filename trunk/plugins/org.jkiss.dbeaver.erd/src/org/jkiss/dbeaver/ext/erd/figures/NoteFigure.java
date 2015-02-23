/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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