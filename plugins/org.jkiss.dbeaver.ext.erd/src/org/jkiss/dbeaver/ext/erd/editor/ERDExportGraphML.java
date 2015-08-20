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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
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

    public ERDExportGraphML(EntityDiagram diagram) {
        this.diagram = diagram;
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

                xml.startElement("graph");
                xml.addAttribute("edgedefault", "directed");
                xml.addAttribute("id", "G");

                Map<ERDEntity, String> entityMap = new HashMap<ERDEntity, String>();
                int nodeNum = 0;
                for (ERDEntity entity : diagram.getEntities()) {
                    nodeNum++;
                    String nodeId = "n" + nodeNum;
                    entityMap.put(entity, nodeId);

                    xml.startElement("node");
                    xml.addAttribute("id", nodeId);
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


}
