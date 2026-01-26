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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.Section;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionResult;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.VerticalButton;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetDecorator;
import org.jkiss.dbeaver.ui.controls.resultset.QueryResultsDecorator;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.Spreadsheet;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

class SingleTabQueryResultsContainer extends QueryResultsContainer {
    private static final Integer MIN_VIEWER_HEIGHT = 150;

    private final SingleTabQueryProcessor queryProcessor;
    private final Section section;

    private GridData rsvConstrainedLayout;

    SingleTabQueryResultsContainer(
        @NotNull Pair<Section, Composite> sectionAndContents,
        @NotNull SingleTabQueryProcessor queryProcessor,
        int resultSetNumber,
        int resultSetIndex,
        boolean singleQuery,
        boolean makeDefault
    ) {
        super(sectionAndContents.getSecond(), queryProcessor, resultSetNumber, resultSetIndex, singleQuery, makeDefault);
        this.queryProcessor = queryProcessor;
        this.section = sectionAndContents.getFirst();
        this.setupSection(sectionAndContents.getSecond());
    }

    SingleTabQueryResultsContainer(
        @NotNull Pair<Section, Composite> sectionAndContents,
        @NotNull SingleTabQueryProcessor queryProcessor,
        int resultSetNumber,
        int resultSetIndex,
        @NotNull DBSDataContainer dataContainer,
        boolean singleQuery
    ) {
        super(sectionAndContents.getSecond(), queryProcessor, resultSetNumber, resultSetIndex, dataContainer, singleQuery);
        this.queryProcessor = queryProcessor;
        this.section = sectionAndContents.getFirst();
        this.setupSection(sectionAndContents.getSecond());
    }

    @Override
    public IResultSetDecorator createResultSetDecorator() {
        if (getOwner().getActivePreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_SHOW_FILTERS_IN_SINGLE_TAB_MODE)) {
            return super.createResultSetDecorator();
        } else {
            return new QueryResultsDecorator() {
                @Override
                public long getDecoratorFeatures() {
                    return FEATURE_STATUS_BAR | FEATURE_PANELS | FEATURE_PRESENTATIONS | FEATURE_EDIT | FEATURE_LINKS;
                }
            };
        }
    }

    private void setupSection(@NotNull Composite sectionContents) {
        Composite control = this.viewer.getControl();
        sectionContents.setData(ResultSetViewer.CONTROL_ID, this.viewer);

        rsvConstrainedLayout = GridDataFactory.swtDefaults()
            .align(GridData.FILL, GridData.FILL).grab(true, false).hint(10, 300).create();
        control.setLayoutData(rsvConstrainedLayout);
        GridData freeLayout = GridDataFactory.swtDefaults()
            .align(GridData.FILL, GridData.FILL).grab(true, false).create();

        Label line = new Label(sectionContents, SWT.SEPARATOR | SWT.HORIZONTAL); // resultset resizing thumb
        line.setLayoutData(GridDataFactory.swtDefaults().align(GridData.FILL, GridData.FILL).grab(true, false).hint(10, 10).create());
        line.setCursor(line.getDisplay().getSystemCursor(SWT.CURSOR_SIZENS));
        line.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                control.setLayoutData(control.getLayoutData() == rsvConstrainedLayout ? freeLayout : rsvConstrainedLayout);
                queryProcessor.relayoutContents();
            }
        });
        line.addMouseMoveListener(e -> {
            if ((e.stateMask & SWT.BUTTON1) != 0) {
                Tracker tracker = new Tracker(queryProcessor.getSectionsContainer(), SWT.RESIZE | SWT.DOWN);
                tracker.setStippled(true);
                tracker.setCursor(tracker.getDisplay().getSystemCursor(SWT.CURSOR_SIZENS));
                Point size = control.getSize();
                Point origin = queryProcessor.getSectionsContainer().toControl(control.toDisplay(control.getLocation()));
                tracker.setRectangles(new Rectangle[] {new Rectangle(origin.x, origin.y, size.x, size.y + line.getSize().y / 2)});
                if (tracker.open()) {
                    Rectangle after = tracker.getRectangles()[0];
                    int newHeight = after.height - line.getSize().y / 2;
                    if (newHeight != rsvConstrainedLayout.heightHint) {
                        rsvConstrainedLayout.heightHint = newHeight;
                        control.setLayoutData(rsvConstrainedLayout);
                        queryProcessor.relayoutContents();
                    }
                }
                tracker.dispose();
            }
        });

        Listener displayListener = event -> { // for the contextual tool buttons it's critical for one result set to be focused
            Control clickedWidget = (Control) event.widget;
            if (clickedWidget instanceof VerticalButton && clickedWidget.getShell() == control.getShell() && control.isVisible()) {
                Point clickedPoint = clickedWidget.toDisplay(event.x, event.y);
                if (control.getClientArea().contains(control.toControl(clickedPoint)) && !this.viewer.isPresentationInFocus()) {
                    for (Control c = control; c != null && !c.isFocusControl(); c = c.getParent()) {
                        if (c == sectionContents) {
                            control.setFocus();
                            break;
                        }
                    }
                }
            }
        };
        control.getDisplay().addFilter(SWT.MouseDown, displayListener);
        control.addDisposeListener(e -> control.getDisplay().removeFilter(SWT.MouseDown, displayListener));

        queryProcessor.relayoutContents();
    }

    @Override
    public void setTabName(@NotNull String tabName) {
        super.setTabName(tabName);
        section.setText(tabName);
    }

    @Override
    public void updateResultsName(@NotNull String resultSetName, @Nullable String toolTip) {
        if (!section.isDisposed()) {
            if (!CommonUtils.isEmpty(resultSetName)) {
                section.setText(resultSetName);
            }
            if (toolTip != null) {
                section.setToolTipText(toolTip);
            }
        }
    }

    @NotNull
    @Override
    public CTabItem getResultsTab() {
        return queryProcessor.getResultsTab();
    }

    @Override
    public boolean isPinned() {
        return isTabPinned(queryProcessor.getResultsTab());
    }

    @Override
    public void setPinned(boolean pinned) {
        setTabPinned(queryProcessor.getResultsTab(), pinned);
    }

    @Override
    public void handleExecuteResult(DBCExecutionResult result) {
        super.handleExecuteResult(result);

        if (this.viewer.getActivePresentation().getControl() instanceof Spreadsheet s) {
            UIUtils.syncExec(() -> {
                Point spreadsheetPreferredSize = s.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
                Point spreadsheetSize = s.getSize();
                int desiredViewerHeight = rsvConstrainedLayout.heightHint - spreadsheetSize.y + spreadsheetPreferredSize.y;
                if (desiredViewerHeight < rsvConstrainedLayout.heightHint) {
                    if (desiredViewerHeight < MIN_VIEWER_HEIGHT) {
                        desiredViewerHeight = MIN_VIEWER_HEIGHT;
                    }
                    rsvConstrainedLayout.heightHint = desiredViewerHeight;
                    queryProcessor.relayoutContents();
                }
            });
        }
    }

    @Override
    protected void dispose() {
        UIUtils.syncExec(section::dispose);
    }
}
