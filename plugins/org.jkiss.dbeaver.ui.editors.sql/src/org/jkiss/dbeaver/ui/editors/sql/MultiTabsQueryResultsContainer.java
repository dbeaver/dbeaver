/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.utils.CommonUtils;

class MultiTabsQueryResultsContainer extends QueryResultsContainer {
    private CTabItem resultsTab;

    MultiTabsQueryResultsContainer(
        @NotNull QueryProcessor queryProcessor,
        int resultSetNumber,
        int resultSetIndex,
        boolean singleQuery,
        boolean makeDefault
    ) {
        super(
            queryProcessor.getOwner().getResultTabsContainer(),
            queryProcessor,
            resultSetNumber,
            resultSetIndex,
            singleQuery,
            makeDefault
        );
        resultsTab = createResultTab(makeDefault);
    }

    MultiTabsQueryResultsContainer(
        @NotNull QueryProcessor queryProcessor,
        int resultSetNumber,
        int resultSetIndex,
        @NotNull DBSDataContainer dataContainer,
        boolean singleQuery
    ) {
        super(
            queryProcessor.getOwner().getResultTabsContainer(),
            queryProcessor,
            resultSetNumber,
            resultSetIndex,
            dataContainer,
            singleQuery
        );
        resultsTab = createResultTab(false);
    }

    @NotNull
    private CTabItem createResultTab(boolean makeDefault) {
        SQLEditor owner = getOwner();
        int tabIndex = owner.obtainDesiredTabIndex(makeDefault);

        CTabItem resultsTab = new CTabItem(owner.getResultTabsContainer(), SWT.NONE, tabIndex);
        resultsTab.setImage(SQLEditor.IMG_DATA_GRID);
        resultsTab.setData(this);
        resultsTab.setShowClose(true);
        resultsTab.setText(owner.getResultsTabName(resultSetNumber, getQueryIndex(), null));
        CSSUtils.markConnectionTypeColor(resultsTab);

        resultsTab.setControl(viewer.getControl());
        resultsTab.addDisposeListener(owner.resultTabDisposeListener);
        UIUtils.disposeControlOnItemDispose(resultsTab);
        return resultsTab;
    }

    @Override
    public void setTabName(@NotNull String tabName) {
        super.setTabName(tabName);
        resultsTab.setText(tabName);
    }

    public void setResultsTab(CTabItem resultsTab) {
        this.resultsTab = resultsTab;
    }

    @Override
    public void updateResultsName(@NotNull String resultSetName, @Nullable String toolTip) {
        CTabItem tabItem = resultsTab;
        if (tabItem != null && !tabItem.isDisposed()) {
            if (!CommonUtils.isEmpty(resultSetName)) {
                tabItem.setText(resultSetName);
            }
            if (toolTip != null) {
                tabItem.setToolTipText(toolTip);
            }
        }
    }

    @NotNull
    @Override
    public CTabItem getResultsTab() {
        return resultsTab;
    }

    @Override
    public boolean isPinned() {
        return isTabPinned(resultsTab);
    }

    @Override
    public void setPinned(boolean pinned) {
        setTabPinned(resultsTab, pinned);
    }

    @Override
    public void detach() {
        super.detach();

        if (detached) {
            resultsTab.dispose();
            resultsTab = null;
        }
    }

    @Override
    protected void dispose() {
        UIUtils.syncExec(resultsTab::dispose);
    }
}
