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
public class TabbedFolderComposite extends Composite implements ITabbedFolderContainer {

    @NotNull
    private final Composite compositePane;
    @Nullable
    private TabbedFolderInfo[] folders;

    private final Map<TabbedFolderInfo, Composite> contentsMap = new HashMap<>();
    private List<ITabbedFolderListener> listeners = new ArrayList<>();
    private FolderPane[] folderPanes;
    private FolderPane lastActiveFolder = null;

    private TabbedFolderState folderState;
    private boolean inLayoutUpdate;

    private class FolderPane {
        TabbedFolderInfo[] folders;
        TabbedFolderList folderList;
        Composite editorPane;
        @Nullable
        private Control curContent;
        @Nullable
        private ITabbedFolder curFolder;
        @Nullable
        private final Sash sash;

        public FolderPane(Composite parent, boolean last) {
            this.folderList = new TabbedFolderList(parent, !last);
            GridData gd = new GridData(GridData.FILL_VERTICAL);
            if (!last) {
                gd.verticalSpan = 2;
            }
            //gd.heightHint = 100;
            this.folderList.setLayoutData(gd);

            editorPane = UIUtils.createPlaceholder(parent, 1);
            gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = folderList.getTabHeight();
            editorPane.setLayoutData(gd);

            if (!last) {
                sash = new Sash(parent, SWT.NONE);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.heightHint = TabbedFolderList.SECTION_DIV_HEIGHT;
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
                        if (shift > 0 && shift > getNextFolderPane(FolderPane.this).editorPane.getBounds().height - folderList.getTabHeight()) {
                            e.doit = false;
                            return;
                        }
                        if (shift < 0 && Math.abs(shift) > editorPane.getBounds().height - folderList.getTabHeight()) {
                            e.doit = false;
                            return;
                        }
                        if (Math.abs(shift) > 0) {
                            TabbedFolderComposite.this.setRedraw(false);
                            try {
                                shiftPane(FolderPane.this, shift);
                            } finally {
                                TabbedFolderComposite.this.setRedraw(true);
                            }
                        }
                    }
                });
            } else {
                this.sash = null;
            }

            folderList.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    onFolderSwitch(folderList.getElementAt(folderList.getSelectionIndex()).getInfo());
                }
            });
        }

        public void setFolders(TabbedFolderInfo[] folders) {
            this.folders = folders;
            this.folderList.setFolders(this.folders);
        }

        private void onFolderSwitch(TabbedFolderInfo folder) {
            Composite newContent = contentsMap.get(folder);
            ITabbedFolder newFolder = folder.getContents();
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

            for (ITabbedFolderListener listener : listeners) {
                listener.folderSelected(folder.getId());
            }
        }

    }

    private void shiftPane(FolderPane curPane, int shift) {
        // Set current height to heightHint
/*
        for (FolderPane pane : folderPanes) {
            Rectangle bounds = pane.editorPane.getBounds();
            GridData gd = (GridData) pane.editorPane.getLayoutData();
            gd.heightHint = bounds.height;
        }
*/

        FolderPane nextPane = getNextFolderPane(curPane);
        ((GridData) curPane.editorPane.getLayoutData()).heightHint += shift;
        ((GridData) nextPane.editorPane.getLayoutData()).heightHint -= shift;

        reLayout();
/*
        if (shift < 0) {
            // Decrease self size and increase next pane's
            //nextPane.editorPane.
        } else {
            // Increase self size and decrease next pane's
        }
*/
    }

    private void reLayout() {
        inLayoutUpdate = true;
        try {
            compositePane.layout();
        } finally {
            inLayoutUpdate = false;
        }
    }

    private FolderPane getNextFolderPane(FolderPane pane) {
        for (int i = 0; i < folderPanes.length - 1; i++) {
            if (pane == folderPanes[i]) {
                return folderPanes[i + 1];
            }
        }
        return null;
    }

    public TabbedFolderComposite(Composite parent, int style) {
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

        addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                FolderPane pane = getActiveFolderPane();
                if (pane != null) {
                    pane.folderList.handleTraverse(e);
                }
            }
        });
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                for (TabbedFolderInfo folderDescription : contentsMap.keySet()) {
                    folderDescription.getContents().dispose();
                }
            }
        });
    }

    /**
     * @param objectId ID used to save/load folders state
     * @param folders  list of folders
     */
    public void setFolders(@NotNull final String objectId, @NotNull final TabbedFolderInfo[] folders) {
        this.folders = folders;

        folderState = TabbedFoldersRegistry.getInstance().getFolderState(objectId);

        List<List<TabbedFolderInfo>> groups = new ArrayList<>();
        List<TabbedFolderInfo> curGroup = null;
        for (TabbedFolderInfo folder : folders) {
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
            List<TabbedFolderInfo> group = groups.get(i);
            FolderPane folderPane = new FolderPane(compositePane, i >= groups.size() - 1);
            folderPane.setFolders(group.toArray(new TabbedFolderInfo[group.size()]));
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
        for (int i = 0; i < folderPanes.length; i++) {
            FolderPane folderPane = folderPanes[i];
            GridData gd = (GridData) folderPane.folderList.getLayoutData();
            // Try to get height info from first folder tab state
            final TabbedFolderState.TabState tabState = folderState.getTabState(folderPane.folders[0].getId(), true);
/*
            if (tabState.height > 0) {
                // Set height for all tabs but last
                gd.heightHint = tabState.height;
            }
*/
            gd.widthHint = maxWidth;
            gd.minimumHeight = folderPane.folderList.getTabHeight();
/*
            folderPane.folderList.addControlListener(new ControlAdapter() {
                @Override
                public void controlResized(ControlEvent e) {
                    if (inLayoutUpdate) return;
                    tabState.height = folderPane.folderList.getSize().y;
                    TabbedFoldersRegistry.getInstance().saveConfig();
                }
            });
*/
        }

        // Re-layout
        reLayout();
    }

    @NotNull
    public TabbedFolderState getFolderState() {
        return folderState;
    }

    @Nullable
    public TabbedFolderInfo[] getFolders() {
        return folders;
    }

    @Override
    public ITabbedFolder getActiveFolder() {
        FolderPane pane = getActiveFolderPane();
        if (pane != null) {
            return getActiveFolder(pane);
        }
        return null;
    }

    public FolderPane getActiveFolderPane() {
        if (folderPanes.length == 1) {
            return folderPanes[0];
        }
        Control focusControl = getDisplay().getFocusControl();
        for (FolderPane folderPane : folderPanes) {
            if (UIUtils.isParent(folderPane.editorPane, focusControl)) {
                lastActiveFolder = folderPane;
                return folderPane;
            }
        }
        if (lastActiveFolder != null) {
            return lastActiveFolder;
        }
        return null;
    }

    private ITabbedFolder getActiveFolder(FolderPane folderPane) {
        TabbedFolderList folderList = folderPane.folderList;
        int selectionIndex = folderList.getSelectionIndex();
        if (selectionIndex < 0) {
            // If no folder was activated - do it now
            selectionIndex = 0;
            folderList.select(selectionIndex);
        }
        return folderList.getElementAt(selectionIndex).getInfo().getContents();
    }

    @Override
    public void switchFolder(@Nullable String folderId) {
        for (FolderPane folderPane : folderPanes) {
            for (int i = 0; i < folderPane.folderList.getNumberOfElements(); i++) {
                if (folderId == null || folderPane.folderList.getElementAt(i).getInfo().getId().equals(folderId)) {
                    folderPane.folderList.select(i);
                    lastActiveFolder = folderPane;
                    break;
                }
            }
        }
    }

    @Override
    public void addFolderListener(ITabbedFolderListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeFolderListener(ITabbedFolderListener listener) {
        listeners.remove(listener);
    }

}
