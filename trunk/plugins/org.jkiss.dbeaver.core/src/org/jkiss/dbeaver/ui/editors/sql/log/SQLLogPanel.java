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

package org.jkiss.dbeaver.ui.editors.sql.log;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.controls.querylog.QueryLogViewer;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

/**
 * ResultSetViewer
 */
public class SQLLogPanel extends Composite
{
    private QueryLogViewer queryLogViewer;

    public SQLLogPanel(Composite parent, SQLEditor editor)
    {
        super(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, true);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;
        setLayout(gl);

        queryLogViewer = new QueryLogViewer(this, editor.getSite(), new SQLLogFilter(editor), false);
    }

    public QueryLogViewer getQueryLogViewer()
    {
        return queryLogViewer;
    }
}