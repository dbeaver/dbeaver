package org.jkiss.dbeaver.ext.hana.model.data;

import java.sql.SQLException;
import java.sql.Types;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;

public class HANARealVectorValueHandler extends HANAVectorValueHandler {

    public static final HANARealVectorValueHandler INSTANCE = new HANARealVectorValueHandler();

    @Override
    protected void bindVectorParameter(JDBCPreparedStatement statement, int paramIndex, JDBCCollection collection)
            throws DBCException, SQLException {
        if (collection.getComponentType().getTypeID() != Types.REAL) {
            throw new DBCException("Only REAL numbers are allowed in REAL_VECTOR");
        }
        float[] nvals = new float[collection.size()];
        for (int i = 0; i < nvals.length; ++i) {
            Float val = (Float) collection.get(i);
            if (val == null) {
                throw new DBCException("NULL elements are not allowed in REAL_VECTOR");
            }
            nvals[i] = val;
        }
        statement.setObject(paramIndex, nvals);
    }
}
