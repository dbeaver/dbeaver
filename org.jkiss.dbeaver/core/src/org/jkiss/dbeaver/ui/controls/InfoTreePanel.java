/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import java.util.Set;
import java.util.HashSet;

/**
 * Column info panel.
 */
public abstract class InfoTreePanel extends Composite {

    private String column1Name = "Property";
    private String column2Name = "Value";

    public InfoTreePanel(Composite parent, int style) {
        super(parent, style);
    }

    public String getColumn1Name() {
        return column1Name;
    }

    public void setColumn1Name(String column1Name) {
        this.column1Name = column1Name;
    }

    public String getColumn2Name() {
        return column2Name;
    }

    public void setColumn2Name(String column2Name) {
        this.column2Name = column2Name;
    }

    public void createControl()
    {
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        this.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = 0;
        gd.verticalIndent = 0;
        this.setLayoutData(gd);

        {
            Tree infoTree = new Tree(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
            gd = new GridData(GridData.FILL_BOTH);
            gd.minimumWidth = 100;
            infoTree.setLayoutData(gd);
            infoTree.setHeaderVisible(true);
            //infoTree.setLinesVisible(true);

            TreeColumn column1 = new TreeColumn(infoTree, SWT.LEFT);
            column1.setText(column1Name);
            column1.setResizable(true);
            TreeColumn column2 = new TreeColumn(infoTree, SWT.LEFT);
            column2.setText(column2Name);
            column2.setResizable(true);

            createItems(infoTree);

            Set<TreeItem> collapsedItems = new HashSet<TreeItem>();
            for (TreeItem item : infoTree.getItems()) {
                if (!item.getExpanded()) {
                    collapsedItems.add(item);
                    item.setExpanded(true);
                }
            }
            column1.pack();
            column2.pack();

            for (TreeItem item : collapsedItems) {
                item.setExpanded(false);
            }
        }
    }

    protected abstract void createItems(Tree infoTree);

}