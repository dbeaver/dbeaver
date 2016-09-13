/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset.panel;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPanel;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;

/**
 * RSV value view panel
 */
public class MetaDataPanel implements IResultSetPanel {

    private static final Log log = Log.getLog(MetaDataPanel.class);

    public static final String PANEL_ID = "results-metadata";

    private IResultSetPresentation presentation;
    private Tree attributeList;

    public MetaDataPanel() {
    }

    @Override
    public String getPanelTitle() {
        return "MetaData";
    }

    @Override
    public DBPImage getPanelImage() {
        return UIIcon.PANEL_METADATA;
    }

    @Override
    public String getPanelDescription() {
        return "Resultset metadata";
    }

    @Override
    public Control createContents(final IResultSetPresentation presentation, Composite parent) {
        this.presentation = presentation;

        this.attributeList = new Tree(parent, SWT.NONE);
        this.attributeList.setLinesVisible(true);

        return this.attributeList;
    }


    @Override
    public void activatePanel() {
        refresh();
    }

    @Override
    public void deactivatePanel() {

    }

    @Override
    public void refresh() {
        attributeList.setRedraw(false);
        try {
            UIUtils.packColumns(attributeList);

        } finally {
            attributeList.setRedraw(true);
        }
    }

    @Override
    public void contributeActions(ToolBarManager manager) {
    }

}
