/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.ui.dnd.LocalObjectTransfer;

/**
 * TabFolderReorder
 */
public class TabFolderReorder
{
    private CTabItem dragItem;

    public TabFolderReorder(CTabFolder folder) {
        final DragSource source = new DragSource(folder, DND.DROP_MOVE);
        source.setTransfer(TabTransfer.INSTANCE);
        source.addDragListener (new DragSourceListener() {
            private Image dragImage;
            @Override
            public void dragStart(DragSourceEvent event) {
                Point point = folder.toControl(folder.getDisplay().getCursorLocation());
                dragItem = folder.getItem(point);

                if (dragItem == null) {
                    return;
                }
                Rectangle columnBounds = dragItem.getBounds();
                if (dragImage != null) {
                    dragImage.dispose();
                    dragImage = null;
                }
                GC gc = new GC(folder);
                dragImage = new Image(Display.getCurrent(), columnBounds.width, columnBounds.height);
                gc.copyArea(
                    dragImage,
                    columnBounds.x,
                    columnBounds.y);
                event.image = dragImage;
                gc.dispose();
            }

            @Override
            public void dragSetData (DragSourceEvent event) {
                event.data = dragItem;
            }
            @Override
            public void dragFinished(DragSourceEvent event) {
                if (dragImage != null) {
                    dragImage.dispose();
                    dragImage = null;
                }
            }
        });

        DropTarget dropTarget = new DropTarget(folder, DND.DROP_MOVE);
        dropTarget.setTransfer(TabTransfer.INSTANCE, TextTransfer.getInstance());
        dropTarget.addDropListener(new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void dragLeave(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void dragOperationChanged(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void dragOver(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void drop(DropTargetEvent event)
            {
                handleDragEvent(event);
                if (event.detail == DND.DROP_MOVE) {
                    moveTabs(folder, event);
                }
            }

            @Override
            public void dropAccept(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            private void handleDragEvent(DropTargetEvent event)
            {
                if (!isDropSupported(folder, event)) {
                    event.detail = DND.DROP_NONE;
                } else {
                    event.detail = DND.DROP_MOVE;
                }
                event.feedback = DND.FEEDBACK_SELECT;
            }

            private boolean isDropSupported(CTabFolder folder, DropTargetEvent event)
            {
                if (dragItem == null) {
                    return false;
                }
                Point point = folder.toControl(folder.getDisplay().getCursorLocation());
                return folder.getItem(new Point(point.x, point.y)) != null;
            }

        });
    }

    private void moveTabs(CTabFolder folder, DropTargetEvent event) {
        Point point = folder.toControl(folder.getDisplay().getCursorLocation());
        CTabItem item = folder.getItem(new Point(point.x, point.y));
        if (item != null && dragItem != null) {
            Control dragControl = dragItem.getControl();
            String dragText = dragItem.getText();
            Image dragImage = dragItem.getImage();
            String dragToolTip = dragItem.getToolTipText();
            boolean dragShowClose = dragItem.getShowClose();
            Object dragData = dragItem.getData();

            dragItem.setText(item.getText());
            dragItem.setImage(item.getImage());
            dragItem.setToolTipText(item.getToolTipText());
            dragItem.setData(item.getData());
            dragItem.setShowClose(item.getShowClose());
            dragItem.setControl(item.getControl());

            item.setText(dragText);
            item.setImage(dragImage);
            item.setToolTipText(dragToolTip);
            item.setData(dragData);
            item.setShowClose(dragShowClose);
            item.setControl(dragControl);

            folder.setSelection(item);
        }
    }

    public final static class TabTransfer extends LocalObjectTransfer<CTabItem> {

        public static final TabTransfer INSTANCE = new TabTransfer();
        private static final String TYPE_NAME = "TabTransfer.CTabItem Transfer" + System.currentTimeMillis() + ":" + INSTANCE.hashCode();//$NON-NLS-1$
        private static final int TYPEID = registerType(TYPE_NAME);

        private TabTransfer() {
        }

        @Override
        protected int[] getTypeIds() {
            return new int[] { TYPEID };
        }

        @Override
        protected String[] getTypeNames() {
            return new String[] { TYPE_NAME };
        }

    }

}