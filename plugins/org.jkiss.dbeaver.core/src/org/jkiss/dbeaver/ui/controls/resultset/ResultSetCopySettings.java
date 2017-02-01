/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.data.DBDDisplayFormat;

/**
 * Copy settings
 */
public class ResultSetCopySettings {
    private boolean copyHeader;
    private boolean copyRowNumbers;
    private boolean cut;
    private boolean quoteCells;
    private String columnDelimiter;
    private String rowDelimiter;
    private DBDDisplayFormat format;

    public ResultSetCopySettings() {
    }

    public ResultSetCopySettings(boolean copyHeader, boolean copyRowNumbers, boolean cut, boolean quoteCells, String columnDelimiter, String rowDelimiter, DBDDisplayFormat format) {
        this.copyHeader = copyHeader;
        this.copyRowNumbers = copyRowNumbers;
        this.cut = cut;
        this.quoteCells = quoteCells;
        this.columnDelimiter = columnDelimiter;
        this.rowDelimiter = rowDelimiter;
        this.format = format;
    }

    public boolean isCopyHeader() {
        return copyHeader;
    }

    public void setCopyHeader(boolean copyHeader) {
        this.copyHeader = copyHeader;
    }

    public boolean isCopyRowNumbers() {
        return copyRowNumbers;
    }

    public void setCopyRowNumbers(boolean copyRowNumbers) {
        this.copyRowNumbers = copyRowNumbers;
    }

    public boolean isCut() {
        return cut;
    }

    public void setCut(boolean cut) {
        this.cut = cut;
    }

    public boolean isQuoteCells() {
        return quoteCells;
    }

    public void setQuoteCells(boolean quoteCells) {
        this.quoteCells = quoteCells;
    }

    public String getColumnDelimiter() {
        return columnDelimiter;
    }

    public void setColumnDelimiter(String columnDelimiter) {
        this.columnDelimiter = columnDelimiter;
    }

    public String getRowDelimiter() {
        return rowDelimiter;
    }

    public void setRowDelimiter(String rowDelimiter) {
        this.rowDelimiter = rowDelimiter;
    }

    public DBDDisplayFormat getFormat() {
        return format;
    }

    public void setFormat(DBDDisplayFormat format) {
        this.format = format;
    }
}
