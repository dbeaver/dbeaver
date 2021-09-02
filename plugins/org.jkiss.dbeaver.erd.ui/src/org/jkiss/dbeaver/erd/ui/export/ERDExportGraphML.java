/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.export;

import org.eclipse.draw2dl.Bendpoint;
import org.eclipse.draw2dl.IFigure;
import org.eclipse.draw2dl.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.*;
import org.jkiss.dbeaver.erd.ui.ERDUIUtils;
import org.jkiss.dbeaver.erd.ui.figures.AttributeListFigure;
import org.jkiss.dbeaver.erd.ui.figures.EntityFigure;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.erd.ui.part.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.Pair;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * GraphML exporter
 */
public class ERDExportGraphML implements ERDExportFormatHandler
{
    private static final Log log = Log.getLog(ERDExportGraphML.class);
    private static final int fontSize = 12;

    @Override
    public void exportDiagram(EntityDiagram diagram, IFigure figure, DiagramPart diagramPart, File targetFile) {
        try {
            try (final OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(targetFile), GeneralUtils.UTF8_CHARSET)) {
                XMLBuilder xml = new XMLBuilder(osw, GeneralUtils.UTF8_ENCODING);
                xml.setButify(true);

                xml.startElement("graphml");
                xml.addAttribute("xmlns", "http://graphml.graphdrawing.org/xmlns");
                xml.addAttribute("xmlns:java", "http://www.yworks.com/xml/yfiles-common/1.0/java");
                xml.addAttribute("xmlns:sys", "http://www.yworks.com/xml/yfiles-common/markup/primitives/2.0");
                xml.addAttribute("xmlns:x", "http://www.yworks.com/xml/yfiles-common/markup/2.0");
                xml.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                xml.addAttribute("xmlns:y", "http://www.yworks.com/xml/graphml");
                xml.addAttribute("xmlns:yed", "http://www.yworks.com/xml/yed/3");
                xml.addAttribute("xsi:schemaLocation", "http://graphml.graphdrawing.org/xmlns http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd");

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

                // A list of diagram elements sorted according to their z-order
                final List<ERDElement<?>> elements = Stream
                    .concat(
                        diagram.getEntities().stream().map(x -> new Pair<>(x, diagram.getVisualInfo(x.getObject()))),
                        diagram.getNotes().stream().map(x -> new Pair<>(x, diagram.getVisualInfo(x))))
                    .sorted(Comparator.comparing(x -> x.getSecond() == null ? 0 : x.getSecond().zOrder))
                    .map(Pair::getFirst)
                    .collect(Collectors.toList());

                final Map<ERDElement<?>, String> associations = new HashMap<>();

                // Export elements and collect their associations
                for (int index = 0; index < elements.size(); index++) {
                    final ERDElement<?> element = elements.get(index);
                    if (element instanceof ERDEntity) {
                        exportEntity(index, (ERDEntity) element, diagramPart.getEntityPart((ERDEntity) element), diagram, associations, xml);
                    } else if (element instanceof ERDNote) {
                        exportNote(index, (ERDNote) element, diagramPart.getNotePart((ERDNote) element), associations, xml);
                    } else {
                        log.debug("Unsupported ERD element: " + element);
                    }
                }

                // Export edges using collected associations
                for (int index = 0; index < elements.size(); index++) {
                    final ERDElement<?> element = elements.get(index);
                    if (element instanceof ERDEntity) {
                        exportEdge(index, element, diagramPart.getEntityPart((ERDEntity) element), associations, xml);
                    } else if (element instanceof ERDNote) {
                        exportEdge(index, element, diagramPart.getNotePart((ERDNote) element), associations, xml);
                    } else {
                        log.debug("Unsupported ERD element: " + element);
                    }
                }

                xml.endElement();

                xml.endElement();

                xml.flush();
                osw.flush();
            }
            ShellUtils.launchProgram(targetFile.getAbsolutePath());
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Save ERD as GraphML", null, e);
        }
    }

    private static void exportEntity(int index, ERDEntity entity, EntityPart entityPart, EntityDiagram diagram, Map<ERDElement<?>, String> associations, XMLBuilder xml) throws IOException {
        final String entityId = "entity" + index;
        final EntityFigure entityFigure = entityPart.getFigure();
        final Rectangle partBounds = entityPart.getBounds();

        associations.put(entity, entityId);

        // node
        xml.startElement("node");
        xml.addAttribute("id", entityId);
        {
            // Graph data
            xml.startElement("data");
            xml.addAttribute("key", "nodegraph");

            {
                // Generic node
                xml.startElement("y:GenericNode");
                xml.addAttribute("configuration", "com.yworks.entityRelationship.big_entity");

                // Geometry
                xml.startElement("y:Geometry");
                xml.addAttribute("height", partBounds.height);
                xml.addAttribute("width", partBounds.width + getExtraTableLength(diagram, entity));
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
                    xml.addAttribute("fontSize", fontSize);
                    xml.addAttribute("fontStyle", "plain");
                    xml.addAttribute("hasLineColor", "false");
                    xml.addAttribute("modelName", "internal");
                    xml.addAttribute("modelPosition", "t");
                    xml.addAttribute("backgroundColor", getHtmlColor(entityFigure.getNameLabel().getBackgroundColor()));
                    xml.addAttribute("textColor", "#FFFFFF");
                    xml.addAttribute("visible", "true");
                    xml.addAttribute("horizontalTextPosition", "center");
                    xml.addAttribute("iconTextGap", "4");
                    xml.addAttribute("height", nameBounds.height);
                    xml.addAttribute("width", nameBounds.width);
                    xml.addAttribute("x", 0);
                    xml.addAttribute("y", 4);

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
                    xml.addAttribute("fontSize", fontSize);
                    xml.addAttribute("fontStyle", "plain");
                    xml.addAttribute("hasLineColor", "false");
                    xml.addAttribute("modelName", "custom");
                    xml.addAttribute("modelPosition", "t");
                    xml.addAttribute("backgroundColor", getHtmlColor(columnsFigure.getBackgroundColor()));
                    xml.addAttribute("textColor", getHtmlColor(columnsFigure.getForegroundColor()));
                    xml.addAttribute("visible", "true");
                    xml.addAttribute("horizontalTextPosition", "center");
                    xml.addAttribute("iconTextGap", "4");

                    xml.addAttribute("height", attrsBounds.height);
                    xml.addAttribute("width", attrsBounds.width);
                    xml.addAttribute("x", 2); //numbers from yEd Graph Editor
                    xml.addAttribute("y", 31.66796875);

                    StringBuilder attrsString = new StringBuilder();
                    for (ERDEntityAttribute attr : entity.getAttributes()) {
                        if (attrsString.length() > 0) {
                            attrsString.append("\n");
                        }
                        attrsString.append(ERDUIUtils.getFullAttributeLabel(diagram, attr, true));
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

    private static void exportNote(int index, ERDNote note, NotePart notePart, Map<ERDElement<?>, String> associations, XMLBuilder xml) throws IOException {
        final String noteId = "note" + index;
        final IFigure noteFigure = notePart.getFigure();
        final Rectangle noteBounds = notePart.getBounds();

        associations.put(note, noteId);

        xml.startElement("node");
        {
            xml.addAttribute("id", noteId);

            xml.startElement("data");
            {
                xml.addAttribute("key", "nodegraph");

                xml.startElement("y:ShapeNode");
                {
                    xml.startElement("y:Geometry");
                    {
                        xml.addAttribute("height", noteBounds.height);
                        xml.addAttribute("width", noteBounds.width);
                        xml.addAttribute("x", noteBounds.x());
                        xml.addAttribute("y", noteBounds.y());
                    }
                    xml.endElement();

                    xml.startElement("y:Fill");
                    {
                        xml.addAttribute("color", getHtmlColor(noteFigure.getBackgroundColor()));
                        xml.addAttribute("transparent", "false");
                    }
                    xml.endElement();

                    xml.startElement("y:BorderStyle");
                    {
                        xml.addAttribute("color", getHtmlColor(noteFigure.getForegroundColor()));
                        xml.addAttribute("type", "line");
                        xml.addAttribute("width", "1.0");
                    }
                    xml.endElement();

                    xml.startElement("y:NodeLabel");
                    {
                        xml.addAttribute("alignment", "left");
                        xml.addAttribute("autoSizePolicy", "content");
                        xml.addAttribute("fontFamily", "Courier");
                        xml.addAttribute("fontSize", fontSize);
                        xml.addAttribute("fontStyle", "plain");
                        xml.addAttribute("hasLineColor", "false");
                        xml.addAttribute("modelName", "custom");
                        xml.addAttribute("modelPosition", "t");
                        xml.addAttribute("backgroundColor", getHtmlColor(noteFigure.getBackgroundColor()));
                        xml.addAttribute("textColor", getHtmlColor(noteFigure.getForegroundColor()));
                        xml.addAttribute("visible", "true");
                        xml.addAttribute("iconTextGap", "4");
                        xml.addAttribute("height", noteBounds.height);
                        xml.addAttribute("width", noteBounds.width);
                        xml.addAttribute("x", 2);
                        xml.addAttribute("y", 2);
                        xml.addText(note.getObject());
                    }
                    xml.endElement();
                }
                xml.endElement();
            }
            xml.endElement();
        }
        xml.endElement();
    }

    private static void exportEdge(int index, ERDElement<?> node, NodePart nodePart, Map<ERDElement<?>, String> associations, XMLBuilder xml) throws IOException {
        for (ERDAssociation association : node.getAssociations()) {
            AssociationPart associationPart = nodePart.getConnectionPart(association, true);
            if (associationPart == null) {
                log.debug("Association part not found");
                continue;
            }

            xml.startElement("edge");
            xml.addAttribute("id", "edge" + index);
            xml.addAttribute("source", associations.get(node));
            xml.addAttribute("target", associations.get(association.getTargetEntity()));

            xml.startElement("data");
            xml.addAttribute("key", "edgegraph");
            xml.startElement("y:PolyLineEdge");
            xml.startElement("y:Path"); // sx="0.0" sy="0.0" tx="0.0" ty="0.0"/>
            xml.addAttribute("sx", 0.0);
            xml.addAttribute("sy", 0.0);
            xml.addAttribute("tx", 0.0);
            xml.addAttribute("ty", 0.0);
            for (Bendpoint bp : associationPart.getBendpoints()) {
                xml.startElement("y:Point");
                xml.addAttribute("x", bp.getLocation().x);
                xml.addAttribute("y", bp.getLocation().y);
                xml.endElement();
            }
            xml.endElement();

            boolean identifying = ERDUtils.isIdentifyingAssociation(association);
            xml.startElement("y:LineStyle");
            xml.addAttribute("color", "#000000");
            xml.addAttribute("type", !identifying || association.isLogical() ? "dashed" : "line");
            xml.addAttribute("width", "1.0");
            xml.endElement();
            xml.startElement("y:Arrows");
            String sourceStyle = !identifying ? "white_diamond" : "none";
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

    private static double getExtraTableLength(EntityDiagram diagram, ERDEntity entity) {
        int maxLength = 0;
        for (ERDEntityAttribute attr : entity.getAttributes()) {
            int attributeLength = (ERDUIUtils.getFullAttributeLabel(diagram, attr, true)).length();
            if (attributeLength > maxLength) {
                maxLength = attributeLength;
            }
        }
        if (entity.getName().length() > maxLength){
            maxLength = entity.getName().length();
        }
        if (maxLength < 18) { // basic table size is enough
            maxLength = 0;
        }
        return (maxLength * (fontSize * 0.12));
    }

    private static String getHtmlColor(Color color) {
        return "#" + getHexColor(color.getRed()) + getHexColor(color.getGreen()) + getHexColor(color.getBlue());
    }

    private static String getHexColor(int value) {
        String hex = Integer.toHexString(value).toUpperCase();
        return hex.length() < 2 ? "0" + hex : hex;
    }


}
