/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.dbeaver.ext.oracle.model.OracleDataType;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;

/**
 * OracleDataTypeManager
 */
public class OracleDataTypeManager extends JDBCObjectManager<OracleDataType> {

/*
    public ITabDescriptor[] getTabDescriptors(IWorkbenchWindow workbenchWindow, final IDatabaseNodeEditor activeEditor, final OracleDataType object)
    {
        List<ITabDescriptor> tabs = new ArrayList<ITabDescriptor>();
        if (!object.isPredefined() || object.hasAttributes() || object.hasMethods()) {
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
        }

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
*/
}

