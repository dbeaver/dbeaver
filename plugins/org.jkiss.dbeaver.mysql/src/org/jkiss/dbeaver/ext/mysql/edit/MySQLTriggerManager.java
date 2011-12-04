/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.editors.MySQLTriggerBodySection;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTrigger;
import org.jkiss.dbeaver.model.edit.DBEObjectTabProvider;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertiesContributor;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertyTabDescriptor;
import org.jkiss.dbeaver.ui.properties.tabbed.SectionDescriptor;

/**
 * MySQLTriggerManager
 */
public class MySQLTriggerManager extends JDBCObjectManager<MySQLTrigger> implements DBEObjectTabProvider<MySQLTrigger> {

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
}

