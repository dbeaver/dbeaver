package org.jkiss.dbeaver.ext.db2.model.fed;

import org.jkiss.dbeaver.model.DBPNamedObject;

public enum DB2WrapperType implements DBPNamedObject {

    N("Non Relational"),

    R("Relational");

    private String name;

    // -----------
    // Constructor
    // -----------

    private DB2WrapperType(String name)
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
