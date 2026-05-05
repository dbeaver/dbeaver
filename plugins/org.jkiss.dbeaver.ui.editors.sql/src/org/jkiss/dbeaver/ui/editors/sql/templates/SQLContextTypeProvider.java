/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids: sdavids@gmx.de - see bug 25376
 *******************************************************************************/
package org.jkiss.dbeaver.ui.editors.sql.templates;


import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;

public class SQLContextTypeProvider extends SQLContextTypeAbstract {

    public SQLContextTypeProvider(DBPDataSourceProviderDescriptor provider)
    {
        super(getTypeId(provider.getId()), provider.getName());
    }

    public static String getTypeId(String providerID)
    {
        return SQLContextTypeBase.ID_SQL + "_"  + providerID;
    }
}
