/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

