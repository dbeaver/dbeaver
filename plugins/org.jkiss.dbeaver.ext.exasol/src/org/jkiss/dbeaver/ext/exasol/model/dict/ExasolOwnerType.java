package org.jkiss.dbeaver.ext.exasol.model.dict;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;

public enum ExasolOwnerType implements DBPNamedObject {
    S("System"),

    U("User");

    private String name;

    // -----------------
    // Constructor
    // -----------------
    private ExasolOwnerType(String name)
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