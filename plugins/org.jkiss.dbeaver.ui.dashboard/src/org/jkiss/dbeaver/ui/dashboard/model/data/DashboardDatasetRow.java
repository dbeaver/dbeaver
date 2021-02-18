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

import java.util.Date;

/**
 * Dashboard dataset row
 */
public class DashboardDatasetRow {

    private Date timestamp;
    private Object[] values;

    public DashboardDatasetRow(Date timestamp, Object[] values) {
        this.timestamp = timestamp;
        this.values = values;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public Object[] getValues() {
        return values;
    }
}
