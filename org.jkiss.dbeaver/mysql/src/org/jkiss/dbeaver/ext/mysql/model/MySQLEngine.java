package org.jkiss.dbeaver.ext.mysql.model;

/**
 * MySQLEngine
 */
public enum MySQLEngine {
    MEMORY,
    InnoDB,
    MyISAM,
    BLACKHOLE,
    MRG_MYISAM,
    CSV,
    ARCHIVE,
    UNKNOWN;

    public static MySQLEngine getByName(String name)
    {
        for (MySQLEngine engine : values()) {
            if (engine.name().equalsIgnoreCase(name)) {
                return engine;
            }
        }
        return UNKNOWN;
    }

}
