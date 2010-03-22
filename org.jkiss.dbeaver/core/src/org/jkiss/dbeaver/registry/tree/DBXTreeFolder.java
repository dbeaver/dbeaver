package org.jkiss.dbeaver.registry.tree;

/**
 * DBXTreeFolder
 */
public class DBXTreeFolder extends DBXTreeNode
{
    private String type;
    private String label;
    private String description;

    public DBXTreeFolder(DBXTreeNode parent, String type, String label)
    {
        super(parent);
        this.type = type;
        this.label = label;
    }

    public String getType()
    {
        return type;
    }

    public String getLabel()
    {
        return label;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
