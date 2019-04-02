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
import org.eclipse.swt.SWT;
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

        Composite sqlPanel = UIUtils.createComposite(partDivider, 1);
        sqlPanel.setLayout(new FillLayout());
        UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
        try {
            serviceSQL.createSQLPanel(controller.getSite(), sqlPanel, controller, "SQL", sqlText);
        } catch (DBException e) {
            Text text = new Text(sqlPanel, SWT.BORDER | SWT.READ_ONLY);
            text.setText(sqlText);
        }

        try {
            boolean widthSet = false;
            IDialogSettings viewSettings = ResultSetUtils.getViewerSettings(SETTINGS_SECTION_ERROR_PANEL);
            if (viewSettings.get(PROP_ERROR_WIDTH) != null) {
                int errorWidth = viewSettings.getInt(PROP_ERROR_WIDTH);
                if (errorWidth > 0) {
                    partDivider.setWeights(new int[] { errorWidth, 1000 - errorWidth } );
                    widthSet = true;
                }
            }
            if (!widthSet) {
                partDivider.setWeights(new int[] { 750, 250 } );
            }
            partDivider.addCustomSashFormListener((firstControlWeight, secondControlWeight) -> {
                viewSettings.put(PROP_ERROR_WIDTH, partDivider.getWeights()[0]);
            });
        } catch (Throwable e) {
            log.debug(e);
        }
    }

    @Override
    public Control getControl() {
        return errorComposite;
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
