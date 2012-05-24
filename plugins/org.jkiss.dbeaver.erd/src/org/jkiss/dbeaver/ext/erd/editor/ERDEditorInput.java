/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.dbeaver.ext.IAutoSaveEditorInput;
import org.jkiss.dbeaver.ui.editors.ProjectFileEditorInput;

/**
 * SQLEditorInput
 */
public class ERDEditorInput extends ProjectFileEditorInput implements IPersistableElement, IAutoSaveEditorInput
{
    static final Log log = LogFactory.getLog(ERDEditorInput.class);

    private String diagramName;

    public ERDEditorInput(IFile file)
    {
        super(file);
        this.diagramName = file.getFullPath().removeFileExtension().lastSegment();
    }

    @Override
    public String getName()
    {
        return diagramName;
    }

    @Override
    public String getToolTipText()
    {
        return "ER Diagram";
    }

    @Override
    public IPersistableElement getPersistable()
    {
        return this;
    }

    @Override
    public String getFactoryId()
    {
        return ERDEditorInputFactory.getFactoryId();
    }

    @Override
    public void saveState(IMemento memento)
    {
        ERDEditorInputFactory.saveState(memento, this);
    }

    @Override
    public boolean isAutoSaveEnabled()
    {
        return false;
    }

}
