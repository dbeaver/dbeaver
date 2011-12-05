/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * SQLEditorControl
 */
public class SQLEditorControl extends Composite {

    private final SQLEditorBase editor;

    public SQLEditorControl(Composite parent, SQLEditorBase editor)
    {
        super(parent, SWT.NONE);
        this.editor = editor;
        setLayout(new FillLayout());
    }

    public SQLEditorBase getEditor()
    {
        return editor;
    }
}
