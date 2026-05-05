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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;

/**
 * ProgressPainter
 */
public class ProgressPainter {

    private final Control control;
    private PaintListener loadingListener;

    public ProgressPainter(Control control) {
        this.control = control;
        loadingListener = new PaintListener() {
            int drawCount = 0;

            @Override
            public void paintControl(PaintEvent e) {
                if (loadingListener == null) {
                    return;
                }
                drawCount++;
                Image image = DBeaverIcons.getImage(ProgressLoaderVisualizer.PROGRESS_IMAGES[drawCount % ProgressLoaderVisualizer.PROGRESS_IMAGES.length]);
                Rectangle bounds = control.getBounds();
                Rectangle imageBounds = image.getBounds();
                e.gc.drawImage(
                    image,
                    (bounds.x + bounds.width / 2) - imageBounds.width / 2,
                    (bounds.y + bounds.height / 2) - imageBounds.height - 5);
                new UIJob("Repaint") {
                    {
                        setSystem(true);
                    }
                    @Override
                    public IStatus runInUIThread(IProgressMonitor monitor) {
                        if (!control.isDisposed()) {
                            control.redraw();
                        }
                        return Status.OK_STATUS;
                    }
                }.schedule(200);
            }
        };
        control.addPaintListener(loadingListener);
    }

    public void close() {
        if (loadingListener != null) {
            if (!control.isDisposed()) {
                control.removePaintListener(loadingListener);
            }
            loadingListener = null;
        }
    }

}