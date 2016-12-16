/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.utils.CommonUtils;

public class DatabaseEditorInputFactory implements IElementFactory
{
    private static final Log log = Log.getLog(DatabaseEditorInputFactory.class);

    public static final String ID_FACTORY = DatabaseEditorInputFactory.class.getName(); //$NON-NLS-1$

    private static final String TAG_CLASS = "class"; //$NON-NLS-1$
    private static final String TAG_DATA_SOURCE = "data-source"; //$NON-NLS-1$
    private static final String TAG_NODE = "node"; //$NON-NLS-1$
    private static final String TAG_NODE_NAME = "node-name"; //$NON-NLS-1$
    private static final String TAG_ACTIVE_PAGE = "page"; //$NON-NLS-1$
    private static final String TAG_ACTIVE_FOLDER = "folder"; //$NON-NLS-1$

    private static volatile boolean lookupEditor;

    public DatabaseEditorInputFactory()
    {
    }

    public static void setLookupEditor(boolean lookupEditor) {
        DatabaseEditorInputFactory.lookupEditor = lookupEditor;
    }

    @Override
    public IAdaptable createElement(IMemento memento)
    {
        // Get the node path.
        final String inputClass = memento.getString(TAG_CLASS);
        final String nodePath = memento.getString(TAG_NODE);
        final String nodeName = memento.getString(TAG_NODE_NAME);
        final String dataSourceId = memento.getString(TAG_DATA_SOURCE);
        if (nodePath == null || inputClass == null || dataSourceId == null) {
            log.error("Corrupted memento"); //$NON-NLS-2$
            return null;
        }
        final String activePageId = memento.getString(TAG_ACTIVE_PAGE);
        final String activeFolderId = memento.getString(TAG_ACTIVE_FOLDER);

        final DBPDataSourceContainer dataSourceContainer = DataSourceRegistry.findDataSource(dataSourceId);
        if (dataSourceContainer == null) {
            log.error("Can't find data source '" + dataSourceId + "'"); //$NON-NLS-2$
            return null;
        }
        if (lookupEditor && !dataSourceContainer.isConnected()) {
            // Do not instantiate editor input if we are just looking for opened editor
            //. for some object. Connection must be instantiated.
            return null;
        }
        final IProject project = dataSourceContainer.getRegistry().getProject();
        final DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
        navigatorModel.ensureProjectLoaded(project);

        return new DatabaseLazyEditorInput(dataSourceContainer, project, nodePath, nodeName, activePageId, activeFolderId);
    }

    public static void saveState(IMemento memento, DatabaseEditorInput input)
    {
        if (!DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS)) {
            return;
        }
        final DBCExecutionContext context = input.getExecutionContext();
        if (context == null) {
            // Detached - nothing to save
            return;
        }
        if (input.getDatabaseObject() != null && !input.getDatabaseObject().isPersisted()) {
            return;
        }

        final DBNDatabaseNode node = input.getNavigatorNode();
        memento.putString(TAG_CLASS, input.getClass().getName());
        memento.putString(TAG_DATA_SOURCE, context.getDataSource().getContainer().getId());
        memento.putString(TAG_NODE, node.getNodeItemPath());
        memento.putString(TAG_NODE_NAME, node.getNodeName());
        if (!CommonUtils.isEmpty(input.getDefaultPageId())) {
            memento.putString(TAG_ACTIVE_PAGE, input.getDefaultPageId());
        }
        if (!CommonUtils.isEmpty(input.getDefaultFolderId())) {
            memento.putString(TAG_ACTIVE_FOLDER, input.getDefaultFolderId());
        }
    }

}