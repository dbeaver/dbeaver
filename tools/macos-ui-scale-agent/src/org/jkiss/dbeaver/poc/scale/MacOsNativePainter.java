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
package org.jkiss.dbeaver.poc.scale;

import org.eclipse.swt.internal.cocoa.NSAffineTransform;
import org.eclipse.swt.internal.cocoa.NSBitmapImageRep;
import org.eclipse.swt.internal.cocoa.NSColor;
import org.eclipse.swt.internal.cocoa.NSGraphicsContext;
import org.eclipse.swt.internal.cocoa.NSImage;
import org.eclipse.swt.internal.cocoa.NSRect;
import org.eclipse.swt.internal.cocoa.NSResponder;
import org.eclipse.swt.internal.cocoa.NSSearchField;
import org.eclipse.swt.internal.cocoa.NSSearchFieldCell;
import org.eclipse.swt.internal.cocoa.NSSize;
import org.eclipse.swt.internal.cocoa.NSString;
import org.eclipse.swt.internal.cocoa.NSText;
import org.eclipse.swt.internal.cocoa.NSView;
import org.eclipse.swt.internal.cocoa.OS;
import org.jkiss.code.NotNull;

final class MacOsNativePainter {
    private static final int NATIVE_SCALE_RASTER_DENSITY = 4;
    private static final long NATIVE_SCALE_MAX_PIXELS = 16_777_216;
    private static final ThreadLocal<Boolean> nativeScalePainting = new ThreadLocal<>();

    private MacOsNativePainter() {
    }

    static boolean nativeScalePaint(long viewId, @NotNull NSRect dirty) {
        double factor = Double.parseDouble(System.getProperty("dbeaver.ui.nativeScale.factor", "1"));
        if (factor == Math.rint(factor) || Boolean.TRUE.equals(nativeScalePainting.get())) {
            return false;
        }
        NSView view = new NSView(viewId);
        if (!view.isKindOfClass(OS.class_NSControl)) {
            return false;
        }
        NSRect visible = view.visibleRect();
        double right = Math.min(visible.x + visible.width, dirty.x + dirty.width);
        double bottom = Math.min(visible.y + visible.height, dirty.y + dirty.height);
        visible.x = Math.floor(Math.max(visible.x, dirty.x));
        visible.y = Math.floor(Math.max(visible.y, dirty.y));
        visible.width = Math.ceil(right) - visible.x;
        visible.height = Math.ceil(bottom) - visible.y;
        long width = (long) visible.width * NATIVE_SCALE_RASTER_DENSITY;
        long height = (long) visible.height * NATIVE_SCALE_RASTER_DENSITY;
        if (width <= 0 || height <= 0 || width * height > NATIVE_SCALE_MAX_PIXELS) {
            return false;
        }
        NSBitmapImageRep bitmap = (NSBitmapImageRep) new NSBitmapImageRep().alloc();
        bitmap = bitmap.initWithBitmapDataPlanes(0, width, height, 8, 4, true, false, OS.NSDeviceRGBColorSpace, 0, 0, 0);
        if (bitmap == null) {
            return false;
        }
        NSGraphicsContext context = NSGraphicsContext.currentContext();
        NSImage image = null;
        nativeScalePainting.set(true);
        context.saveGraphicsState();
        try {
            NSSize size = new NSSize();
            size.width = visible.width;
            size.height = visible.height;
            bitmap.setSize(size);
            // native theme artwork needs an integer backing scale before the view transform is applied
            if (view.isKindOfClass(OS.class_NSSearchField)) {
                nativeScalePaintSearch(new NSSearchField(view.id), bitmap, visible);
            } else {
                view.cacheDisplayInRect(visible, bitmap);
            }
            NSGraphicsContext.setCurrentContext(context);
            image = (NSImage) new NSImage().alloc();
            image.initWithSize(size);
            image.addRepresentation(bitmap);
            NSAffineTransform transform = NSAffineTransform.transform();
            transform.translateXBy(0, 2 * visible.y + visible.height);
            transform.scaleXBy(1, -1);
            transform.concat();
            NSRect source = new NSRect();
            source.width = visible.width;
            source.height = visible.height;
            context.setImageInterpolation(OS.NSImageInterpolationHigh);
            image.drawInRect(visible, source, OS.NSCompositingOperationSourceOver, 1);
            return true;
        } finally {
            NSGraphicsContext.setCurrentContext(context);
            context.restoreGraphicsState();
            nativeScalePainting.remove();
            if (image != null) {
                image.release();
            }
            bitmap.release();
        }
    }

    private static void nativeScalePaintSearch(
        @NotNull NSSearchField original, @NotNull NSBitmapImageRep bitmap, @NotNull NSRect visible
    ) {
        NSSearchField proxy = (NSSearchField) new NSSearchField().alloc();
        proxy.initWithFrame(original.bounds());
        // SWT globally replaces NSSearchField.cellClass; the offscreen field needs the stock implementation
        NSSearchFieldCell cell = (NSSearchFieldCell) new NSSearchFieldCell().alloc().init();
        proxy.setCell(cell);
        cell.release();
        long previousAppearance = OS.objc_msgSend(OS.class_NSAppearance, OS.sel_currentAppearance);
        try {
            proxy.setFont(original.font());
            proxy.setAlignment(OS.objc_msgSend(original.id, OS.sel_registerName("alignment")));
            proxy.setStringValue(original.stringValue());
            proxy.setEnabled(original.isEnabled());
            proxy.setFocusRingType(OS.NSFocusRingTypeNone);
            long textColor = OS.objc_msgSend(original.id, OS.sel_registerName("textColor"));
            NSResponder responder = original.window().firstResponder();
            NSText editor = original.window().fieldEditor(false, original);
            long delegateSelector = OS.sel_registerName("delegate");
            boolean editing = OS.objc_msgSend(original.id, OS.sel_registerName("currentEditor")) != 0
                || editor != null && editor.isDescendantOf(original)
                || responder != null && (responder.id == original.id || responder.respondsToSelector(delegateSelector)
                    && OS.objc_msgSend(responder.id, delegateSelector) == original.id);
            OS.objc_msgSend(proxy.id, OS.sel_registerName("setTextColor:"), editing ? NSColor.clearColor().id : textColor);
            long appearance = OS.objc_msgSend(original.id, OS.sel_registerName("effectiveAppearance"));
            OS.objc_msgSend(proxy.id, OS.sel_registerName("setAppearance:"), appearance);
            OS.objc_msgSend(OS.class_NSAppearance, OS.sel_registerName("setCurrentAppearance:"), appearance);
            NSSearchFieldCell originalCell = new NSSearchFieldCell(original.cell());
            NSSearchFieldCell proxyCell = new NSSearchFieldCell(proxy.cell());
            OS.objc_msgSend(proxyCell.id, OS.sel_registerName("setBezeled:"), true);
            long placeholder = editing ? 0 : OS.objc_msgSend(originalCell.id, OS.sel_registerName("placeholderString"));
            proxyCell.setPlaceholderString(editing ? NSString.string() : placeholder == 0 ? null : new NSString(placeholder));
            if (originalCell.searchButtonCell() == null) {
                proxyCell.setSearchButtonCell(null);
            }
            if (originalCell.cancelButtonCell() == null) {
                proxyCell.setCancelButtonCell(null);
            }
            NSGraphicsContext bitmapContext = NSGraphicsContext.graphicsContextWithBitmapImageRep(bitmap);
            NSGraphicsContext.setCurrentContext(
                NSGraphicsContext.graphicsContextWithGraphicsPort(bitmapContext.graphicsPort(), true));
            OS.CGContextTranslateCTM(bitmapContext.graphicsPort(), -visible.x, visible.height + visible.y);
            OS.CGContextScaleCTM(bitmapContext.graphicsPort(), 1, -1);
            proxy.drawRect(proxy.bounds());
        } finally {
            OS.objc_msgSend(OS.class_NSAppearance, OS.sel_registerName("setCurrentAppearance:"), previousAppearance);
            proxy.release();
        }
    }
}
