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
package org.jkiss.dbeaver.ui.views.session;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;

/**
 * AbstractSessionEditor
 */
public abstract class AbstractSessionEditor extends SinglePageDatabaseEditor<IDatabaseEditorInput>
{
    private SessionManagerViewer sessionsViewer;

    public SessionManagerViewer getSessionsViewer() {
        return sessionsViewer;
    }

    @Override
    public void dispose()
    {
        if (sessionsViewer != null) {
            sessionsViewer.dispose();
        }
        super.dispose();
    }

    @Override
    public void createPartControl(Composite parent) {
        final DBCExecutionContext executionContext = getExecutionContext();
        assert executionContext != null;

        setPartName("Sessions - " + executionContext.getDataSource().getContainer().getName());
        sessionsViewer = createSessionViewer(executionContext, parent);
        sessionsViewer.refreshSessions();
    }

    protected abstract SessionManagerViewer createSessionViewer(DBCExecutionContext executionContext, Composite parent);

    @Override
    public void refreshPart(Object source, boolean force)
    {
        sessionsViewer.refreshSessions();
    }

}