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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.chart.util.ResourceBundleWrapper;
import org.jfree.swt.SWTUtils;

import java.util.ResourceBundle;

/**
 * An editor for chart title properties.
 */
class SWTTitleEditor extends Composite {

    /** Whether or not to display the title on the chart. */
    private boolean showTitle;

    /** The checkbox to indicate whether or not to display the title. */
    private Button showTitleCheckBox;

    /** A field for displaying/editing the title text. */
    private Text titleField;

    /** The font used to draw the title. */
    private FontData titleFont;

    /** A field for displaying a description of the title font. */
    private Text fontField;

    /** The button to use to select a new title font. */
    private Button selectFontButton;

    /** The paint (color) used to draw the title. */
    private Color titleColor;

    /** The button to use to select a new paint (color) to draw the title. */
    private Button selectColorButton;

    /** The resourceBundle for the localization. */
    protected static ResourceBundle localizationResources
            = ResourceBundleWrapper.getBundle(
                    "org.jfree.chart.editor.LocalizationBundle");

    /** Font object used to handle a change of font. */
    private Font font;

    /**
     * Standard constructor: builds a panel for displaying/editing the
     * properties of the specified title.
     *
     * @param parent  the parent.
     * @param style  the style.
     * @param title  the title, which should be changed.
     *
     */
    SWTTitleEditor(Composite parent, int style, Title title) {
        super(parent, style);
        FillLayout layout = new FillLayout();
        layout.marginHeight = layout.marginWidth = 4;
        setLayout(layout);

        TextTitle t = (title != null ? (TextTitle) title
                : new TextTitle(localizationResources.getString("Title")));
        this.showTitle = (title != null);
        this.titleFont = SWTUtils.toSwtFontData(getDisplay(), t.getFont(),
                true);
        this.titleColor = SWTUtils.toSwtColor(getDisplay(), t.getPaint());

        Group general = new Group(this, SWT.NONE);
        general.setLayout(new GridLayout(3, false));
        general.setText(localizationResources.getString("General"));
        // row 1
        Label label = new Label(general, SWT.NONE);
        label.setText(localizationResources.getString("Show_Title"));
        GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        label.setLayoutData(gridData);
        this.showTitleCheckBox = new Button(general, SWT.CHECK);
        this.showTitleCheckBox.setSelection(this.showTitle);
        this.showTitleCheckBox.setLayoutData(new GridData(SWT.CENTER,
                SWT.CENTER, false, false));
        this.showTitleCheckBox.addSelectionListener(
                new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent event) {
                        SWTTitleEditor.this.showTitle = SWTTitleEditor.this
                                .showTitleCheckBox.getSelection();
                    }
                });
        // row 2
        new Label(general, SWT.NONE).setText(localizationResources.getString(
                "Text"));
        this.titleField = new Text(general, SWT.BORDER);
        this.titleField.setText(t.getText());
        this.titleField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false));
        new Label(general, SWT.NONE).setText("");
        // row 3
        new Label(general, SWT.NONE).setText(localizationResources.getString(
                "Font"));
        this.fontField = new Text(general, SWT.BORDER);
        this.fontField.setText(this.titleFont.toString());
        this.fontField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false));
        this.selectFontButton = new Button(general, SWT.PUSH);
        this.selectFontButton.setText(localizationResources.getString(
                "Select..."));
        this.selectFontButton.addSelectionListener(
                new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent event) {
                        // Create the font-change dialog
                        FontDialog dlg = new FontDialog(getShell());
                        dlg.setText(localizationResources.getString(
                                "Font_Selection"));
                        dlg.setFontList(new FontData[] {
                                SWTTitleEditor.this.titleFont });
                        if (dlg.open() != null) {
                            // Dispose of any fonts we have created
                            if (SWTTitleEditor.this.font != null) {
                                SWTTitleEditor.this.font.dispose();
                            }
                            // Create the new font and set it into the title
                            // label
                            SWTTitleEditor.this.font = new Font(
                                    getShell().getDisplay(), dlg.getFontList());
                            //titleField.setFont(font);
                            SWTTitleEditor.this.fontField.setText(
                                    SWTTitleEditor.this.font.getFontData()[0]
                                    .toString());
                            SWTTitleEditor.this.titleFont
                                    = SWTTitleEditor.this.font.getFontData()[0];
                        }
                    }
                }
        );
        // row 4
        new Label(general, SWT.NONE).setText(localizationResources.getString(
                "Color"));
        // Use a SwtPaintCanvas to show the color, note that we must set the
        // heightHint.
        final SWTPaintCanvas colorCanvas = new SWTPaintCanvas(general,
                SWT.NONE, this.titleColor);
        GridData canvasGridData = new GridData(SWT.FILL, SWT.CENTER, true,
                false);
        canvasGridData.heightHint = 20;
        colorCanvas.setLayoutData(canvasGridData);
        this.selectColorButton = new Button(general, SWT.PUSH);
        this.selectColorButton.setText(localizationResources.getString(
                "Select..."));
        this.selectColorButton.addSelectionListener(
                new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent event) {
                        // Create the color-change dialog
                        ColorDialog dlg = new ColorDialog(getShell());
                        dlg.setText(localizationResources.getString(
                                "Title_Color"));
                        dlg.setRGB(SWTTitleEditor.this.titleColor.getRGB());
                        RGB rgb = dlg.open();
                        if (rgb != null) {
                            // create the new color and set it to the
                            // SwtPaintCanvas
                            SWTTitleEditor.this.titleColor = new Color(
                                    getDisplay(), rgb);
                            colorCanvas.setColor(
                                    SWTTitleEditor.this.titleColor);
                        }
                    }
                }
        );
    }

    /**
     * Returns the title text entered in the panel.
     *
     * @return The title text entered in the panel.
     */
    public String getTitleText() {
        return this.titleField.getText();
    }

    /**
     * Returns the font selected in the panel.
     *
     * @return The font selected in the panel.
     */
    public FontData getTitleFont() {
        return this.titleFont;
    }

    /**
     * Returns the font selected in the panel.
     *
     * @return The font selected in the panel.
     */
    public Color getTitleColor() {
        return this.titleColor;
    }

    /**
     * Sets the properties of the specified title to match the properties
     * defined on this panel.
     *
     * @param chart  the chart whose title is to be modified.
     */
    public void setTitleProperties(JFreeChart chart) {
        if (this.showTitle) {
            TextTitle title = chart.getTitle();
            if (title == null) {
                title = new TextTitle();
                chart.setTitle(title);
            }
            title.setText(getTitleText());
            title.setFont(SWTUtils.toAwtFont(getDisplay(), getTitleFont(),
                    true));
            title.setPaint(SWTUtils.toAwtColor(getTitleColor()));
        }
        else {
            chart.setTitle((TextTitle) null);
        }
    }
}
