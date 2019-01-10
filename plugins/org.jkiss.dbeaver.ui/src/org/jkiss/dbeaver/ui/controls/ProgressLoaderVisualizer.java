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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.ProxyProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * PairListControl
 */
public class ProgressLoaderVisualizer<RESULT> implements ILoadVisualizer<RESULT>
{
    private static final Log log = Log.getLog(ProgressLoaderVisualizer.class);

    protected static final int PROGRESS_VISUALIZE_PERIOD = 100;

    static final DBIcon[] PROGRESS_IMAGES = {
        UIIcon.PROGRESS0, UIIcon.PROGRESS1, UIIcon.PROGRESS2, UIIcon.PROGRESS3,
        UIIcon.PROGRESS4, UIIcon.PROGRESS5, UIIcon.PROGRESS6, UIIcon.PROGRESS7,
        UIIcon.PROGRESS8, UIIcon.PROGRESS9
    };

    private final ILoadService<RESULT> loadService;
    private final Composite progressPane;
    private volatile boolean finished = false;
    private ControlEditor progressOverlay;
    private volatile int drawCount = 0;
    private Button cancelButton;
    private PaintListener painListener;
    private Color shadowColor;
    private String progressMessage;
    private long loadStartTime;

    public ProgressLoaderVisualizer(ILoadService<RESULT> loadService, Composite progressPane) {
        this.loadService = loadService;
        this.progressPane = progressPane;
        this.progressMessage = "Initializing";
    }

    @Override
    public DBRProgressMonitor overwriteMonitor(DBRProgressMonitor monitor) {
        DBRProgressMonitor progressMonitor = new ProxyProgressMonitor(monitor) {
            @Override
            public void subTask(String name) {
                if (loadStartTime == 0) {
                    loadStartTime = System.currentTimeMillis();
                }
                progressMessage = name;
                super.subTask(name);
            }
        };
        return progressMonitor;
    }

    @Override
    public boolean isCompleted() {
        return finished;
    }

    @Override
    public void completeLoading(RESULT result) {
        this.finished = true;
    }

    @Override
    public void visualizeLoading() {
        if (!progressPane.isDisposed()) {
            if (shadowColor == null) {
                shadowColor = progressPane.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
            }
            if (!finished) {
                try {
                    showProgress();
                } catch (Exception e) {
                    log.error("Internal error during progress visualization", e);
                    // Something went terribly wrong
                    // We shouldn't be here ever. In any case we must finish the job
                    finishProgress();
                }
            }
            if (finished) {
                finishProgress();
            }
        }
    }

    private void showProgress() {
        if (loadStartTime == 0) {
            return;
        }
        if (progressOverlay == null) {
            // Start progress visualization
            cancelButton = new Button(progressPane, SWT.PUSH);
            cancelButton.setText("Cancel");
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.verticalIndent = DBeaverIcons.getImage(UIIcon.PROGRESS0).getBounds().height * 2;
            cancelButton.setLayoutData(gd);
            cancelButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    cancelButton.setText("Canceled");
                    cancelButton.setEnabled(false);
                    Point buttonSize = cancelButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                    progressOverlay.minimumWidth = buttonSize.x;
                    progressOverlay.minimumHeight = buttonSize.y;
                    progressOverlay.layout();
                    try {
                        loadService.cancel();
                    } catch (InvocationTargetException e1) {
                        log.error(e1.getTargetException());
                    }
                }
            });

            painListener = e -> {
                if (cancelButton.isDisposed()) {
                    return;
                }
                Image image = DBeaverIcons.getImage(PROGRESS_IMAGES[drawCount % PROGRESS_IMAGES.length]);
                Rectangle buttonBounds = cancelButton.getBounds();
                Rectangle imageBounds = image.getBounds();
                e.gc.drawImage(
                    image,
                    (buttonBounds.x + buttonBounds.width / 2) - imageBounds.width / 2,
                    buttonBounds.y - imageBounds.height - 5);

                long elapsedTime = System.currentTimeMillis() - loadStartTime;
                String elapsedString = elapsedTime > 10000 ?
                    String.valueOf(elapsedTime / 1000) :
                    String.valueOf(((double) (elapsedTime / 100)) / 10);
                String statusMessage = CommonUtils.truncateString(
                    progressMessage.replaceAll("\\s", " "), 64);
                String status = statusMessage + " - " + elapsedString + "s";
                Point statusSize = e.gc.textExtent(status);

                int statusX = (buttonBounds.x + buttonBounds.width / 2) - statusSize.x / 2;
                int statusY = buttonBounds.y - imageBounds.height - 10 - statusSize.y;
                e.gc.setForeground(progressPane.getForeground());
                e.gc.setBackground(progressPane.getBackground());
                e.gc.fillRectangle(statusX - 2, statusY - 2, statusSize.x + 4, statusSize.y + 4);
                e.gc.drawText(status, statusX, statusY, true);
                e.gc.setForeground(shadowColor);
                e.gc.drawRoundRectangle(statusX - 3, statusY - 3, statusSize.x + 5, statusSize.y + 5, 5, 5);
            };
            progressPane.addPaintListener(painListener);

            progressOverlay = new ControlEditor(progressPane);
            Point buttonSize = cancelButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            progressOverlay.minimumWidth = buttonSize.x;
            progressOverlay.minimumHeight = buttonSize.y;
            progressOverlay.setEditor(cancelButton);
        }
        drawCount++;
        if (progressOverlay != null) {
            progressOverlay.layout();
            progressPane.redraw();
        }
    }

    private void finishProgress() {
        // Last update - remove progress visualization
        if (progressOverlay != null) {
            if (!progressPane.isDisposed()) {
                progressPane.removePaintListener(painListener);
                progressOverlay.dispose();
            }
            progressOverlay = null;
            if (!cancelButton.isDisposed()) {
                cancelButton.dispose();
            }
            progressPane.redraw();
        }
    }

}