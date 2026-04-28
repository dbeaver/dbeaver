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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.column.impl;

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
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingResultsContainer;

import java.util.List;
import java.util.Objects;

public class PercentGroupingFunctionColumn extends TransformerGroupingFunctionColumn {


    private static final String FUNCTION_COUNT = "COUNT";

    public PercentGroupingFunctionColumn(
        @NotNull DBPDataSource dataSource,
        @NotNull GroupingResultsContainer groupingResultsContainer,
        @NotNull String preferenceKey
    ) {
        super(dataSource, groupingResultsContainer, preferenceKey);
    }

    @NotNull
    @Override
    public DBDAttributeTransformer getTransformer() {
        return new PercentOfTotalGroupingAttributeTransformer(this::getTotalRowCount);
    }

    @NotNull
    @Override
    public String getColumnExpression() {
        return FUNCTION_COUNT + "(" + dataSource.getSQLDialect().getDefaultGroupAttribute() + ")";
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
}
