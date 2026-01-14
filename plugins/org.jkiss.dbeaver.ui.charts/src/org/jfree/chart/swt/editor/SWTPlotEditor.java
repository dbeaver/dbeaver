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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.util.ResourceBundleWrapper;
import org.jfree.swt.SWTUtils;

import java.awt.*;
import java.util.ResourceBundle;

/**
 * An editor for plot properties.
 */
class SWTPlotEditor extends Composite {

    /**
     * A panel used to display/edit the properties of the domain axis (if any).
     */
    private SWTAxisEditor domainAxisPropertyPanel;

    /**
     * A panel used to display/edit the properties of the range axis (if any).
     */
    private SWTAxisEditor rangeAxisPropertyPanel;

    private SWTPlotAppearanceEditor plotAppearance;

    /** The resourceBundle for the localization. */
    protected static ResourceBundle localizationResources
            = ResourceBundleWrapper.getBundle(
                    "org.jfree.chart.editor.LocalizationBundle");

    /**
     * Creates a new editor for the specified plot.
     *
     * @param parent  the parent.
     * @param style  the style.
     * @param plot  the plot.
     */
    public SWTPlotEditor(Composite parent, int style, Plot plot) {
        super(parent, style);
        FillLayout layout = new FillLayout();
        layout.marginHeight = layout.marginWidth = 4;
        setLayout(layout);

        Group plotType = new Group(this, SWT.NONE);
        FillLayout plotTypeLayout = new FillLayout();
        plotTypeLayout.marginHeight = plotTypeLayout.marginWidth = 4;
        plotType.setLayout(plotTypeLayout);
        plotType.setText(plot.getPlotType() + localizationResources.getString(
                ":"));

        TabFolder tabs = new TabFolder(plotType, SWT.NONE);

        //deal with domain axis
        TabItem item1 = new TabItem(tabs, SWT.NONE);
        item1.setText(localizationResources.getString("Domain_Axis"));
        Axis domainAxis = null;
        if (plot instanceof CategoryPlot) {
            domainAxis = ((CategoryPlot) plot).getDomainAxis();
        }
        else if (plot instanceof XYPlot) {
            domainAxis = ((XYPlot) plot).getDomainAxis();
        }
        this.domainAxisPropertyPanel = SWTAxisEditor.getInstance(tabs,
                SWT.NONE, domainAxis);
        item1.setControl(this.domainAxisPropertyPanel);

        //deal with range axis
        TabItem item2 = new TabItem(tabs, SWT.NONE);
        item2.setText(localizationResources.getString("Range_Axis"));
        Axis rangeAxis = null;
        if (plot instanceof CategoryPlot) {
            rangeAxis = ((CategoryPlot) plot).getRangeAxis();
        }
        else if (plot instanceof XYPlot) {
            rangeAxis = ((XYPlot) plot).getRangeAxis();
        }
        this.rangeAxisPropertyPanel = SWTAxisEditor.getInstance(tabs, SWT.NONE,
                rangeAxis);
        item2.setControl(this.rangeAxisPropertyPanel);

        //deal with plot appearance
        TabItem item3 = new TabItem(tabs, SWT.NONE);
        item3.setText(localizationResources.getString("Appearance"));
        this.plotAppearance = new SWTPlotAppearanceEditor(tabs, SWT.NONE, plot);
        item3.setControl(this.plotAppearance);
    }

    /**
     * Returns the current outline stroke.
     *
     * @return The current outline stroke.
     */
    public Color getBackgroundPaint() {
        return this.plotAppearance.getBackGroundPaint();
    }

    /**
     * Returns the current outline stroke.
     *
     * @return The current outline stroke.
     */
    public Color getOutlinePaint() {
        return this.plotAppearance.getOutlinePaint();
    }

    /**
     * Returns the current outline stroke.
     *
     * @return The current outline stroke.
     */
    public Stroke getOutlineStroke() {
        return this.plotAppearance.getStroke();
    }


    /**
     * Updates the plot properties to match the properties
     * defined on the panel.
     *
     * @param plot  The plot.
     */
    public void updatePlotProperties(Plot plot) {
        // set the plot properties...
        plot.setBackgroundPaint(SWTUtils.toAwtColor(getBackgroundPaint()));
        plot.setOutlinePaint(SWTUtils.toAwtColor(getOutlinePaint()));
        plot.setOutlineStroke(getOutlineStroke());

        // set the axis properties
        if (this.domainAxisPropertyPanel != null) {
            Axis domainAxis = null;
            if (plot instanceof CategoryPlot) {
                CategoryPlot p = (CategoryPlot) plot;
                domainAxis = p.getDomainAxis();
            }
            else if (plot instanceof XYPlot) {
                XYPlot p = (XYPlot) plot;
                domainAxis = p.getDomainAxis();
            }
            if (domainAxis != null)
                this.domainAxisPropertyPanel.setAxisProperties(domainAxis);
        }
        if (this.rangeAxisPropertyPanel != null) {
            Axis rangeAxis = null;
            if (plot instanceof CategoryPlot) {
                CategoryPlot p = (CategoryPlot) plot;
                rangeAxis = p.getRangeAxis();
            }
            else if (plot instanceof XYPlot) {
                XYPlot p = (XYPlot) plot;
                rangeAxis = p.getRangeAxis();
            }
            if (rangeAxis != null)
                this.rangeAxisPropertyPanel.setAxisProperties(rangeAxis);
        }
        if (this.plotAppearance.getPlotOrientation() != null) {
            if (plot instanceof CategoryPlot) {
                CategoryPlot p = (CategoryPlot) plot;
                p.setOrientation(this.plotAppearance.getPlotOrientation());
            }
            else if (plot instanceof XYPlot) {
                XYPlot p = (XYPlot) plot;
                p.setOrientation(this.plotAppearance.getPlotOrientation());
            }
        }
    }
}
