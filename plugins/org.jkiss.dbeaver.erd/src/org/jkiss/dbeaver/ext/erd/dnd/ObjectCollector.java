package org.jkiss.dbeaver.ext.erd.dnd;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTable;

import java.util.*;

/**
 * Table collector
 */
public class ObjectCollector {

    private final EntityDiagram diagram;
    private final List<ERDTable> tables = new ArrayList<ERDTable>();
    private final Map<DBSTable, ERDTable> tableMap = new HashMap<DBSTable, ERDTable>();

    public ObjectCollector(EntityDiagram diagram)
    {
        this.diagram = diagram;
        this.tableMap.putAll(diagram.getTableMap());
    }

    public void collectTables(
        DBRProgressMonitor monitor,
        Collection<DBNNode> nodes)
        throws DBException
    {
        for (DBNNode node : nodes) {
            if (monitor.isCanceled()) {
                break;
            }
            if (node instanceof DBNDatabaseNode) {
                final DBSObject object = ((DBNDatabaseNode) node).getObject();
                if (object instanceof DBSTable) {
                    addTable(monitor, (DBSTable) object);
                } else if (object instanceof DBSEntityContainer) {
                    final Collection<? extends DBSEntity> children = ((DBSEntityContainer) object).getChildren(monitor);
                    if (!CommonUtils.isEmpty(children)) {
                        for (DBSEntity entity : children) {
                            if (entity instanceof DBSTable) {
                                addTable(monitor, (DBSTable)entity);
                            }
                        }
                    }
                }
            }
        }

        // Add new relations
        for (ERDTable erdTable : tables) {
            erdTable.addRelations(monitor, tableMap, false);
        }
    }

    private void addTable(DBRProgressMonitor monitor, DBSTable table)
    {
        if (diagram.containsTable(table)) {
            // Avoid duplicates
            return;
        }
        ERDTable erdTable = ERDTable.fromObject(monitor, table);
        tables.add(erdTable);
        tableMap.put(table, erdTable);
    }

    public List<ERDTable> getTables()
    {
        return tables;
    }
}
