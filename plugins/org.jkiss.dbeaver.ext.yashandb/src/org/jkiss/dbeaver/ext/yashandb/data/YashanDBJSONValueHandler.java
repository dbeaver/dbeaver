package org.jkiss.dbeaver.ext.yashandb.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;

public class YashanDBJSONValueHandler extends JDBCContentValueHandler {

    private static final Log log = Log.getLog(YashanDBJSONValueHandler.class);

    public static final YashanDBJSONValueHandler INSTANCE = new YashanDBJSONValueHandler();

    public static final String YASHANDB_OBJECT_CLASS = "com.yashandb.util.YasObject";

    @Override
    protected DBDContent fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws SQLException {
        String json = resultSet.getString(index);
        return new YashanDBContentJSON(session.getExecutionContext(), json);
    }

    @Override
    public DBDContent getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (object != null && object.getClass().getName().equals(YASHANDB_OBJECT_CLASS)){
            object = extractYashanDBObjectValue(object);
        }

        if (object == null) {
            return new YashanDBContentJSON(session.getExecutionContext(), null);
        } else if (object instanceof YashanDBContentJSON) {
            return copy ? ((YashanDBContentJSON) object).cloneValue(session.getProgressMonitor()) : (YashanDBContentJSON) object;
        } else if (object instanceof String) {
            return new YashanDBContentJSON(session.getExecutionContext(), (String) object);
        }
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    private Object extractYashanDBObjectValue(Object YashanDBObject) {
        try {
            return YashanDBObject.getClass().getMethod("getValue").invoke(YashanDBObject);
        } catch (Exception e) {
            log.debug("Can't extract value from " + YashanDBObject.getClass().getName(), e);
        }
        return null;
    }

}
