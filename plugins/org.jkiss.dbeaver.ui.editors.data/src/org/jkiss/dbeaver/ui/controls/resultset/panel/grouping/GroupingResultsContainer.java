/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.DataEditorFeatures;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.view.EmptyPresentation;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GroupingResultsContainer implements IResultSetContainer {

    private static final Log log = Log.getLog(GroupingResultsContainer.class);

    public static final String FUNCTION_COUNT = "COUNT";

    private final IResultSetPresentation presentation;
    private final GroupingDataContainer dataContainer;
    private final ResultSetViewer groupingViewer;
    private final List<SQLGroupingAttribute> groupAttributes = new ArrayList<>();
    private final List<String> groupFunctions = new ArrayList<>();

    public GroupingResultsContainer(Composite parent, IResultSetPresentation presentation) {
        this.presentation = presentation;
        this.dataContainer = new GroupingDataContainer(presentation.getController());
        this.groupingViewer = new ResultSetViewer(parent, presentation.getController().getSite(), this);

        initDefaultSettings();
    }

    private String getDefaultFunction() {
        DBPDataSource dataSource = dataContainer.getDataSource();
        return FUNCTION_COUNT + "(" +
            (dataSource == null ? SQLConstants.COLUMN_ASTERISK :
            dataSource.getSQLDialect().getDefaultGroupAttribute()) + ")";
    }

    private void initDefaultSettings() {
        this.groupAttributes.clear();
        this.groupFunctions.clear();
        addGroupingFunctions(Collections.singletonList(getDefaultFunction()));
    }

    public IResultSetPresentation getOwnerPresentation() {
        return presentation;
    }

    public List<SQLGroupingAttribute> getGroupAttributes() {
        return groupAttributes;
    }

    public List<String> getGroupFunctions() {
        return groupFunctions;
    }

    @Nullable
    @Override
    public DBPProject getProject() {
        DBSDataContainer dataContainer = getDataContainer();
        return dataContainer == null || dataContainer.getDataSource() == null ? null : dataContainer.getDataSource().getContainer().getProject();
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return presentation.getController().getExecutionContext();
    }

    @NotNull
    @Override
    public IResultSetController getResultSetController() {
        return groupingViewer;
    }

    @NotNull
    @Override
    public DBSDataContainer getDataContainer() {
        return this.dataContainer;
    }

    @Override
    public boolean isReadyToRun() {
        return true;
    }

    @Override
    public void openNewContainer(DBRProgressMonitor monitor, @NotNull DBSDataContainer dataContainer, @NotNull DBDDataFilter newFilter) {

    }

    @Override
    public IResultSetDecorator createResultSetDecorator() {
        return new GroupingResultsDecorator(this);
    }

    void clearGroupingAttributes() {
        groupAttributes.clear();
    }

    void addGroupingAttributes(List<SQLGroupingAttribute> attributes) {
        for (SQLGroupingAttribute attr : attributes) {
            if (!groupAttributes.contains(attr)) {
                groupAttributes.add(attr);
            }
        }
    }

    boolean removeGroupingAttribute(List<SQLGroupingAttribute> attributes) {
        boolean changed = false;
        for (SQLGroupingAttribute attr : attributes) {
            if (groupAttributes.contains(attr)) {
                groupAttributes.remove(attr);
                changed = true;
            }
        }
        if (changed) {
            resetDataFilters();
        }
        return changed;
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
        groupingViewer.clearData(true);

        groupingViewer.clearDataFilter(false);
        groupingViewer.resetHistory();
        dataContainer.setGroupingQuery(null);
        dataContainer.setGroupingAttributes(null);
        if (!(groupingViewer.getActivePresentation() instanceof EmptyPresentation)) {
            groupingViewer.showEmptyPresentation();
        }
    }

    public void rebuildGrouping() throws DBException {
        if (groupAttributes.isEmpty() || groupFunctions.isEmpty()) {
            groupingViewer.showEmptyPresentation();
            return;
        }
        DBCStatistics statistics = presentation.getController().getModel().getStatistics();
        if (statistics == null) {
            throw new DBException("No main query - can't perform grouping");
        }
        DBSDataContainer dbsDataContainer = presentation.getController().getDataContainer();
        boolean isCustomQuery = !(dbsDataContainer instanceof DBSEntity);
        DBPDataSource dataSource = dataContainer.getDataSource();
        if (dataSource == null) {
            throw new DBException("No active datasource");
        }
        SQLDialect dialect = SQLUtils.getDialectFromDataSource(dataSource);
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, presentation.getController().getPreferenceStore());
        String queryText = statistics.getQueryText();
        boolean isShowDuplicatesOnly = dataSource.getContainer().getPreferenceStore().getBoolean(ResultSetPreferences.RS_GROUPING_SHOW_DUPLICATES_ONLY);

        var groupingQueryGenerator = new SQLGroupingQueryGenerator(dataSource, dbsDataContainer, dialect, syntaxManager, groupAttributes, groupFunctions, isShowDuplicatesOnly);
        dataContainer.setGroupingQuery(groupingQueryGenerator.generateGroupingQuery(queryText));
        dataContainer.setGroupingAttributes(groupAttributes.toArray(SQLGroupingAttribute[]::new));
        DBDDataFilter dataFilter;
        if (presentation.getController().getModel().isMetadataChanged()) {
            dataFilter = new DBDDataFilter();
        } else {
            dataFilter = new DBDDataFilter(groupingViewer.getModel().getDataFilter());
        }

        boolean isDefaultGrouping = groupFunctions.size() == 1 && groupFunctions.get(0).equalsIgnoreCase(getDefaultFunction());
        String defaultSorting = dataSource.getContainer().getPreferenceStore().getString(ResultSetPreferences.RS_GROUPING_DEFAULT_SORTING);
        if (!CommonUtils.isEmpty(defaultSorting) && isDefaultGrouping) {
            if (false/*dialect.supportsOrderByIndex()*/) {
                // By default sort by count in desc order
                int countPosition = groupAttributes.size() + 1;
                StringBuilder orderBy = new StringBuilder();
                orderBy.append(countPosition).append(" ").append(defaultSorting);
                for (int i = 0; i < groupAttributes.size(); i++) {
                    orderBy.append(",").append(i + 1);
                }
                dataFilter.setOrder(orderBy.toString());
            } else {
                var funcAliases = groupingQueryGenerator.getFuncAliases();
                dataFilter.setOrder(funcAliases[funcAliases.length - 1] + " " + defaultSorting);
            }
        }
        DataEditorFeatures.RESULT_SET_PANEL_GROUPING.use(Map.of(
            "custom", isCustomQuery,
            "default", isDefaultGrouping,
            "dups", isShowDuplicatesOnly));
        groupingViewer.setDataFilter(dataFilter, true);
        //groupingViewer.refresh();
    }

    void setGrouping(List<SQLGroupingAttribute> attributes, List<String> functions) {
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
