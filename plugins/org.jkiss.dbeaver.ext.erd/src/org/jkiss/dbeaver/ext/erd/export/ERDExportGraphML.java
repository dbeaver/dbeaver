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

import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.figures.AttributeListFigure;
import org.jkiss.dbeaver.ext.erd.figures.EntityFigure;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.ext.erd.model.ERDEntityAttribute;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.AssociationPart;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * GraphML exporter
 */
public class ERDExportGraphML implements ERDExportFormatHandler
{
    private static final Log log = Log.getLog(ERDExportGraphML.class);

    @Override
    public void exportDiagram(EntityDiagram diagram, IFigure figure, DiagramPart diagramPart, File targetFile) throws DBException
    {
        try {
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                XMLBuilder xml = new XMLBuilder(fos, GeneralUtils.UTF8_ENCODING);
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

                xml.startElement("key");
                xml.addAttribute("for", "edge");
                xml.addAttribute("id", "edgegraph");
                xml.addAttribute("yfiles.type", "edgegraphics");
                xml.endElement();

                xml.startElement("graph");
                xml.addAttribute("edgedefault", "directed");
                xml.addAttribute("id", "G");

                Map<ERDEntity, String> entityMap = new HashMap<>();
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
                            EntityPart entityPart = diagramPart.getEntityPart(entity);
                            EntityFigure entityFigure = (EntityFigure) entityPart.getFigure();
                            Rectangle partBounds = entityPart.getBounds();
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
                            xml.addAttribute("color", getHtmlColor(entityFigure.getBackgroundColor()));
                            //xml.addAttribute("color2", partBounds.width);
                            xml.addAttribute("transparent", "false");
                            xml.endElement();

                            // Border
                            xml.startElement("y:BorderStyle");
                            xml.addAttribute("color", getHtmlColor(entityFigure.getForegroundColor()));
                            xml.addAttribute("type", "line");
                            xml.addAttribute("width", "1.0");
                            xml.endElement();

                            {
                                // Entity Name
                                Rectangle nameBounds = entityFigure.getNameLabel().getBounds();

                                xml.startElement("y:NodeLabel");
                                xml.addAttribute("alignment", "center");
                                xml.addAttribute("autoSizePolicy", "content");
                                xml.addAttribute("configuration", "com.yworks.entityRelationship.label.name");
                                xml.addAttribute("fontFamily", "Courier");
                                xml.addAttribute("fontSize", "12");
                                xml.addAttribute("fontStyle", "plain");
                                xml.addAttribute("hasLineColor", "false");
                                xml.addAttribute("modelName", "internal");
                                xml.addAttribute("modelPosition", "t");
                                xml.addAttribute("backgroundColor", getHtmlColor(entityFigure.getNameLabel().getBackgroundColor()));
                                xml.addAttribute("textColor", getHtmlColor(entityFigure.getNameLabel().getForegroundColor()));
                                xml.addAttribute("visible", "true");

                                xml.addAttribute("height", nameBounds.height());
                                xml.addAttribute("width", nameBounds.width);
                                xml.addAttribute("x", nameBounds.x());
                                xml.addAttribute("y", nameBounds.y());

                                xml.addText(entity.getName());

                                xml.endElement();
                            }

                            {
                                // Attributes
                                AttributeListFigure columnsFigure = entityFigure.getColumnsFigure();
                                Rectangle attrsBounds = columnsFigure.getBounds();

                                xml.startElement("y:NodeLabel");
                                xml.addAttribute("alignment", "left");
                                xml.addAttribute("autoSizePolicy", "content");
                                xml.addAttribute("configuration", "com.yworks.entityRelationship.label.attributes");
                                xml.addAttribute("fontFamily", "Courier");
                                xml.addAttribute("fontSize", "12");
                                xml.addAttribute("fontStyle", "plain");
                                xml.addAttribute("hasLineColor", "false");
                                xml.addAttribute("modelName", "custom");
                                xml.addAttribute("modelPosition", "t");
                                xml.addAttribute("backgroundColor", getHtmlColor(columnsFigure.getBackgroundColor()));
                                xml.addAttribute("textColor", getHtmlColor(columnsFigure.getForegroundColor()));
                                xml.addAttribute("visible", "true");

                                xml.addAttribute("height", attrsBounds.height());
                                xml.addAttribute("width", attrsBounds.width);
                                xml.addAttribute("x", attrsBounds.x());
                                xml.addAttribute("y", attrsBounds.y());

                                StringBuilder attrsString = new StringBuilder();
                                for (ERDEntityAttribute attr : entity.getColumns()) {
                                    if (attrsString.length() > 0) {
                                        attrsString.append("\n");
                                    }
                                    attrsString.append(attr.getName());
                                }

                                xml.addText(attrsString.toString());

                                xml.startElement("y:LabelModel");
                                xml.startElement("y:ErdAttributesNodeLabelModel");
                                xml.endElement();
                                xml.endElement();

                                xml.startElement("y:ModelParameter");
                                xml.startElement("y:ErdAttributesNodeLabelModelParameter");
                                xml.endElement();
                                xml.endElement();

                                xml.endElement();
                            }

                            xml.endElement();
                        }

                        xml.endElement();
                    }

                    xml.endElement();
                }

                int edgeNum = 0;
                for (ERDEntity entity : diagram.getEntities()) {
                    EntityPart entityPart = diagramPart.getEntityPart(entity);
                    for (ERDAssociation association : entity.getForeignKeyRelationships()) {
                        AssociationPart associationPart = entityPart.getConnectionPart(association, true);
                        if (associationPart == null) {
                            log.debug("Association part not found");
                            continue;
                        }

                        edgeNum++;
                        String edgeId = "e" + edgeNum;

                        xml.startElement("edge");
                        xml.addAttribute("id", edgeId);
                        xml.addAttribute("source", entityMap.get(entity));
                        xml.addAttribute("target", entityMap.get(association.getPrimaryKeyEntity()));

                        xml.startElement("data");
                        xml.addAttribute("key", "edgegraph");
                        xml.startElement("y:PolyLineEdge");
                        xml.startElement("y:Path");// sx="0.0" sy="0.0" tx="0.0" ty="0.0"/>
                        for (Bendpoint bp : associationPart.getBendpoints()) {
                            xml.startElement("y:Point");
                            xml.addAttribute("x", bp.getLocation().x);
                            xml.addAttribute("y", bp.getLocation().x);
                            xml.endElement();
                        }
                        xml.endElement();
                        xml.startElement("y:LineStyle");
                        xml.addAttribute("color", "#000000");
                        xml.addAttribute("type", !association.isIdentifying() || association.isLogical() ? "dashed" : "line");
                        xml.addAttribute("width", "1.0");
                        xml.endElement();
                        xml.startElement("y:Arrows");
                        String sourceStyle = !association.isIdentifying() ? "white_diamond" : "none";
                        xml.addAttribute("source", sourceStyle);
                        xml.addAttribute("target", "circle");
                        xml.endElement();
                        xml.startElement("y:BendStyle");
                        xml.addAttribute("smoothed", "false");
                        xml.endElement();

                        xml.endElement();
                        xml.endElement();

                        xml.endElement();
                    }
                }

                xml.endElement();

                xml.endElement();

                xml.flush();
                fos.flush();
            }
            UIUtils.launchProgram(targetFile.getAbsolutePath());
        } catch (Exception e) {
            DBUserInterface.getInstance().showError("Save ERD as GraphML", null, e);
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
