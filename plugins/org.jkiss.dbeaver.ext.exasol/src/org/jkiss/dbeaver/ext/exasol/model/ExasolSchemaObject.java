package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;

public class ExasolSchemaObject extends ExasolObject<ExasolSchema> implements DBPQualifiedObject {

	public ExasolSchemaObject(ExasolSchema schema, String name, boolean persisted) {
		super(schema, name, persisted);
	}


	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getParentObject(),this);
	}

}
