/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.dataset;

/**
 * Dataset results view settings
 */
public class DBDDataViewSettings {

    private boolean showQueryText;
    private boolean showPanels;
    private boolean showStatusBar;
    private boolean showFilters;
    private String presentationId;
    private boolean readOnly;
    private boolean recordMode;
    private int fetchSize;

    public boolean isShowQueryText() {
        return showQueryText;
    }

    public void setShowQueryText(boolean showQueryText) {
        this.showQueryText = showQueryText;
    }

    public boolean isShowPanels() {
        return showPanels;
    }

    public void setShowPanels(boolean showPanels) {
        this.showPanels = showPanels;
    }

    public boolean isShowStatusBar() {
        return showStatusBar;
    }

    public void setShowStatusBar(boolean showStatusBar) {
        this.showStatusBar = showStatusBar;
    }

    public boolean isShowFilters() {
        return showFilters;
    }

    public void setShowFilters(boolean showFilters) {
        this.showFilters = showFilters;
    }

    public String getPresentationId() {
        return presentationId;
    }

    public void setPresentationId(String presentationId) {
        this.presentationId = presentationId;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isRecordMode() {
        return recordMode;
    }

    public void setRecordMode(boolean recordMode) {
        this.recordMode = recordMode;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }
}
