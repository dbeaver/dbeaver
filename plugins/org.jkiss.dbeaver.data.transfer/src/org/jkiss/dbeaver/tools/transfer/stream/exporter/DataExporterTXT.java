/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.utils.CommonUtils;

import java.util.Date;
import java.util.Map;

/**
 * XML Exporter
 */
public class DataExporterTXT extends StreamExporterAbstract {

    private static final String PROP_MAX_COLUMN_LENGTH = "maxColumnLength";
    private static final String PROP_SHOW_NULLS = "showNulls";
    private static final String PROP_DELIM_LEADING = "delimLeading";
    private static final String PROP_DELIM_HEADER = "delimHeader";
    private static final String PROP_DELIM_TRAILING = "delimTrailing";

    private DBDAttributeBinding[] columns;
    private String tableName;
    private int maxColumnSize = 100;
    private int minColumnSize = 3;
    private boolean showNulls;
    private boolean delimLeading, delimHeader, delimTrailing;

    private int[] colWidths;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException {
        super.init(site);
        Map<Object, Object> properties = site.getProperties();
        this.maxColumnSize = CommonUtils.toInt(properties.get(PROP_MAX_COLUMN_LENGTH), 100);
        this.showNulls = CommonUtils.getBoolean(properties.get(PROP_SHOW_NULLS), false);
        this.delimLeading = CommonUtils.getBoolean(properties.get(PROP_DELIM_LEADING), true);
        this.delimHeader = CommonUtils.getBoolean(properties.get(PROP_DELIM_HEADER), true);
        this.delimTrailing = CommonUtils.getBoolean(properties.get(PROP_DELIM_TRAILING), true);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void exportHeader(DBCSession session) {
        columns = getSite().getAttributes();
        printHeader();
    }

    private static String getAttributeName(DBDAttributeBinding attr) {
        if (CommonUtils.isEmpty(attr.getLabel())) {
            return attr.getName();
        } else {
            return attr.getLabel();
        }
    }

    private void printHeader() {
        colWidths = new int[columns.length];

        for (int i = 0; i < columns.length; i++) {
            DBDAttributeBinding attr = columns[i];
            int maxLength = (int) attr.getMaxLength();
            if (attr.getDataKind() == DBPDataKind.DATETIME) {
                // DATETIME attributes are converted to strings so their actual length may differ
                maxLength = getCellString(attr, new Date(), DBDDisplayFormat.EDIT).length();
            }
            colWidths[i] = Math.max(getAttributeName(attr).length(), maxLength);
        }
        for (int i = 0; i < colWidths.length; i++) {
            if (colWidths[i] > maxColumnSize) {
                colWidths[i] = maxColumnSize;
            } else if (colWidths[i] < minColumnSize) {
                colWidths[i] = minColumnSize;
            }

        }

        StringBuilder txt = new StringBuilder();
        if (delimLeading) txt.append("|");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) txt.append("|");
            DBDAttributeBinding attr = columns[i];
            String attrName = getAttributeName(attr);
            txt.append(attrName);
            for (int k = colWidths[i] - attrName.length(); k > 0; k--) {
                txt.append(" ");
            }
        }
        if (delimTrailing) txt.append("|");
        txt.append("\n");

        if (delimHeader) {
            // Print divider
            // Print header
            if (delimLeading) txt.append("|");
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) txt.append("|");
                for (int k = colWidths[i]; k > 0; k--) {
                    txt.append("-");
                }
            }
            if (delimTrailing) txt.append("|");
            txt.append("\n");
        }
        getWriter().print(txt);
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) {
        StringBuilder txt = new StringBuilder();
        if (delimLeading) txt.append("|");
        for (int k = 0; k < columns.length; k++) {
            if (k > 0) txt.append("|");
            DBDAttributeBinding attr = columns[k];
            String displayString = getCellString(attr, row[k], DBDDisplayFormat.EDIT);
            if (displayString.length() > colWidths[k]) {
                displayString = CommonUtils.truncateString(displayString, colWidths[k]);
            }
            txt.append(displayString);
            for (int j = colWidths[k] - displayString.length(); j > 0; j--) {
                txt.append(" ");
            }
        }
        if (delimTrailing) txt.append("|");
        txt.append("\n");
        getWriter().print(txt);
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) {
    }

    private String getCellString(DBDAttributeBinding attr, Object value, DBDDisplayFormat displayFormat) {
        String displayString = attr.getValueHandler().getValueDisplayString(attr, value, displayFormat);

        if (showNulls && displayString.isEmpty() && DBUtils.isNullValue(value)) {
            return DBConstants.NULL_VALUE_LABEL;
        }
        return CommonUtils.getSingleLineString(displayString);
    }

}
