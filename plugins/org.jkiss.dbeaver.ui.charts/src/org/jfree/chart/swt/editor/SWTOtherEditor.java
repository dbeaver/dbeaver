/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jfree.chart.swt.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.util.ResourceBundleWrapper;
import org.jfree.swt.SWTUtils;

import java.util.ResourceBundle;

/**
 * An editor for miscellaneous chart properties.
 */
class SWTOtherEditor extends Composite {

    /** A checkbox indicating whether or not
     * the chart is drawn with anti-aliasing. */
    private Button antialias;

    /** The chart background color. */
    private SWTPaintCanvas backgroundPaintCanvas;

    /** The resourceBundle for the localization. */
    protected static ResourceBundle localizationResources
            = ResourceBundleWrapper.getBundle(
                    "org.jfree.chart.editor.LocalizationBundle");

    /**
     * Creates a new instance.
     *
     * @param parent  the parent.
     * @param style  the style.
     * @param chart  the chart.
     */
    public SWTOtherEditor(Composite parent, int style, JFreeChart chart) {
        super(parent, style);
        FillLayout layout = new FillLayout();
        layout.marginHeight = layout.marginWidth = 4;
        setLayout(layout);

        Group general = new Group(this, SWT.NONE);
        general.setLayout(new GridLayout(3, false));
        general.setText(localizationResources.getString("General"));

        // row 1: antialiasing
        this.antialias = new Button(general, SWT.CHECK);
        this.antialias.setText(localizationResources.getString(
                "Draw_anti-aliased"));
        this.antialias.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true,
                false, 3, 1));
        this.antialias.setSelection(chart.getAntiAlias());

        //row 2: background paint for the chart
        new Label(general, SWT.NONE).setText(localizationResources.getString(
                "Background_paint"));
        this.backgroundPaintCanvas = new SWTPaintCanvas(general, SWT.NONE,
                SWTUtils.toSwtColor(getDisplay(), chart.getBackgroundPaint()));
        GridData bgGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        bgGridData.heightHint = 20;
        this.backgroundPaintCanvas.setLayoutData(bgGridData);
        Button selectBgPaint = new Button(general, SWT.PUSH);
        selectBgPaint.setText(localizationResources.getString("Select..."));
        selectBgPaint.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false,
                false));
        selectBgPaint.addSelectionListener(
                new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent event) {
                        ColorDialog dlg = new ColorDialog(getShell());
                        dlg.setText(localizationResources.getString(
                                "Background_paint"));
                        dlg.setRGB(SWTOtherEditor.this.backgroundPaintCanvas
                                .getColor().getRGB());
                        RGB rgb = dlg.open();
                        if (rgb != null) {
                            SWTOtherEditor.this.backgroundPaintCanvas.setColor(
                                    new Color(getDisplay(), rgb));
                        }
                    }
                }
        );
    }

    /**
     * Updates the chart.
     *
     * @param chart  the chart.
     */
    public void updateChartProperties(JFreeChart chart) {
        chart.setAntiAlias(this.antialias.getSelection());
        chart.setBackgroundPaint(SWTUtils.toAwtColor(
                this.backgroundPaintCanvas.getColor()));
    }

}
