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

package org.jkiss.dbeaver.ui.controls.resultset.view;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.AbstractPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCopySettings;
import org.jkiss.utils.CommonUtils;

/**
 * Empty presentation.
 * Used when RSV has no results (initially).
 */
public class EmptyPresentation extends AbstractPresentation {

    private Composite placeholder;

    @Override
    public void createPresentation(@NotNull final IResultSetController controller, @NotNull Composite parent) {
        super.createPresentation(controller, parent);

        UIUtils.createHorizontalLine(parent);
        placeholder = new Canvas(parent, SWT.NONE);
        placeholder.setLayoutData(new GridData(GridData.FILL_BOTH));
        placeholder.setBackground(controller.getDefaultBackground());

        final Font normalFont = parent.getFont();
        FontData[] fontData = normalFont.getFontData();
        fontData[0].setStyle(fontData[0].getStyle() | SWT.BOLD);
        fontData[0].setHeight(18);
        final Font largeFont = new Font(normalFont.getDevice(), fontData[0]);
        placeholder.addDisposeListener(e -> UIUtils.dispose(largeFont));

        placeholder.addPaintListener(e -> {
            if (controller.isRefreshInProgress()) {
                return;
            }
            e.gc.setFont(largeFont);
            e.gc.setForeground(UIStyles.getDefaultTextForeground());
            //int fontSize = largeFont.getFontData()[0].getHeight();
            String emptyDataMessage = controller.getDecorator().getEmptyDataMessage();
            if (!CommonUtils.isEmpty(emptyDataMessage)) {
                Point emSize = e.gc.textExtent(emptyDataMessage);
                UIUtils.drawMessageOverControl(placeholder, e, emptyDataMessage, -emSize.y);
            }
            e.gc.setFont(normalFont);
            String emptyDataDescription = controller.getDecorator().getEmptyDataDescription();
            if (!CommonUtils.isEmpty(emptyDataDescription)) {
                UIUtils.drawMessageOverControl(placeholder, e, emptyDataDescription, 10);
            }
        });

        trackPresentationControl();
    }

    @Override
    public Control getControl() {
        return placeholder;
    }

    @Override
    public void refreshData(boolean refreshMetadata, boolean append, boolean keepState) {

    }

    @Override
    public void formatData(boolean refreshData) {

    }

    @Override
    public void clearMetaData() {

    }

    @Override
    public void updateValueView() {

    }

    @Override
    public void fillMenu(@NotNull IMenuManager menu) {

    }

    @Override
    public void changeMode(boolean recordMode) {

    }

    @Override
    public void scrollToRow(@NotNull RowPosition position) {

    }

    @Nullable
    @Override
    public DBDAttributeBinding getCurrentAttribute() {
        return null;
    }

    @Override
    public void setCurrentAttribute(@NotNull DBDAttributeBinding attribute) {

    }

    @Override
    public Point getCursorLocation() {
        return null;
    }

    @Nullable
    @Override
    public String copySelectionToString(ResultSetCopySettings settings) {
        return null;
    }

    @Override
    public void printResultSet() {

    }


}
