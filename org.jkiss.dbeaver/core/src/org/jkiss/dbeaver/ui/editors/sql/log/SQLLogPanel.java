/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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

        queryLogViewer = new QueryLogViewer(this, editor.getSite(), new SQLLogFilter(editor));
    }

    public QueryLogViewer getQueryLogViewer()
    {
        return queryLogViewer;
    }
}