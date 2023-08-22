package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBSchemaObject extends YashanDBObject<YashanDBSchema> implements DBPQualifiedObject {
    protected YashanDBSchemaObject(
            YashanDBSchema schema,
            String name,
            boolean persisted) {
        super(schema, name, persisted);
    }

    public YashanDBSchema getSchema() {
        return getParentObject();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
                getParentObject(),
                this);
    }
}
