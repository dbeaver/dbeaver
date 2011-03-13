/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import net.sf.jkiss.utils.CommonUtils;
import net.sf.jkiss.utils.xml.XMLBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.jkiss.dbeaver.ext.erd.part.AssociationPart;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity diagram loader/saver
 * @author Serge Rieder
 */
public class DiagramLoader
{
    static final Log log = LogFactory.getLog(DiagramLoader.class);

    private static class TableInfo {
        final ERDTable erdTable;
        final EntityPart tablePart;
        final int objectId;

        private TableInfo(ERDTable erdTable, EntityPart tablePart, int objectId)
        {
            this.erdTable = erdTable;
            this.tablePart = tablePart;
            this.objectId = objectId;
        }
    }

    private static class DataSourceObjects {
        List<ERDTable> tables = new ArrayList<ERDTable>();
    }


    public static void load(DiagramPart diagramPart, InputStream in)
        throws IOException
    {

    }

    public static void save(DiagramPart diagramPart, OutputStream out)
        throws IOException
    {
        final EntityDiagram diagram = diagramPart == null ? null : diagramPart.getDiagram();

        // Prepare DS objects map
        Map<DBSDataSourceContainer, DataSourceObjects> dsMap = new IdentityHashMap<DBSDataSourceContainer, DataSourceObjects>();
        if (diagram != null) {
            for (ERDTable erdTable : diagram.getTables()) {
                final DBSDataSourceContainer dsContainer = erdTable.getObject().getDataSource().getContainer();
                DataSourceObjects desc = dsMap.get(dsContainer);
                if (desc == null) {
                    desc = new DataSourceObjects();
                    dsMap.put(dsContainer, desc);
                }
                desc.tables.add(erdTable);
            }
        }

        Map<ERDTable, TableInfo> infoMap = new IdentityHashMap<ERDTable, TableInfo>();

        // Save as XML
        XMLBuilder xml = new XMLBuilder(out, ContentUtils.DEFAULT_FILE_CHARSET);
        xml.setButify(true);

        xml.startElement("diagram");
        xml.addAttribute("version", 1);
        if (diagram != null) {
            xml.addAttribute("name", diagram.getName());
        }
        xml.addAttribute("time", RuntimeUtils.getCurrentTimeStamp());

        if (diagram != null) {
            xml.startElement("entities");
            for (DBSDataSourceContainer dsContainer : dsMap.keySet()) {
                xml.startElement("data-source");
                xml.addAttribute("id", dsContainer.getId());

                final DataSourceObjects desc = dsMap.get(dsContainer);
                int tableCounter = 1;
                for (ERDTable erdTable : desc.tables) {
                    final DBSTable table = erdTable.getObject();
                    EntityPart tablePart = null;
                    for (Object child : diagramPart.getChildren()) {
                        if (child instanceof EntityPart && ((EntityPart) child).getTable() == erdTable) {
                            tablePart = (EntityPart) child;
                            break;
                        }
                    }

                    if (tablePart == null) {
                        log.warn("Cannot find part for table " + table);
                        continue;
                    }
                    TableInfo info = new TableInfo(erdTable, tablePart, tableCounter++);
                    infoMap.put(erdTable, info);

                    xml.startElement("entity");
                    xml.addAttribute("id", info.objectId);
                    xml.addAttribute("name", table.getName());
                    xml.addAttribute("fq-name", table.getFullQualifiedName());
                    for (DBSObject parent = table.getParentObject(); parent != null && parent != dsContainer; parent = parent.getParentObject()) {
                        xml.startElement("path");
                        xml.addText(parent.getName());
                        xml.endElement();
                    }
                    serializeBounds(xml, tablePart.getBounds());
                    xml.endElement();
                }
                xml.endElement();
            }
            xml.endElement();

            // Relations
            xml.startElement("relations");

            for (ERDTable erdTable : diagram.getTables()) {
                for (ERDAssociation rel : erdTable.getPrimaryKeyRelationships()) {
                    xml.startElement("relation");
                    xml.addAttribute("name", rel.getObject().getName());
                    xml.addAttribute("fq-name", rel.getObject().getFullQualifiedName());
                    xml.addAttribute("type", rel.getObject().getConstraintType().getName());
                    TableInfo pkInfo = infoMap.get(rel.getPrimaryKeyTable());
                    if (pkInfo == null) {
                        log.error("Cannot find PK table '" + rel.getPrimaryKeyTable().getObject().getFullQualifiedName() + "' in info map");
                        continue;
                    }
                    TableInfo fkInfo = infoMap.get(rel.getForeignKeyTable());
                    if (fkInfo == null) {
                        log.error("Cannot find FK table '" + rel.getForeignKeyTable().getObject().getFullQualifiedName() + "' in info map");
                        continue;
                    }
                    xml.addAttribute("pk-ref", pkInfo.objectId);
                    xml.addAttribute("fk-ref", fkInfo.objectId);

                    AssociationPart associationPart = null;
                    for (Object conn : pkInfo.tablePart.getTargetConnections()) {
                        if (conn instanceof AssociationPart && ((AssociationPart) conn).getAssociation() == rel) {
                            associationPart = (AssociationPart) conn;
                            break;
                        }
                    }
                    if (associationPart != null) {
                        final List<Bendpoint> bendpoints = associationPart.getBendpoints();
                        if (!CommonUtils.isEmpty(bendpoints)) {
                            for (Bendpoint bendpoint : bendpoints) {
                                xml.startElement("bend");
                                xml.addAttribute("x", bendpoint.getLocation().x);
                                xml.addAttribute("y", bendpoint.getLocation().y);
                                xml.endElement();
                            }
                        }
                    } else {
                        log.warn("Cannot find part for relation '" + rel.getObject().getName() + "'");
                    }

                    xml.endElement();
                }
            }

            xml.endElement();

            // Notes
            xml.startElement("notes");
            xml.endElement();
        }

        xml.endElement();

        xml.flush();
    }

    private static void serializeBounds(XMLBuilder xml, Rectangle bounds) throws IOException
    {
        xml
            .startElement("bounds")
            .addAttribute("x", bounds.x)
            .addAttribute("y", bounds.y)
            .addAttribute("w", bounds.width)
            .addAttribute("h", bounds.height)
            .endElement();
    }

}