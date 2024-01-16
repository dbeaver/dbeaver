/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.Map;

/**
 * Error message presentation.
 */
public class ErrorPresentation extends AbstractPresentation {

    private static final String SETTINGS_SECTION_ERROR_PANEL = ErrorPresentation.class.getSimpleName();
    private static final String PROP_ERROR_WIDTH = "errorWidth";
    private static final boolean REMEBER_SASH_RATIO = false;

    private final String sqlText;
    private final IStatus status;
    private StyledText textWidget;
    private Object editorPanel;
    private IResultSetContainerExt resultSetContainer;

    public ErrorPresentation(String sqlText, IStatus status, @Nullable IResultSetContainerExt resultSetContainer) {
        this.sqlText = sqlText;
        this.status = status;
        this.resultSetContainer = resultSetContainer;
    }

    @Override
    public void createPresentation(@NotNull IResultSetController controller, @NotNull Composite parent) {
        super.createPresentation(controller, parent);

        CustomSashForm partDivider = UIUtils.createPartDivider(controller.getSite().getPart(), parent, SWT.HORIZONTAL);
        partDivider.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite errorComposite = UIUtils.createComposite(partDivider, 1);
        errorComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        new ErrorDetailsPart(errorComposite, status, resultSetContainer);

        for (Control child : errorComposite.getChildren()) {
            if (child instanceof Text) {
                TextEditorUtils.enableHostEditorKeyBindingsSupport(controller.getSite(), child);
            }
        }

        Composite sqlPanel = UIUtils.createComposite(partDivider, 1);
        sqlPanel.setLayout(new FillLayout());
        UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
        if (serviceSQL != null) {
            try {
                editorPanel = serviceSQL.createSQLPanel(controller.getSite(), sqlPanel, controller, "SQL", true, sqlText);
                if (editorPanel instanceof TextViewer) {
                    textWidget = ((TextViewer) editorPanel).getTextWidget();
                }
            } catch (DBException e) {
                textWidget = new StyledText(sqlPanel, SWT.BORDER | SWT.READ_ONLY);
                textWidget.setText(sqlText);
            }
        }
        if (REMEBER_SASH_RATIO) {
            boolean widthSet = false;
            IDialogSettings viewSettings = ResultSetUtils.getViewerSettings(SETTINGS_SECTION_ERROR_PANEL);
            String errorWidth = viewSettings.get(PROP_ERROR_WIDTH);
            if (errorWidth != null) {
                String[] widthStrs = errorWidth.split(":");
                if (widthStrs.length == 2) {
                    partDivider.setWeights(Integer.parseInt(widthStrs[0]), Integer.parseInt(widthStrs[1]));
                }
                widthSet = true;
            }
            if (!widthSet) {
                partDivider.setWeights(700, 300);
            }
            partDivider.addCustomSashFormListener((firstControlWeight, secondControlWeight) -> {
                int[] weights = partDivider.getWeights();
                viewSettings.put(PROP_ERROR_WIDTH, weights[0] + ":" + weights[1]);
            });
        }
    }

    @Override
    public Control getControl() {
        return textWidget;
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
    public void changeMode(boolean recordMode) {

    }

    @Nullable
    @Override
    public DBDAttributeBinding getCurrentAttribute() {
        return null;
    }

    @NotNull
    @Override
    public Map<Transfer, Object> copySelection(ResultSetCopySettings settings) {
        return Collections.singletonMap(
            TextTransfer.getInstance(),
            CommonUtils.notEmpty(status.getMessage()));
    }

    @Override
    public void dispose() {
        super.dispose();
        UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
        if (serviceSQL != null) {
            serviceSQL.disposeSQLPanel(editorPanel);
        }
    }

}
