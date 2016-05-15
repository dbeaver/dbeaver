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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.ProjectFileEditorInput;

/**
 * SQLEditorInput
 */
public class SQLEditorInput extends ProjectFileEditorInput implements IPersistableElement, DBPContextProvider, IDataSourceContainerProvider
{

    private static final Log log = Log.getLog(SQLEditorInput.class);

    public SQLEditorInput(IFile file)
    {
        super(file);
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer()
    {
        return EditorUtils.getScriptDataSource(getFile());
    }

    @Override
    public String getFactoryId()
    {
//        if (!restoreEditorState()) {
//            return null;
//        }
        return SQLEditorInputFactory.getFactoryId();
    }

    @Override
    public void saveState(IMemento memento)
    {
        SQLEditorInputFactory.saveState(memento, this);
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext()
    {
        DBPDataSourceContainer container = getDataSourceContainer();
        if (container != null) {
            DBPDataSource dataSource = container.getDataSource();
            if (dataSource != null) {
                return dataSource.getDefaultContext(false);
            }
        }
        return null;
    }

}
