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

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.utils.RuntimeUtils;

public abstract class CenteredImageLabelProvider extends OwnerDrawLabelProvider {
    @Override
    protected void measure(Event event, Object element) {
        if (RuntimeUtils.isWindows()) {
            // On Windows, if a partially hidden label in any list view mode
            // lacks tooltip text, the list-view control will unfold the label,
            // which is not desired for an image-only cell. This is controlled
            // using the LVS_EX_LABELTIP flag, set by SWT's Table#createHandle.
            // Setting bounds to zero seems to prevent this.

            event.x = 0;
            event.y = 0;
            event.width = 0;
            event.height = 0;
        }
    }

    @Override
    protected void paint(Event event, Object element) {
        Image image = getImage(element);
        if (image == null) {
            return;
        }

        Rectangle imageBounds = image.getBounds();
        Rectangle itemBounds = switch (event.item) {
            case TreeItem i -> i.getBounds(event.index);
            case TableItem i -> i.getBounds(event.index);
            default -> throw new IllegalArgumentException(String.valueOf(event.item));
        };

        int x = itemBounds.x + (itemBounds.width - imageBounds.width) / 2;
        int y = itemBounds.y + (itemBounds.height - imageBounds.height) / 2;

        if (event.index == 0 && itemBounds.x > 0 && RuntimeUtils.isWindows()) {
            // On Windows, incorrect bounds are returned for the first column.
            // Not sure if this semantic is completely correct, though...

            x -= itemBounds.x / 2;
        }

        event.gc.drawImage(image, x, y);
    }

    @Override
    protected void erase(Event event, Object element) {
        // do nothing
    }

    @Nullable
    protected abstract Image getImage(Object element);
}
