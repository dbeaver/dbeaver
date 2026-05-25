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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.jkiss.code.NotNull;

final class TitledCompositeLayout extends Layout {
    private static final int INDENT = 7;
    private static final int SPACING = 3;

    @NotNull
    @Override
    protected Point computeSize(@NotNull Composite composite, int wHint, int hHint, boolean flushCache) {
        var tc = (TitledComposite) composite;
        var size = tc.title.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
        if (tc.client != null) {
            Point preferredSize = tc.client.computeSize(wHint, hHint, flushCache);
            size.x = Math.max(size.x, preferredSize.x);
            size.y += preferredSize.y;

            size.x += INDENT;
            size.y += SPACING;
        }
        return size;
    }

    @Override
    protected void layout(@NotNull Composite composite, boolean flushCache) {
        var tc = (TitledComposite) composite;
        var size = tc.title.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);

        tc.title.setBounds(0, 0, size.x, size.y);

        if (tc.client != null) {
            var client = tc.getClientArea();

            tc.client.setBounds(
                client.x + INDENT,
                client.y + size.y + SPACING,
                client.width - INDENT,
                client.height - size.y - SPACING
            );
        }
    }
}
