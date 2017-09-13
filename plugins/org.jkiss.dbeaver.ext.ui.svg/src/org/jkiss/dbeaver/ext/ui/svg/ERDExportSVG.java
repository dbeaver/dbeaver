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
package org.jkiss.dbeaver.ext.ui.svg;

import org.apache.batik.ext.awt.image.codec.png.PNGImageWriter;
import org.apache.batik.ext.awt.image.spi.ImageWriterRegistry;
import org.apache.batik.svggen.SVGGraphics2D;
import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.editparts.LayerManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.export.ERDExportFormatHandler;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;

import java.io.File;

/**
 * SVG exporter
 */
public class ERDExportSVG implements ERDExportFormatHandler {
    private static final Log log = Log.getLog(ERDExportSVG.class);

    static {
        // For some reason image writers aren't registered in Batik registry automatically
        // Probably because of cut dependencies (which are fucking huge for Batic codec)
        ImageWriterRegistry.getInstance().register(new PNGImageWriter());
    }

    @Override
    public void exportDiagram(EntityDiagram diagram, IFigure diagramFigure, DiagramPart diagramPart, File targetFile) throws DBException {
        try {
            IFigure figure = diagramPart.getFigure();
            Rectangle contentBounds = figure instanceof FreeformLayeredPane ? ((FreeformLayeredPane) figure).getFreeformExtent() : figure.getBounds();

            String svgNS = "http://www.w3.org/2000/svg";
            Document document = XMLUtils.createDocument();//domImpl.createDocument(svgNS, "svg", null);
            document.createAttributeNS(svgNS, "svg");
            SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

            // We need a converter from Draw2D.Graphics (GEF) to awt.Graphics2D (Batik)
            Graphics graphics = new GraphicsToGraphics2DAdaptor(svgGenerator);

            // Reset origin to make it the top/left most part of the diagram
            graphics.translate(contentBounds.x * -1, contentBounds.y * -1);
            paintDiagram(graphics, figure);

            LayerManager layerManager = (LayerManager) diagramPart.getViewer().getEditPartRegistry().get(LayerManager.ID);
            IFigure connectionLayer = layerManager.getLayer("Connection Layer");
            if (connectionLayer != null) {
                paintDiagram(graphics, connectionLayer);
            }

            String filePath = targetFile.getAbsolutePath();

            svgGenerator.stream(filePath);

            UIUtils.launchProgram(filePath);
        } catch (Exception e) {
            DBUserInterface.getInstance().showError("Save ERD as SVG", null, e);
        }
    }


    /**
     * Paints the figure onto the given graphics
     */
    public static void paintDiagram(Graphics g, IFigure figure) {
        // Store state, so modified state of Graphics (while painting children) can be easily restored
        g.pushState();
        try {
            IClippingStrategy clippingStrategy = figure.getClippingStrategy();

            // Iterate over the children to check whether a child is a(nother) layer or an actual figure
            // Not painting the layers themselves is likely to get rid of borders and graphics settings that are not
            // supported (like Graphics#setTextAntiAliassing())
            for (Object childObject : figure.getChildren()) {
                if (childObject instanceof Layer) {
                    // Found another layer, process it to search for actual figures
                    paintDiagram(g, (IFigure) childObject);
                } else {
                    // Found something to draw
                    // Use same/similar method as being using in Figure#paintChildren() in order to get clipping right
                    IFigure child = (IFigure) childObject;
                    if (child.isVisible()) {
                        // determine clipping areas for child
                        Rectangle[] clipping = null;
                        if (clippingStrategy != null) {
                            clipping = clippingStrategy.getClip(child);
                        } else {
                            // default clipping behaviour is to clip at bounds
                            clipping = new Rectangle[]{child.getBounds()};
                        }
                        // child may now paint inside the clipping areas
                        for (int j = 0; j < clipping.length; j++) {
                            if (clipping[j].intersects(g.getClip(Rectangle.SINGLETON))) {
                                g.clipRect(clipping[j]);
                                child.paint(g);
                                g.restoreState();
                            }
                        }
                    }
                }
            }

            for (Object childObject : figure.getChildren()) {
                if (childObject instanceof Layer) {
                    // Found another layer, process it to search for actual figures
                    paintDiagram(g, (IFigure) childObject);
                } else {
                    // Found something to draw
                    // Use same/similar method as being using in Figure#paintChildren() in order to get clipping right
                    IFigure child = (IFigure) childObject;
                    if (child.isVisible()) {
                        // determine clipping areas for child
                        Rectangle[] clipping = null;
                        if (clippingStrategy != null) {
                            clipping = clippingStrategy.getClip(child);
                        } else {
                            // default clipping behaviour is to clip at bounds
                            clipping = new Rectangle[]{child.getBounds()};
                        }
                        // child may now paint inside the clipping areas
                        for (int j = 0; j < clipping.length; j++) {
                            if (clipping[j].intersects(g.getClip(Rectangle.SINGLETON))) {
                                g.clipRect(clipping[j]);
                                child.paint(g);
                                g.restoreState();
                            }
                        }
                    }
                }
            }

        } finally {
            // Always pop the state again to prevent problems
            g.popState();
        }
    }

}
