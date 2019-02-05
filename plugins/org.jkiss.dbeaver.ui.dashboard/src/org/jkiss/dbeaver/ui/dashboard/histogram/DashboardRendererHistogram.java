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
package org.jkiss.dbeaver.ui.dashboard.histogram;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardChartComposite;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardRenderer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardDataset;

import java.awt.*;
import java.text.SimpleDateFormat;

/**
 * Dashboard renderer
 */
public class DashboardRendererHistogram implements DashboardRenderer {

    @Override
    public Control createDashboard(Composite composite, DashboardContainer container, Point preferredSize) {

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        generateSampleSeries(container, dataset);

        JFreeChart histogramChart = ChartFactory.createXYLineChart(
            null,
            "Time",
            "Value",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false);

        ChartPanel chartPanel = new ChartPanel( histogramChart );
        chartPanel.setPreferredSize( new java.awt.Dimension( preferredSize.x, preferredSize.y ) );
        final XYPlot plot = histogramChart.getXYPlot( );

        DateAxis domainAxis = new DateAxis("Time");
        domainAxis.setDateFormatOverride(new SimpleDateFormat("MM/dd HH:mm"));
        domainAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
        domainAxis.setAutoRange(true);
        domainAxis.setLabel(null);
        plot.setDomainAxis(domainAxis);

        plot.getRangeAxis().setLabel(null);

        // Set background
        org.eclipse.swt.graphics.RGB swtBgColor = composite.getBackground().getRGB();
        plot.setBackgroundPaint(new Color(swtBgColor.red, swtBgColor.green, swtBgColor.blue));

        DashboardChartComposite chartComposite = new DashboardChartComposite(container, composite, SWT.NONE, preferredSize);
        chartComposite.setChart(histogramChart);

        return chartComposite;
    }

    private void generateSampleSeries(DashboardContainer container, TimeSeriesCollection dataset) {
        TimeSeries seriesSin = new TimeSeries("Sin");
        long startTime = System.currentTimeMillis() - 1000 * 60 * 60 * 2;
        for (int i = 0; i < 100; i++) {
            seriesSin.add(new TimeSeriesDataItem(new FixedMillisecond(startTime + i * 60 * 1000), Math.sin(0.1 * i) * 100));
        }
        dataset.addSeries(seriesSin);

        TimeSeries seriesCos = new TimeSeries("Cos");
        for (int i = 0; i < 100; i++) {
            seriesCos.add(new TimeSeriesDataItem(new FixedMillisecond(startTime + i * 60 * 1000), Math.cos(0.1 * i) * 100));
        }
        dataset.addSeries(seriesCos);

    }

    @Override
    public void updateDashboardData(DashboardContainer container, DashboardDataset dataset) {

    }

    @Override
    public void disposeDashboard(DashboardContainer container) {

    }

}
