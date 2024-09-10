package org.jkiss.dbeaver.ext.gbase8s.model;

import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericPackage;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

public class GBase8sProcedure extends GenericProcedure {

	public GBase8sProcedure(GenericStructContainer container, String procedureName, String specificName,
			String description, DBSProcedureType procedureType, GenericFunctionResultType functionResultType) {
		super(container, procedureName, specificName, description, procedureType, functionResultType);
	}

	@Property(hidden = true, order = 3)
	@Override
	public GenericCatalog getCatalog() {
		return getContainer().getCatalog();
	}

	@Property(hidden = true, order = 5)
	@Override
	public GenericPackage getPackage() {
		return getContainer() instanceof GenericPackage ? (GenericPackage) getContainer() : null;
	}
}
