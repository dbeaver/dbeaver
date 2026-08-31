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
package org.jkiss.dbeaver.ext.duckdb.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.duckdb.model.DuckDBDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Exports the current result set to a Parquet file using the DuckDB {@code COPY (...) TO} statement.
 * <p>
 * Parquet is an immutable columnar format, so this is a full-file rewrite rather than an in-place
 * update. The query behind the result set (with any active grid filters/sorting applied) is written
 * out natively by DuckDB. The command is only available for DuckDB connections.
 */
public class ExportResultSetToParquetHandler extends AbstractHandler {

    private static final Log log = Log.getLog(ExportResultSetToParquetHandler.class);

    private static final String TASK_TITLE = "Export to Parquet file";

    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        IWorkbenchPart part = HandlerUtil.getActivePart(event);
        IResultSetController resultSet = ResultSetHandlerMain.getActiveResultSet(part);
        if (resultSet == null) {
            DBWorkbench.getPlatformUI().showError(TASK_TITLE, "No active result set found");
            return null;
        }
        DBCExecutionContext context = resultSet.getExecutionContext();
        if (context == null) {
            DBWorkbench.getPlatformUI().showError(TASK_TITLE, "There is no active connection for this result set");
            return null;
        }
        DBPDataSource dataSource = context.getDataSource();
        if (!(dataSource instanceof DuckDBDataSource)) {
            DBWorkbench.getPlatformUI().showError(TASK_TITLE, "Export to Parquet is only available for DuckDB connections");
            return null;
        }

        String sourceQuery;
        try {
            sourceQuery = buildSourceQuery(resultSet, dataSource);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(TASK_TITLE, "Unable to build the export query", e);
            return null;
        }
        if (CommonUtils.isEmptyTrimmed(sourceQuery)) {
            DBWorkbench.getPlatformUI().showError(TASK_TITLE, "Unable to determine the query behind this result set");
            return null;
        }

        Shell shell = HandlerUtil.getActiveShell(event);
        Path targetFile = DialogUtils.selectFileForSave(
            shell, TASK_TITLE, new String[]{"*.parquet", "*"}, suggestFileName(resultSet));
        if (targetFile == null) {
            return null;
        }

        exportToParquet(context, sourceQuery, targetFile);
        return null;
    }

    /**
     * Builds the {@code SELECT} that produced the result set. For SQL editor results this is the
     * query text; for a browsed table/view it is {@code SELECT * FROM <qualified name>}. Any filters
     * or sorting currently applied in the grid are folded in so the export matches what is displayed.
     */
    @Nullable
    private static String buildSourceQuery(@NotNull IResultSetController resultSet, @NotNull DBPDataSource dataSource)
        throws DBException {
        DBSDataContainer dataContainer = resultSet.getDataContainer();
        String baseQuery;
        if (dataContainer instanceof SQLQueryContainer queryContainer) {
            SQLScriptElement query = queryContainer.getQuery();
            baseQuery = query == null ? null : query.getText();
        } else if (dataContainer != null) {
            baseQuery = "SELECT * FROM " + DBUtils.getObjectFullName(dataSource, dataContainer, DBPEvaluationContext.DML);
        } else {
            return null;
        }
        baseQuery = stripTrailingDelimiters(baseQuery);
        if (CommonUtils.isEmptyTrimmed(baseQuery)) {
            return baseQuery;
        }

        DBDDataFilter dataFilter = resultSet.getModel().getDataFilter();
        if (dataFilter != null && dataFilter.hasFilters()) {
            try {
                baseQuery = dataSource.getSQLDialect().addFiltersToQuery(
                    new VoidProgressMonitor(), dataSource, baseQuery, dataFilter);
            } catch (DBException e) {
                // Filters could not be woven in - export the unfiltered query rather than failing
                log.warn("Cannot apply result set filters to the export query", e);
            }
        }
        return baseQuery;
    }

    private void exportToParquet(
        @NotNull DBCExecutionContext context,
        @NotNull String sourceQuery,
        @NotNull Path targetPath
    ) {
        AbstractJob exportJob = new AbstractJob("Export result set to Parquet file") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                monitor.beginTask(TASK_TITLE, 1);
                Path tempPath = null;
                try {
                    Path absoluteTarget = targetPath.toAbsolutePath();
                    Path targetDir = absoluteTarget.getParent();
                    if (targetDir == null) {
                        throw new IOException("Cannot resolve the target directory for " + absoluteTarget);
                    }
                    // Write to a temporary sibling first: an interrupted or failing COPY then never
                    // damages an existing file, and reading from + overwriting the same source
                    // Parquet file stays safe (the read completes before the file is replaced).
                    tempPath = targetDir.resolve(absoluteTarget.getFileName() + ".tmp-" + System.nanoTime());
                    String copySql = "COPY (\n" + sourceQuery + "\n) TO " +
                        quoteStringLiteral(tempPath.toString()) + " (FORMAT PARQUET)";

                    monitor.subTask("Writing Parquet file");
                    try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, TASK_TITLE)) {
                        try (DBCStatement statement = session.prepareStatement(
                            DBCStatementType.SCRIPT, copySql, false, false, false)) {
                            statement.executeStatement();
                        }
                    }

                    moveFile(tempPath, absoluteTarget);
                    tempPath = null;

                    Path exportedPath = absoluteTarget;
                    UIUtils.asyncExec(() -> DBWorkbench.getPlatformUI().showMessageBox(
                        TASK_TITLE, "Result set exported to\n" + exportedPath, false));
                    return Status.OK_STATUS;
                } catch (Throwable e) {
                    return GeneralUtils.makeExceptionStatus("Failed to export result set to Parquet file", e);
                } finally {
                    if (tempPath != null) {
                        try {
                            Files.deleteIfExists(tempPath);
                        } catch (IOException e) {
                            log.debug("Cannot delete temporary Parquet file " + tempPath, e);
                        }
                    }
                    monitor.done();
                }
            }
        };
        exportJob.setUser(true);
        exportJob.schedule();
    }

    @NotNull
    private static String suggestFileName(@NotNull IResultSetController resultSet) {
        DBSDataContainer dataContainer = resultSet.getDataContainer();
        if (dataContainer instanceof DBSEntity && !CommonUtils.isEmpty(dataContainer.getName())) {
            return dataContainer.getName() + ".parquet";
        }
        return "export.parquet";
    }

    @Nullable
    private static String stripTrailingDelimiters(@Nullable String query) {
        if (query == null) {
            return null;
        }
        String result = query.strip();
        while (result.endsWith(";")) {
            result = result.substring(0, result.length() - 1).strip();
        }
        return result;
    }

    @NotNull
    private static String quoteStringLiteral(@NotNull String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static void moveFile(@NotNull Path source, @NotNull Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
