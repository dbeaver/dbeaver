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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.column;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.impl.data.transformers.PercentOfTotalGroupingAttributeTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLGroupingAttribute;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingDataContainer;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingResultsContainer;

import java.util.List;
import java.util.Objects;

public class PercentGroupingFunctionColumn extends TransformerGroupingFunctionColumn implements UniqueGroupingColumn {

    public static final String PERCENT_FUNCTION_ID = "percent_func";

    private static final String FUNCTION_COUNT = "COUNT";

    private final GroupingResultsContainer groupingResultsContainer;

    public PercentGroupingFunctionColumn(
        @NotNull DBPDataSource dataSource,
        @NotNull GroupingResultsContainer groupingResultsContainer
    ) {
        super(getCountFunction(dataSource), dataSource);
        this.groupingResultsContainer = groupingResultsContainer;
    }


    @Override
    public boolean afterDeleteAction() {
        GroupingDataContainer dataContainer = groupingResultsContainer.getDataContainer();
        DBPDataSource dataSource = dataContainer.getDataSource();
        if (dataSource != null) {
            dataSource.getContainer().getPreferenceStore().setValue(ResultSetPreferences.RS_GROUPING_SHOW_PERCENT_OF_TOTAL_ROWS, false);
        }
        return true;
    }

    @Override
    public boolean isShowToUser() {
        return false;
    }


    @NotNull
    @Override
    public String getId() {
        return PERCENT_FUNCTION_ID;
    }

    @NotNull
    @Override
    public DBDAttributeTransformer getTransformer() {
        return new PercentOfTotalGroupingAttributeTransformer(this::getTotalRowCount);
    }

    private long getTotalRowCount(@NotNull DBRProgressMonitor monitor) throws DBException {
        return DBUtils.readRowCount(
            monitor,
            groupingResultsContainer.getResultSetController().getExecutionContext(),
            groupingResultsContainer.getOwnerPresentation().getController().getDataContainer(),
            filterExcludingGroupingColumns(),
            groupingResultsContainer.getResultSetController()
        );
    }

    @Nullable
    private DBDDataFilter filterExcludingGroupingColumns() {
        DBDDataFilter dataFilter = groupingResultsContainer.getCurrentFilter();
        if (dataFilter == null) {
            return null;
        }
        // rows count can not be filtered with grouping functions
        List<DBDAttributeConstraint> attributeConstraints = groupingResultsContainer.getGroupAttributes().stream()
            .map(ga -> ga instanceof SQLGroupingAttribute.BoundAttribute boundAttribute
                ? boundAttribute.getBindingName()
                : ga.getDisplayName())
            .map(dataFilter::getConstraint)
            .filter(Objects::nonNull).toList();
        DBDDataFilter newFilter = new DBDDataFilter(attributeConstraints);
        newFilter.setWhere(dataFilter.getWhere());
        return newFilter;
    }

    private static String getCountFunction(@NotNull DBPDataSource dataSource) {
        return FUNCTION_COUNT + "(" + dataSource.getSQLDialect().getDefaultGroupAttribute() + ")";
    }
}
