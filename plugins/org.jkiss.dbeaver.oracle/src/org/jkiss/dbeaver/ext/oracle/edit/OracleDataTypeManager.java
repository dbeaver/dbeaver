/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.oracle.editors.OracleSourceViewSection;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataType;
import org.jkiss.dbeaver.model.edit.DBEObjectTabProvider;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertiesContributor;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertyTabDescriptor;
import org.jkiss.dbeaver.ui.properties.tabbed.SectionDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * OracleDataTypeManager
 */
public class OracleDataTypeManager extends JDBCObjectManager<OracleDataType> implements DBEObjectTabProvider<OracleDataType> {

    public ITabDescriptor[] getTabDescriptors(IWorkbenchWindow workbenchWindow, final IDatabaseNodeEditor activeEditor, final OracleDataType object)
    {
        List<ITabDescriptor> tabs = new ArrayList<ITabDescriptor>();
        tabs.add(
            new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_INFO,
                "type.declaration",
                "Declaration",
                DBIcon.SOURCES.getImage(),
                new SectionDescriptor("default", "Declaration") {
                    public ISection getSectionClass()
                    {
                        return new OracleSourceViewSection(activeEditor, false);
                    }
                }));

        if (object.hasMethods()) {
            tabs.add(new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_INFO,
                "type.definition",
                "Definition",
                DBIcon.SOURCES.getImage(),
                new SectionDescriptor("default", "Definition") {
                    public ISection getSectionClass()
                    {
                        return new OracleSourceViewSection(activeEditor, true);
                    }
                }));
        }
        return tabs.toArray(new ITabDescriptor[tabs.size()]);
    }
}

