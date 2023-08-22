package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBObjectValidateAction extends YashanDBObjectPersistAction {

    private final YashanDBSourceObject object;

    public YashanDBObjectValidateAction(YashanDBSourceObject object, YashanDBObjectType objectType, String title, String script) {
        super(objectType, title, script);
        this.object = object;
    }

    @Override
    public void afterExecute(DBCSession session, Throwable error) throws DBCException {
        if (error != null) {
            return;
        }
    }
}
