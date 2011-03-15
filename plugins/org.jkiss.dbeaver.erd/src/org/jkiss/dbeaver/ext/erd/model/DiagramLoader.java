/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import net.sf.jkiss.utils.CommonUtils;
import net.sf.jkiss.utils.xml.XMLBuilder;
import net.sf.jkiss.utils.xml.XMLException;
import net.sf.jkiss.utils.xml.XMLUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.erd.part.AssociationPart;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Entity diagram loader/saver
 * @author Serge Rieder
 */
public class DiagramLoader
{
    static final Log log = LogFactory.getLog(DiagramLoader.class);

    public static final String TAG_DIAGRAM = "diagram";
    public static final String TAG_ENTITIES = "entities";
    public static final String TAG_DATA_SOURCE = "data-source";
    public static final String TAG_ENTITY = "entity";
    public static final String TAG_PATH = "path";
    public static final String TAG_RELATIONS = "relations";
    public static final String TAG_RELATION = "relation";
    public static final String TAG_BEND = "bend";

    public static final String ATTR_VERSION = "version";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_TIME = "time";
    public static final String ATTR_ID = "id";
    public static final String ATTR_FQ_NAME = "fq-name";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_PK_REF = "pk-ref";
    public static final String ATTR_FK_REF = "fk-ref";
    public static final String ATTR_X = "x";
    public static final String ATTR_Y = "y";

    public static final int ERD_VERSION_1 = 1;

    private static class TableSaveInfo {
        final ERDTable erdTable;
        final EntityPart tablePart;
        final int objectId;

        private TableSaveInfo(ERDTable erdTable, EntityPart tablePart, int objectId)
        {
            this.erdTable = erdTable;
            this.tablePart = tablePart;
            this.objectId = objectId;
        }
    }

    private static class TableLoadInfo {
        final String objectId;
        final DBSTable table;
        final Rectangle bounds;

        private TableLoadInfo(String objectId, DBSTable table, Rectangle bounds)
        {
            this.objectId = objectId;
            this.table = table;
            this.bounds = bounds;
        }
    }

    private static class RelationLoadInfo {
        final String name;
        final String type;
        final TableLoadInfo pkTable;
        final TableLoadInfo fkTable;
        final List<Point> bends = new ArrayList<Point>();

        private RelationLoadInfo(String name, String type, TableLoadInfo pkTable, TableLoadInfo fkTable)
        {
            this.name = name;
            this.type = type;
            this.pkTable = pkTable;
            this.fkTable = fkTable;
        }
    }

    private static class DataSourceObjects {
        List<ERDTable> tables = new ArrayList<ERDTable>();
    }

    public static void load(DBRProgressMonitor monitor, IProject project, DiagramPart diagramPart, InputStream in)
        throws IOException, XMLException, DBException
    {
        final EntityDiagram diagram = diagramPart.getDiagram();

        final DataSourceRegistry dsRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
        if (dsRegistry == null) {
            throw new DBException("Cannot find datasource registry for project '" + project.getName() + "'");
        }

        final Document document = XMLUtils.parseDocument(in);

        final Element diagramElem = document.getDocumentElement();

        // Check version
        final String diagramVersion = diagramElem.getAttribute(ATTR_VERSION);
        if (CommonUtils.isEmpty(diagramVersion)) {
            throw new DBException("Diagram version not found");
        }
        if (!diagramVersion.equals(String.valueOf(ERD_VERSION_1))) {
            throw new DBException("Unsupported diagram version: " + diagramVersion);
        }

        List<TableLoadInfo> tableInfos = new ArrayList<TableLoadInfo>();
        List<RelationLoadInfo> relInfos = new ArrayList<RelationLoadInfo>();
        Map<String, TableLoadInfo> tableMap = new HashMap<String, TableLoadInfo>();

        final Element entitiesElem = XMLUtils.getChildElement(diagramElem, TAG_ENTITIES);
        if (entitiesElem != null) {

            // Parse data source
            for (Element dsElem : XMLUtils.getChildElementList(entitiesElem, TAG_DATA_SOURCE)) {
                String dsId = dsElem.getAttribute(ATTR_ID);
                if (CommonUtils.isEmpty(dsId)) {
                    log.warn("Missing datasource ID");
                    continue;
                }
                // Get connected datasource
                final DataSourceDescriptor dataSourceContainer = dsRegistry.getDataSource(dsId);
                if (dataSourceContainer == null) {
                    log.warn("Datasource '" + dsId + "' not found");
                    continue;
                }
                if (!dataSourceContainer.isConnected()) {
                    dataSourceContainer.connect(monitor);
                }
                final DBPDataSource dataSource = dataSourceContainer.getDataSource();
                if (!(dataSource instanceof DBSEntityContainer)) {
                    log.warn("Datasource '" + dataSourceContainer.getName() + "' entities cannot be loaded - no entity container found");
                    continue;
                }
                DBSEntityContainer rootContainer = (DBSEntityContainer)dataSource;
                // Parse entities
                for (Element entityElem : XMLUtils.getChildElementList(dsElem, TAG_ENTITY)) {
                    String tableId = entityElem.getAttribute(ATTR_ID);
                    String tableName = entityElem.getAttribute(ATTR_NAME);
                    List<String> path = new ArrayList<String>();
                    for (Element pathElem : XMLUtils.getChildElementList(entityElem, TAG_PATH)) {
                        path.add(0, pathElem.getAttribute(ATTR_NAME));
                    }
                    DBSEntityContainer container = rootContainer;
                    for (String conName : path) {
                        final DBSEntity child = container.getChild(monitor, conName);
                        if (child == null) {
                            log.warn("Object '" + conName + "' not found within '" + container.getName() + "'");
                            container = null;
                            break;
                        }
                        if (child instanceof DBSEntityContainer) {
                            container = (DBSEntityContainer) child;
                        } else {
                            log.warn("Object '" + child.getName() + "' is not a container");
                            container = null;
                            break;
                        }
                    }
                    if (container == null) {
                        continue;
                    }
                    final DBSEntity child = container.getChild(monitor, tableName);
                    if (!(child instanceof DBSTable)) {
                        log.warn("Cannot find table '" + tableName + "' in '" + container.getName() + "'");
                        continue;
                    }
                    String locX = entityElem.getAttribute(ATTR_X);
                    String locY = entityElem.getAttribute(ATTR_Y);

                    DBSTable table = (DBSTable) child;
                    Rectangle bounds = new Rectangle();
                    if (CommonUtils.isEmpty(locX) || CommonUtils.isEmpty(locY)) {
                        diagram.setNeedsAutoLayout(true);
                    } else {
                        bounds.x = Integer.parseInt(locX);
                        bounds.y = Integer.parseInt(locY);
                    }

                    TableLoadInfo info = new TableLoadInfo(tableId, table, bounds);
                    tableInfos.add(info);
                    tableMap.put(info.objectId, info);
                }
            }
        }

        final Element relationsElem = XMLUtils.getChildElement(diagramElem, TAG_RELATIONS);
        if (relationsElem != null) {
            // Parse relations
            for (Element relElem : XMLUtils.getChildElementList(relationsElem, TAG_RELATION)) {
                String relName = relElem.getAttribute(ATTR_NAME);
                String relType = relElem.getAttribute(ATTR_TYPE);
                String pkRefId = relElem.getAttribute(ATTR_PK_REF);
                String fkRefId = relElem.getAttribute(ATTR_FK_REF);
                if (CommonUtils.isEmpty(relName) || CommonUtils.isEmpty(pkRefId) || CommonUtils.isEmpty(fkRefId)) {
                    log.warn("Missing relation ID");
                    continue;
                }
                TableLoadInfo pkTable = tableMap.get(pkRefId);
                TableLoadInfo fkTable = tableMap.get(fkRefId);
                if (pkTable == null || fkTable == null) {
                    log.warn("PK (" + pkRefId + ") or FK (" + fkRefId +") table(s) not found for relation " + relName);
                    continue;
                }
                RelationLoadInfo relationLoadInfo = new RelationLoadInfo(relName, relType, pkTable, fkTable);
                relInfos.add(relationLoadInfo);

                for (Element bendElem : XMLUtils.getChildElementList(relElem, TAG_BEND)) {
                    String locX = bendElem.getAttribute(ATTR_X);
                    String locY = bendElem.getAttribute(ATTR_Y);
                    if (!CommonUtils.isEmpty(locX) && !CommonUtils.isEmpty(locY)) {
                        relationLoadInfo.bends.add(new Point(
                            Integer.parseInt(locX),
                            Integer.parseInt(locY)));
                    }
                }
            }
        }

        List<DBSTable> tableList = new ArrayList<DBSTable>();
        for (TableLoadInfo info : tableInfos) {
            tableList.add(info.table);
        }
        diagram.fillTables(monitor, tableList, null);

        // Set initial bounds
        for (TableLoadInfo info : tableInfos) {
            final ERDTable erdTable = diagram.getERDTable(info.table);
            if (erdTable != null) {
                diagram.addInitBounds(erdTable, info.bounds);
            }
        }
        // Set relations' bends
        for (RelationLoadInfo info : relInfos) {
            final ERDTable sourceTable = diagram.getERDTable(info.pkTable.table);
            final ERDTable targetTable = diagram.getERDTable(info.fkTable.table);
            diagram.addInitRelationBends(sourceTable, targetTable, info.name, info.bends);
        }
    }

    public static void save(DiagramPart diagramPart, final EntityDiagram diagram, boolean verbose, OutputStream out)
        throws IOException
    {
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

        Map<ERDTable, TableSaveInfo> infoMap = new IdentityHashMap<ERDTable, TableSaveInfo>();

        // Save as XML
        XMLBuilder xml = new XMLBuilder(out, ContentUtils.DEFAULT_FILE_CHARSET);
        xml.setButify(true);
        if (verbose) {
            xml.addContent(
                "\n<!DOCTYPE diagram [\n" +
                "<!ATTLIST diagram version CDATA #REQUIRED\n" +
                " name CDATA #IMPLIED\n" +
                " time CDATA #REQUIRED>\n" +
                "<!ELEMENT diagram (entities, relations, notes)>\n" +
                "<!ELEMENT entities (data-source*)>\n" +
                "<!ELEMENT data-source (entity*)>\n" +
                "<!ATTLIST data-source id CDATA #REQUIRED>\n" +
                "<!ELEMENT entity (path*)>\n" +
                "<!ATTLIST entity id ID #REQUIRED\n" +
                " name CDATA #REQUIRED\n" +
                " fq-name CDATA #REQUIRED>\n" +
                "<!ELEMENT relations (relation*)>\n" +
                "<!ELEMENT relation (bend*)>\n" +
                "<!ATTLIST relation name CDATA #REQUIRED\n" +
                " fq-name CDATA #REQUIRED\n" +
                " pk-ref IDREF #REQUIRED\n" +
                " fk-ref IDREF #REQUIRED>\n" +
                "]>\n"
            );
        }
        xml.startElement(TAG_DIAGRAM);
        xml.addAttribute(ATTR_VERSION, ERD_VERSION_1);
        if (diagram != null) {
            xml.addAttribute(ATTR_NAME, diagram.getName());
        }
        xml.addAttribute(ATTR_TIME, RuntimeUtils.getCurrentTimeStamp());

        if (diagram != null) {
            xml.startElement(TAG_ENTITIES);
            for (DBSDataSourceContainer dsContainer : dsMap.keySet()) {
                xml.startElement(TAG_DATA_SOURCE);
                xml.addAttribute(ATTR_ID, dsContainer.getId());

                final DataSourceObjects desc = dsMap.get(dsContainer);
                int tableCounter = ERD_VERSION_1;
                for (ERDTable erdTable : desc.tables) {
                    final DBSTable table = erdTable.getObject();
                    EntityPart tablePart = null;
                    if (diagramPart != null) {
                        for (Object child : diagramPart.getChildren()) {
                            if (child instanceof EntityPart && ((EntityPart) child).getTable() == erdTable) {
                                tablePart = (EntityPart) child;
                                break;
                            }
                        }
                    }

                    TableSaveInfo info = new TableSaveInfo(erdTable, tablePart, tableCounter++);
                    infoMap.put(erdTable, info);

                    xml.startElement(TAG_ENTITY);
                    xml.addAttribute(ATTR_ID, info.objectId);
                    xml.addAttribute(ATTR_NAME, table.getName());
                    xml.addAttribute(ATTR_FQ_NAME, table.getFullQualifiedName());
                    Rectangle tableBounds;
                    if (tablePart != null) {
                        tableBounds = tablePart.getBounds();
                    } else {
                        tableBounds = diagram.getInitBounds(erdTable);
                    }
                    if (tableBounds != null) {
                        xml.addAttribute(ATTR_X, tableBounds.x);
                        xml.addAttribute(ATTR_Y, tableBounds.y);
                    }
                    for (DBSObject parent = table.getParentObject(); parent != null && parent != dsContainer; parent = parent.getParentObject()) {
                        xml.startElement(TAG_PATH);
                        xml.addAttribute(ATTR_NAME, parent.getName());
                        xml.endElement();
                    }
                    xml.endElement();
                }
                xml.endElement();
            }
            xml.endElement();

            // Relations
            xml.startElement(TAG_RELATIONS);

            for (ERDTable erdTable : diagram.getTables()) {
                for (ERDAssociation rel : erdTable.getPrimaryKeyRelationships()) {
                    xml.startElement(TAG_RELATION);
                    xml.addAttribute(ATTR_NAME, rel.getObject().getName());
                    xml.addAttribute(ATTR_FQ_NAME, rel.getObject().getFullQualifiedName());
                    xml.addAttribute(ATTR_TYPE, rel.getObject().getConstraintType().getName());
                    TableSaveInfo pkInfo = infoMap.get(rel.getPrimaryKeyTable());
                    if (pkInfo == null) {
                        log.error("Cannot find PK table '" + rel.getPrimaryKeyTable().getObject().getFullQualifiedName() + "' in info map");
                        continue;
                    }
                    TableSaveInfo fkInfo = infoMap.get(rel.getForeignKeyTable());
                    if (fkInfo == null) {
                        log.error("Cannot find FK table '" + rel.getForeignKeyTable().getObject().getFullQualifiedName() + "' in info map");
                        continue;
                    }
                    xml.addAttribute(ATTR_PK_REF, pkInfo.objectId);
                    xml.addAttribute(ATTR_FK_REF, fkInfo.objectId);

                    // Save bends
                    if (pkInfo.tablePart != null) {
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
                                    xml.startElement(TAG_BEND);
                                    xml.addAttribute(ATTR_X, bendpoint.getLocation().x);
                                    xml.addAttribute(ATTR_Y, bendpoint.getLocation().y);
                                    xml.endElement();
                                }
                            }
                        }
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


}