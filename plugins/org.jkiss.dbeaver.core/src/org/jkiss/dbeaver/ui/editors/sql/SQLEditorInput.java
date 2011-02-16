/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

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
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.ProjectFileEditorInput;

/**
 * SQLEditorInput
 */
public class SQLEditorInput extends ProjectFileEditorInput implements IPersistableElement, IAutoSaveEditorInput, IDatabaseEditorInput
{
    public static final QualifiedName PROP_DATA_SOURCE_ID = new QualifiedName("org.jkiss.dbeaver", "sql-editor-data-source-id");

    static final Log log = LogFactory.getLog(SQLEditorInput.class);

    private DBSDataSourceContainer dataSourceContainer;
    private String scriptName;

    public SQLEditorInput(IFile file, DBSDataSourceContainer dataSourceContainer)
    {
        super(file);
        if (dataSourceContainer != null) {
            this.setDataSourceContainer(dataSourceContainer);
        }
        this.scriptName = file.getFullPath().removeFileExtension().lastSegment();
    }

    public SQLEditorInput(IFile file)
    {
        super(file);
        this.scriptName = file.getFullPath().removeFileExtension().lastSegment();
        try {
            String dataSourceId = getFile().getPersistentProperty(PROP_DATA_SOURCE_ID);
            if (dataSourceId != null) {
                DBSDataSourceContainer container = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(file.getProject()).getDataSource(dataSourceId);
                if (container == null) {
                    log.warn("Data source '" + dataSourceId + "' not found in project '" + file.getProject().getName() + "'");
                } else {
                    setDataSourceContainer(container);
                }
            }
        } catch (CoreException e) {
            log.error(e);
        }
    }

    public IProject getProject()
    {
        return getFile().getProject();
    }

    public DBSDataSourceContainer getDataSourceContainer()
    {
        return dataSourceContainer;
    }

    public void setDataSourceContainer(DBSDataSourceContainer container)
    {
        dataSourceContainer = container;
        try {
            IFile file = getFile();
            if (file != null) {
                if (dataSourceContainer == null) {
                    file.setPersistentProperty(PROP_DATA_SOURCE_ID, null);
                } else {
                    file.setPersistentProperty(PROP_DATA_SOURCE_ID, dataSourceContainer.getId());
                }
            }
        } catch (CoreException e) {
            log.error(e);
        }
    }

    public String getName()
    {
        String dsName = "<None>";
        if (dataSourceContainer != null) {
            dsName = dataSourceContainer.getName();
        }
        return scriptName + " (" + dsName + ")";
    }

    public String getToolTipText()
    {
        if (dataSourceContainer == null) {
            return super.getName();
        }
        return
            "Connection: " + dataSourceContainer.getName() + "\n" +
            "Type: " + (dataSourceContainer.getDriver() == null ? "Unknown" : dataSourceContainer.getDriver().getName()) + "\n" +
            "URL: " + dataSourceContainer.getConnectionInfo().getUrl();
    }

    @Override
    public IPersistableElement getPersistable()
    {
        return this;
    }

    public String getFactoryId()
    {
        return SQLEditorInputFactory.getFactoryId();
    }

    public void saveState(IMemento memento) 
    {
        SQLEditorInputFactory.saveState(memento, this);
    }

    public boolean isAutoSaveEnabled()
    {
        return true;
    }

    public DBNDatabaseNode getTreeNode()
    {
        return DBeaverCore.getInstance().getNavigatorModel().findNode(getDataSourceContainer());
    }

    public DBSObject getDatabaseObject()
    {
        return getDataSourceContainer();
    }

    public String getDefaultPageId()
    {
        return null;
    }

    public DBPDataSource getDataSource()
    {
        return getDataSourceContainer().getDataSource();
    }

}
