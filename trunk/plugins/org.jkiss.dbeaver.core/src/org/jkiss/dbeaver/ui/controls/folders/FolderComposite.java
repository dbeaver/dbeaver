/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.controls.folders;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

    @NotNull
    private final Composite compositePane;
    @Nullable
    private FolderInfo[] folders;

    private final Map<FolderInfo, Composite> contentsMap = new HashMap<FolderInfo, Composite>();
    private List<IFolderListener> listeners = new ArrayList<IFolderListener>();
    private List<FolderPane> folderPanes = new ArrayList<FolderPane>();

    private class FolderPane extends Composite {
        FolderInfo[] folders;
        FolderList folderList;
        Composite pane;
        @Nullable
        private Control curContent;
        @Nullable
        private IFolder curFolder;

        public FolderPane(Composite parent, boolean last) {
            super(parent, SWT.NONE);

            GridData gd = new GridData(SWT.FILL,SWT.FILL,true,true);
            // Set constant height hint to make all panes the same height
            gd.heightHint = 100;

            this.setLayoutData(gd);
            GridLayout gl = new GridLayout(2, false);
            gl.marginWidth = 0;
            gl.marginHeight = 0;
            gl.horizontalSpacing = 0;
            gl.verticalSpacing = 0;
            this.setLayout(gl);

            this.folderList = new FolderList(this);
            gd = new GridData(GridData.FILL_VERTICAL);
            gd.verticalSpan = 2;
            this.folderList.setLayoutData(gd);

            pane = UIUtils.createPlaceholder(this, 1);
            pane.setLayoutData(new GridData(GridData.FILL_BOTH));

            if (!last) {
                Sash horizontalLine = new Sash(this, SWT.NONE);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.heightHint = 5;
                horizontalLine.setLayoutData(gd);
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
                newContent = new Composite(pane, SWT.NONE);
                newContent.setLayoutData(new GridData(GridData.FILL_BOTH));
                newContent.setLayout(new FillLayout());
                newFolder.createControl(newContent);
                contentsMap.put(folder, newContent);
            }
            if (curContent != null && curFolder != null) {
                curContent.setVisible(false);
                curFolder.aboutToBeHidden();
                ((GridData)curContent.getLayoutData()).exclude = true;
            }
            ((GridData)newContent.getLayoutData()).exclude = false;
            newFolder.aboutToBeShown();
            newContent.setVisible(true);
            curContent = newContent;
            curFolder = newFolder;

            pane.layout();

            for (IFolderListener listener : listeners) {
                listener.folderSelected(folder.getId());
            }
        }

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
        gl = new GridLayout(1, false);
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

        List<List<FolderInfo>> groups = new ArrayList<List<FolderInfo>>();
        List<FolderInfo> curGroup = null;
        for (FolderInfo folder : folders) {
            if (folder.isEmbeddable()) {
                groups.add(curGroup = new ArrayList<FolderInfo>());
                curGroup.add(folder);
                curGroup = null;
            } else {
                if (curGroup == null) {
                    groups.add(curGroup = new ArrayList<FolderInfo>());
                }
                curGroup.add(folder);
            }
        }

        for (int i = 0; i < groups.size(); i++) {
            List<FolderInfo> group = groups.get(i);
            FolderPane folderPane = new FolderPane(compositePane, i >= groups.size() - 1);
            folderPane.setFolders(group.toArray(new FolderInfo[group.size()]));
            folderPanes.add(folderPane);
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

/*
        getShell().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                //createFlatFolders(folders);
                for (FolderInfo fi : folders) {
                    fi.getContents().aboutToBeShown();
                }
            }
        });
*/
    }

    private void createFlatFolders(FolderInfo[] folders) {
        FolderList[] subFolders = new FolderList[folders.length];
        for (int i = 0; i < folders.length; i++) {
            FolderInfo folder = folders[i];
            Composite folderGroup = UIUtils.createPlaceholder(compositePane, 2);
            folderGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

            FolderList nestedFolders = new FolderList(folderGroup);
            GridData gd = new GridData(GridData.FILL_VERTICAL);
            gd.verticalSpan = 2;
            gd.widthHint = 250;
            nestedFolders.setLayoutData(gd);
            nestedFolders.setFolders(new FolderInfo[]{folder});
            nestedFolders.select(0);
            subFolders[i] = nestedFolders;

            Composite folderPH = UIUtils.createPlaceholder(folderGroup, 1);
            folderPH.setLayoutData(new GridData(GridData.FILL_BOTH));
            folderPH.setLayout(new FillLayout());
            IFolder contents = folder.getContents();
            contents.createControl(folderPH);
            //contents.aboutToBeShown();

            contentsMap.put(folder, folderPH);

            if (i < folders.length - 1) {
                Sash horizontalLine = new Sash(folderGroup, SWT.NONE);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.heightHint = 5;
                horizontalLine.setLayoutData(gd);
            }
        }
    }

    @Nullable
    public FolderInfo[] getFolders() {
        return folders;
    }

    @Override
    public IFolder getActiveFolder() {
        return null;
        //return folderList == null ? null : folderList.getElementAt(folderList.getSelectionIndex()).getInfo().getContents();
    }

    @Override
    public void switchFolder(String folderId) {
        for (FolderPane folderPane : folderPanes) {
            for (int i = 0; i < folderPane.folderList.getNumberOfElements(); i++) {
                if (folderPane.folderList.getElementAt(i).getInfo().getId().equals(folderId)) {
                    folderPane.folderList.select(i);
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
