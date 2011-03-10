/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IAutoSaveEditorInput;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.ProjectFileEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorInputFactory;

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

    public String getName()
    {
        return diagramName;
    }

    public String getToolTipText()
    {
        return diagramName;
    }

    @Override
    public IPersistableElement getPersistable()
    {
        return this;
    }

    public String getFactoryId()
    {
        return ERDEditorInputFactory.getFactoryId();
    }

    public void saveState(IMemento memento) 
    {
        ERDEditorInputFactory.saveState(memento, this);
    }

    public boolean isAutoSaveEnabled()
    {
        return true;
    }

}
