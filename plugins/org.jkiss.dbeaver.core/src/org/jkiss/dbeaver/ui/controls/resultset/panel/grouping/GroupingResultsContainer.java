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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.view.EmptyPresentation;

import java.util.ArrayList;
import java.util.List;

public class GroupingResultsContainer implements IResultSetContainer {

    private final IResultSetPresentation presentation;
    private GroupingDataContainer dataContainer;
    private ResultSetViewer groupingViewer;
    private List<String> groupAttributes = new ArrayList<>();
    private List<String> groupFunctions = new ArrayList<>();

    public GroupingResultsContainer(Composite parent, IResultSetPresentation presentation) {
        this.presentation = presentation;
        this.dataContainer = new GroupingDataContainer(presentation.getController());
        this.groupingViewer = new ResultSetViewer(parent, presentation.getController().getSite(), this);

        initDefaultSettings();
    }

    private void initDefaultSettings() {
        this.groupAttributes.clear();
        this.groupFunctions.clear();
        this.groupFunctions.add("COUNT(*)");
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
        return this.dataContainer;
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
        return new GroupingResultsDecorator(this);
    }

    public void addGroupingAttribute(List<DBDAttributeBinding> attributes) {
        for (DBDAttributeBinding attr : attributes) {
            String attrName = attr.getFullyQualifiedName(DBPEvaluationContext.DML);
            if (!groupAttributes.contains(attrName)) {
                groupAttributes.add(attrName);
            }
        }
    }

    public void clearGrouping() {
        initDefaultSettings();
        if (!(groupingViewer.getActivePresentation() instanceof EmptyPresentation)) {
            groupingViewer.setEmptyPresentation();
        }
    }

    public void rebuildGrouping() throws DBException {
        DBCStatistics statistics = presentation.getController().getModel().getStatistics();
        if (statistics == null) {
            throw new DBException("No main query - can't perform grouping");
        }
        String queryText = statistics.getQueryText();
        if (queryText == null || queryText.isEmpty()) {
            DBSDataContainer dataContainer = presentation.getController().getDataContainer();
            if (dataContainer != null) {
                queryText = dataContainer.getName();
            } else {
                throw new DBException("Empty data container");
            }
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        for (int i = 0; i < groupAttributes.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(groupAttributes.get(i));
        }
        for (String func : groupFunctions) {
            sql.append(", ").append(func);
        }
        sql.append(" FROM (\n");
        sql.append(queryText);
        sql.append(") src\nGROUP BY ");
        for (int i = 0; i < groupAttributes.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(groupAttributes.get(i));
        }

        dataContainer.setGroupingQuery(sql.toString());
        groupingViewer.refresh();
    }

}
