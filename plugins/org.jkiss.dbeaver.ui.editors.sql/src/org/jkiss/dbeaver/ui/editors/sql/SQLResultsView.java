/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.controls.resultset.*;

public class SQLResultsView extends ViewPart
{
    public static final String VIEW_ID = "org.jkiss.dbeaver.core.sqlResults";

    private ResultSetViewer viewer;
    private DetachedContainer detachedContainer = new DetachedContainer();

    public ResultSetViewer getViewer() {
        return viewer;
    }

    public void setContainer(IResultSetContainer container) {
        detachedContainer.currentContainer = container;
        final DBSDataContainer dataContainer = container.getDataContainer();
        String partName = dataContainer == null ? "Data" : dataContainer.getName();
        setPartName(partName);
    }

    @Override
    public void createPartControl(Composite parent)
    {
        viewer = new ResultSetViewer(parent, getSite(), detachedContainer);
    }

    @Override
    public void setFocus()
    {
        viewer.getControl().setFocus();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        if (adapter == ResultSetViewer.class) {
            return adapter.cast(viewer);
        }
        return viewer.getAdapter(adapter);
    }

    private class DetachedContainer implements IResultSetContainer {
        private IResultSetContainer currentContainer;

        @Nullable
        @Override
        public DBPProject getProject() {
            return currentContainer == null ? null : currentContainer.getProject();
        }

        @Nullable
        @Override
        public DBCExecutionContext getExecutionContext() {
            return currentContainer == null ? null : currentContainer.getExecutionContext();
        }

        @Nullable
        @Override
        public IResultSetController getResultSetController() {
            return currentContainer == null ? null : currentContainer.getResultSetController();
        }

        @Nullable
        @Override
        public DBSDataContainer getDataContainer() {
            return currentContainer == null ? null : currentContainer.getDataContainer();
        }

        @Override
        public boolean isReadyToRun() {
            return currentContainer != null && currentContainer.isReadyToRun();
        }

        @Override
        public void openNewContainer(DBRProgressMonitor monitor, @NotNull DBSDataContainer dataContainer, @NotNull DBDDataFilter newFilter) {
            DBWorkbench.getPlatformUI().showError("Data container", "Not supported");
        }

        @Override
        public IResultSetDecorator createResultSetDecorator() {
            return new QueryResultsDecorator();
        }
    }

}
