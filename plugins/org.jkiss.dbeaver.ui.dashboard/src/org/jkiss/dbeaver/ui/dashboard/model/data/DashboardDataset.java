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
package org.jkiss.dbeaver.ui.dashboard.model.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard raw dataset
 */
public class DashboardDataset {

    private String[] columnNames;
    private List<DashboardDatasetRow> rows = new ArrayList<>();

    public DashboardDataset(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public List<DashboardDatasetRow> getRows() {
        return rows;
    }

    public void addRow(DashboardDatasetRow row) {
        rows.add(row);
    }

}
