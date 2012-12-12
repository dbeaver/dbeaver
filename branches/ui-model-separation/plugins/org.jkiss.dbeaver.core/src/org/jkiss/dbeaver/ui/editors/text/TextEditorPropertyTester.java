/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.model.DBPCommentsManager;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.utils.CommonUtils;

/**
 * DatabaseEditorPropertyTester
 */
public class TextEditorPropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.ui.editors.text";
    public static final String PROP_CAN_COMMENT = "canComment";

    public TextEditorPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof BaseTextEditor)) {
            return false;
        }
        BaseTextEditor editor = (BaseTextEditor)receiver;
        if (property.equals(PROP_CAN_COMMENT)) {
            if (editor.getSelectionProvider().getSelection().isEmpty()) {
                return false;
            }
            DBPCommentsManager commentsSupport = editor.getCommentsSupport();
            if (commentsSupport == null) {
                return false;
            }
            if ("single".equals(expectedValue)) {
                return !CommonUtils.isEmpty(commentsSupport.getSingleLineComments());
            } else if ("multi".equals(expectedValue)) {
                return commentsSupport.getMultiLineComments() != null;
            }
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}