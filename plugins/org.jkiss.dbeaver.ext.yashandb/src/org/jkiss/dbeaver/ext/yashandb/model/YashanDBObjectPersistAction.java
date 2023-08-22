package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBObjectPersistAction extends SQLDatabasePersistAction {

    private final YashanDBObjectType objectType;

    public YashanDBObjectPersistAction(YashanDBObjectType objectType, String title, String script) {
        super(title, script);
        this.objectType = objectType;
    }

    public YashanDBObjectPersistAction(YashanDBObjectType objectType, String script) {
        super(script);
        this.objectType = objectType;
    }

    public YashanDBObjectType getObjectType() {
        return objectType;
    }

}
