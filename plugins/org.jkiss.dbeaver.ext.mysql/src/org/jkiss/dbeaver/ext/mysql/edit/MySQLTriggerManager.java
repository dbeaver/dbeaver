/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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

package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.dbeaver.ext.mysql.model.MySQLTrigger;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;

/**
 * MySQLTriggerManager
 */
public class MySQLTriggerManager extends AbstractObjectManager<MySQLTrigger> {

/*
    @Override
    protected JDBCAbstractCache<MySQLCatalog, MySQLTrigger> getObjectsCache(MySQLTrigger object)
    {
        return object.getContainer().getProceduresCache();
    }
*/

/*
    public ITabDescriptor[] getTabDescriptors(IWorkbenchWindow workbenchWindow, final IDatabaseEditor activeEditor, final MySQLTrigger object)
    {
        return new ITabDescriptor[] {
            new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_INFO,
                "trigger.body",
                MySQLMessages.edit_procedure_manager_body,
                DBIcon.SOURCES.getImage(),
                new SectionDescriptor("default", MySQLMessages.edit_procedure_manager_body) {
                    public ISection getSectionClass()
                    {
                        return new MySQLTriggerBodySection(activeEditor);
                    }
                })
        };
    }
*/

}

