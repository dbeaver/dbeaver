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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.RelativeBendpoint;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.part.*;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Entity diagram loader/saver
 * @author Serge Rider
 */
public class DiagramLoader
{
    private static final Log log = Log.getLog(DiagramLoader.class);

    private static final String TAG_DIAGRAM = "diagram";
    private static final String TAG_ENTITIES = "entities";
    private static final String TAG_DATA_SOURCE = "data-source";
    private static final String TAG_ENTITY = "entity";
    private static final String TAG_PATH = "path";
    private static final String TAG_RELATIONS = "relations";
    private static final String TAG_RELATION = "relation";
    private static final String TAG_BEND = "bend";

    private static final String ATTR_VERSION = "version";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_TIME = "time";
    private static final String ATTR_ID = "id";
    private static final String ATTR_ORDER = "order";
    private static final String ATTR_COLOR_BG = "color-bg";
    private static final String ATTR_FQ_NAME = "fq-name";
    private static final String ATTR_REF_NAME = "ref-name";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_PK_REF = "pk-ref";
    private static final String ATTR_FK_REF = "fk-ref";
    private static final String TAG_COLUMN = "column";
    private static final String ATTR_X = "x";
    private static final String ATTR_Y = "y";
    private static final String ATTR_W = "w";
    private static final String ATTR_H = "h";

    private static final int ERD_VERSION_1 = 1;
    private static final String BEND_ABSOLUTE = "abs";
    private static final String BEND_RELATIVE = "rel";
    private static final String TAG_NOTES = "notes";
    private static final String TAG_NOTE = "note";

    private static class TableSaveInfo {
        final ERDEntity erdEntity;
        final EntityPart tablePart;
        final int objectId;

        private TableSaveInfo(ERDEntity erdEntity, EntityPart tablePart, int objectId)
        {
            this.erdEntity = erdEntity;
            this.tablePart = tablePart;
            this.objectId = objectId;
        }
    }

    private static class TableLoadInfo {
        final String objectId;
        final DBSEntity table;
        final EntityDiagram.NodeVisualInfo visualInfo;

        private TableLoadInfo(String objectId, DBSEntity table, EntityDiagram.NodeVisualInfo visualInfo)
        {
            this.objectId = objectId;
            this.table = table;
            this.visualInfo = visualInfo;
        }
    }

    private static class RelationLoadInfo {
        final String name;
        final String type;
        final TableLoadInfo pkTable;
        final TableLoadInfo fkTable;
        final Map<String, String> columns = new LinkedHashMap<>();
        final List<Point> bends = new ArrayList<>();

        private RelationLoadInfo(String name, String type, TableLoadInfo pkTable, TableLoadInfo fkTable)
        {
            this.name = name;
            this.type = type;
            this.pkTable = pkTable;
            this.fkTable = fkTable;
        }
    }

    private static class DataSourceObjects {
        List<ERDEntity> entities = new ArrayList<>();
    }

    public static void load(DBRProgressMonitor monitor, IProject project, DiagramPart diagramPart, InputStream in)
        throws IOException, XMLException, DBException
    {
        monitor.beginTask("Parse diagram", 1);
        final EntityDiagram diagram = diagramPart.getDiagram();

        final DataSourceRegistry dsRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
        if (dsRegistry == null) {
            throw new DBException("Cannot find datasource registry for project '" + project.getName() + "'");
        }

        final Document document = XMLUtils.parseDocument(in);

        final Element diagramElem = document.getDocumentElement();
        monitor.done();

        // Check version
        final String diagramVersion = diagramElem.getAttribute(ATTR_VERSION);
        if (CommonUtils.isEmpty(diagramVersion)) {
            throw new DBException("Diagram version not found");
        }
        if (!diagramVersion.equals(String.valueOf(ERD_VERSION_1))) {
            throw new DBException("Unsupported diagram version: " + diagramVersion);
        }

        List<TableLoadInfo> tableInfos = new ArrayList<>();
        List<RelationLoadInfo> relInfos = new ArrayList<>();
        Map<String, TableLoadInfo> tableMap = new HashMap<>();

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
                    monitor.subTask("Connect to '" + dataSourceContainer.getName() + "'");
                    try {
                        dataSourceContainer.connect(monitor, true, true);
                    } catch (DBException e) {
                        diagram.addErrorMessage("Can't connect to '" + dataSourceContainer.getName() + "': " + e.getMessage());
                        continue;
                    }
                }
                final DBPDataSource dataSource = dataSourceContainer.getDataSource();
                if (!(dataSource instanceof DBSObjectContainer)) {
                    diagram.addErrorMessage("Datasource '" + dataSourceContainer.getName() + "' entities cannot be loaded - no entity container found");
                    continue;
                }
                DBSObjectContainer rootContainer = (DBSObjectContainer)dataSource;
                // Parse entities
                Collection<Element> entityElemList = XMLUtils.getChildElementList(dsElem, TAG_ENTITY);
                monitor.beginTask("Parse entities", entityElemList.size());
                for (Element entityElem : entityElemList) {
                    String tableId = entityElem.getAttribute(ATTR_ID);
                    String tableName = entityElem.getAttribute(ATTR_NAME);
                    monitor.subTask("Load " + tableName);
                    List<String> path = new ArrayList<>();
                    for (Element pathElem : XMLUtils.getChildElementList(entityElem, TAG_PATH)) {
                        path.add(0, pathElem.getAttribute(ATTR_NAME));
                    }
                    DBSObjectContainer container = rootContainer;
                    for (String conName : path) {
                        final DBSObject child = container.getChild(monitor, conName);
                        if (child == null) {
                            diagram.addErrorMessage("Object '" + conName + "' not found within '" + container.getName() + "'");
                            container = null;
                            break;
                        }
                        if (child instanceof DBSObjectContainer) {
                            container = (DBSObjectContainer) child;
                        } else {
                            diagram.addErrorMessage("Object '" + child.getName() + "' is not a container");
                            container = null;
                            break;
                        }
                    }
                    if (container == null) {
                        continue;
                    }
                    final DBSObject child = container.getChild(monitor, tableName);
                    if (!(child instanceof DBSEntity)) {
                        diagram.addErrorMessage("Cannot find table '" + tableName + "' in '" + container.getName() + "'");
                        continue;
                    }
                    String locX = entityElem.getAttribute(ATTR_X);
                    String locY = entityElem.getAttribute(ATTR_Y);

                    DBSEntity table = (DBSEntity) child;
                    EntityDiagram.NodeVisualInfo visualInfo = new EntityDiagram.NodeVisualInfo();

                    visualInfo.initBounds = new Rectangle();
                    if (CommonUtils.isEmpty(locX) || CommonUtils.isEmpty(locY)) {
                        diagram.setNeedsAutoLayout(true);
                    } else {
                        visualInfo.initBounds.x = Integer.parseInt(locX);
                        visualInfo.initBounds.y = Integer.parseInt(locY);
                    }
                    String colorBg = entityElem.getAttribute(ATTR_COLOR_BG);
                    if (!CommonUtils.isEmpty(colorBg)) {
                        visualInfo.bgColor = UIUtils.getSharedColor(colorBg);
                    }
                    String orderStr = entityElem.getAttribute(ATTR_ORDER);
                    if (!CommonUtils.isEmpty(orderStr)) {
                        visualInfo.zOrder = Integer.parseInt(orderStr);
                    }

                    TableLoadInfo info = new TableLoadInfo(tableId, table, visualInfo);
                    tableInfos.add(info);
                    tableMap.put(info.objectId, info);
                    monitor.worked(1);
                }
                monitor.done();
            }
        }

        final Element relationsElem = XMLUtils.getChildElement(diagramElem, TAG_RELATIONS);
        if (relationsElem != null) {
            // Parse relations
            Collection<Element> relElemList = XMLUtils.getChildElementList(relationsElem, TAG_RELATION);
            monitor.beginTask("Parse relations", relElemList.size());
            for (Element relElem : relElemList) {
                String relName = relElem.getAttribute(ATTR_NAME);
                monitor.subTask("Load " + relName);
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
                    log.debug("PK (" + pkRefId + ") or FK (" + fkRefId +") table(s) not found for relation " + relName);
                    continue;
                }
                RelationLoadInfo relationLoadInfo = new RelationLoadInfo(relName, relType, pkTable, fkTable);
                relInfos.add(relationLoadInfo);

                // Load columns (present only in logical relations)
                for (Element columnElem : XMLUtils.getChildElementList(relElem, TAG_COLUMN)) {
                    String name = columnElem.getAttribute(ATTR_NAME);
                    String refName = columnElem.getAttribute(ATTR_REF_NAME);
                    relationLoadInfo.columns.put(name, refName);
                }

                // Load bends
                for (Element bendElem : XMLUtils.getChildElementList(relElem, TAG_BEND)) {
                    String type = bendElem.getAttribute(ATTR_TYPE);
                    if (!BEND_RELATIVE.equals(type)) {
                        String locX = bendElem.getAttribute(ATTR_X);
                        String locY = bendElem.getAttribute(ATTR_Y);
                        if (!CommonUtils.isEmpty(locX) && !CommonUtils.isEmpty(locY)) {
                            relationLoadInfo.bends.add(new Point(
                                Integer.parseInt(locX),
                                Integer.parseInt(locY)));
                        }
                    }
                }
                monitor.worked(1);
            }
            monitor.done();
        }

        // Load notes
        final Element notesElem = XMLUtils.getChildElement(diagramElem, TAG_NOTES);
        if (notesElem != null) {
            // Parse relations
            Collection<Element> noteElemList = XMLUtils.getChildElementList(notesElem, TAG_NOTE);
            monitor.beginTask("Parse notes", noteElemList.size());
            for (Element noteElem : noteElemList) {
                final String noteText = XMLUtils.getElementBody(noteElem);
                ERDNote note = new ERDNote(noteText);
                diagram.addNote(note, false);
                String locX = noteElem.getAttribute(ATTR_X);
                String locY = noteElem.getAttribute(ATTR_Y);
                String locW = noteElem.getAttribute(ATTR_W);
                String locH = noteElem.getAttribute(ATTR_H);
                EntityDiagram.NodeVisualInfo visualInfo = new EntityDiagram.NodeVisualInfo();
                if (!CommonUtils.isEmpty(locX) && !CommonUtils.isEmpty(locY) && !CommonUtils.isEmpty(locW) && !CommonUtils.isEmpty(locH)) {
                    visualInfo.initBounds = new Rectangle(
                        Integer.parseInt(locX), Integer.parseInt(locY), Integer.parseInt(locW), Integer.parseInt(locH));
                }
                String colorBg = noteElem.getAttribute(ATTR_COLOR_BG);
                if (!CommonUtils.isEmpty(colorBg)) {
                    visualInfo.bgColor = UIUtils.getSharedColor(colorBg);
                }
                String orderStr = noteElem.getAttribute(ATTR_ORDER);
                if (!CommonUtils.isEmpty(orderStr)) {
                    visualInfo.zOrder = Integer.parseInt(orderStr);
                }
                diagram.addVisualInfo(note, visualInfo);
            }
        }

        // Fill entities
        List<DBSEntity> tableList = new ArrayList<>();
        for (TableLoadInfo info : tableInfos) {
            tableList.add(info.table);
        }
        diagram.fillTables(monitor, tableList, null);

        // Set initial bounds
        for (TableLoadInfo info : tableInfos) {
            final ERDEntity erdEntity = diagram.getERDTable(info.table);
            if (erdEntity != null) {
                diagram.addVisualInfo(erdEntity, info.visualInfo);
            }
        }

        // Add logical relations
        for (RelationLoadInfo info : relInfos) {
            if (info.type.equals(ERDConstants.CONSTRAINT_LOGICAL_FK.getId())) {
                final ERDEntity sourceEntity = diagram.getERDTable(info.pkTable.table);
                final ERDEntity targetEntity = diagram.getERDTable(info.fkTable.table);
                if (sourceEntity != null && targetEntity != null) {
                    new ERDAssociation(targetEntity, sourceEntity, false);
                }
            }
        }
        // Set relations' bends
        for (RelationLoadInfo info : relInfos) {
            if (!CommonUtils.isEmpty(info.bends)) {
                final ERDEntity sourceEntity = diagram.getERDTable(info.pkTable.table);
                if (sourceEntity == null) {
                    log.warn("Source table " + info.pkTable.table.getName() + " not found");
                    continue;
                }
                final ERDEntity targetEntity = diagram.getERDTable(info.fkTable.table);
                if (targetEntity == null) {
                    log.warn("Target table " + info.pkTable.table.getName() + " not found");
                    continue;
                }
                diagram.addInitRelationBends(sourceEntity, targetEntity, info.name, info.bends);
            }
        }
    }

    public static void save(DBRProgressMonitor monitor, @Nullable DiagramPart diagramPart, final EntityDiagram diagram, boolean verbose, OutputStream out)
        throws IOException
    {
        List allNodeFigures = diagramPart == null ? new ArrayList() : diagramPart.getFigure().getChildren();

        // Prepare DS objects map
        Map<DBPDataSourceContainer, DataSourceObjects> dsMap = new IdentityHashMap<>();
        if (diagram != null) {
            for (ERDEntity erdEntity : diagram.getEntities()) {
                final DBPDataSourceContainer dsContainer = erdEntity.getObject().getDataSource().getContainer();
                DataSourceObjects desc = dsMap.get(dsContainer);
                if (desc == null) {
                    desc = new DataSourceObjects();
                    dsMap.put(dsContainer, desc);
                }
                desc.entities.add(erdEntity);
            }
        }

        Map<ERDEntity, TableSaveInfo> infoMap = new IdentityHashMap<>();

        // Save as XML
        XMLBuilder xml = new XMLBuilder(out, GeneralUtils.UTF8_ENCODING);
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
            for (DBPDataSourceContainer dsContainer : dsMap.keySet()) {
                xml.startElement(TAG_DATA_SOURCE);
                xml.addAttribute(ATTR_ID, dsContainer.getId());

                final DataSourceObjects desc = dsMap.get(dsContainer);
                int tableCounter = ERD_VERSION_1;
                for (ERDEntity erdEntity : desc.entities) {
                    final DBSEntity table = erdEntity.getObject();
                    EntityPart tablePart = diagramPart == null ? null : diagramPart.getEntityPart(erdEntity);
                    TableSaveInfo info = new TableSaveInfo(erdEntity, tablePart, tableCounter++);
                    infoMap.put(erdEntity, info);

                    xml.startElement(TAG_ENTITY);
                    xml.addAttribute(ATTR_ID, info.objectId);
                    xml.addAttribute(ATTR_NAME, table.getName());
                    if (table instanceof DBPQualifiedObject) {
                        xml.addAttribute(ATTR_FQ_NAME, ((DBPQualifiedObject)table).getFullyQualifiedName(DBPEvaluationContext.UI));
                    }
                    EntityDiagram.NodeVisualInfo visualInfo;
                    if (tablePart != null) {
                        visualInfo = new EntityDiagram.NodeVisualInfo();
                        visualInfo.initBounds = tablePart.getBounds();
                        visualInfo.bgColor = tablePart.getCustomBackgroundColor();

                        saveColorAndOrder(allNodeFigures, xml, tablePart);

                    } else {
                        visualInfo = diagram.getVisualInfo(erdEntity);
                    }
                    if (visualInfo != null && visualInfo.initBounds != null) {
                        xml.addAttribute(ATTR_X, visualInfo.initBounds.x);
                        xml.addAttribute(ATTR_Y, visualInfo.initBounds.y);
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

            for (ERDEntity erdEntity : diagram.getEntities()) {
                for (ERDAssociation rel : erdEntity.getPrimaryKeyRelationships()) {
                    xml.startElement(TAG_RELATION);
                    DBSEntityAssociation association = rel.getObject();
                    xml.addAttribute(ATTR_NAME, association.getName());
                    if (association instanceof DBPQualifiedObject) {
                        xml.addAttribute(ATTR_FQ_NAME, ((DBPQualifiedObject) association).getFullyQualifiedName(DBPEvaluationContext.UI));
                    }
                    xml.addAttribute(ATTR_TYPE, association.getConstraintType().getId());
                    TableSaveInfo pkInfo = infoMap.get(rel.getPrimaryKeyEntity());
                    if (pkInfo == null) {
                        log.error("Cannot find PK table '" + DBUtils.getObjectFullName(rel.getPrimaryKeyEntity().getObject(), DBPEvaluationContext.UI) + "' in info map");
                        continue;
                    }
                    TableSaveInfo fkInfo = infoMap.get(rel.getForeignKeyEntity());
                    if (fkInfo == null) {
                        log.error("Cannot find FK table '" + DBUtils.getObjectFullName(rel.getForeignKeyEntity().getObject(), DBPEvaluationContext.UI) + "' in info map");
                        continue;
                    }
                    xml.addAttribute(ATTR_PK_REF, pkInfo.objectId);
                    xml.addAttribute(ATTR_FK_REF, fkInfo.objectId);

                    if (association instanceof ERDLogicalForeignKey) {
                        // Save columns
                        for (DBSEntityAttributeRef column : ((ERDLogicalForeignKey) association).getAttributeReferences(new VoidProgressMonitor())) {
                            xml.startElement(TAG_COLUMN);
                            xml.addAttribute(ATTR_NAME, column.getAttribute().getName());
                            try {
                                DBSEntityAttribute referenceAttribute = DBUtils.getReferenceAttribute(monitor, association, column.getAttribute());
                                if (referenceAttribute != null) {
                                    xml.addAttribute(ATTR_REF_NAME, referenceAttribute.getName());
                                }
                            } catch (DBException e) {
                                log.warn("Error getting reference attribute", e);
                            }
                            xml.endElement();
                        }
                    }

                    // Save bends
                    if (pkInfo.tablePart != null) {
                        AssociationPart associationPart = pkInfo.tablePart.getConnectionPart(rel, false);
                        if (associationPart != null) {
                            final List<Bendpoint> bendpoints = associationPart.getBendpoints();
                            if (!CommonUtils.isEmpty(bendpoints)) {
                                for (Bendpoint bendpoint : bendpoints) {
                                    xml.startElement(TAG_BEND);
                                    if (bendpoint instanceof AbsoluteBendpoint) {
                                        xml.addAttribute(ATTR_TYPE, BEND_ABSOLUTE);
                                        xml.addAttribute(ATTR_X, bendpoint.getLocation().x);
                                        xml.addAttribute(ATTR_Y, bendpoint.getLocation().y);
                                    } else if (bendpoint instanceof RelativeBendpoint) {
                                        xml.addAttribute(ATTR_TYPE, BEND_RELATIVE);
                                        xml.addAttribute(ATTR_X, bendpoint.getLocation().x);
                                        xml.addAttribute(ATTR_Y, bendpoint.getLocation().y);
                                    }
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
            xml.startElement(TAG_NOTES);
            for (ERDNote note : diagram.getNotes()) {
                NotePart notePart = diagramPart == null ? null : diagramPart.getNotePart(note);
                xml.startElement(TAG_NOTE);
                if (notePart != null) {
                    saveColorAndOrder(allNodeFigures, xml, notePart);

                    Rectangle noteBounds = notePart.getBounds();
                    if (noteBounds != null) {
                        xml.addAttribute(ATTR_X, noteBounds.x);
                        xml.addAttribute(ATTR_Y, noteBounds.y);
                        xml.addAttribute(ATTR_W, noteBounds.width);
                        xml.addAttribute(ATTR_H, noteBounds.height);
                    }
                }
                xml.addText(note.getObject());
                xml.endElement();
            }
            xml.endElement();
        }

        xml.endElement();

        xml.flush();
    }

    private static void saveColorAndOrder(List allNodeFigures, XMLBuilder xml, NodePart nodePart) throws IOException {
        if (nodePart != null) {
            xml.addAttribute(ATTR_ORDER, allNodeFigures.indexOf(nodePart.getFigure()));
            Color color = nodePart.getCustomBackgroundColor();
            if (color != null) {
                xml.addAttribute(ATTR_COLOR_BG, StringConverter.asString(color.getRGB()));
            }
        }
    }


}