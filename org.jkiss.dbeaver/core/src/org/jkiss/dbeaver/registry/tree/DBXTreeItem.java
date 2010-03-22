package org.jkiss.dbeaver.registry.tree;

/**
 * DBXTreeItem
 */
public class DBXTreeItem extends DBXTreeNode
{
    private String label;
    private String path;
    private String propertyName;
    private boolean optional;

    public DBXTreeItem(DBXTreeNode parent, String label, String path, String propertyName, boolean optional)
    {
        super(parent);
        this.label = label;
        this.path = path;
        this.propertyName = propertyName;
        this.optional = optional;
    }

    public String getPath()
    {
        return path;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public boolean isOptional()
    {
        return optional;
    }

    public String getLabel()
    {
        return label;
    }
}
