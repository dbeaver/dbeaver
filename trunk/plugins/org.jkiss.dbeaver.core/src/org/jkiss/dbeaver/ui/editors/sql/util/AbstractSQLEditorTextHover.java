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