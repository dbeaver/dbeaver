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

package org.jkiss.dbeaver.ui.editors.binary;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.data.IHexEditorService;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Hex editor service implementation
 */
public class HexEditorService implements IHexEditorService {

    private static final Log log = Log.getLog(HexEditorService.class);

    @Override
    public Control createHexControl(Composite parent, boolean readOnly) {
        HexEditControl hexEditControl = new HexEditControl(parent, readOnly ? SWT.READ_ONLY : SWT.NONE, 6, 8);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 200;
        gd.minimumWidth = hexEditControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        hexEditControl.setLayoutData(gd);
        return hexEditControl;
    }

    @Override
    public byte[] getHexContent(Control control) {
        HexEditControl hexEditControl = (HexEditControl) control;
        BinaryContent content = hexEditControl.getContent();
        ByteBuffer buffer = ByteBuffer.allocate((int) content.length());
        try {
            content.get(buffer, 0);
        } catch (IOException e) {
            log.error(e);
        }
        return buffer.array();
    }

    @Override
    public void setHexContent(Control control, byte[] bytes, String charset) {
        HexEditControl hexEditControl = (HexEditControl) control;
        hexEditControl.setContent(bytes, charset);
    }
}
