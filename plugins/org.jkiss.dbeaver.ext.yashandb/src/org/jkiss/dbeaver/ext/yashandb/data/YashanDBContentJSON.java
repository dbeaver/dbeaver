package org.jkiss.dbeaver.ext.yashandb.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentChars;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;

public class YashanDBContentJSON extends JDBCContentChars {

    public YashanDBContentJSON(DBCExecutionContext executionContext, String json)
    {
        super(executionContext, json);
    }

    private YashanDBContentJSON(YashanDBContentJSON copyFrom) {
        super(copyFrom);
    }

    @NotNull
    @Override
    public String getContentType()
    {
        return MimeTypes.TEXT_JSON;
    }

    @Override
    public void bindParameter(
            JDBCSession session,
            JDBCPreparedStatement preparedStatement,
            DBSTypedObject columnType,
            int paramIndex)
            throws DBCException
    {
        try {
            if (data != null) {
                preparedStatement.setObject(paramIndex, data, Types.OTHER);
            } else {
                preparedStatement.setNull(paramIndex, columnType.getTypeID());
            }
        }
        catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Override
    public String getDisplayString(DBDDisplayFormat format) {
        return data == null ? null :
                (format == DBDDisplayFormat.EDIT ? data : CommonUtils.compactWhiteSpaces(data));
    }

    @Override
    public YashanDBContentJSON cloneValue(DBRProgressMonitor monitor)
    {
        return new YashanDBContentJSON(this);
    }
}
