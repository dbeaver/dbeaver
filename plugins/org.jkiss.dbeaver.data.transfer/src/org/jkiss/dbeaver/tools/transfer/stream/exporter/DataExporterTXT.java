/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.tools.transfer.stream.exporter;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;

/**
 * TXT Exporter
 */
public class DataExporterTXT extends StreamExporterAbstract {

    private static final String PROP_BATCH_SIZE = "batchSize";
    private static final String PROP_MIN_COLUMN_LENGTH = "minColumnLength";
    private static final String PROP_MAX_COLUMN_LENGTH = "maxColumnLength";
    private static final String PROP_SHOW_NULLS = "showNulls";
    private static final String PROP_DELIM_LEADING = "delimLeading";
    private static final String PROP_DELIM_HEADER = "delimHeader";
    private static final String PROP_DELIM_TRAILING = "delimTrailing";
    private static final String PROP_DELIM_BETWEEN = "delimBetween";

    private int batchSize = 200;
    private int maxColumnSize = 0;
    private int minColumnSize = 3;
    private boolean showNulls;
    private boolean delimLeading, delimHeader, delimTrailing, delimBetween;
    private Deque<String[]> batchQueue;

    private DBDAttributeBinding[] columns;
    private int[] colWidths;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException {
        super.init(site);
        Map<String, Object> properties = site.getProperties();
        this.batchSize = Math.max(CommonUtils.toInt(properties.get(PROP_BATCH_SIZE), 200), 200);
        this.minColumnSize = Math.max(CommonUtils.toInt(properties.get(PROP_MIN_COLUMN_LENGTH), 3), 3);
        this.maxColumnSize = Math.max(CommonUtils.toInt(properties.get(PROP_MAX_COLUMN_LENGTH), 0), 0);
        this.showNulls = CommonUtils.getBoolean(properties.get(PROP_SHOW_NULLS), false);
        this.delimLeading = CommonUtils.getBoolean(properties.get(PROP_DELIM_LEADING), true);
        this.delimHeader = CommonUtils.getBoolean(properties.get(PROP_DELIM_HEADER), true);
        this.delimTrailing = CommonUtils.getBoolean(properties.get(PROP_DELIM_TRAILING), true);
        this.delimBetween = CommonUtils.getBoolean(properties.get(PROP_DELIM_BETWEEN), true);
        this.batchQueue = new ArrayDeque<>(this.batchSize);
        if (this.maxColumnSize > 0) {
            this.maxColumnSize = Math.max(this.maxColumnSize, this.minColumnSize);
        }
    }

    @Override
    public void exportHeader(DBCSession session) throws DBException, IOException {
        columns = getSite().getAttributes();
        colWidths = new int[columns.length];
        Arrays.fill(colWidths, minColumnSize);

        final String[] header = new String[columns.length];

        for (int index = 0; index < columns.length; index++) {
            header[index] = getAttributeName(columns[index]);
        }

        appendRow(header);
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws DBException, IOException {
        final String[] values = new String[columns.length];

        for (int index = 0; index < columns.length; index++) {
            values[index] = getCellString(columns[index], row[index]);
        }

        appendRow(values);
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws DBException, IOException {
        writeQueue();
    }

    private void appendRow(String[] row) {
        if (batchQueue.size() == batchSize) {
            writeQueue();
        }

        batchQueue.add(row);
    }

    private void writeQueue() {
        if (batchQueue.isEmpty()) {
            return;
        }

        for (String[] row : batchQueue) {
            for (int index = 0; index < columns.length; index++) {
                final String cell = row[index];

                if (maxColumnSize > 0 && cell.length() > maxColumnSize) {
                    colWidths[index] = maxColumnSize;
                } else if (cell.length() > colWidths[index]) {
                    colWidths[index] = cell.length();
                }
            }
        }

        while (!batchQueue.isEmpty()) {
            writeRow(batchQueue.poll(), ' ');

            if (delimHeader) {
                delimHeader = false;
                writeRow(null, '-');
            }
        }

        getWriter().flush();
    }

    private void writeRow(String[] values, char fill) {
        final StringBuilder sb = new StringBuilder();

        if (delimLeading) {
            sb.append('|');
        }

        for (int index = 0, length = columns.length; index < length; index++) {
            final String cell = ArrayUtils.isEmpty(values) ? "" : values[index];

            if (maxColumnSize > 0) {
                sb.append(CommonUtils.truncateString(cell, maxColumnSize));
            } else {
                sb.append(cell);
            }

            if (index < length - 1 || delimTrailing || fill != ' ') {
                for (int width = cell.length(); width < colWidths[index]; width++) {
                    sb.append(fill);
                }
            }

            if (index < length - 1) {
                sb.append(delimBetween ? '|' : ' ');
            }
        }

        if (delimTrailing) {
            sb.append('|');
        }

        sb.append(CommonUtils.getLineSeparator());

        getWriter().write(sb.toString());
    }

    private String getCellString(DBDAttributeBinding attr, Object value) {
        final String displayString = attr.getValueHandler().getValueDisplayString(attr, value, DBDDisplayFormat.EDIT);

        if (showNulls && displayString.isEmpty() && DBUtils.isNullValue(value)) {
            return DBConstants.NULL_VALUE_LABEL;
        }

        return CommonUtils.getSingleLineString(displayString);
    }

    private static String getAttributeName(DBDAttributeBinding attr) {
        if (CommonUtils.isEmpty(attr.getLabel())) {
            return attr.getName();
        } else {
            return attr.getLabel();
        }
    }
}
