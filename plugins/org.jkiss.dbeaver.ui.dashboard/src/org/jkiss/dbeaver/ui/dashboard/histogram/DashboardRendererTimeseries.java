/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.*;
import org.jfree.ui.RectangleInsets;
import org.jkiss.dbeaver.ui.AWTUtils;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.charts.BaseChartDrawingSupplier;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardChartComposite;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardItem;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardRendererBase;
import org.jkiss.dbeaver.ui.dashboard.model.*;
import org.jkiss.dbeaver.ui.dashboard.model.data.DashboardDataset;
import org.jkiss.dbeaver.ui.dashboard.model.data.DashboardDatasetRow;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Histogram dashboard renderer
 */
public class DashboardRendererTimeseries extends DashboardRendererBase {

    private static final Font DEFAULT_TICK_LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
    public static final int MAX_TIMESERIES_RANGE_LABELS = 25;

    @Override
    public DashboardChartComposite createDashboard(Composite composite, DashboardContainer container, DashboardViewContainer viewContainer, Point preferredSize) {

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        //generateSampleSeries(container, dataset);

        DashboardItemViewConfiguration viewConfig = viewContainer.getViewConfiguration().getDashboardConfig(container.getDashboardId());

        Color gridColor = AWTUtils.makeAWTColor(UIStyles.getDefaultTextForeground());

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

        createDefaultLegend(viewConfig, histogramChart);

        ChartPanel chartPanel = new ChartPanel( histogramChart );
        chartPanel.setPreferredSize( new java.awt.Dimension( preferredSize.x, preferredSize.y ) );

        final XYPlot plot = histogramChart.getXYPlot( );
        // Remove border
        plot.setOutlinePaint(null);
        // Remove background
        plot.setShadowGenerator(null);

        plot.setDrawingSupplier(new BaseChartDrawingSupplier());

        //XYItemRenderer renderer = new XYLine3DRenderer();
        //plot.setRenderer(renderer);

//        renderer.setSeriesOutlinePaint(0, Color.black);
//        renderer.setSeriesOutlineStroke(0, new BasicStroke(0.5f));

        {
            DateAxis domainAxis = new DateAxis("Time");
            domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
            domainAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
            domainAxis.setAutoRange(true);
            domainAxis.setLabel(null);
            domainAxis.setLowerMargin(0);
            domainAxis.setUpperMargin(0);
            domainAxis.setTickLabelPaint(gridColor);
            domainAxis.setTickLabelFont(DEFAULT_TICK_LABEL_FONT);
            domainAxis.setTickLabelInsets(RectangleInsets.ZERO_INSETS);
            DateTickUnitType unitType;
            switch (container.getDashboardInterval()) {
                case minute:
                    unitType = DateTickUnitType.MINUTE;
                    break;
                case hour:
                    unitType = DateTickUnitType.HOUR;
                    break;
                case day:
                case week:
                    unitType = DateTickUnitType.DAY;
                    break;
                case month:
                    unitType = DateTickUnitType.MONTH;
                    break;
                case year:
                    unitType = DateTickUnitType.YEAR;
                    break;
                default:
                    unitType = DateTickUnitType.SECOND;
                    break;
            }
            int tickCount = container.getDashboardMaxItems();
            if (tickCount > 40) {
                tickCount = container.getDashboardMaxItems() / 5;
            }
            if (tickCount <= 1) {
                tickCount = 10;
            }
            domainAxis.setTickUnit(new DateTickUnit(unitType, Math.min(MAX_TIMESERIES_RANGE_LABELS, tickCount)));
            if (viewConfig != null && !viewConfig.isDomainTicksVisible()) {
                domainAxis.setVisible(false);
            }
            plot.setDomainAxis(domainAxis);
        }

        {
            ValueAxis rangeAxis = plot.getRangeAxis();
            rangeAxis.setLabel(null);
            rangeAxis.setTickLabelPaint(gridColor);
            rangeAxis.setTickLabelFont(DEFAULT_TICK_LABEL_FONT);
            rangeAxis.setTickLabelInsets(RectangleInsets.ZERO_INSETS);
            rangeAxis.setStandardTickUnits(DashboardUtils.getTickUnitsSource(container.getDashboardValueType()));
            if (container.getDashboardValueType() == DashboardValueType.percent) {
                rangeAxis.setLowerBound(0);
                rangeAxis.setUpperBound(100);
            }
            if (viewConfig != null && !viewConfig.isRangeTicksVisible()) {
                rangeAxis.setVisible(false);
            }
            //rangeAxis.setLowerMargin(0.2);
            //rangeAxis.setLowerBound(.1);
        }

        XYItemRenderer plotRenderer = plot.getRenderer();
        plotRenderer.setBaseItemLabelPaint(gridColor);

        BasicStroke stroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f);
        plot.getRenderer().setBaseStroke(stroke);


        // Set background
        plot.setBackgroundPaint(histogramChart.getBackgroundPaint());

/*
        Stroke gridStroke = new BasicStroke(0.1f,
            BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1.0f,
            new float[] {1.0f, 1.0f}, 0.0f);
*/

        plot.setDomainGridlinePaint(gridColor);
        //plot.setDomainGridlineStroke(gridStroke);
        plot.setDomainGridlinesVisible(viewConfig == null || viewConfig.isGridVisible());
        plot.setRangeGridlinePaint(gridColor);
        //plot.setRangeGridlineStroke(gridStroke);
        plot.setRangeGridlinesVisible(viewConfig == null || viewConfig.isGridVisible());

        DashboardChartComposite chartComposite = createChartComposite(composite, container, viewContainer, preferredSize);
        chartComposite.setChart(histogramChart);

        return chartComposite;
    }

    @Override
    public void updateDashboardData(DashboardContainer container, Date lastUpdateTime, DashboardDataset dataset) {
        DashboardChartComposite chartComposite = getChartComposite(container);
        if (chartComposite.isDisposed()) {
            return;
        }
        JFreeChart chart = chartComposite.getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        TimeSeriesCollection chartDataset = (TimeSeriesCollection) plot.getDataset();

        if (container.getDashboardFetchType() == DashboardFetchType.stats) {
            // Clean previous data before stats update
            chartDataset.removeAllSeries();
        }

        long currentTime = System.currentTimeMillis();
        long secondsPassed = lastUpdateTime == null ? 1 : (currentTime - lastUpdateTime.getTime()) / 1000;
        if (secondsPassed <= 0) {
            secondsPassed = 1;
        }

        DashboardDatasetRow lastRow = (DashboardDatasetRow) chartComposite.getData("last_row");

        List<DashboardDatasetRow> rows = dataset.getRows();

        String[] srcSeries = dataset.getColumnNames();
        for (int i = 0; i < srcSeries.length; i++) {
            String seriesName = srcSeries[i];

            TimeSeries series = chartDataset.getSeries(seriesName);
            if (series == null) {
                series = new TimeSeries(seriesName);
                series.setMaximumItemCount(container.getDashboardMaxItems());
                series.setMaximumItemAge(container.getDashboardMaxAge());
                chartDataset.addSeries(series);
                plot.getRenderer().setSeriesStroke(chartDataset.getSeriesCount() - 1, plot.getRenderer().getBaseStroke());
            }

            switch (container.getDashboardCalcType()) {
                case value: {
                    int maxDP = 200;
                    Date startTime = null;

                    for (DashboardDatasetRow row : rows) {
                        if (startTime == null) {
                            startTime = row.getTimestamp();
                        } else {
                            if (container.getDashboardInterval() == DashboardInterval.second || container.getDashboardInterval() == DashboardInterval.millisecond) {
                                long diffSeconds = (row.getTimestamp().getTime() - startTime.getTime()) / 1000;
                                if (diffSeconds > maxDP) {
                                    // Too big difference between start and end points. Stop here otherwise we'll flood chart with too many ticks
                                    break;
                                }
                            }
                        }
                        Object value = row.getValues()[i];
                        if (value instanceof Number) {
                            series.addOrUpdate(makeDataItem(container, row), (Number) value);
                        }
                    }
                    break;
                }
                case delta: {
                    if (lastUpdateTime == null) {
                        return;
                    }
                    //System.out.println("LAST=" + lastUpdateTime + "; CUR=" + new Date());
                    for (DashboardDatasetRow row : rows) {
                        if (lastRow != null) {
                            Object prevValue = lastRow.getValues()[i];
                            Object newValue = row.getValues()[i];
                            if (newValue instanceof Number && prevValue instanceof Number) {
                                double deltaValue = ((Number) newValue).doubleValue() - ((Number) prevValue).doubleValue();
                                deltaValue /= secondsPassed;
                                if (container.getDashboardValueType() != DashboardValueType.decimal) {
                                    deltaValue = Math.round(deltaValue);
                                }
                                series.addOrUpdate(
                                    makeDataItem(container, row),
                                    deltaValue);
                            }
                        }
                    }
                    break;
                }
            }
        }

        if (!rows.isEmpty()) {
            chartComposite.setData("last_row", rows.get(rows.size() - 1));
        }
    }

    private RegularTimePeriod makeDataItem(DashboardContainer container, DashboardDatasetRow row) {
        switch (container.getDashboardInterval()) {
            case second: return new FixedMillisecond(row.getTimestamp().getTime());
            case minute: return new Minute(row.getTimestamp());
            case hour: return new Hour(row.getTimestamp());
            case day: return new Day(row.getTimestamp());
            case week: return new Week(row.getTimestamp());
            case month: return new Month(row.getTimestamp());
            case year: return new Year(row.getTimestamp());
            default:
                return new FixedMillisecond(row.getTimestamp().getTime());
        }
    }

    @Override
    public void resetDashboardData(DashboardContainer container, Date lastUpdateTime) {
        XYPlot plot = getDashboardPlot(container);
        if (plot != null) {
            TimeSeriesCollection chartDataset = (TimeSeriesCollection) plot.getDataset();
            chartDataset.removeAllSeries();
        }
    }

    @Override
    public void updateDashboardView(DashboardItem dashboardItem) {
        XYPlot plot = getDashboardPlot(dashboardItem);
        if (plot != null) {
            DashboardChartComposite chartComposite = getChartComposite(dashboardItem);
            DashboardViewConfiguration viewConfiguration = chartComposite.getViewContainer().getViewConfiguration();
            DashboardItemViewConfiguration dashboardConfig = viewConfiguration.getDashboardConfig(dashboardItem.getDashboardId());
            if (dashboardConfig != null) {
                plot.getRangeAxis().setVisible(dashboardConfig.isRangeTicksVisible());
                plot.getDomainAxis().setVisible(dashboardConfig.isDomainTicksVisible());

                plot.setDomainGridlinesVisible(dashboardConfig.isGridVisible());
                plot.setRangeGridlinesVisible(dashboardConfig.isGridVisible());

                chartComposite.getChart().getLegend().setVisible(dashboardConfig.isLegendVisible());

                TimeSeriesCollection chartDataset = (TimeSeriesCollection) plot.getDataset();
                for (int i = 0; i < chartDataset.getSeriesCount(); i++) {
                    TimeSeries series = chartDataset.getSeries(i);
                    series.setMaximumItemCount(dashboardConfig.getMaxItems());
                    series.setMaximumItemAge(dashboardConfig.getMaxAge());
                }
            }
        }
        dashboardItem.getParent().layout(true, true);
    }

    private XYPlot getDashboardPlot(DashboardContainer container) {
        DashboardChartComposite chartComposite = getChartComposite(container);
        JFreeChart chart = chartComposite.getChart();
        return chart == null ? null : (XYPlot) chart.getPlot();
    }

}
