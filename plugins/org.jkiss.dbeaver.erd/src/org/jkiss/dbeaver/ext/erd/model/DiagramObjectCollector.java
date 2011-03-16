package org.jkiss.dbeaver.ext.erd.model;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.*;

/**
 * Table collector
 */
public class DiagramObjectCollector {

    private final EntityDiagram diagram;
    private final List<ERDTable> erdTables = new ArrayList<ERDTable>();
    private final Map<DBSTable, ERDTable> tableMap = new HashMap<DBSTable, ERDTable>();

    public DiagramObjectCollector(EntityDiagram diagram)
    {
        this.diagram = diagram;
        this.tableMap.putAll(diagram.getTableMap());
    }

    public static Collection<DBSTable> collectTables(
        DBRProgressMonitor monitor,
        Collection<? extends DBSObject> roots)
        throws DBException
    {
        Set<DBSTable> tables = new LinkedHashSet<DBSTable>();
        collectTables(monitor, roots, tables);
        return tables;
    }

    private static void collectTables(
        DBRProgressMonitor monitor,
        Collection<? extends DBSObject> roots,
        Set<DBSTable> tables)
        throws DBException
    {
        for (DBSObject root : roots) {
            if (monitor.isCanceled()) {
                break;
            }
            if (root instanceof DBSFolder) {
                collectTables(monitor, ((DBSFolder) root).getChildrenObjects(monitor), tables);
            } else if (root instanceof DBSTable) {
                tables.add((DBSTable) root);
            }
            if (root instanceof DBSEntityContainer) {
                collectTables(monitor, (DBSEntityContainer) root, tables);
            }
        }
    }

    private static void collectTables(
        DBRProgressMonitor monitor,
        DBSEntityContainer container,
        Set<DBSTable> tables)
        throws DBException
    {
        if (monitor.isCanceled()) {
            return;
        }
        container.cacheStructure(monitor, DBSEntityContainer.STRUCT_ALL);
        final Collection<? extends DBSEntity> children = container.getChildren(monitor);
        if (!CommonUtils.isEmpty(children)) {
            for (DBSEntity entity : children) {
                if (monitor.isCanceled()) {
                    break;
                }
                if (entity instanceof DBSTable) {
                    tables.add((DBSTable) entity);
                } else if (entity instanceof DBSEntityContainer) {
                    collectTables(monitor, (DBSEntityContainer) entity, tables);
                }
            }
        }
    }

    public void generateDiagramObjects(
        DBRProgressMonitor monitor,
        Collection<? extends DBSObject> roots)
        throws DBException
    {
        Collection<DBSTable> tables = collectTables(monitor, roots);
        for (DBSTable table : tables) {
            addDiagramTable(monitor, table);
        }

        // Add new relations
        for (ERDTable erdTable : erdTables) {
            erdTable.addRelations(monitor, tableMap, false);
        }
    }

    private void addDiagramTable(DBRProgressMonitor monitor, DBSTable table)
    {
        if (diagram.containsTable(table)) {
            // Avoid duplicates
            return;
        }
        ERDTable erdTable = ERDTable.fromObject(monitor, table);
        erdTables.add(erdTable);
        tableMap.put(table, erdTable);
    }

    public List<ERDTable> getDiagramTables()
    {
        return erdTables;
    }

}
