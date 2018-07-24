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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.view.EmptyPresentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupingResultsContainer implements IResultSetContainer {

    public static final String FUNCTION_COUNT = "COUNT";

    public static final String DEFAULT_FUNCTION = FUNCTION_COUNT + "(*)";

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
        addGroupingFunctions(Collections.singletonList(DEFAULT_FUNCTION));
    }

    public IResultSetPresentation getOwnerPresentation() {
        return presentation;
    }

    public List<String> getGroupAttributes() {
        return groupAttributes;
    }

    public List<String> getGroupFunctions() {
        return groupFunctions;
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

    public void addGroupingAttributes(List<String> attributes) {
        for (String attrName : attributes) {
            attrName = cleanupObjectName(attrName);
            if (!groupAttributes.contains(attrName)) {
                groupAttributes.add(attrName);
            }
        }
    }

    public boolean removeGroupingAttribute(List<String> attributes) {
        boolean changed = false;
        for (String attrName : attributes) {
            attrName = cleanupObjectName(attrName);
            if (groupAttributes.contains(attrName)) {
                groupAttributes.remove(attrName);
                changed = true;
            }
        }
        if (changed) {
            resetDataFilters();
        }
        return changed;
    }

    private String cleanupObjectName(String attrName) {
        DBPDataSource dataSource = getDataContainer().getDataSource();
        if (DBUtils.isQuotedIdentifier(dataSource, attrName)) {
            attrName = DBUtils.getUnQuotedIdentifier(dataSource, attrName);
        } else {
            attrName = DBObjectNameCaseTransformer.transformName(dataSource, attrName);
        }
        return attrName;
    }

    public void addGroupingFunctions(List<String> functions) {
        for (String func : functions) {
            func = DBUtils.getUnQuotedIdentifier(getDataContainer().getDataSource(), func);
            if (!groupFunctions.contains(func)) {
                groupFunctions.add(func);
            }
        }
    }

    public boolean removeGroupingFunction(List<String> attributes) {
        boolean changed = false;
        for (String func : attributes) {
            func = DBUtils.getUnQuotedIdentifier(getDataContainer().getDataSource(), func);
            if (groupFunctions.contains(func)) {
                groupFunctions.remove(func);
                changed = true;
            }
        }
        return changed;
    }

    public void clearGrouping() {
        initDefaultSettings();
        groupingViewer.resetDataFilter(false);
        groupingViewer.resetHistory();
        dataContainer.setGroupingQuery(null);
        if (!(groupingViewer.getActivePresentation() instanceof EmptyPresentation)) {
            groupingViewer.setEmptyPresentation();
        }
    }

    public void rebuildGrouping() throws DBException {
        if (groupAttributes.isEmpty() || groupFunctions.isEmpty()) {
            getResultSetController().setEmptyPresentation();
            return;
        }
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

    public void setGrouping(List<String> attributes, List<String> functions) {
        groupAttributes.clear();
        addGroupingAttributes(attributes);

        groupFunctions.clear();
        addGroupingFunctions(functions);

        resetDataFilters();
    }

    private void resetDataFilters() {
        groupingViewer.getModel().createDataFilter();
    }
}
