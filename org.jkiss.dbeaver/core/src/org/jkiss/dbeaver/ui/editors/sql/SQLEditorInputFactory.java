/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;

public class SQLEditorInputFactory implements IElementFactory
{
    static final Log log = LogFactory.getLog(SQLEditorInputFactory.class);

    private static final String ID_FACTORY = "org.jkiss.dbeaver.ui.editors.sql.SQLEditorInputFactory"; //$NON-NLS-1$

    private static final String TAG_PATH = "path"; //$NON-NLS-1$
    private static final String TAG_NAME = "name"; //$NON-NLS-1$
    private static final String TAG_DATA_SOURCE = "data-source"; //$NON-NLS-1$

    public SQLEditorInputFactory()
    {
    }

    public IAdaptable createElement(IMemento memento)
    {
        // Get the file name.
        String fileName = memento.getString(TAG_PATH);
        if (fileName == null) {
            return null;
        }

        // Get a handle to the IFile...which can be a handle
        // to a resource that does not exist in workspace
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(fileName));
        if (file != null) {
            String dataSourceName = memento.getString(TAG_DATA_SOURCE);
            String scriptName = memento.getString(TAG_NAME);
            if (dataSourceName != null) {
                DataSourceDescriptor dataSource = DataSourceRegistry.getDefault().getDataSource(dataSourceName);
                if (dataSource == null) {
                    log.warn("Can't find datasource '" + dataSourceName + "' for file '" + fileName + "'");
                } else {
                    if (scriptName == null) {
                        scriptName = "";
                    }
                    return new SQLEditorInput(file, dataSource, scriptName);
                }
            }
        }
        return null;
    }

    public static String getFactoryId()
    {
        return ID_FACTORY;
    }

    public static void saveState(IMemento memento, SQLEditorInput input)
    {
        IFile file = input.getFile();
        memento.putString(TAG_PATH, file.getFullPath().toString());
        memento.putString(TAG_NAME, input.getScriptName());
        if (input.getDataSourceContainer() != null) {
            memento.putString(TAG_DATA_SOURCE, input.getDataSourceContainer().getName());
        }
    }
}