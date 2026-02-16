/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.impl.data.transformers.PercentOfTotalGroupingAttributeTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.DataEditorFeatures;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.view.EmptyPresentation;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class GroupingResultsContainer implements IResultSetContainer {

    private static final Log log = Log.getLog(GroupingResultsContainer.class);

    public static final String FUNCTION_COUNT = "COUNT";

    private final IResultSetPresentation presentation;
    private final GroupingDataContainer dataContainer;
    private final ResultSetViewer groupingViewer;
    private final GroupingColumnsContainer columnsContainer = new GroupingColumnsContainer();
    private final AtomicReference<DBDDataFilter> currentFiler = new AtomicReference<>();

    public GroupingResultsContainer(Composite parent, IResultSetPresentation presentation) {
        this.presentation = presentation;
        this.dataContainer = new GroupingDataContainer(presentation.getController());
        this.groupingViewer = new ResultSetViewer(parent, presentation.getController().getSite(), this) {
            @Override
            public void refreshWithFilter(DBDDataFilter filter) {
                currentFiler.set(filter);
                super.refreshWithFilter(filter);
            }
        };

        initDefaultSettings();
    }

    private String getDefaultFunction() {
        DBPDataSource dataSource = dataContainer.getDataSource();
        return FUNCTION_COUNT + "(" +
            (dataSource == null ? SQLConstants.COLUMN_ASTERISK :
                dataSource.getSQLDialect().getDefaultGroupAttribute()) + ")";
    }

    private void initDefaultSettings() {
        columnsContainer.clear();
        removePercentColumn();
    }

    public IResultSetPresentation getOwnerPresentation() {
        return presentation;
    }

    @NotNull
    public List<SQLGroupingAttribute> getGroupAttributes() {
        return columnsContainer.getGroupAttributes();
    }

    @NotNull
    public List<String> getUserDefinedGroupFunctions() {
        return columnsContainer.getUserDefinedFunctions();
    }

    @Nullable
    @Override
    public DBPProject getProject() {
        DBSDataContainer dataContainer = getDataContainer();
        return dataContainer == null || dataContainer.getDataSource() == null
            ? null
            : dataContainer.getDataSource().getContainer().getProject();
    }

    @Nullable
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

    @Nullable
    @Override
    public IResultSetContainer getParentContainer() {
        return presentation.getController().getContainer();
    }

    void addGroupingAttributes(@NotNull List<SQLGroupingAttribute> attributes) {
        attributes.forEach(columnsContainer::addAttribute);
    }

    public boolean removeColumns(@NotNull List<GroupingColumnsContainer.RemoveColumnStrategy> removeStrategies) {
        Set<GroupingColumnsContainer.InstanceType> executedStrategies = removeStrategies.stream()
            .filter(GroupingColumnsContainer.RemoveColumnStrategy::removeColumn)
            .map(GroupingColumnsContainer.RemoveColumnStrategy::getType)
            .collect(Collectors.toSet());

        if (executedStrategies.contains(GroupingColumnsContainer.InstanceType.ATTRIBUTE)) {
            resetDataFilters();
        }
        if (executedStrategies.contains(GroupingColumnsContainer.InstanceType.PERCENT_GROUPING_FUNCTION)) {
            removePercentColumn();
            DBPDataSource dataSource = getDataContainer().getDataSource();
            if (dataSource != null) {
                dataSource.getContainer().getPreferenceStore().setValue(ResultSetPreferences.RS_GROUPING_SHOW_PERCENT_OF_TOTAL_ROWS, false);
            }
        }
        return !executedStrategies.isEmpty();
    }

    public void addGroupingFunctions(@NotNull List<String> functions) {
        for (String func : functions) {
            DBPDataSource dataSource = getDataContainer().getDataSource();
            if (dataSource != null) {
                func = DBUtils.getUnQuotedIdentifier(dataSource, func);
                columnsContainer.addGroupingFunction(func);
            }
        }
    }

    public void clearGrouping() {
        initDefaultSettings();
        groupingViewer.clearData(true);

        groupingViewer.clearDataFilter(false);
        groupingViewer.resetHistory();
        dataContainer.setGroupingQuery(null);
        dataContainer.setGroupingAttributes(null);
        dataContainer.removeAttributeTransformer();
        if (!(groupingViewer.getActivePresentation() instanceof EmptyPresentation)) {
            groupingViewer.showEmptyPresentation();
        }
    }

    public void rebuildGrouping() throws DBException {
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
        addPercentColumn(dataSource);
        if (columnsContainer.isEmpty()) {
            groupingViewer.showEmptyPresentation();
            return;
        }
        SQLDialect dialect = SQLUtils.getDialectFromDataSource(dataSource);
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, presentation.getController().getPreferenceStore());
        String queryText = statistics.getQueryText();
        boolean isShowDuplicatesOnly = dataSource.getContainer().getPreferenceStore()
            .getBoolean(ResultSetPreferences.RS_GROUPING_SHOW_DUPLICATES_ONLY);
        DBDDataFilter dataFilter = getDataFilter();

        var groupAttributes = columnsContainer.getGroupAttributes();
        var groupFunctions = columnsContainer.getGroupFunctions();
        var groupingQueryGenerator = new SQLGroupingQueryGenerator(
            dataSource,
            dbsDataContainer,
            dialect,
            syntaxManager,
            groupAttributes,
            groupFunctions,
            isShowDuplicatesOnly
        );
        dataContainer.setGroupingQuery(groupingQueryGenerator.generateGroupingQuery(queryText));
        dataContainer.setGroupingAttributes(groupAttributes.toArray(SQLGroupingAttribute[]::new));

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
            "dups", isShowDuplicatesOnly
        ));
        groupingViewer.setDataFilter(dataFilter, true);
    }

    public void removePercentColumn() {
        dataContainer.removeAttributeTransformer();
        columnsContainer.removePercentFunction();
        if (columnsContainer.isFunctionsEmpty()) {
            addGroupingFunctions(Collections.singletonList(getDefaultFunction()));
        }
    }

    @NotNull
    public GroupingColumnsContainer getColumnsContainer() {
        return columnsContainer;
    }

    @NotNull
    private DBDDataFilter getDataFilter() {
        return presentation.getController().getModel().isMetadataChanged()
            ? new DBDDataFilter()
            : new DBDDataFilter(groupingViewer.getModel().getDataFilter());
    }

    void setGrouping(List<SQLGroupingAttribute> attributes, List<String> functions) {
        columnsContainer.clear();
        addGroupingAttributes(attributes);
        addGroupingFunctions(functions);
        resetDataFilters();
    }

    private void addPercentColumn(@NotNull DBPDataSource dataSource) {
        boolean isShowTotalPercentColumn = dataSource.getContainer().getPreferenceStore()
            .getBoolean(ResultSetPreferences.RS_GROUPING_SHOW_PERCENT_OF_TOTAL_ROWS);
        if (isShowTotalPercentColumn) {
            if (!columnsContainer.isPercentFunctionPresent()) {
                columnsContainer.addPercentFunction(getDefaultFunction());
            }
            dataContainer.setAttributeTransformer(
                columnsContainer.getPercentFunctionIndex(),
                new PercentOfTotalGroupingAttributeTransformer(this::getTotalRowCount)
            );
        }
    }

    private long getTotalRowCount(@NotNull DBRProgressMonitor monitor) throws DBException {
        return DBUtils.readRowCount(
            monitor,
            groupingViewer.getExecutionContext(),
            presentation.getController().getDataContainer(),
            filterExcludingGroupingColumns(),
            groupingViewer
        );
    }

    @Nullable
    private DBDDataFilter filterExcludingGroupingColumns() {
        DBDDataFilter dataFilter = currentFiler.get();
        if (dataFilter == null) {
            return null;
        }
        List<DBDAttributeConstraint> attributeConstraints = columnsContainer.getGroupAttributes().stream()
            .map(ga -> ga instanceof SQLGroupingAttribute.BoundAttribute boundAttribute
                ? boundAttribute.getBindingName()
                : ga.getDisplayName())
            .map(dataFilter::getConstraint)
            .filter(Objects::nonNull).toList();
        DBDDataFilter newFilter = new DBDDataFilter(attributeConstraints);
        newFilter.setWhere(dataFilter.getWhere());
        return newFilter;
    }

    private void resetDataFilters() {
        groupingViewer.getModel().createDataFilter();
    }
}
