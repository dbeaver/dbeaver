/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ICommentsSupport;
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