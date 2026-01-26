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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.Section;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.utils.Pair;

class SingleTabQueryProcessor extends QueryProcessor {
    private static final int SCROLL_SPEED = 10;
    private boolean tabCreated;
    private CTabItem resultsTab;
    private ScrolledComposite tabContentScroller;
    private Composite sectionsContainer;

    SingleTabQueryProcessor(@NotNull SQLEditor owner, boolean makeDefault) {
        super(owner, false, makeDefault);
    }

    Composite getSectionsContainer() {
        return sectionsContainer;
    }

    CTabItem getResultsTab() {
        return resultsTab;
    }

    public void setResultsTab(CTabItem resultsTab) {
        this.resultsTab = resultsTab;
    }

    @NotNull
    @Override
    protected QueryResultsContainer createQueryResultsContainer(
        int resultSetNumber,
        int resultSetIndex,
        boolean singleQuery,
        boolean makeDefault
    ) {
        return new SingleTabQueryResultsContainer(
            createSection(makeDefault),
            this,
            resultSetNumber,
            resultSetIndex,
            singleQuery,
            makeDefault
        );
    }

    @NotNull
    @Override
    protected QueryResultsContainer createQueryResultsContainer(
        int resultSetNumber,
        int resultSetIndex,
        @NotNull DBSDataContainer dataContainer,
        boolean singleQuery
    ) {
        return new SingleTabQueryResultsContainer(
            createSection(false),
            this,
            resultSetNumber,
            resultSetIndex,
            dataContainer,
            singleQuery
        );
    }

    @NotNull
    private Pair<Section, Composite> createSection(boolean makeDefault) {
        if (!tabCreated) {
            tabCreated = true;
            prepareResultSetContainerHost(makeDefault);
        }

        Section section = new Section(sectionsContainer, Section.TWISTIE | Section.EXPANDED);
        section.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        Composite contents = UIUtils.createComposite(section, 1);
        section.setClient(contents);
        section.addExpansionListener(new ExpansionAdapter() {
            @Override
            public void expansionStateChanged(ExpansionEvent e) {
                relayoutContents();
            }
        });
        return new Pair<>(section, contents);
    }

    public void relayoutContents() {
        tabContentScroller.setMinSize(sectionsContainer.computeSize(tabContentScroller.getBorderWidth(), SWT.DEFAULT));
        sectionsContainer.layout();
    }

    private void prepareResultSetContainerHost(boolean makeDefault) {
        SQLEditor owner = getOwner();
        tabContentScroller = new ScrolledComposite(owner.getResultTabsContainer(), SWT.V_SCROLL | SWT.BORDER);
        tabContentScroller.setExpandHorizontal(true);
        tabContentScroller.setExpandVertical(true);

        int tabIndex = owner.obtainDesiredTabIndex(makeDefault);
        resultsTab = new CTabItem(owner.getResultTabsContainer(), SWT.NONE, tabIndex);
        resultsTab.setImage(SQLEditor.IMG_DATA_GRID);
        resultsTab.setData(this);
        resultsTab.setShowClose(true);
        int queryIndex = owner.queryProcessors.indexOf(this);
        resultsTab.setText(owner.getResultsTabName(0, queryIndex, null));
        CSSUtils.markConnectionTypeColor(resultsTab);

        resultsTab.setControl(tabContentScroller);
        resultsTab.addDisposeListener(owner.resultTabDisposeListener);
        UIUtils.disposeControlOnItemDispose(resultsTab);

        sectionsContainer = new Composite(tabContentScroller, SWT.NONE);
        sectionsContainer.setLayout(new GridLayout(1, false));
        sectionsContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        tabContentScroller.setContent(sectionsContainer);

        Listener scrollListener = event -> {
            Control underScroll = (Control) event.widget;
            if (underScroll.getShell() == tabContentScroller.getShell() && tabContentScroller.isVisible() && (
                (event.stateMask & SWT.CTRL) == SWT.CTRL)) {
                Point clickedPoint = underScroll.toDisplay(event.x, event.y);
                if (tabContentScroller.getClientArea().contains(tabContentScroller.toControl(clickedPoint))) {
                    for (Control c = underScroll; c != null; c = c.getParent()) {
                        if (c == tabContentScroller) {
                            Point offset = tabContentScroller.getOrigin();
                            offset.y -= event.count * SCROLL_SPEED;
                            if (offset.y < 0) {
                                offset.y = 0;
                            }
                            tabContentScroller.setOrigin(offset);
                            event.doit = false;
                        }
                    }
                }
            }
        };
        tabContentScroller.getDisplay().addFilter(SWT.MouseVerticalWheel, scrollListener);
        tabContentScroller.addDisposeListener(e -> tabContentScroller.getDisplay()
            .removeFilter(SWT.MouseVerticalWheel, scrollListener));
    }
}
