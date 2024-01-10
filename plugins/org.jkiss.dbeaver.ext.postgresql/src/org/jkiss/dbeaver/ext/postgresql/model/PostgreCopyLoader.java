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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataBulkLoader;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Bulk loader based on CopyManager
 *
 * //        new CopyManager((BaseConnection) conn)
 * //            .copyIn(
 * //                "COPY table1 FROM STDIN (FORMAT csv)",
 * //                new BufferedReader(new FileReader("data.csv"))
 * //            );
 */
public class PostgreCopyLoader implements DBSDataBulkLoader, DBSDataBulkLoader.BulkLoadManager {

    private static final Log log = Log.getLog(PostgreCopyLoader.class);

    private final PostgreDataSource dataSource;
    private PostgreTableReal table;
    private Object copyManager;
    private Method copyInMethod;
    private Writer csvWriter;
    private Path csvFile;

    private AttrMapping[] mappings;

    private int copyBufferSize = 100 * 1024;

    private static class AttrMapping {
        PostgreTableColumn tableAttr;
        DBDValueHandler valueHandler;
        int srcPos;

        AttrMapping(PostgreTableColumn tableAttr, DBDValueHandler valueHandler, int srcPos) {
            this.tableAttr = tableAttr;
            this.valueHandler = valueHandler;
            this.srcPos = srcPos;
        }
    }

    public PostgreCopyLoader(PostgreDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @NotNull
    @Override
    public BulkLoadManager createBulkLoad(
        @NotNull DBCSession session,
        @NotNull DBSDataContainer dataContainer,
        @NotNull DBSAttributeBase[] attributes,
        @NotNull DBCExecutionSource source,
        int batchSize,
        Map<String, Object> options) throws DBCException
    {
        this.table = (PostgreTableReal) dataContainer;
        try {
            // Use reflection to create copy manager
            Connection pgConnection = ((JDBCSession) session).getOriginal();
            ClassLoader driverClassLoader = pgConnection.getClass().getClassLoader();

            Class<?> baseConnectionClass = Class.forName("org.postgresql.core.BaseConnection", true, driverClassLoader);
            Class<?> copyManagerClass = Class.forName("org.postgresql.copy.CopyManager", true, driverClassLoader);

            // Get method copyIn(final String sql, Reader from, int bufferSize)
            copyInMethod = copyManagerClass.getMethod("copyIn", String.class, Reader.class, Integer.TYPE);

            copyManager = copyManagerClass.getConstructor(baseConnectionClass).newInstance(pgConnection);

            Path tempFolder = DBWorkbench.getPlatform().getTempFolder(session.getProgressMonitor(), "postgesql-copy-datasets");
            csvFile = tempFolder.resolve(CommonUtils.escapeFileName(table.getFullyQualifiedName(DBPEvaluationContext.DML)) + "-" + System.currentTimeMillis() + ".csv");  //$NON-NLS-1$ //$NON-NLS-2$
            try {
                Files.createFile(csvFile);
            } catch (IOException ex) {
                throw new IOException("Can't create CSV file " + csvFile);
            }

            csvWriter = new BufferedWriter(
                Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8),
                copyBufferSize
                );

            List<? extends PostgreTableColumn> tableAttrs = CommonUtils.safeList(table.getAttributes(session.getProgressMonitor()));
            tableAttrs.removeIf(a -> a.getOrdinalPosition() < 0);
            mappings = new AttrMapping[tableAttrs.size()];

            for (int i = 0; i < tableAttrs.size(); i++) {
                PostgreTableColumn attr = tableAttrs.get(i);
                DBDValueHandler valueHandler = DBUtils.findValueHandler(session, attr);
                AttrMapping mapping = new AttrMapping(
                    attr,
                    valueHandler,
                    ArrayUtils.indexOf(attributes, attr)
                );
                mappings[i] = mapping;
            }
        } catch (Exception e) {
            throw new DBCException("Can't instantiate CopyManager", e);
        }
        return this;
    }

    @Override
    public void addRow(@NotNull DBCSession session, @NotNull Object[] attributeValues) throws DBCException {
        StringBuilder line = new StringBuilder();
        boolean hasCell = false;
        for (AttrMapping mapping : mappings) {
            if (mapping.srcPos >= 0) {
                if (hasCell) {
                    line.append(",");
                }
                Object srcValue = attributeValues[mapping.srcPos];
                if (!DBUtils.isNullValue(srcValue)) {
                    if (srcValue instanceof Number) {
                        line.append(srcValue);
                    } else {
                        String strValue = mapping.valueHandler.getValueDisplayString(
                            mapping.tableAttr, srcValue, DBDDisplayFormat.NATIVE);
                        strValue = convertStringValueToCell(strValue);
                        line.append(strValue);
                    }
                }
                hasCell = true;
            }
        }
        line.append("\n");
        try {
            csvWriter.write(line.toString());
        } catch (IOException e) {
            throw new DBCException("Error writing CSV line", e);
        }
    }

    private String convertStringValueToCell(String strValue) {
        return '"' +
            strValue.replace("\"", "\\\"") +
            '"';
    }

    @Override
    public void flushRows(@NotNull DBCSession session) throws DBCException {
        try {
            csvWriter.flush();
        } catch (IOException e) {
            throw new DBCException("Error saving CSV data", e);
        }
    }

    @Override
    public void finishBulkLoad(@NotNull DBCSession session) throws DBCException {
        try {
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            log.debug(e);
        }
        csvWriter = null;

        String tableFQN = table.getFullyQualifiedName(DBPEvaluationContext.DML);

        session.getProgressMonitor().subTask("Copy into " + tableFQN);

        String queryText = "COPY " + tableFQN + " FROM STDIN (FORMAT CSV, ESCAPE '\\')";

        try {
            Object rowCount;
            try (Reader csvReader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
                rowCount = copyInMethod.invoke(copyManager, queryText, csvReader, copyBufferSize);
            }

            // Commit changes
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
            if (txnManager != null && !txnManager.isAutoCommit()) {
                session.getProgressMonitor().subTask("Commit COPY");
                txnManager.commit(session);
            }

            log.debug("CSV has been imported (" + rowCount + ")");
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException) e).getTargetException();
            }
            throw new DBCException("Error copying dataset on remote server", e);
        }
    }

    @Override

    public void close() {
        if (csvFile != null && Files.exists(csvFile)) {
            try {
                Files.delete(csvFile);
            } catch (IOException e) {
                log.debug("Error deleting CSV file " + csvFile, e);
                csvFile.toFile().deleteOnExit();
            }
        }
    }
}
