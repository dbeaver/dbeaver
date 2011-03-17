/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.utils.ViewUtils;

public class NavigatorHandlerCopyObject extends NavigatorHandlerCopyAbstract {

    @Override
    protected String getObjectDisplayString(Object object)
    {
        Object adapted = Platform.getAdapterManager().getAdapter(object, DBPNamedObject.class);
        if (adapted != null) {
            return ViewUtils.convertObjectToString(adapted);
        } else {
            return null;
        }
    }
}
