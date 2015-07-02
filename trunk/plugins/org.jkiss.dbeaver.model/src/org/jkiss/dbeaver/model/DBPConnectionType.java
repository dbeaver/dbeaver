package org.jkiss.dbeaver.model;

/**
 * Connection type
 */
public class DBPConnectionType {

    public static final DBPConnectionType DEV = new DBPConnectionType("dev", "Development", "255,255,255", "Regular development database", true, false, true);
    public static final DBPConnectionType TEST = new DBPConnectionType("test", "Test", "196,255,181", "Test (QA) database", true, false, true);
    public static final DBPConnectionType PROD = new DBPConnectionType("prod", "Production", "247,159,129", "Production database", false, true, true);

    public static final DBPConnectionType[] SYSTEM_TYPES = { DEV, TEST, PROD };
    public static final DBPConnectionType DEFAULT_TYPE = DEV;

    private String id;
    private String name;
    private String color;
    private String description;
    private boolean autocommit;
    private boolean confirmExecute;
    private final boolean predefined;

    public DBPConnectionType(DBPConnectionType source)
    {
        this(source.id, source.name, source.color, source.description, source.autocommit, source.confirmExecute, source.predefined);
    }

    public DBPConnectionType(String id, String name, String color, String description, boolean autocommit, boolean confirmExecute)
    {
        this(id, name, color, description, autocommit, confirmExecute, false);
    }

    private DBPConnectionType(String id, String name, String color, String description, boolean autocommit, boolean confirmExecute, boolean predefined)
    {
        this.id = id;
        this.name = name;
        this.color = color;
        this.description = description;
        this.autocommit = autocommit;
        this.confirmExecute = confirmExecute;
        this.predefined = predefined;
    }

    public boolean isPredefined()
    {
        return predefined;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public boolean isAutocommit()
    {
        return autocommit;
    }

    public void setAutocommit(boolean autocommit)
    {
        this.autocommit = autocommit;
    }

    public boolean isConfirmExecute()
    {
        return confirmExecute;
    }

    public void setConfirmExecute(boolean confirmExecute)
    {
        this.confirmExecute = confirmExecute;
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof DBPConnectionType && id.equals(((DBPConnectionType)obj).id);
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }
}
