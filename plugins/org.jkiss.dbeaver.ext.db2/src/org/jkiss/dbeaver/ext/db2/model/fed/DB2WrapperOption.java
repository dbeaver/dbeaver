/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.db2.model.fed;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.db2.model.DB2Object;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * DB2 Federated Wrapper Option
 * 
 * @author Denis Forveille
 */
public class DB2WrapperOption extends DB2Object<DB2Wrapper> {

    private final DB2Wrapper wrapper;

    private String setting;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2WrapperOption(DB2Wrapper wrapper, ResultSet dbResult)
    {
        super(wrapper, JDBCUtils.safeGetString(dbResult, "OPTION"), true);

        this.wrapper = wrapper;
        this.setting = JDBCUtils.safeGetString(dbResult, "SETTING");
    }

    public DB2Wrapper getWrapper()
    {
        return wrapper;
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, editable = false, order = 2)
    public String getSetting()
    {
        return setting;
    }

}
