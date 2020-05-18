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
package org.jkiss.dbeaver.ext.erd.export;

import org.eclipse.draw2d.FreeformLayeredPane;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ImageUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.File;
import java.io.FileOutputStream;

public class ERDExportRasterImage implements ERDExportFormatHandler
{

    private static final int MARGIN_X = 10;
    private static final int MARGIN_Y = 10;

    @Override
    public void exportDiagram(EntityDiagram diagram, IFigure figure, DiagramPart diagramPart, File targetFile) throws DBException
    {
        int imageType = SWT.IMAGE_BMP;
        {
            String filePath = targetFile.getName().toLowerCase();
            if (filePath.endsWith(".jpg")) {
                imageType = SWT.IMAGE_JPEG;
            } else if (filePath.endsWith(".png")) {
                imageType = SWT.IMAGE_PNG;
            } else if (filePath.endsWith(".gif")) {
                imageType = SWT.IMAGE_GIF;
            }
        }

        Rectangle contentBounds = figure instanceof FreeformLayeredPane ? ((FreeformLayeredPane) figure).getFreeformExtent() : figure.getBounds();
        try {
            if (contentBounds.isEmpty()) {
                throw new DBException("Can't serializeDiagram empty diagram");
            }
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                Rectangle r = figure.getBounds();
                GC gc = null;
                Graphics graphics = null;
                try {
                    Image image = new Image(null, contentBounds.width + MARGIN_X * 4, contentBounds.height + MARGIN_Y * 4);
                    try {
                        gc = new GC(image);
                        //gc.setClipping(0, 0, contentBounds.width + MARGIN_X * 2, contentBounds.height + MARGIN_Y * 2);
                        graphics = new SWTGraphics(gc);
                        graphics.translate(r.x * -1 + MARGIN_X, r.y * -1 + MARGIN_Y);
                        figure.paint(graphics);
                        ImageLoader imageLoader = new ImageLoader();
                        imageLoader.data = new ImageData[1];
                        if (imageType == SWT.IMAGE_GIF) {
                            // Convert to 8bit color
                            imageLoader.data[0] = ImageUtils.makeWebImageData(image);
                        } else {
                            // Use maximum colors for JPEG, PNG
                            imageLoader.data[0] = ImageUtils.getImageDataAtCurrentZoom(image);
                        }
                        imageLoader.save(fos, imageType);
                    } finally {
                        UIUtils.dispose(image);
                    }
                } finally {
                    if (graphics != null) {
                        graphics.dispose();
                    }
                    UIUtils.dispose(gc);
                }

                fos.flush();
            }

            UIUtils.launchProgram(targetFile.getAbsolutePath());

        } catch (Throwable e) {
            DBWorkbench.getPlatformUI().showError("Save ERD as image", null, e);
        }

    }
}
