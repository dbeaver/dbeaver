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
package org.jkiss.dbeaver.ui.dashboard.control;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.ui.RectangleEdge;
import org.jkiss.dbeaver.ui.AWTUtils;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemViewConfiguration;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewContainer;

import java.awt.*;

/**
 * Base dashboard renderer
 */
public abstract class DashboardRendererBase implements DashboardRenderer {

    private static final Font DEFAULT_LEGEND_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 9);
    private static final Font DEFAULT_TICK_LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 8);

    protected void generateSampleSeries(DashboardContainer container, TimeSeriesCollection dataset) {
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
    public void moveDashboardView(DashboardItem toItem, DashboardItem fromItem, boolean clearOriginal) {
        DashboardChartComposite toComp = getChartComposite(toItem);
        DashboardChartComposite fromComp = getChartComposite(fromItem);
        toComp.setChart(fromComp.getChart());
        if (clearOriginal) {
            fromComp.setChart(null);
        }

/*
        XYPlot plotTo = getDashboardPlot(dashboardItem);
        XYPlot plotFrom = getDashboardPlot(fromItem);
        if (plotTo != null && plotFrom != null) {
            TimeSeriesCollection datasetTo = (TimeSeriesCollection) plotTo.getDataset();
            TimeSeriesCollection datasetFrom = (TimeSeriesCollection) plotFrom.getDataset();
            datasetTo.removeAllSeries();
            for (int i = 0; i < datasetFrom.getSeriesCount(); i++) {
                TimeSeries seriesFrom = datasetFrom.getSeries(i);
                TimeSeries seriesTo = new TimeSeries(seriesFrom.getKey(), seriesFrom.getDomainDescription(), seriesFrom.getRangeDescription());
                seriesTo.setMaximumItemAge(seriesFrom.getMaximumItemAge());
                seriesTo.setMaximumItemCount(seriesFrom.getMaximumItemCount());
                for (Object si : seriesFrom.getItems()) {
                    seriesTo.add((TimeSeriesDataItem) si);
                }
                datasetTo.addSeries(seriesTo);
                plotTo.getRenderer().setSeriesStroke(datasetTo.getSeriesCount() - 1, plotTo.getRenderer().getBaseStroke());
            }

        }
*/
    }

    @Override
    public void disposeDashboard(DashboardContainer container) {
        DashboardChartComposite chartComposite = getChartComposite(container);
        if (chartComposite != null) {
            chartComposite.setChart(null);
        }
    }

    protected DashboardChartComposite getChartComposite(DashboardContainer container) {
        return (DashboardChartComposite) container.getDashboardControl();
    }

    protected DashboardChartComposite createChartComposite(Composite composite, DashboardContainer container, DashboardViewContainer viewContainer, org.eclipse.swt.graphics.Point preferredSize) {
        return new DashboardChartComposite(container, viewContainer, composite, SWT.DOUBLE_BUFFERED, preferredSize);
    }

    protected void createDefaultLegend(DashboardItemViewConfiguration viewConfig, JFreeChart chart) {
        Color gridColor = AWTUtils.makeAWTColor(UIStyles.getDefaultTextForeground());
        LegendTitle legend = chart.getLegend();
        legend.setPosition(RectangleEdge.BOTTOM);
        legend.setBorder(0, 0, 0, 0);
        legend.setBackgroundPaint(chart.getBackgroundPaint());
        legend.setItemPaint(gridColor);
        legend.setItemFont(DEFAULT_LEGEND_FONT);

        if (viewConfig != null && !viewConfig.isLegendVisible()) {
            legend.setVisible(false);
        }
    }

}
