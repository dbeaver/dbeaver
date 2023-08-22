package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.dbeaver.model.struct.rdb.DBSTable;

import java.sql.ResultSet;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBSchemaTrigger extends YashanDBTrigger<YashanDBSchema> {
    public YashanDBSchemaTrigger(YashanDBSchema schema, String name) {
        super(schema, name);
    }

    public YashanDBSchemaTrigger(
            YashanDBSchema schema,
            ResultSet dbResult) {
        super(schema, dbResult);
    }


    @Override
    public DBSTable getTable() {
        return null;
    }

    @Override
    public YashanDBSchema getSchema() {
        return parent;
    }
}
