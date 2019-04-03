/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.part.StatusPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.dbeaver.ui.controls.resultset.*;

/**
 * Error message presentation.
 */
public class ErrorPresentation extends AbstractPresentation {

    private static final Log log = Log.getLog(ErrorPresentation.class);

    private static final String SETTINGS_SECTION_ERROR_PANEL = ErrorPresentation.class.getSimpleName();
    private static final String PROP_ERROR_WIDTH = "errorWidth";

    private final String sqlText;
    private final IStatus status;
    private Composite errorComposite;
    private StatusPart statusPart;
    private Composite sqlPanel;
    private StyledText textWidget;

    public ErrorPresentation(String sqlText, IStatus status) {
        this.sqlText = sqlText;
        this.status = status;
    }

    @Override
    public void createPresentation(@NotNull IResultSetController controller, @NotNull Composite parent) {
        super.createPresentation(controller, parent);

        CustomSashForm partDivider = UIUtils.createPartDivider(controller.getSite().getPart(), parent, SWT.HORIZONTAL);
        partDivider.setLayoutData(new GridData(GridData.FILL_BOTH));

        errorComposite = UIUtils.createComposite(partDivider, 1);
        errorComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        statusPart = new StatusPart(errorComposite, status);

        sqlPanel = UIUtils.createComposite(partDivider, 1);
        sqlPanel.setLayout(new FillLayout());
        UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
        try {
            Object panel = serviceSQL.createSQLPanel(controller.getSite(), sqlPanel, controller, "SQL", sqlText);
            if (panel instanceof TextViewer) {
                textWidget = ((TextViewer) panel).getTextWidget();
            }
        } catch (DBException e) {
            textWidget = new StyledText(sqlPanel, SWT.BORDER | SWT.READ_ONLY);
            textWidget.setText(sqlText);
        }

        try {
            boolean widthSet = false;
            IDialogSettings viewSettings = ResultSetUtils.getViewerSettings(SETTINGS_SECTION_ERROR_PANEL);
            String errorWidth = viewSettings.get(PROP_ERROR_WIDTH);
            if (errorWidth != null) {
                String[] widthStrs = errorWidth.split(":");
                if (widthStrs.length == 2) {
                    partDivider.setWeights(new int[]{
                        Integer.parseInt(widthStrs[0]),
                        Integer.parseInt(widthStrs[1])});
                }
                widthSet = true;
            }
            if (!widthSet) {
                partDivider.setWeights(new int[] { 700, 300 } );
            }
            partDivider.addCustomSashFormListener((firstControlWeight, secondControlWeight) -> {
                int[] weights = partDivider.getWeights();
                viewSettings.put(PROP_ERROR_WIDTH, weights[0] + ":" + weights[1]);
            });
        } catch (Throwable e) {
            log.debug(e);
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

    @Nullable
    @Override
    public String copySelectionToString(ResultSetCopySettings settings) {
        return null;
    }

}
