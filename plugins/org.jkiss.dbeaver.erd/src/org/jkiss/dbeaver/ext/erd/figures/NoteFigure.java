/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.*;
import org.jkiss.dbeaver.ext.erd.model.ERDNote;

/**
 * Figure used to represent a note
 *
 * @author Serge Rieder
 */
public class NoteFigure extends EditableLabel {

    public NoteFigure(ERDNote note)
    {
        super(note.getObject());

        setBackgroundColor(ColorConstants.tooltipBackground);
        setForegroundColor(ColorConstants.tooltipForeground);
        setOpaque(true);

        setBorder(new LineBorder(ColorConstants.black, 1));
    }

}