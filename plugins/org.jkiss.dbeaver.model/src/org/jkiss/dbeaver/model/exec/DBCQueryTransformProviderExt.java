package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.sql.SQLQuery;

public interface DBCQueryTransformProviderExt {

    boolean isForceTransform(DBCSession session, SQLQuery sqlQuery);
}
