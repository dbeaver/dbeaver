/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ext.ui.IObjectEditor;

/**
 * AbstractObjectEditor
 */
public abstract class AbstractObjectEditor extends EditorPart implements IObjectEditor
{
    static final Log log = LogFactory.getLog(AbstractObjectEditor.class);

    @SuppressWarnings("unchecked")
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.setSite(site);
        super.setInput(input);
    }

    public void dispose()
    {
        super.dispose();
    }

    public void doSave(IProgressMonitor monitor)
    {
    }

    public void doSaveAs()
    {
    }

    public boolean isDirty()
    {
        return false;
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    public void setFocus() {

    }

    public void deactivatePart() {
        // do nothing by default
    }
}