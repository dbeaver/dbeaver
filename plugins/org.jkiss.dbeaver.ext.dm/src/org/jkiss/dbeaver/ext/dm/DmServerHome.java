package org.jkiss.dbeaver.ext.dm;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.LocalNativeClientLocation;

public class DmServerHome extends LocalNativeClientLocation{
	
	private static final Log log = Log.getLog(DmServerHome.class);
	
    private String name;

    DmServerHome(String path, String name)
    {
        super(path, path);
        this.name = name == null ? path : name;
    }

    @Override
    public String getDisplayName()
    {
        return name;
    }

}
