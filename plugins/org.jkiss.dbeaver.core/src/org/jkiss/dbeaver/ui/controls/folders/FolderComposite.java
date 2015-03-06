/*******************************************************************************
 * Copyright (c) 2001, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mariot Chauvin <mariot.chauvin@obeo.fr> - bug 259553
 *     Amit Joglekar <joglekar@us.ibm.com> - Support for dynamic images (bug 385795)
 *
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
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
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
 * SWT.SINGLE, SWT.MULTI - use single-page or multi-page mode
 */
public class FolderComposite extends Composite implements IFolderContainer {

    @Nullable
    private FolderList folderList;
    @NotNull
    private final Composite pane;
    @Nullable
    private FolderInfo[] folders;
    @Nullable
    private IFolder curFolder;
    @Nullable
    private Control curContent;

    private final Map<FolderInfo, Composite> contentsMap = new HashMap<FolderInfo, Composite>();
    private List<IFolderListener> listeners = new ArrayList<IFolderListener>();

    public FolderComposite(Composite parent, int style) {
        super(parent, style);
        GridLayout gl = new GridLayout(2, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        setLayout(gl);

        boolean single = (style & SWT.SINGLE) != 0;
        if (!single && ((style & SWT.LEFT) != 0 || (style & SWT.RIGHT) == 0)) {
            folderList = new FolderList(this);
            folderList.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        }

        pane = new Composite(this, SWT.NONE);
        gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        pane.setLayout(gl);
        pane.setLayoutData(new GridData(GridData.FILL_BOTH));
        if (!single && (style & SWT.RIGHT) != 0) {
            folderList = new FolderList(this);
            folderList.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        }

        if (!single) {
            folderList.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    onFolderSwitch(folderList.getElementAt(folderList.getSelectionIndex()).getInfo());
                }
            });
        }

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                for (FolderInfo folderDescription : contentsMap.keySet()) {
                    folderDescription.getContents().dispose();
                }
            }
        });
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

    public void setFolders(@NotNull final FolderInfo[] folders) {
        this.folders = folders;
        if (this.folderList == null) {
            createFlatFolders(folders);
            getShell().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    //createFlatFolders(folders);
                    for (FolderInfo fi : folders) {
                        fi.getContents().aboutToBeShown();
                    }
                }
            });
        } else {
            boolean firstTime = folderList.getNumberOfElements() == 0;
            folderList.setFolders(folders);
            folderList.select(0);
            if (firstTime) {
                layout();
            }
        }
    }

    private void createFlatFolders(FolderInfo[] folders) {
        FolderList[] subFolders = new FolderList[folders.length];
        for (int i = 0; i < folders.length; i++) {
            FolderInfo folder = folders[i];
            Composite folderGroup = UIUtils.createPlaceholder(pane, 2);
            folderGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            folderGroup.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    super.focusGained(e);
                }

                @Override
                public void focusLost(FocusEvent e) {
                    super.focusLost(e);
                }
            });

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

        int maxWidth = 0;
        for (FolderList f : subFolders) {
            int width = f.computeSize(-1, -1, false).x;
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        for (FolderList f : subFolders) {
            ((GridData)f.getLayoutData()).widthHint = maxWidth;
        }

        // Make all sub folders the same size
        pane.layout();
    }

    @Nullable
    public FolderInfo[] getFolders() {
        return folders;
    }

    @Override
    public IFolder getActiveFolder() {
        return folderList == null ? null : folderList.getElementAt(folderList.getSelectionIndex()).getInfo().getContents();
    }

    @Override
    public void switchFolder(String folderId) {
        if (folderList == null) {
            return;
        }
        for (int i = 0; i < folderList.getNumberOfElements(); i++) {
            if (folderList.getElementAt(i).getInfo().getId().equals(folderId)) {
                folderList.select(i);
                //folderList.getElementAt(i).getInfo();
                break;
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
