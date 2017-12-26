/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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

import java.util.List;

public class ResultSetDataContainerOptions {

    private boolean exportSelectedRows;
    private List<Long> selectedRows;
    private boolean exportSelectedColumns;
    private List<String> selectedColumns;

    public boolean isExportSelectedRows() {
        return exportSelectedRows;
    }

    public void setExportSelectedRows(boolean exportSelectedRows) {
        this.exportSelectedRows = exportSelectedRows;
    }

    public List<Long> getSelectedRows() {
        return selectedRows;
    }

    public void setSelectedRows(List<Long> selectedRows) {
        this.selectedRows = selectedRows;
    }

    public boolean isExportSelectedColumns() {
        return exportSelectedColumns;
    }

    public void setExportSelectedColumns(boolean exportSelectedColumns) {
        this.exportSelectedColumns = exportSelectedColumns;
    }

    public List<String> getSelectedColumns() {
        return selectedColumns;
    }

    public void setSelectedColumns(List<String> selectedColumns) {
        this.selectedColumns = selectedColumns;
    }
}
