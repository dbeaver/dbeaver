/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.oracle.editors.OracleProcedureBodySection;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedure;
import org.jkiss.dbeaver.model.edit.DBEObjectTabProvider;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertiesContributor;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertyTabDescriptor;
import org.jkiss.dbeaver.ui.properties.tabbed.SectionDescriptor;

/**
 * OracleProcedureManager
 */
public class OracleProcedureManager extends JDBCObjectManager<OracleProcedure> implements DBEObjectTabProvider<OracleProcedure> {

    public ITabDescriptor[] getTabDescriptors(IWorkbenchWindow workbenchWindow, final IDatabaseNodeEditor activeEditor, final OracleProcedure object)
    {
        return new ITabDescriptor[] {
            new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_INFO,
                "procedure.body",
                "Body",
                DBIcon.SOURCES.getImage(),
                new SectionDescriptor("default", "Body") {
                    public ISection getSectionClass()
                    {
                        return new OracleProcedureBodySection(activeEditor);
                    }
                })
        };
    }
}

