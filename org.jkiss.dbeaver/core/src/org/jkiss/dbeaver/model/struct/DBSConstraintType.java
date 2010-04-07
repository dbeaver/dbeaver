package org.jkiss.dbeaver.model.struct;

/**
 * DBSConstraintType
 */
public enum DBSConstraintType
{
    FOREIGN_KEY(false),
    PRIMARY_KEY(true),
    UNIQUE_KEY(true),
    CHECK(false),
    NOT_NULL(false);

    private final boolean unique;

    DBSConstraintType(boolean unique)
    {
        this.unique = unique;
    }

    public boolean isUnique()
    {
        return unique;
    }
}