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
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLine3DRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jkiss.dbeaver.ui.AWTUtils;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardChartComposite;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardRenderer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardFetchType;
import org.jkiss.dbeaver.ui.dashboard.model.data.DashboardDataset;
import org.jkiss.dbeaver.ui.dashboard.model.data.DashboardDatasetRow;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Dashboard renderer
 */
public class DashboardRendererHistogram implements DashboardRenderer {

    @Override
    public DashboardChartComposite createDashboard(Composite composite, DashboardContainer container, Point preferredSize) {

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        //generateSampleSeries(container, dataset);

        JFreeChart histogramChart = ChartFactory.createXYLineChart(
            null,
            "Time",
            "Value",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false);
        histogramChart.setBorderVisible(false);
        histogramChart.setPadding(new RectangleInsets(0, 0, 0, 0));
        histogramChart.setTextAntiAlias(true);
        histogramChart.setBackgroundPaint(AWTUtils.makeAWTColor(UIStyles.getDefaultTextBackground()));
        //histogramChart.getLegend().setBorder(0, 0, 0, 0);
        //histogramChart.removeLegend();

        LegendTitle legend = histogramChart.getLegend();
        legend.setPosition(RectangleEdge.BOTTOM);
        legend.setBorder(0, 0, 0, 0);
        legend.setBackgroundPaint(histogramChart.getBackgroundPaint());
        //legend.setAnchor(Legend.EAST);

        ChartPanel chartPanel = new ChartPanel( histogramChart );
        chartPanel.setPreferredSize( new java.awt.Dimension( preferredSize.x, preferredSize.y ) );

        final XYPlot plot = histogramChart.getXYPlot( );
        // Remove border
        plot.setOutlinePaint(null);
        // Remove background
        plot.setShadowGenerator(null);

        //XYItemRenderer renderer = new XYLine3DRenderer();
        //plot.setRenderer(renderer);

//        renderer.setSeriesOutlinePaint(0, Color.black);
//        renderer.setSeriesOutlineStroke(0, new BasicStroke(0.5f));

        DateAxis domainAxis = new DateAxis("Time");
        domainAxis.setDateFormatOverride(new SimpleDateFormat("MM/dd HH:mm"));
        domainAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
        domainAxis.setAutoRange(true);
        domainAxis.setLabel(null);
        domainAxis.setLowerMargin(0);
        domainAxis.setUpperMargin(0);
        plot.setDomainAxis(domainAxis);

        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setLabel(null);
        //rangeAxis.setLowerMargin(0.2);
        //rangeAxis.setLowerBound(.1);

        XYItemRenderer plotRenderer = plot.getRenderer();


        // Set background
        plot.setBackgroundPaint(histogramChart.getBackgroundPaint());
        Color gridColor = AWTUtils.makeAWTColor(UIStyles.getDefaultTextForeground());
        plot.setDomainGridlinePaint(gridColor);
        plot.setRangeGridlinePaint(gridColor);

        DashboardChartComposite chartComposite = new DashboardChartComposite(container, composite, SWT.DOUBLE_BUFFERED, preferredSize);
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
    public void updateDashboardData(DashboardContainer container, Date lastUpdateTime, DashboardDataset dataset) {
        DashboardChartComposite chartComposite = (DashboardChartComposite) container.getDashboardControl();
        if (chartComposite.isDisposed()) {
            return;
        }
        JFreeChart chart = chartComposite.getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        TimeSeriesCollection chartDataset = (TimeSeriesCollection) plot.getDataset();

        DashboardDatasetRow lastRow = (DashboardDatasetRow) chartComposite.getData("last_row");

        List<DashboardDatasetRow> rows = dataset.getRows();

        if (container.getDashboardFetchType() == DashboardFetchType.columns) {
            String[] srcSeries = dataset.getColumnNames();
            for (int i = 0; i < srcSeries.length; i++) {
                String seriesName = srcSeries[i];

                TimeSeries series = chartDataset.getSeries(seriesName);
                if (series == null) {
                    series = new TimeSeries(seriesName);
                    series.setMaximumItemCount(container.getDashboardMaxItems());
                    series.setMaximumItemAge(container.getDashboardMaxAge());

                    BasicStroke stroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f);
                    plot.getRenderer().setSeriesStroke(chartDataset.getSeriesCount(), stroke);

                    chartDataset.addSeries(series);
                }

                switch (container.getDashboardCalcType()) {
                    case value: {
                        for (DashboardDatasetRow row : rows) {
                            Object value = row.getValues()[i];
                            if (value instanceof Number) {
                                series.add(new FixedMillisecond(row.getTimestamp().getTime()), (Number) value, false);
                            }
                        }
                        break;
                    }
                    case delta: {
                        for (DashboardDatasetRow row : rows) {
                            if (lastRow != null) {
                                Object prevValue = lastRow.getValues()[i];
                                Object newValue = row.getValues()[i];
                                if (newValue instanceof Number && prevValue instanceof Number) {
                                    double delataValue = ((Number) newValue).doubleValue() - ((Number) prevValue).doubleValue();
                                    series.add(
                                        new FixedMillisecond(
                                            row.getTimestamp().getTime()),
                                        delataValue,
                                        false);
                                }
                            }
                        }
                        break;
                    }
                }
                series.fireSeriesChanged();
            }
        } else {
            // Not supported

        }

        if (!rows.isEmpty()) {
            chartComposite.setData("last_row", rows.get(rows.size() - 1));
        }
    }

    @Override
    public void disposeDashboard(DashboardContainer container) {

    }

}
