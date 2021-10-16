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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
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
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.view.EmptyPresentation;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupingResultsContainer implements IResultSetContainer {

    private static final Log log = Log.getLog(GroupingResultsContainer.class);

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

    void addGroupingAttributes(List<String> attributes) {
        for (String attrName : attributes) {
            attrName = cleanupObjectName(attrName);
            if (!groupAttributes.contains(attrName)) {
                groupAttributes.add(attrName);
            }
        }
    }

    boolean removeGroupingAttribute(List<String> attributes) {
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
        groupingViewer.clearDataFilter(false);
        groupingViewer.resetHistory();
        dataContainer.setGroupingQuery(null);
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
        DBPDataSource dataSource = dataContainer.getDataSource();
        if (dataSource == null) {
            throw new DBException("No active datasource");
        }
        SQLDialect dialect = SQLUtils.getDialectFromDataSource(dataSource);
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, presentation.getController().getPreferenceStore());
        String queryText = statistics.getQueryText();
        if (queryText == null || queryText.isEmpty()) {
            DBSDataContainer dataContainer = presentation.getController().getDataContainer();
            if (dataContainer != null) {
                queryText = dataContainer.getName();
            } else {
                throw new DBException("Empty data container");
            }
        }
        for (String delimiter : syntaxManager.getStatementDelimiters()) {
            while (queryText.endsWith(delimiter)) {
                queryText = queryText.substring(0, queryText.length() - delimiter.length());
            }
        }

        StringBuilder sql = new StringBuilder();
        if (dialect.supportsSubqueries()) {
            sql.append("SELECT ");
            for (int i = 0; i < groupAttributes.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(quotedGroupingString(dataSource, groupAttributes.get(i)));
            }
            for (String func : groupFunctions) {
                sql.append(", ").append(func);
            }
            sql.append(" FROM (\n");
            sql.append(queryText);
            sql.append("\n) src");
        } else {
            try {
                Statement statement = SQLSemanticProcessor.parseQuery(dataSource.getSQLDialect(), queryText);
                if (statement instanceof Select && ((Select) statement).getSelectBody() instanceof PlainSelect) {
                    PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();
                    select.setOrderByElements(null);

                    List<SelectItem> selectItems = new ArrayList<>();
                    select.setSelectItems(selectItems);
                    for (String groupAttribute : groupAttributes) {
                        selectItems.add(
                            new SelectExpressionItem(
                                new Column(
                                    quotedGroupingString(dataSource, groupAttribute))));
                    }
                    for (String func : groupFunctions) {
                        Expression expression = CCJSqlParserUtil.parseExpression(func);
                        selectItems.add(new SelectExpressionItem(expression));
                    }
                }
                queryText = statement.toString();
            } catch (Throwable e) {
                log.debug("SQL parse error", e);
            }
            sql.append(queryText);
        }

        sql.append("\nGROUP BY ");
        for (int i = 0; i < groupAttributes.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(quotedGroupingString(dataSource, groupAttributes.get(i)));
        }
        boolean isDefaultGrouping = groupFunctions.size() == 1 && groupFunctions.get(0).equals(DEFAULT_FUNCTION);

        boolean isShowDuplicatesOnly = dataSource.getContainer().getPreferenceStore().getBoolean(ResultSetPreferences.RS_GROUPING_SHOW_DUPLICATES_ONLY);
        if (isDefaultGrouping && isShowDuplicatesOnly) {
            sql.append("\nHAVING ").append(DEFAULT_FUNCTION).append(" > 1");
        }

        dataContainer.setGroupingQuery(sql.toString());
        DBDDataFilter dataFilter;
        if (presentation.getController().getModel().isMetadataChanged()) {
            dataFilter = new DBDDataFilter();
        } else {
            dataFilter = new DBDDataFilter(groupingViewer.getModel().getDataFilter());
        }

        String defaultSorting = dataSource.getContainer().getPreferenceStore().getString(ResultSetPreferences.RS_GROUPING_DEFAULT_SORTING);
        if (!CommonUtils.isEmpty(defaultSorting) && isDefaultGrouping) {
            if (dialect.supportsOrderByIndex()) {
                // By default sort by count in desc order
                int countPosition = groupAttributes.size() + 1;
                StringBuilder orderBy = new StringBuilder();
                orderBy.append(countPosition).append(" ").append(defaultSorting);
                for (int i = 0; i < groupAttributes.size(); i++) {
                    orderBy.append(",").append(i + 1);
                }
                dataFilter.setOrder(orderBy.toString());
            } else {
                dataFilter.setOrder(groupFunctions.get(groupFunctions.size() - 1) + " " + defaultSorting);
            }
        }
        groupingViewer.setDataFilter(dataFilter, true);
        //groupingViewer.refresh();
    }

    void setGrouping(List<String> attributes, List<String> functions) {
        groupAttributes.clear();
        addGroupingAttributes(attributes);

        groupFunctions.clear();
        addGroupingFunctions(functions);

        resetDataFilters();
    }

    private void resetDataFilters() {
        groupingViewer.getModel().createDataFilter();
    }

    private String quotedGroupingString(DBPDataSource dataSource, String string) {
        try {
            Expression expression = CCJSqlParserUtil.parseExpression(string);
            if (!(expression instanceof Column)) {
                return string;
            }
        } catch (JSQLParserException e) {
            log.debug("Can't parse expression " + string, e);
        }
        return DBUtils.getQuotedIdentifier(dataSource, string);
    }
}
