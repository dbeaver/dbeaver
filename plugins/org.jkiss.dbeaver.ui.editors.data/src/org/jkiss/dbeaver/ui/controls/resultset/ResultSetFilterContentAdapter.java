/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.ui.contentassist.StyledTextContentAdapter;

public class ResultSetFilterContentAdapter extends StyledTextContentAdapter {

    private final ResultSetViewer viewer;

    public ResultSetFilterContentAdapter(ResultSetViewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public void insertControlContents(Control control, String contents, int cursorPosition) {
        StyledText text = (StyledText) control;
        String curValue = text.getText().toUpperCase();
        Point selection = text.getSelection();

        if (selection.x == selection.y) {
            // Try to replace text under cursor contents starts with
            String contentsUC = contents.toUpperCase().trim();
            DBPDataSource dataSource = viewer.getDataSource();
            if (dataSource != null) {
                contentsUC = DBUtils.getUnQuotedIdentifier(dataSource, contentsUC);
            }
            for (int i = selection.x - 1; i >= 0; i--) {
                String prefix = curValue.substring(i, selection.x);
                if (contentsUC.startsWith(prefix)) {
                    if (i == 0 || !Character.isJavaIdentifierPart(curValue.charAt(i - 1))) {
                        text.setSelection(i, selection.x);
                        break;
                    }
                }
                char ch = Character.toUpperCase(curValue.charAt(i));
                if (!Character.isLetterOrDigit(ch) && contentsUC.indexOf(ch) == -1) {
                    // Work break
                    break;
                }
            }
        }
        text.insert(contents);

        // Insert will leave the cursor at the end of the inserted text. If this
        // is not what we wanted, reset the selection.
        if (cursorPosition <= contents.length()) {
            text.setSelection(selection.x + cursorPosition,
                selection.x + cursorPosition);
        }
    }

}
