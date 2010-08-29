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
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;

/**
 * AbstractDatabaseEditor
 */
public abstract class AbstractDatabaseEditor<INPUT_TYPE extends IDatabaseEditorInput> extends EditorPart
{
    static final Log log = LogFactory.getLog(AbstractDatabaseEditor.class);

    @SuppressWarnings("unchecked")
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.setSite(site);
        super.setInput(input);
        this.setPartName(input.getName());
        this.setTitleImage(input.getImageDescriptor().createImage());
    }

    public void dispose()
    {
        super.dispose();
    }

    @Override
    @SuppressWarnings("unchecked")
    public INPUT_TYPE getEditorInput()
    {
        return (INPUT_TYPE)super.getEditorInput();
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

}