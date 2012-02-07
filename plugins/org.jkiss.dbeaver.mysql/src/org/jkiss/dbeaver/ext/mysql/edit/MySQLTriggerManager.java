/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.dbeaver.ext.mysql.model.MySQLTrigger;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;

/**
 * MySQLTriggerManager
 */
public class MySQLTriggerManager extends JDBCObjectManager<MySQLTrigger> {

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

