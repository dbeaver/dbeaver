/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.text.*;
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