/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.DBPNamedObjectQualified;

public class NavigatorHandlerCopyFullName extends NavigatorHandlerCopyAbstract {

    @Override
    protected String getObjectDisplayString(Object object)
    {
        Object adapted = Platform.getAdapterManager().getAdapter(object, DBPNamedObjectQualified.class);
        if (adapted instanceof DBPNamedObjectQualified) {
            return ((DBPNamedObjectQualified)adapted).getFullQualifiedName();
        } else {
            return null;
        }
    }
}