/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.views.session;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.ISearchContextProvider;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;

/**
 * AbstractSessionEditor
 */
public abstract class AbstractSessionEditor extends SinglePageDatabaseEditor<IEditorInput> implements ISearchContextProvider
{
    private SessionManagerViewer sessionsViewer;

    public SessionManagerViewer getSessionsViewer() {
        return sessionsViewer;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == ISearchContextProvider.class) {
            return adapter.cast(this);
        }
        return super.getAdapter(adapter);
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
    public void createEditorControl(Composite parent) {
        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext != null) {
            setPartName("Sessions - " + executionContext.getDataSource().getContainer().getName());
            sessionsViewer = createSessionViewer(executionContext, parent);
            sessionsViewer.loadSettings(this);
            sessionsViewer.refreshSessions();
        }
    }

    protected abstract SessionManagerViewer createSessionViewer(DBCExecutionContext executionContext, Composite parent);

    @Override
    public RefreshResult refreshPart(Object source, boolean force)
    {
        sessionsViewer.refreshSessions();
        return RefreshResult.REFRESHED;
    }

    @Override
    public void setFocus() {
        if (sessionsViewer != null) {
            sessionsViewer.getControl().setFocus();
        }
    }

    // ISearchContextProvider

    @Override
    public boolean isSearchPossible() {
        return true;
    }

    @Override
    public boolean isSearchEnabled() {
        return true;
    }

    @Override
    public boolean performSearch(SearchType searchType) {
        if (sessionsViewer != null) {
            return sessionsViewer.getSessionListControl().performSearch(searchType);
        }
        return false;
    }

}