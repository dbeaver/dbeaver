/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.charts;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ExtensionFactory;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.Zoomable;
import org.jfree.chart.swt.ChartComposite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.charts.internal.UIChartsMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.awt.geom.Point2D;
import java.io.IOException;

/**
 * Base chart composite
 */
public class BaseChartComposite extends ChartComposite {

    public BaseChartComposite(Composite parent, int style, Point preferredSize) {
        super(parent, style, null,
            preferredSize.x, preferredSize.y,
            30, 20,
            10000, 10000,
            true, false, true, true, true, true);
    }

    public Canvas getChartCanvas() {
        Control[] children = getChildren();
        return children.length == 0 ? null : (Canvas) children[0];
    }

    @Override
    public void mouseDoubleClick(MouseEvent event) {
        if (showChartConfigDialog()) {
            forceRedraw();
        }
    }

    @Override
    protected Menu createPopupMenu(boolean properties, boolean save, boolean print, boolean zoom) {
        MenuManager manager = new MenuManager();
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(this::fillContextMenu);
        Menu contextMenu = manager.createContextMenu(this);
        addDisposeListener(e -> manager.dispose());
        return contextMenu;
    }

    protected void fillContextMenu(IMenuManager manager) {
        final Zoomable zoomable = GeneralUtils.adapt(getChart().getPlot(), Zoomable.class);
        if (zoomable != null) {
            manager.add(new Action(UIChartsMessages.base_chart_composite_action_zoom_in, DBeaverIcons.getImageDescriptor(UIIcon.ZOOM_IN)) {
                @Override
                public void runWithEvent(Event e) {
                    doZoom(zoomable, getChartCanvas().toControl(getDisplay().getCursorLocation()), getZoomInFactor());
                }
            });
            manager.add(new Action(UIChartsMessages.base_chart_composite_action_zoom_out, DBeaverIcons.getImageDescriptor(UIIcon.ZOOM_OUT)) {
                @Override
                public void runWithEvent(Event e) {
                    doZoom(zoomable, getChartCanvas().toControl(getDisplay().getCursorLocation()), getZoomOutFactor());
                }
            });
            manager.add(new Action(UIChartsMessages.base_chart_composite_action_zoom_reset, DBeaverIcons.getImageDescriptor(UIIcon.ZOOM)) {
                @Override
                public void runWithEvent(Event e) {
                    restoreAutoBounds();
                }
            });
            manager.add(new Separator());
        }
        manager.add(new Action(UIChartsMessages.base_chart_composite_action_copy_to_clipboard) {
            @Override
            public void runWithEvent(Event event) {
                doCopyToClipboard();
            }
        });
        manager.add(new Action(UIChartsMessages.base_chart_composite_action_save_as) {
            @Override
            public void run() {
                try {
                    doSaveAs();
                } catch (IOException e) {
                    DBWorkbench.getPlatformUI().showError(UIChartsMessages.base_chart_composite_error_title_save_image,
                            UIChartsMessages.base_chart_composite_error_message_error_saving_chart_image, e);
                }
            }
        });
        manager.add(new Action(UIChartsMessages.base_chart_composite_action_print) {
            @Override
            public void run() {
                createChartPrintJob();
            }
        });

        if (hasConfigurationDialog()) {
            manager.add(new Separator());

            manager.add(new Action(UIChartsMessages.base_chart_composite_action_settings, DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION)) {
                @Override
                public void run() {
                    showChartConfigDialog();
                }
            });
        }
        if (hasColorsConfiguration()) {
            manager.add(new Action(UIChartsMessages.base_chart_composite_action_colors, DBeaverIcons.getImageDescriptor(UIIcon.PALETTE)) {
                @Override
                public void run() {
                    PreferenceDialog preferenceDialog = PreferencesUtil.createPreferenceDialogOn(getShell(), ExtensionFactory.COLORS_AND_FONTS_PREFERENCE_PAGE, null, null);
                    if (preferenceDialog != null) {
                        preferenceDialog.open();
                    }
                    //showChartConfigDialog();
                }
            });
        }
    }

    private void doZoom(@NotNull Zoomable zoomable, @NotNull Point origin, double factor) {
        final PlotRenderingInfo info = getChartRenderingInfo().getPlotInfo();
        final Point2D anchor = translateScreenToJava2D(new java.awt.Point(origin.x, origin.y));

        if (zoomable.isDomainZoomable()) {
            zoomable.zoomDomainAxes(factor, info, anchor, true);
        }

        if (zoomable.isRangeZoomable()) {
            zoomable.zoomRangeAxes(factor, info, anchor, true);
        }
    }

    protected void doCopyToClipboard() {
        Image image = new Image(Display.getDefault(), this.getBounds());
        try {
            GC gc = new GC(image);
            try {
                this.print(gc);
            } finally {
                gc.dispose();
            }

            ImageTransfer imageTransfer = ImageTransfer.getInstance();
            Clipboard clipboard = new Clipboard(Display.getCurrent());
            clipboard.setContents(new Object[] {image.getImageData()}, new Transfer[]{imageTransfer});
        } finally {
            image.dispose();
        }
    }

    protected boolean hasConfigurationDialog() {
        return true;
    }

    protected boolean showChartConfigDialog() {
        return false;
    }

    protected boolean hasColorsConfiguration() {
        return false;
    }

}
