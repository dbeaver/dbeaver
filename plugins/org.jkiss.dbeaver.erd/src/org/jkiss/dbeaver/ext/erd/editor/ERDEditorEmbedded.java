/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;
import org.jkiss.dbeaver.ext.erd.model.ERDTableColumn;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.ui.IDatabaseObjectEditor;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBOManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Embedded ERD editor
 */
public class ERDEditorEmbedded extends ERDEditorPart implements IDatabaseObjectEditor<DBOManager<DBSObject>>, IRefreshablePart {

    private DBOManager<DBSObject> objectManager;

    /**
     * No-arg constructor
     */
    public ERDEditorEmbedded()
    {
    }

    DBSObject getSourceObject()
    {
        return objectManager.getObject();
    }

    public DBOManager<DBSObject> getObjectManager()
    {
        return objectManager;
    }

    public void initObjectEditor(DBOManager<DBSObject> manager)
    {
        objectManager = manager;
    }

    public void activatePart()
    {
        if (isLoaded) {
            return;
        }
        loadDiagram();
        isLoaded = true;
    }

    public void deactivatePart()
    {
    }

    protected synchronized void loadDiagram()
    {
        DBSObject object = getSourceObject();
        if (object == null) {
            return;
        }
        if (diagramLoadingJob != null) {
            // Do not start new one while old is running
            return;
        }
        diagramLoadingJob = LoadingUtils.createService(
            new DatabaseLoadService<EntityDiagram>("Load diagram '" + object.getName() + "'", object.getDataSource()) {
                public EntityDiagram evaluate()
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        return loadFromDatabase(getProgressMonitor());
                    } catch (DBException e) {
                        log.error(e);
                    }

                    return null;
                }
            },
            progressControl.createLoadVisualizer());
        diagramLoadingJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event)
            {
                diagramLoadingJob = null;
            }
        });
        diagramLoadingJob.schedule();
    }

    public DBPDataSource getDataSource()
    {
        return objectManager.getDataSource();
    }

    public void refreshPart(Object source)
    {
        if (isLoaded) {
            loadDiagram();
        }
    }

    private EntityDiagram loadFromDatabase(DBRProgressMonitor monitor)
        throws DBException
    {
        if (getSourceObject() == null) {
            log.error("Database object must be entity container to render ERD diagram");
            return null;
        }
        DBSObject dbObject = getSourceObject();
        EntityDiagram diagram = new EntityDiagram(dbObject, dbObject.getName());

        Collection<DBSTable> tables = collectDatabaseTables(monitor, dbObject);

        // Load entities
        Map<DBSTable, ERDTable> tableMap = new HashMap<DBSTable, ERDTable>();
        for (DBSTable table : tables) {
            if (monitor.isCanceled()) {
                break;
            }
            ERDTable erdTable = new ERDTable(table);
            if (table == dbObject) {
                erdTable.setPrimary(true);
            }

            try {
                List<DBSTableColumn> idColumns = DBUtils.getBestTableIdentifier(monitor, table);

                Collection<? extends DBSTableColumn> columns = table.getColumns(monitor);
                if (!CommonUtils.isEmpty(columns)) {
                    for (DBSTableColumn column : columns) {
                        ERDTableColumn c1 = new ERDTableColumn(column, idColumns.contains(column));
                        erdTable.addColumn(c1);
                    }
                }
            } catch (DBException e) {
                // just skip this problematic columns
                log.debug("Could not load table '" + table.getName() + "'columns", e);
            }
            diagram.addTable(erdTable);
            tableMap.put(table, erdTable);
        }

        // Load relations
        for (DBSTable table : tables) {
            if (monitor.isCanceled()) {
                break;
            }
            ERDTable table1 = tableMap.get(table);
            try {
                Set<DBSTableColumn> fkColumns = new HashSet<DBSTableColumn>();
                // Make associations
                Collection<? extends DBSForeignKey> fks = table.getForeignKeys(monitor);
                for (DBSForeignKey fk : fks) {
                    fkColumns.addAll(DBUtils.getTableColumns(monitor, fk));
                    ERDTable table2 = tableMap.get(fk.getReferencedKey().getTable());
                    if (table2 == null) {
                        //log.warn("Table '" + fk.getReferencedKey().getTable().getFullQualifiedName() + "' not found in ERD");
                    } else {
                        //if (table1 != table2) {
                        new ERDAssociation(fk, table2, table1);
                        //}
                    }
                }

                // Mark column's fk flag
                for (ERDTableColumn column : table1.getColumns()) {
                    if (fkColumns.contains(column.getObject())) {
                        column.setInForeignKey(true);
                    }
                }

            } catch (DBException e) {
                log.warn("Could not load table '" + table.getName() + "' foreign keys", e);
            }
        }

        return diagram;
    }

    private Collection<DBSTable> collectDatabaseTables(DBRProgressMonitor monitor, DBSObject root) throws DBException
    {
        Set<DBSTable> result = new HashSet<DBSTable>();

        // Cache structure
        if (root instanceof DBSEntityContainer) {
            DBSEntityContainer entityContainer = (DBSEntityContainer) root;
            entityContainer.cacheStructure(monitor, DBSEntityContainer.STRUCT_ENTITIES | DBSEntityContainer.STRUCT_ASSOCIATIONS | DBSEntityContainer.STRUCT_ATTRIBUTES);
            Collection<? extends DBSObject> entities = entityContainer.getChildren(monitor);
            for (DBSObject entity : entities) {
                if (entity instanceof DBSTable) {
                    result.add((DBSTable) entity);
                }
            }

        } else if (root instanceof DBSTable) {
            DBSTable rootTable = (DBSTable) root;
            result.add(rootTable);
            try {
                Collection<? extends DBSForeignKey> fks = rootTable.getForeignKeys(monitor);
                if (fks != null) {
                    for (DBSForeignKey fk : fks) {
                        result.add(fk.getReferencedKey().getTable());
                    }
                }
            } catch (DBException e) {
                log.warn("Could not load table foreign keys", e);
            }
            if (monitor.isCanceled()) {
                return result;
            }
            try {
                Collection<? extends DBSForeignKey> refs = rootTable.getReferences(monitor);
                if (refs != null) {
                    for (DBSForeignKey ref : refs) {
                        result.add(ref.getTable());
                    }
                }
            } catch (DBException e) {
                log.warn("Could not load table references", e);
            }
        }

        return result;
    }
/*
    private void loadContentFromFile(IFileEditorInput input) {

        IFile file = input.getFile();
        try {
            setPartName(file.getName());
            InputStream is = file.getContents(true);
            ObjectInputStream ois = new ObjectInputStream(is);
            entityDiagram = (EntityDiagram) ois.readObject();
            ois.close();
        }
        catch (Exception e) {
            log.error("Error loading diagram from file '" + file.getFullPath().toString() + "'", e);
            entityDiagram = getContent();
        }
    }
*/

/*
    public DBNNode getRootNode() {
        IEditorInput editorInput = getEditorInput();
        if (editorInput instanceof IDatabaseNodeEditorInput) {
            return ((IDatabaseNodeEditorInput)editorInput).getTreeNode();
        }
        return null;
    }

    public Viewer getNavigatorViewer() {
        return null;
    }

    public IWorkbenchPart getWorkbenchPart() {
        return this;
    }
*/

}
