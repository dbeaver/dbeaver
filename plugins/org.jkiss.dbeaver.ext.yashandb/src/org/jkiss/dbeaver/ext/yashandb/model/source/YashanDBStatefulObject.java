package org.jkiss.dbeaver.ext.yashandb.model.source;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public interface YashanDBStatefulObject extends DBSObject, DBPStatefulObject {
    @NotNull
    @Override
    YashanDBDataSource getDataSource();

    @Nullable
    YashanDBSchema getSchema();
}
