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
package org.jkiss.dbeaver.ui.controls.resultset;

/**
 * Save report
 */
public class ResultSetSaveReport {
    private int inserts;
    private int updates;
    private int deletes;
    private boolean hasReferences;

    public int getInserts() {
        return inserts;
    }

    public void setInserts(int inserts) {
        this.inserts = inserts;
    }

    public int getUpdates() {
        return updates;
    }

    public void setUpdates(int updates) {
        this.updates = updates;
    }

    public int getDeletes() {
        return deletes;
    }

    public void setDeletes(int deletes) {
        this.deletes = deletes;
    }

    public boolean isHasReferences() {
        return hasReferences;
    }

    public void setHasReferences(boolean hasReferences) {
        this.hasReferences = hasReferences;
    }
}
