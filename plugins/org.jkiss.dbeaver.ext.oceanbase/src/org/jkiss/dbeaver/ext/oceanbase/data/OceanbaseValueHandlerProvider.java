package org.jkiss.dbeaver.ext.oceanbase.data;

import java.util.Locale;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStandardValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

public class OceanbaseValueHandlerProvider extends JDBCStandardValueHandlerProvider{
	
	public OceanbaseValueHandlerProvider() {
		super();
	}
	
	@Nullable
    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDFormatSettings preferences, DBSTypedObject typedObject)
    {
		if(typedObject.getTypeName().toLowerCase(Locale.ENGLISH).equals("rowid")) {
			return OceanbaseRowIDValueHandler.INSTANCE;
		}
		return super.getValueHandler(dataSource, preferences, typedObject);
    }

}
