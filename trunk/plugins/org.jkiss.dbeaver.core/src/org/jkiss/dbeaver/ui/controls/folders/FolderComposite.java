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
 * Copyright (C) 2010-2014 Serge Rieder
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Folders composite
 */
public class FolderComposite extends Composite implements IFolderContainer {


    private final FolderList folderList;
    private final Composite pane;
    private final Map<IFolderDescription, Control> contentsMap = new HashMap<IFolderDescription, Control>();
    private Control curContent;
    private List<IFolderListener> listeners = new ArrayList<IFolderListener>();

    public FolderComposite(Composite parent, int style) {
        super(parent, style);
        GridLayout gl = new GridLayout(2, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        setLayout(gl);

        folderList = new FolderList(this);
        folderList.setLayoutData(new GridData(GridData.FILL_BOTH));
        pane = new Composite(this, SWT.NONE);
        gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        pane.setLayout(gl);
        pane.setLayoutData(new GridData(GridData.FILL_BOTH));

        folderList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onFolderSwitch(folderList.getElementAt(folderList.getSelectionIndex()).getTabItem());
            }
        });

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                for (IFolderDescription folderDescription : contentsMap.keySet()) {
                    folderDescription.getContents().dispose();
                }
            }
        });
    }

    private void onFolderSwitch(IFolderDescription folder) {
        Control newContent = contentsMap.get(folder);
        if (newContent == null) {
            folder.getContents().createControl(pane);
            newContent = folder.getContents().getControl();
            newContent.setLayoutData(new GridData(GridData.FILL_BOTH));
            contentsMap.put(folder, newContent);
        }
        if (curContent != null) {
            curContent.setVisible(false);
            ((GridData)curContent.getLayoutData()).exclude = true;
        }
        ((GridData)newContent.getLayoutData()).exclude = false;
        newContent.setVisible(true);
        curContent = newContent;
        pane.layout();

        for (IFolderListener listener : listeners) {
            listener.folderSelected(folder.getId());
        }
    }

    public void setFolders(IFolderDescription[] folders) {
        folderList.setFolders(folders);
        folderList.select(0);
    }

    @Override
    public Object getActiveFolder() {
        return folderList.getElementAt(folderList.getSelectionIndex()).getTabItem();
    }

    @Override
    public void switchFolder(String folderId) {
        for (int i = 0; i < folderList.getNumberOfElements(); i++) {
            if (folderList.getElementAt(i).getTabItem().getId().equals(folderId)) {
                folderList.select(i);
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
