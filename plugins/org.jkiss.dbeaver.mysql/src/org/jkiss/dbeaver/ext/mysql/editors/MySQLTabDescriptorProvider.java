/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.*;
import org.jkiss.dbeaver.ext.mysql.model.MySQLView;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.properties.tabbed.StandardPropertiesSection;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertiesContributor;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertyTabDescriptor;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * PropertyTabDescriptorProvider
 */
public class MySQLTabDescriptorProvider implements ITabDescriptorProvider {

    //static final Log log = LogFactory.getLog(MySQLTabDescriptorProvider.class);

    public ITabDescriptor[] getTabDescriptors(IWorkbenchPart part, ISelection selection)
    {
        ITabDescriptor extraTab = null;
        final DBNNode node = ViewUtils.getSelectedNode(selection);
        if (node instanceof DBNDatabaseNode) {
            DBSObject object = ((DBNDatabaseNode) node).getObject();
            if (object instanceof MySQLView) {

            }
        }
        if (extraTab == null) {
            return new ITabDescriptor[0];
        } else {
            return new ITabDescriptor[] { extraTab };
        }
    }

    private void makeStandardPropertiesTabs(List<ITabDescriptor> tabList)
    {
        List<ISectionDescriptor> standardSections = new ArrayList<ISectionDescriptor>();
        standardSections.add(new AbstractSectionDescriptor() {
            public String getId()
            {
                return PropertiesContributor.SECTION_STANDARD;
            }

            public ISection getSectionClass()
            {
                return new StandardPropertiesSection();
            }

            public String getTargetTab()
            {
                return PropertiesContributor.TAB_STANDARD;
            }

            @Override
            public boolean appliesTo(IWorkbenchPart part, ISelection selection)
            {
                return true;
            }
        });
        tabList.add(new PropertyTabDescriptor(
            PropertiesContributor.CATEGORY_INFO,
            PropertiesContributor.TAB_STANDARD,
            "Information",
            DBIcon.TREE_INFO.getImage(),
            standardSections));
    }


}
