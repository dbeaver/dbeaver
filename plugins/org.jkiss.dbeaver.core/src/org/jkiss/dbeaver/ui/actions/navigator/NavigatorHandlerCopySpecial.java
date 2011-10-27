/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.struct.DBSEntityQualified;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

public class NavigatorHandlerCopySpecial extends NavigatorHandlerCopyAbstract {

    @Override
    protected String getObjectDisplayString(Object object)
    {
        DBSEntityQualified adapted = RuntimeUtils.getObjectAdapter(object, DBSEntityQualified.class);
        if (adapted != null) {
            return adapted.getFullQualifiedName();
        } else {
            return null;
        }
    }

    @Override
    protected String getSelectionTitle(IStructuredSelection selection)
    {
        return (selection.size() > 1 ?
                CoreMessages.actions_navigator_copy_fqn_title :
                CoreMessages.actions_navigator_copy_fqn_titles);
    }

}