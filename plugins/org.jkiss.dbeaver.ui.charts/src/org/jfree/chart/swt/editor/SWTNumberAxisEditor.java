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
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;

/**
 * An editor for {@link NumberAxis} properties.
 */
class SWTNumberAxisEditor extends SWTAxisEditor implements FocusListener {
    
    /** A flag that indicates whether or not the axis range is determined
     *  automatically.
     */
    private boolean autoRange;

    /** The lowest value in the axis range. */
    private double minimumValue;

    /** The highest value in the axis range. */
    private double maximumValue;

    /** A checkbox that indicates whether or not the axis range is determined
     *  automatically.
     */
    private Button autoRangeCheckBox;

    /** A text field for entering the minimum value in the axis range. */
    private Text minimumRangeValue;

    /** A text field for entering the maximum value in the axis range. */
    private Text maximumRangeValue;

    /**
     * Creates a new editor.
     * 
     * @param parent  the parent.
     * @param style  the style.
     * @param axis  the axis.
     */
    public SWTNumberAxisEditor(Composite parent, int style, NumberAxis axis) {
        super(parent, style, axis);
        this.autoRange = axis.isAutoRange();
        this.minimumValue = axis.getLowerBound();
        this.maximumValue = axis.getUpperBound();

        TabItem item2 = new TabItem(getOtherTabs(), SWT.NONE);
        item2.setText(" " + localizationResources.getString("Range") + " ");
        Composite range = new Composite(getOtherTabs(), SWT.NONE);
        range.setLayout(new GridLayout(2, true));
        item2.setControl(range);
        
        this.autoRangeCheckBox = new Button(range, SWT.CHECK);
        this.autoRangeCheckBox.setText(localizationResources.getString(
                "Auto-adjust_range"));
        this.autoRangeCheckBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, 
                true, false, 2, 1));
        this.autoRangeCheckBox.setSelection(this.autoRange);
        this.autoRangeCheckBox.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) { 
                    toggleAutoRange();
                }
            });
        new Label(range, SWT.NONE).setText(localizationResources.getString(
                "Minimum_range_value"));
        this.minimumRangeValue = new Text(range, SWT.BORDER);
        this.minimumRangeValue.setText(String.valueOf(this.minimumValue));
        this.minimumRangeValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
                true, false));
        this.minimumRangeValue.setEnabled(!this.autoRange);
        //this.minimumRangeValue.addModifyListener(this);
        //this.minimumRangeValue.addVerifyListener(this);
        this.minimumRangeValue.addFocusListener(this);
        new Label(range, SWT.NONE).setText(localizationResources.getString(
                "Maximum_range_value"));
        this.maximumRangeValue = new Text(range, SWT.BORDER);
        this.maximumRangeValue.setText(String.valueOf(this.maximumValue));
        this.maximumRangeValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
                true, false));
        this.maximumRangeValue.setEnabled(!this.autoRange);
        //this.maximumRangeValue.addModifyListener(this);
        //this.maximumRangeValue.addVerifyListener(this);
        this.maximumRangeValue.addFocusListener(this);
    }

    /**
     *  Toggle the auto range setting.
     */
    public void toggleAutoRange() {
        this.autoRange = this.autoRangeCheckBox.getSelection();
        if (this.autoRange) {
            this.minimumRangeValue.setText(Double.toString(this.minimumValue));
            this.minimumRangeValue.setEnabled(false);
            this.maximumRangeValue.setText(Double.toString(this.maximumValue));
            this.maximumRangeValue.setEnabled(false);
        }
        else {
            this.minimumRangeValue.setEnabled(true);
            this.maximumRangeValue.setEnabled(true);
        }
    }

    /**
     * Revalidate the range minimum:
     * it should be less than the current maximum.
     * 
     * @param candidate  the minimum value
     * 
     * @return A boolean.
     */
    public boolean validateMinimum(String candidate)
    {
        boolean valid = true;
        try {
            if (Double.parseDouble(candidate) >= this.maximumValue) {
                valid = false;
            }
        }
        catch (NumberFormatException e) {
            valid = false;
        }
        return valid;
    }

    /**
     * Revalidate the range maximum:
     * it should be greater than the current minimum
     * 
     * @param candidate  the maximum value
     * 
     * @return A boolean.
     */
    public boolean validateMaximum(String candidate)
    {
        boolean valid = true;
        try {
            if (Double.parseDouble(candidate) <= this.minimumValue) {
                valid = false;
            }
        }
        catch (NumberFormatException e) {
            valid = false;
        }
        return valid;
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.events.FocusListener#focusGained(
     * org.eclipse.swt.events.FocusEvent)
     */
    public void focusGained(FocusEvent e) {
        // don't need to do anything
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.events.FocusListener#focusLost(
     * org.eclipse.swt.events.FocusEvent)
     */
    public void focusLost(FocusEvent e) {
        if (e.getSource() == this.minimumRangeValue) {
            // verify min value
            if (!validateMinimum(this.minimumRangeValue.getText()))
                this.minimumRangeValue.setText(String.valueOf(
                        this.minimumValue));
            else
                this.minimumValue = Double.parseDouble(
                        this.minimumRangeValue.getText());
        }
        else if (e.getSource() == this.maximumRangeValue) {
            // verify max value
            if (!validateMaximum(this.maximumRangeValue.getText()))
                this.maximumRangeValue.setText(String.valueOf(
                        this.maximumValue));
            else
                this.maximumValue = Double.parseDouble(
                        this.maximumRangeValue.getText());
        }
    }

    /**
     * Sets the properties of the specified axis to match 
     * the properties defined on this panel.
     *
     * @param axis  the axis.
     */
    public void setAxisProperties(Axis axis) {
        super.setAxisProperties(axis);
        NumberAxis numberAxis = (NumberAxis) axis;
        numberAxis.setAutoRange(this.autoRange);
        if (!this.autoRange) {
            numberAxis.setRange(this.minimumValue, this.maximumValue);
        }
    }
}
