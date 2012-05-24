/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.util;

import org.eclipse.jface.text.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

public abstract class AbstractSQLEditorTextHover implements ITextHover, ITextHoverExtension {

    /**
     *
     */
    public AbstractSQLEditorTextHover()
    {
    }

    /**
     * Associates a SQL editor with this hover. Subclass can cache it for later use.
     *
     * @param editor
     */
    public abstract void setEditor(IEditorPart editor);

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
     */
    @Override
    public IInformationControlCreator getHoverControlCreator()
    {
        return new IInformationControlCreator() {
            @Override
            public IInformationControl createInformationControl(Shell parent)
            {
                DefaultInformationControl control = new DefaultInformationControl(parent, true);
                control.setSizeConstraints(60, 10);
                return control;
            }
        };
    }

    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset)
    {
        return SQLWordFinder.findWord(textViewer.getDocument(), offset);
    }

}