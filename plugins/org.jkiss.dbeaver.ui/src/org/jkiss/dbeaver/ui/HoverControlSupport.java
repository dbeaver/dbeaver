/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;

import java.util.function.BiConsumer;

/**
 * Allows creation of a toolbar-like controls over
 * a control that are shown when hovering over it.
 *
 * @param <T> type of the target control
 */
public abstract class HoverControlSupport<T extends Control> {
    private final T target;
    private Control overlay;
    private boolean hovering;

    public HoverControlSupport(@NotNull T target) {
        this.target = target;

        Display display = target.getDisplay();
        Listener filter = event -> {
            boolean nowHovering = isHoveringTarget(event);
            if (nowHovering != hovering) {
                hovering = nowHovering;
                onHoverChange(nowHovering);
            }
        };

        display.addFilter(SWT.MouseMove, filter);
        target.addDisposeListener(e -> {
            onHoverChange(false);
            display.removeFilter(SWT.MouseMove, filter);
        });
    }

    /**
     * A convenient helper that implements {@link #createControl(Composite, Control)}.
     *
     * @param target  target control to show the overlay over when hovered
     * @param creator a consumer resembling {@link #createControl(Composite, Control)}
     * @param <T>     type of the target control
     */
    public static <T extends Control> void install(@NotNull T target, @NotNull BiConsumer<Composite, T> creator) {
        new HoverControlSupport<>(target) {
            @Override
            protected void createControl(@NotNull Composite parent, @NotNull T target) {
                creator.accept(parent, target);
            }
        };
    }

    /**
     * Creates controls that will be shown in the overlay.
     * <p>
     * This method is called every time the overlay is shown, so it shouldn't do
     * any heavy work that might result cause the UI to freeze.
     *
     * @param parent parent composite
     * @param target target control
     */
    protected abstract void createControl(@NotNull Composite parent, @NotNull T target);

    /**
     * Disposes of controls created in {@link #createControl(Composite, Control)}.
     * <p>
     * The default implementation does nothing.
     */
    protected void dispose() {
        // do nothing
    }

    /**
     * Creates a control that represents the overlay.
     * <p>
     * When overriding this method, you must also call {@link #createControl(Composite, Control)}.
     *
     * @param parent parent composite
     * @param target target control
     * @return control that represents the overlay
     */
    @NotNull
    protected Composite createOverlayControl(@NotNull Composite parent, @NotNull T target) {
        Composite holder = new Composite(parent, SWT.NONE);
        holder.setLayout(RowLayoutFactory.fillDefaults().margins(1, 1).spacing(5).wrap(false).create());
        new CompositeBorderPainter(holder);

        createControl(holder, target);

        return holder;
    }

    /**
     * Picks a location within the {@code target}'s coordinate space.
     *
     * @param target target control
     * @param size   computed size of the overlay
     * @return location where to show the overlay
     */
    @NotNull
    protected Point pickOverlayLocation(@NotNull T target, @NotNull Point size) {
        Control origin = pickOverlayControl(target);

        Rectangle bounds = origin.getBounds();
        Point location = target.getShell().toControl(origin.toDisplay(new Point(0, 0)));
        location.x += bounds.width - size.x - 5;
        location.y += bounds.height - size.y - 5;

        return location;
    }

    /**
     * Picks a target control, let it be a child of, or the {@code target} itself, to later
     * compute a location of the overlay relative to using {@link #pickOverlayLocation(Control, Point)}.
     * <p>
     * The default implementation simply returns the passed {@code target}.
     *
     * @param target target control
     * @return target control for overlay location computation
     */
    @NotNull
    protected Control pickOverlayControl(@NotNull T target) {
        return target;
    }

    private void onHoverChange(boolean hovering) {
        if (overlay != null) {
            dispose();
            overlay.dispose();
            overlay = null;
        }

        if (!hovering) {
            return;
        }

        Composite holder = createOverlayControl(target.getShell(), target);

        Point size = holder.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point location = pickOverlayLocation(target, size);

        holder.setSize(size);
        holder.setLocation(location);
        holder.moveAbove(null);

        overlay = holder;
    }

    private boolean isHoveringTarget(@NotNull Event event) {
        if (target.isDisposed() || !target.isVisible()) {
            return false;
        }

        if (!(event.widget instanceof Control control) || control.isDisposed()) {
            return false;
        }

        var targetLocation = target.toDisplay(0, 0);
        var targetSize = target.getSize();
        var targetBounds = new Rectangle(targetLocation.x, targetLocation.y, targetSize.x, targetSize.y);
        var eventLocation = control.toDisplay(event.x, event.y);

        // Are we within the target's bounds?
        if (!targetBounds.contains(eventLocation)) {
            return false;
        }

        // Are we hovering the target or overlay; e.g., it's not obstructed by anything?
        return UIUtils.isParent(target, control) || UIUtils.isParent(overlay, control);
    }
}
