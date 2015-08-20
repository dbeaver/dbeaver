/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * GraphML exporter
 */
public class ERDExportGraphML
{
    static final Log log = Log.getLog(ERDExportGraphML.class);

    private final EntityDiagram diagram;
    private final DiagramPart diagramPart;

    public ERDExportGraphML(EntityDiagram diagram, DiagramPart diagramPart) {
        this.diagram = diagram;
        this.diagramPart = diagramPart;
    }

    void exportDiagramToGraphML(String filePath) {
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            try {
                XMLBuilder xml = new XMLBuilder(fos, "utf-8");
                xml.setButify(true);

                xml.startElement("graphml");
                xml.addAttribute("xmlns", "http://graphml.graphdrawing.org/xmlns");
                xml.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                xml.addAttribute("xsi:schemaLocation", "http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd");
                xml.addAttribute("xmlns:y", "http://www.yworks.com/xml/graphml");

                xml.startElement("key");
                xml.addAttribute("for", "node");
                xml.addAttribute("id", "nodegraph");
                xml.addAttribute("yfiles.type", "nodegraphics");
                xml.endElement();

                xml.startElement("graph");
                xml.addAttribute("edgedefault", "directed");
                xml.addAttribute("id", "G");

                Map<ERDEntity, String> entityMap = new HashMap<ERDEntity, String>();
                int nodeNum = 0;
                for (ERDEntity entity : diagram.getEntities()) {
                    nodeNum++;
                    String nodeId = "n" + nodeNum;
                    entityMap.put(entity, nodeId);

                    // node
                    xml.startElement("node");
                    xml.addAttribute("id", nodeId);
                    {
                        // Graph data
                        xml.startElement("data");
                        xml.addAttribute("key", "nodegraph");
                        {
                            // Generic node
                            EntityPart part = diagramPart.getEntityPart(entity);
                            Rectangle partBounds = part.getBounds();
                            xml.startElement("y:GenericNode");
                            xml.addAttribute("configuration", "com.yworks.entityRelationship.big_entity");

                            // Geometry
                            xml.startElement("y:Geometry");
                            xml.addAttribute("height", partBounds.height());
                            xml.addAttribute("width", partBounds.width);
                            xml.addAttribute("x", partBounds.x());
                            xml.addAttribute("y", partBounds.y());
                            xml.endElement();

                            // Fill
                            xml.startElement("y:Fill");
                            xml.addAttribute("color", getHtmlColor(part.getContentPane().getBackgroundColor()));
                            //xml.addAttribute("color2", partBounds.width);
                            xml.addAttribute("transparent", "false");
                            xml.endElement();

                            // Border
                            xml.startElement("y:BorderStyle");
                            xml.addAttribute("color", getHtmlColor(part.getContentPane().getForegroundColor()));
                            xml.addAttribute("type", "line");
                            xml.addAttribute("width", "1.0");
                            xml.endElement();

                            // Entity Name
                            //<y:NodeLabel alignment="center" autoSizePolicy="content" backgroundColor="#B7C9E3" configuration="com.yworks.entityRelationship.label.name" fontFamily="Dialog" fontSize="12" fontStyle="plain" hasLineColor="false" height="18.701171875" modelName="internal" modelPosition="t" textColor="#000000" visible="true" width="40.685546875" x="19.6572265625" y="4.0">Entity1</y:NodeLabel>

                            xml.endElement();
                        }
                        xml.endElement();
                    }

                    xml.endElement();
                }

                int edgeNum = 0;
                for (ERDEntity entity : diagram.getEntities()) {
                    for (ERDAssociation association : entity.getForeignKeyRelationships()) {
                        edgeNum++;
                        String edgeId = "e" + edgeNum;

                        xml.startElement("edge");
                        xml.addAttribute("id", edgeId);
                        xml.addAttribute("source", entityMap.get(entity));
                        xml.addAttribute("target", entityMap.get(association.getPrimaryKeyEntity()));
                        xml.endElement();
                    }
                }

                xml.endElement();

                xml.endElement();

                xml.flush();
                fos.flush();
            } finally {
                ContentUtils.close(fos);
            }
            RuntimeUtils.launchProgram(filePath);
        } catch (Exception e) {
            UIUtils.showErrorDialog(null, "Save ERD as GraphML", null, e);
        }
    }

    private String getHtmlColor(Color color) {
        return "#" + getHexColor(color.getRed()) + getHexColor(color.getGreen()) + getHexColor(color.getBlue());
    }

    private String getHexColor(int value) {
        String hex = Integer.toHexString(value).toUpperCase();
        return hex.length() < 2 ? "0" + hex : hex;
    }


}
