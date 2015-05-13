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