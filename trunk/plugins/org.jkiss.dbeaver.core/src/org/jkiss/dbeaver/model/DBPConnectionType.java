package org.jkiss.dbeaver.model;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.jkiss.dbeaver.core.DBeaverCore;

/**
 * Connection type
 */
public class DBPConnectionType {

    public static final DBPConnectionType DEV = new DBPConnectionType("dev", "Development", new RGB(0xFF, 0xFF, 0xFF), "Regular development database");
    public static final DBPConnectionType TEST = new DBPConnectionType("test", "Test", new RGB(0xFF, 0xE4, 0xB5), "Test (QA) database");
    public static final DBPConnectionType PROD = new DBPConnectionType("prod", "Production", new RGB(0xC4, 0xFF, 0xB5), "Production database");

    public static final DBPConnectionType[] SYSTEM_TYPES = { DEV, TEST, PROD };
    public static final DBPConnectionType DEFAULT_TYPE = DEV;

    private String id;
    private String name;
    private Color color;
    private RGB rgb;
    private String description;

    public DBPConnectionType(String id, String name, RGB rgb, String description)
    {
        this.id = id;
        this.name = name;
        this.rgb = rgb;
        this.color = DBeaverCore.getInstance().getSharedTextColors().getColor(rgb);
        this.description = description;
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
        this.rgb = color.getRGB();
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
