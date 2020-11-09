package org.jkiss.dbeaver.ext.hive.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;


public class HiveIndex extends GenericTableIndex {
    String indexType;
    HiveTable indexTable;
    String description;

    protected HiveIndex(GenericTableBase table, String name, String description, String indexType, HiveTable indexTable) {
        super(table, true, "", 0, name, DBSIndexType.OTHER, true);
        this.description = description;
        this.indexType = indexType;
        this.indexTable = indexTable;
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    @Property(viewable = true, order = 6)
    public String getHiveIndexType() {
        return indexType;
    }

    @Property(viewable = true, order = 7)
    public HiveTable getIndexTable() {
        return indexTable;
    }
}
