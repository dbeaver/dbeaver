/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.ImageUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.File;
import java.io.FileOutputStream;

public class ERDExportRasterImage implements ERDExportFormatHandler
{

    @Override
    public void exportDiagram(EntityDiagram diagram, IFigure figure, DiagramPart diagramPart, File targetFile) throws DBException
    {
        String filePath = targetFile.getAbsolutePath().toLowerCase();
        int imageType = SWT.IMAGE_BMP;
        if (filePath.endsWith(".jpg")) {
            imageType = SWT.IMAGE_JPEG;
        } else if (filePath.endsWith(".png")) {
            imageType = SWT.IMAGE_PNG;
        } else if (filePath.endsWith(".gif")) {
            imageType = SWT.IMAGE_GIF;
        }

        Rectangle contentBounds = figure instanceof FreeformLayeredPane ? ((FreeformLayeredPane) figure).getFreeformExtent() : figure.getBounds();
        try {
            if (contentBounds.isEmpty()) {
                throw new DBException("Can't save empty diagram");
            }
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                Rectangle r = figure.getBounds();
                GC gc = null;
                Graphics g = null;
                try {
                    Image image = new Image(null, contentBounds.x * 2 + contentBounds.width, contentBounds.y * 2 + contentBounds.height);
                    try {
                        gc = new GC(image);
                        gc.setClipping(contentBounds.x, contentBounds.y, contentBounds.width, contentBounds.height);
                        g = new SWTGraphics(gc);
                        g.translate(r.x * -1, r.y * -1);
                        figure.paint(g);
                        ImageLoader imageLoader = new ImageLoader();
                        imageLoader.data = new ImageData[1];
                        if (imageType != SWT.IMAGE_JPEG) {
                            // Convert to 8bit color
                            imageLoader.data[0] = ImageUtils.makeWebImageData(image);
                        } else {
                            // Use maximum colors for JPEG
                            imageLoader.data[0] = image.getImageData();
                        }
                        imageLoader.save(fos, imageType);
                    } finally {
                        UIUtils.dispose(image);
                    }
                } finally {
                    if (g != null) {
                        g.dispose();
                    }
                    UIUtils.dispose(gc);
                }

                fos.flush();
            }

            UIUtils.launchProgram(filePath);

        } catch (Throwable e) {
            DBUserInterface.getInstance().showError("Save ERD as image", null, e);
        }

    }
}
