package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * ERDTable
 */
public class ERDTable extends ERDNode {

    private DBSTable table;
    private List<ERDTableColumn> columns = new ArrayList<ERDTableColumn>();

    private int tableRectWidth, tableRectHeight;
    private boolean independent;

    public ERDTable(DBSTable table)
    {
        super(table);
        this.table = table;
    }

    public String getName()
    {
        return table.getName();
    }

    public DBSTable getTable()
    {
        return table;
    }

    public List<ERDTableColumn> getColumns()
    {
        return columns;
    }

    public int getRectWidth()
    {
        return tableRectWidth;
    }

    public int getRectHeight()
    {
        return tableRectHeight;
    }

    /**
     * Independent table do not have FK columns in it's PK
     * @return true or false
     */
    public boolean isIndependent()
    {
        return independent;
    }

    private boolean isInForeignKey(DBRProgressMonitor monitor, DBSTableColumn tableColumn)
        throws DBException
    {
        Collection<? extends DBSForeignKey> constraints = getTable().getForeignKeys(monitor);
        if (constraints != null) {
            for (DBSConstraint constraint : constraints) {
                if (constraint.getColumn(monitor, tableColumn) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    DBSConstraint getPrimaryKey(DBRProgressMonitor monitor)
        throws DBException
    {
        // Try to get key number from primary key constraint column
        Collection<? extends DBSConstraint> constraints = getTable().getUniqueKeys(monitor);
        if (constraints != null) {
            for (DBSConstraint constraint : constraints) {
                if (constraint.getConstraintType().isUnique()) {
                    return constraint;
                }
            }
        }
        return null;
    }

    public void calculateContent(DBRProgressMonitor monitor, Graphics graphics)
        throws DBException
    {
        FontMetrics titleMetrics = graphics.getFontMetrics();
        FontMetrics attrMetrics = graphics.getFontMetrics();

        // Get all columns
        Collection<? extends DBSTableColumn> dbsTableColumns = getTable().getColumns(monitor);
        for (DBSTableColumn column : dbsTableColumns) {
            ERDTableColumn erdColumn = new ERDTableColumn(monitor, this, column);
            columns.add(erdColumn);
            int columnStrWidth = attrMetrics.stringWidth(erdColumn.getName());
            if (columnStrWidth > tableRectWidth) {
                tableRectWidth = columnStrWidth;
            }
        }
        tableRectWidth += 10; // Borders

        // Calculate rect height
        tableRectHeight = titleMetrics.getHeight();
        tableRectHeight += 10; // Borders + divider
        int totalAttrs = columns.size();
        if (totalAttrs < 2) {
            totalAttrs = 2;
        }
        tableRectHeight += totalAttrs * attrMetrics.getHeight();

        // Order columns
        Collections.sort(columns);

        // check independence
        independent = true;
        DBSConstraint primaryKey = getPrimaryKey(monitor);
        if (primaryKey != null && primaryKey.getColumns(monitor) != null) {
            for (DBSConstraintColumn constrCol : primaryKey.getColumns(monitor)) {
                if (isInForeignKey(monitor, constrCol.getTableColumn())) {
                    independent = false;
                }
            }
        }
    }

    public String getTipString()
    {
        return getName();
    }

    @Override
    public String getId()
    {
        return table.getFullQualifiedName();
    }
}
