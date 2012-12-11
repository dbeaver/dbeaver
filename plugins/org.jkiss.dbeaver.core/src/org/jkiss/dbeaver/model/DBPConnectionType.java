package org.jkiss.dbeaver.model;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.jkiss.dbeaver.core.DBeaverCore;

/**
 * Connection type
 */
public class DBPConnectionType {

    public static final DBPConnectionType DEV = new DBPConnectionType("dev", "Development", new RGB(0xFF, 0xFF, 0xFF), "Regular development database", true, false, true);
    public static final DBPConnectionType TEST = new DBPConnectionType("test", "Test", new RGB(0xC4, 0xFF, 0xB5), "Test (QA) database", true, false, true);
    public static final DBPConnectionType PROD = new DBPConnectionType("prod", "Production", new RGB(0xF7, 0x9F, 0x81), "Production database", false, true, true);

    public static final DBPConnectionType[] SYSTEM_TYPES = { DEV, TEST, PROD };
    public static final DBPConnectionType DEFAULT_TYPE = DEV;

    private String id;
    private String name;
    private Color color;
    private String description;
    private boolean autocommit;
    private boolean confirmExecute;
    private final boolean predefined;

    public DBPConnectionType(DBPConnectionType source)
    {
        this(source.id, source.name, source.color.getRGB(), source.description, source.autocommit, source.confirmExecute, source.predefined);
    }

    public DBPConnectionType(String id, String name, RGB rgb, String description, boolean autocommit, boolean confirmExecute)
    {
        this(id, name, rgb, description, autocommit, confirmExecute, false);
    }

    private DBPConnectionType(String id, String name, RGB rgb, String description, boolean autocommit, boolean confirmExecute, boolean predefined)
    {
        this.id = id;
        this.name = name;
        this.color = DBeaverCore.getInstance().getSharedTextColors().getColor(rgb);
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

    public Color getColor()
    {
        return color;
    }

    public void setColor(Color color)
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
}
