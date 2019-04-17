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

package org.jkiss.dbeaver.ui.gis.presentation;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.ui.controls.resultset.AbstractPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCopySettings;
import org.jkiss.dbeaver.ui.gis.panel.GISLeafletViewer;

/**
 * Geometry presentation.
 */
public class GeometryPresentation extends AbstractPresentation {

    private static final Log log = Log.getLog(GeometryPresentation.class);

    private GISLeafletViewer leafletViewer;

    @Override
    public void createPresentation(@NotNull final IResultSetController controller, @NotNull Composite parent) {
        super.createPresentation(controller, parent);

        leafletViewer = new GISLeafletViewer(parent, null);
        leafletViewer.getBrowser().setLayoutData(new GridData(GridData.FILL_BOTH));
/*
        canvas = new ResultsChartComposite(this, parent, SWT.NONE);
        canvas.setLayoutData(new GridData(GridData.FILL_BOTH));
        canvas.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        canvas.addDisposeListener(e -> {

        });

        registerContextMenu();
        trackPresentationControl();

        TextEditorUtils.enableHostEditorKeyBindingsSupport(controller.getSite(), canvas);

        applyThemeSettings();
*/
    }

    @Override
    protected void applyThemeSettings() {
/*
        {
            boolean isDark = TextEditorUtils.isDarkThemeEnabled();

            ChartTheme newTheme;
            if (isDark) {
                newTheme = DARK_THEME;
            } else {
                newTheme = CLASSIC_THEME;
            }
            if (ChartFactory.getChartTheme() != newTheme) {
                ChartFactory.setChartTheme(newTheme);
                if (canvas.getChart() != null) {
                    refreshData(false, false, true);
                }
            }
        }
*/
    }

    @Override
    public Composite getControl() {
        return leafletViewer.getBrowser();
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

    @Override
    public void scrollToRow(@NotNull RowPosition position) {
        super.scrollToRow(position);
    }

    @Nullable
    @Override
    public DBDAttributeBinding getCurrentAttribute() {
        return controller.getModel().getDocumentAttribute();
    }

    @Nullable
    @Override
    public String copySelectionToString(ResultSetCopySettings settings) {
        return null;
    }

    ///////////////////////////////////////////////////////////////////////
    // ISelectionProvider

    @Override
    public ISelection getSelection() {
        return new StructuredSelection();
    }

    @Override
    public void setSelection(ISelection selection) {
    }

    @Override
    public void refreshData(boolean refreshMetadata, boolean append, boolean keepState) {
    }


}
