/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.controls.resultset.*;

public class GroupingResultsContainer implements IResultSetContainer {

    private final IResultSetPresentation presentation;
    private GroupingDataContainer dataContainer;
    private ResultSetViewer groupingViewer;

    public GroupingResultsContainer(Composite parent, IResultSetPresentation presentation) {
        this.presentation = presentation;
        this.dataContainer = new GroupingDataContainer(presentation.getController());
        this.groupingViewer = new ResultSetViewer(parent, presentation.getController().getSite(), this);
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return presentation.getController().getExecutionContext();
    }

    @Override
    public IResultSetController getResultSetController() {
        return groupingViewer;
    }

    @Override
    public DBSDataContainer getDataContainer() {
        return null;
    }

    @Override
    public boolean isReadyToRun() {
        return true;
    }

    @Override
    public void openNewContainer(DBRProgressMonitor monitor, DBSDataContainer dataContainer, DBDDataFilter newFilter) {

    }

    @Override
    public IResultSetDecorator createResultSetDecorator() {
        return new GroupingResultsDecorator();
    }

}
