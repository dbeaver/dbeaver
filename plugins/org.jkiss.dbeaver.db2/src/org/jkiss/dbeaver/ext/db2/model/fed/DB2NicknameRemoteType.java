package org.jkiss.dbeaver.ext.db2.model.fed;

import org.jkiss.dbeaver.model.DBPNamedObject;

public enum DB2NicknameRemoteType implements DBPNamedObject {

    A("A (Alias"),

    N("N (Nickname)"),

    S("S (MQT)"),

    T("T (Table"),

    V("V (View)");

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

    @Override
    public String getName()
    {
        return name;
    }
}
