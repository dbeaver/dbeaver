/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * JDBCTrace
 */
public class JDBCTrace {
    private static final Log log = Log.getLog(JDBCTrace.class);

    private static final boolean apiTraceEnabled;
    private static final PrintWriter traceWriter;

    private static final String QUERY_DIVIDER = "=======================================================";
    private static final String RS_DIVIDER =    "-------------------------------------------------------";

    static {
        boolean traceEnabled = CommonUtils.toBoolean(System.getProperty("dbeaver.jdbc.trace"));

        if (traceEnabled) {
            File traceFile = new File(
                DBWorkbench.getPlatform().getWorkspace().getMetadataFolder(),
                "jdbc-api-trace.log");
            PrintWriter writer;
            try {
                writer = new PrintWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(traceFile), StandardCharsets.UTF_8));
            } catch (IOException e) {
                log.error("Error opening JDBC trace file", e);
                writer = null;
            }
            traceWriter = writer;
        } else {
            traceWriter = null;
        }
        apiTraceEnabled = traceEnabled;
    }

    /////////////////////////////////////////////////////////////
    // API trace

    public static boolean isApiTraceEnabled() {
        return apiTraceEnabled;
    }

    public static void traceQueryBegin(String string) {
        if (!apiTraceEnabled) return;
        traceWriter.println(QUERY_DIVIDER);
        traceWriter.println(string);
        traceWriter.flush();
    }

    public static void dumpResultSetRow(ResultSet dbResult)
    {
        if (!apiTraceEnabled) return;
        try {
            ResultSetMetaData md = dbResult.getMetaData();
            int count = md.getColumnCount();
            for (int i = 1; i <= count; i++) {
                Object colValue = dbResult.getObject(i);
                traceWriter.print(colValue + "\t|\t");
            }
            traceWriter.println();
            traceWriter.flush();
        } catch (Exception e) {
            log.debug(e);
        }
    }

    public static void dumpResultSetOpen(ResultSet dbResult) {
        if (!apiTraceEnabled) return;
        traceWriter.println(RS_DIVIDER);
        try {
            ResultSetMetaData md = dbResult.getMetaData();
            int count = md.getColumnCount();
            for (int i = 1; i <= count; i++) {
                traceWriter.print(md.getColumnName(i) + " [" + md.getColumnTypeName(i) + "]\t|\t");
            }
            traceWriter.println();
            traceWriter.flush();
        } catch (Exception e) {
            log.debug(e);
        }
    }

    public static void dumpResultSetClose() {
        traceWriter.println(RS_DIVIDER);
    }
}
