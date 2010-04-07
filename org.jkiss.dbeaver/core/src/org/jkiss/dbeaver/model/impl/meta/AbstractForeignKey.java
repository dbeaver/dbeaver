package org.jkiss.dbeaver.model.impl.meta;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.struct.*;

/**
 * AbstractForeignKey
 */
public abstract class AbstractForeignKey<
    DATASOURCE extends DBPDataSource,
    CONTAINER extends DBSStructureContainer<DATASOURCE>,
    TABLE extends DBSTable<DATASOURCE, CONTAINER>,
    PRIMARY_KEY extends DBSConstraint<DATASOURCE, TABLE>>
    extends AbstractConstraint<DATASOURCE, CONTAINER, TABLE>
    implements DBSForeignKey<DATASOURCE, TABLE>
{
    private PRIMARY_KEY referencedKey;
    private DBSConstraintCascade deleteRule;
    private DBSConstraintCascade updateRule;

    public AbstractForeignKey(
        TABLE table,
        String name,
        String description,
        PRIMARY_KEY referencedKey,
        DBSConstraintCascade deleteRule,
        DBSConstraintCascade updateRule)
    {
        super(table, name, description);
        this.referencedKey = referencedKey;
        this.deleteRule = deleteRule;
        this.updateRule = updateRule;
    }

    public DBSConstraintType getConstraintType()
    {
        return DBSConstraintType.FOREIGN_KEY;
    }

    @Property(name = "Ref Table", viewable = true, order = 3)
    public TABLE getReferencedTable()
    {
        return referencedKey.getTable();
    }

    @Property(name = "Ref Constraint", viewable = true, order = 4)
    public PRIMARY_KEY getReferencedKey()
    {
        return referencedKey;
    }

    @Property(name = "On Delete", viewable = true, order = 5)
    public DBSConstraintCascade getDeleteRule()
    {
        return deleteRule;
    }

    @Property(name = "On Update", viewable = true, order = 6)
    public DBSConstraintCascade getUpdateRule()
    {
        return updateRule;
    }

}