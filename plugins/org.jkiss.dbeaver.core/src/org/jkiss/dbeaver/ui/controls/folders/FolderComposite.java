/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui.controls.folders;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Sash;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Folders composite.
 * Styles:
 * SWT.LEFT, SWT.RIGHT - tabs orientation
 */
public class FolderComposite extends Composite implements IFolderContainer {

    public static final int MIN_PANE_HEIGHT = 60;
    @NotNull
    private final Composite compositePane;
    @Nullable
    private FolderInfo[] folders;

    private final Map<FolderInfo, Composite> contentsMap = new HashMap<>();
    private List<IFolderListener> listeners = new ArrayList<>();
    private FolderPane[] folderPanes;
    private FolderPane lastActiveFolder = null;

    private class FolderPane {
        FolderInfo[] folders;
        FolderList folderList;
        Composite editorPane;
        @Nullable
        private Control curContent;
        @Nullable
        private IFolder curFolder;

        public FolderPane(Composite parent, boolean last) {
            this.folderList = new FolderList(parent, !last);
            GridData gd = new GridData(GridData.FILL_VERTICAL);
            if (!last) {
                gd.verticalSpan = 2;
            }
            //gd.heightHint = 100;
            this.folderList.setLayoutData(gd);

            editorPane = UIUtils.createPlaceholder(parent, 1);
            gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = MIN_PANE_HEIGHT;
            editorPane.setLayoutData(gd);

            if (!last) {
                final Sash sash = new Sash(parent, SWT.NONE);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.heightHint = FolderList.SECTION_DIV_HEIGHT;
                sash.setLayoutData(gd);
                sash.addPaintListener(new PaintListener() {
                    @Override
                    public void paintControl(PaintEvent e) {
                        e.gc.setBackground(folderList.widgetBackground);
                        e.gc.setForeground(folderList.widgetForeground);
                        e.gc.fillRectangle(0, 1, e.width, e.height - 2);
                        e.gc.setForeground(folderList.widgetNormalShadow);
                        e.gc.drawLine(0, 0, e.width - 1, 0);
                        e.gc.drawLine(0, e.height - 1, e.width - 1, e.height - 1);
                    }
                });
                sash.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        // Resize folders
                        Rectangle sashBounds = sash.getBounds();

                        int shift = e.y - sashBounds.y;
                        if (shift > 0 && shift > getNextFolderPane(FolderPane.this).editorPane.getBounds().height - MIN_PANE_HEIGHT) {
                            e.doit = false;
                            return;
                        }
                        if (shift < 0 && Math.abs(shift) > editorPane.getBounds().height - MIN_PANE_HEIGHT) {
                            e.doit = false;
                            return;
                        }
                        if (Math.abs(shift) > 0) {
                            FolderComposite.this.setRedraw(false);
                            try {
                                shiftPane(FolderPane.this, shift);
                            } finally {
                                FolderComposite.this.setRedraw(true);
                            }
                        }
                    }
                });
            }

            folderList.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    onFolderSwitch(folderList.getElementAt(folderList.getSelectionIndex()).getInfo());
                }
            });
        }

        public void setFolders(FolderInfo[] folders) {
            this.folders = folders;
            this.folderList.setFolders(this.folders);
            this.folderList.select(0);
        }

        private void onFolderSwitch(FolderInfo folder) {
            Composite newContent = contentsMap.get(folder);
            IFolder newFolder = folder.getContents();
            if (newContent == null) {
                newContent = new Composite(editorPane, SWT.NONE);
                newContent.setLayoutData(new GridData(GridData.FILL_BOTH));
                newContent.setLayout(new FillLayout());
                newFolder.createControl(newContent);
                contentsMap.put(folder, newContent);
            }

            // Notify part about hide/show
            if (curContent != null && curFolder != null) {
                curFolder.aboutToBeHidden();
            }
            newFolder.aboutToBeShown();

            // Make actual hide/show
            if (curContent != null && curFolder != null) {
                curContent.setVisible(false);
                ((GridData)curContent.getLayoutData()).exclude = true;
            }
            ((GridData)newContent.getLayoutData()).exclude = false;
            newContent.setVisible(true);

            // Layout and notify listeners
            curContent = newContent;
            curFolder = newFolder;

            editorPane.layout();

            for (IFolderListener listener : listeners) {
                listener.folderSelected(folder.getId());
            }
        }

    }

    private void shiftPane(FolderPane curPane, int shift) {
        // Set current height to heightHint
        for (FolderPane pane : folderPanes) {
            Rectangle bounds = pane.editorPane.getBounds();
            GridData gd = (GridData) pane.editorPane.getLayoutData();
            gd.heightHint = bounds.height;
        }

        FolderPane nextPane = getNextFolderPane(curPane);
        ((GridData) curPane.editorPane.getLayoutData()).heightHint += shift;
        ((GridData) nextPane.editorPane.getLayoutData()).heightHint -= shift;

        compositePane.layout();
/*
        if (shift < 0) {
            // Decrease self size and increase next pane's
            //nextPane.editorPane.
        } else {
            // Increase self size and decrease next pane's
        }
*/
    }

    private FolderPane getNextFolderPane(FolderPane pane) {
        for (int i = 0; i < folderPanes.length - 1; i++) {
            if (pane == folderPanes[i]) {
                return folderPanes[i + 1];
            }
        }
        return null;
    }

    public FolderComposite(Composite parent, int style) {
        super(parent, style);
        GridLayout gl = new GridLayout(2, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        setLayout(gl);

        compositePane = new Composite(this, SWT.NONE);
        gl = new GridLayout(2, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        compositePane.setLayout(gl);
        compositePane.setLayoutData(new GridData(GridData.FILL_BOTH));

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                for (FolderInfo folderDescription : contentsMap.keySet()) {
                    folderDescription.getContents().dispose();
                }
            }
        });
    }

    public void setFolders(@NotNull final FolderInfo[] folders) {
        this.folders = folders;

        List<List<FolderInfo>> groups = new ArrayList<>();
        List<FolderInfo> curGroup = null;
        for (FolderInfo folder : folders) {
            if (folder.isEmbeddable()) {
                groups.add(curGroup = new ArrayList<>());
                curGroup.add(folder);
                curGroup = null;
            } else {
                if (curGroup == null) {
                    groups.add(curGroup = new ArrayList<>());
                }
                curGroup.add(folder);
            }
        }

        folderPanes = new FolderPane[groups.size()];
        for (int i = 0; i < groups.size(); i++) {
            List<FolderInfo> group = groups.get(i);
            FolderPane folderPane = new FolderPane(compositePane, i >= groups.size() - 1);
            folderPane.setFolders(group.toArray(new FolderInfo[group.size()]));
            folderPanes[i] = folderPane;
        }

        // Make all sub folders the same size
        int maxWidth = 0;
        for (FolderPane folderPane : folderPanes) {
            int width = folderPane.folderList.computeSize(-1, -1, false).x;
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        for (FolderPane folderPane : folderPanes) {
            ((GridData)folderPane.folderList.getLayoutData()).widthHint = maxWidth;
        }

        // Re-layout
        compositePane.layout();
    }

    @Nullable
    public FolderInfo[] getFolders() {
        return folders;
    }

    @Override
    public IFolder getActiveFolder() {
        if (folderPanes.length == 1) {
            return getActiveFolder(folderPanes[0]);
        }
        Control focusControl = getDisplay().getFocusControl();
        for (FolderPane folderPane : folderPanes) {
            if (UIUtils.isParent(folderPane.editorPane, focusControl)) {
                lastActiveFolder = folderPane;
                return getActiveFolder(folderPane);
            }
        }
        if (lastActiveFolder != null) {
            return getActiveFolder(lastActiveFolder);
        }
        return null;
    }

    private IFolder getActiveFolder(FolderPane folderPane) {
        FolderList folderList = folderPane.folderList;
        return folderList.getElementAt(folderList.getSelectionIndex()).getInfo().getContents();
    }

    @Override
    public void switchFolder(String folderId) {
        for (FolderPane folderPane : folderPanes) {
            for (int i = 0; i < folderPane.folderList.getNumberOfElements(); i++) {
                if (folderPane.folderList.getElementAt(i).getInfo().getId().equals(folderId)) {
                    folderPane.folderList.select(i);
                    lastActiveFolder = folderPane;
                    break;
                }
            }
        }
    }

    @Override
    public void addFolderListener(IFolderListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeFolderListener(IFolderListener listener) {
        listeners.remove(listener);
    }

}
