package org.jkiss.dbeaver.ext.db2.model.fed;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DB2 Federated Nickname Remote Type
 * 
 * @author Denis Forveille
 */
public enum DB2NicknameRemoteType implements DBPNamedObject {

    A("Alias"),

    N("Nickname"),

    S("MQT"),

    T("Table"),

    V("View");

    private String name;

    // -----------
    // Constructor
    // -----------

    private DB2NicknameRemoteType(String name)
    {
        this.name = name;
    }

    // -----------------------
    // Display @Property Value
    // -----------------------
    @Override
    public String toString()
    {
        return name;
    }

    // ----------------
    // Standard Getters
    // ----------------

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }
}
