/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Clipboard data
 */
public class ClipboardData
{
    private final Map<Transfer, Object> formats = new IdentityHashMap<>();

    public ClipboardData() {

    }

    public boolean hasData() {
        return !formats.isEmpty();
    }

    public void addTransfer(Transfer transfer, Object data) {
        formats.put(transfer, data);
    }

    public boolean hasTransfer(Transfer transfer) {
        return formats.containsKey(transfer);
    }

    public void pushToClipboard(Display display) {
        final int size = formats.size();
        final Transfer[] transfers = formats.keySet().toArray(new Transfer[0]);
        final Object[] objects = formats.values().toArray(new Object[0]);

        Clipboard clipboard = new Clipboard(display);
        try {
            clipboard.setContents(objects, transfers);
        } finally {
            clipboard.dispose();
        }
    }

}