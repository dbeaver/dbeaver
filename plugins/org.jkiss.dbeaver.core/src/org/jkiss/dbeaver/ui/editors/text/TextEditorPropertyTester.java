/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ui.ICommentsSupport;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.utils.ArrayUtils;

/**
 * DatabaseEditorPropertyTester
 */
public class TextEditorPropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.ui.editors.text";
    public static final String PROP_AVAILABLE = "available";
    public static final String PROP_CAN_LOAD = "canLoad";
    public static final String PROP_CAN_SAVE = "canSave";
    public static final String PROP_CAN_COMMENT = "canComment";

    public TextEditorPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        BaseTextEditor editor = BaseTextEditor.getTextEditor((IEditorPart) receiver);
        if (editor == null) {
            return false;
        }
        switch (property) {
            case PROP_AVAILABLE:
            case PROP_CAN_SAVE:
                return true;
            case PROP_CAN_LOAD:
                return !editor.isReadOnly();
            case PROP_CAN_COMMENT:
                if (editor.getSelectionProvider() == null ||
                    editor.getSelectionProvider().getSelection() == null ||
                    editor.getSelectionProvider().getSelection().isEmpty()) {
                    return false;
                }
                ICommentsSupport commentsSupport = editor.getCommentsSupport();
                if (commentsSupport == null) {
                    return false;
                }
                if ("single".equals(expectedValue)) {
                    return !ArrayUtils.isEmpty(commentsSupport.getSingleLineComments());
                } else if ("multi".equals(expectedValue)) {
                    return commentsSupport.getMultiLineComments() != null;
                }
                break;
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}