/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
    private String delimiter;
    private DBDDisplayFormat format;

    public ResultSetCopySettings() {
    }

    public ResultSetCopySettings(boolean copyHeader, boolean copyRowNumbers, boolean cut, String delimiter, DBDDisplayFormat format) {
        this.copyHeader = copyHeader;
        this.copyRowNumbers = copyRowNumbers;
        this.cut = cut;
        this.delimiter = delimiter;
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

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public DBDDisplayFormat getFormat() {
        return format;
    }

    public void setFormat(DBDDisplayFormat format) {
        this.format = format;
    }
}
