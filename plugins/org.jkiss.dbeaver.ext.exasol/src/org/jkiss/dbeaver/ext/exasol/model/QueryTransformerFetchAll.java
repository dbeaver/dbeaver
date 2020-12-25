package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.sql.SQLQuery;

import java.sql.SQLException;
import java.sql.Statement;

public class QueryTransformerFetchAll implements DBCQueryTransformer {

	@Override
	public void setParameters(Object... parameters) {
	}

	@Override
	public String transformQueryString(SQLQuery query) throws DBCException {
		return query.getText();
	}

	@Override
	public void transformStatement(DBCStatement statement, int parameterIndex) throws DBCException {
		try {
            ((Statement)statement).setFetchSize(2000);
		} catch (SQLException e) {
			throw new DBCException(e, statement.getSession().getExecutionContext());
		}
	}

}
