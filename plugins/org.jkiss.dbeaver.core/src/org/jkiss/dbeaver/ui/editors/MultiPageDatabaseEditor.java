/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * MultiPageDatabaseEditor
 */
public abstract class MultiPageDatabaseEditor extends MultiPageAbstractEditor implements IDatabaseEditor, IDataSourceProvider
{
    private DatabaseEditorListener listener;

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);

        listener = new DatabaseEditorListener(this);
    }

    @Override
    public void dispose()
    {
        listener.dispose();
        super.dispose();
    }

    @Override
    public IDatabaseEditorInput getEditorInput() {
        return (IDatabaseEditorInput)super.getEditorInput();
    }

    @Override
    public DBPDataSource getDataSource() {
        return getEditorInput().getDataSource();
    }

    @Override
    protected void createPages()
    {
        super.createPages();
        DatabaseEditorUtils.setPartBackground(this, getContainer());
    }

}