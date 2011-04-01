/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;

public class ObjectEditorPageControl extends ProgressPageControl {

    private ObjectEditorHandler objectEditorHandler;

    public ObjectEditorPageControl(Composite parent, int style, IDatabaseNodeEditor workbenchPart)
    {
        super(parent, style);
        objectEditorHandler = new ObjectEditorHandler(workbenchPart);
    }

    @Override
    public void dispose()
    {
        objectEditorHandler.dispose();
        super.dispose();
    }

    @Override
    protected Composite createProgressPanel(Composite container) {
        Composite panel = super.createProgressPanel(container);
        objectEditorHandler.createEditorControls(panel);
        return panel;
    }

    public ObjectEditorHandler getObjectEditorHandler()
    {
        return objectEditorHandler;
    }
}
