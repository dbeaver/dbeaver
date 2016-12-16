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

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

/**
 * MultiPageDatabaseEditor
 */
public abstract class MultiPageDatabaseEditor extends MultiPageAbstractEditor implements IDatabaseEditor, DBPContextProvider
{
    private DatabaseEditorListener listener;

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);
    }

    @Override
    public void dispose()
    {
        if (listener != null) {
            listener.dispose();
        }
        super.dispose();
    }

    @Override
    public IDatabaseEditorInput getEditorInput() {
        return (IDatabaseEditorInput)super.getEditorInput();
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);

        if (listener != null) {
            listener.dispose();
        }
        listener = new DatabaseEditorListener(this);
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return getEditorInput().getExecutionContext();
    }

    @Override
    protected void createPages()
    {
        super.createPages();
        DatabaseEditorUtils.setPartBackground(this, getContainer());
    }

}