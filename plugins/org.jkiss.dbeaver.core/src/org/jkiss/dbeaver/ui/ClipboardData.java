/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
        final Transfer[] transfers = formats.keySet().toArray(new Transfer[size]);
        final Object[] objects = formats.values().toArray(new Object[size]);

        Clipboard clipboard = new Clipboard(display);
        try {
            clipboard.setContents(objects, transfers);
        } finally {
            clipboard.dispose();
        }
    }

}