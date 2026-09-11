/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentChars;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.*;

public class ContentInlineEditorTest extends DBeaverUnitTest {
    @Test
    public void extractionLeavesTheControllerValueUnmodified() throws DBException {
        JDBCContentChars original = new JDBCContentChars(mock(DBCExecutionContext.class), "original");
        IValueController controller = mock(IValueController.class);
        when(controller.getValue()).thenReturn(original);
        ContentInlineEditor editor = new ContentInlineEditor(controller);
        editor.control = mock(Text.class);
        when(editor.control.getText()).thenReturn("edited");
        DBDContent edited = (DBDContent) editor.extractEditorValue();
        try {
            assertNotSame(original, edited);
            assertEquals("original", original.getRawValue());
            assertEquals("edited", edited.getRawValue());
            verify(controller, never()).updateValue(any(), anyBoolean());
        } finally {
            edited.release();
            original.release();
        }
    }
}
