/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * PairListControl
 */
public class PairListControl<ELEMENT> extends Composite
{
    private static final Log log = Log.getLog(PairListControl.class);

    public static final int MOVE_LEFT = 0;
    public static final int MOVE_RIGHT = 1;

    private Table leftList;
    private Table rightList;

    private Collection<ELEMENT> leftElements;
    private Collection<ELEMENT> rightElements;
    private List<ELEMENT> movedElements = new ArrayList<>();
    private Font movedFont;
    private SelectionListener selListener;

    public PairListControl(
        Composite parent,
        int style,
        String leftTitle,
        String rightTitle)
    {
        super(parent, style);
        createPairList(leftTitle, rightTitle);
        movedFont = UIUtils.modifyFont(getFont(), SWT.ITALIC);
    }

    public void setModel(Collection<ELEMENT> leftElements, Collection<ELEMENT> rightElements)
    {
        setListData(leftList, this.leftElements = leftElements);
        setListData(rightList, this.rightElements = rightElements);

        updateControls();
    }

    private void createPairList(String leftTitle, String rightTitle)
    {
        this.setLayout(new GridLayout(3, false));

        GridData gd = new GridData(GridData.FILL_BOTH);
        this.setLayoutData(gd);

        Label leftLabel = new Label(this, SWT.NONE);
        leftLabel.setText(leftTitle);

        new Label(this, SWT.NONE);

        Label rightLabel = new Label(this, SWT.NONE);
        rightLabel.setText(rightTitle);

        leftList = createList(this);

        {
            Composite buttonsPane = new Composite(this, SWT.NONE);
            gd = new GridData(GridData.VERTICAL_ALIGN_CENTER);
            gd.minimumWidth = 50;
            buttonsPane.setLayoutData(gd);
            GridLayout gl = new GridLayout(1, false);
            buttonsPane.setLayout(gl);

            final Button btnMoveRight = createButton(buttonsPane, DBeaverIcons.getImage(UIIcon.ARROW_RIGHT), new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    moveElements(true);
                }
            });
            final Button btnMoveRightAll = createButton(buttonsPane, DBeaverIcons.getImage(UIIcon.ARROW_RIGHT_ALL), new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    leftList.selectAll();
                    moveElements(true);
                }
            });
            final Button btnMoveLeft = createButton(buttonsPane, DBeaverIcons.getImage(UIIcon.ARROW_LEFT), new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    moveElements(false);
                }
            });
            final Button btnMoveLeftAll = createButton(buttonsPane, DBeaverIcons.getImage(UIIcon.ARROW_LEFT_ALL), new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    rightList.selectAll();
                    moveElements(false);
                }
            });
            createButton(buttonsPane, DBeaverIcons.getImage(UIIcon.ARROW_RESET), new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    setListData(leftList, leftElements);
                    setListData(rightList, rightElements);
                }
            });

            selListener = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    btnMoveLeft.setEnabled(rightList.getSelectionCount() > 0);
                    btnMoveLeftAll.setEnabled(rightList.getItemCount() > 0);
                    btnMoveRight.setEnabled(leftList.getSelectionCount() > 0);
                    btnMoveRightAll.setEnabled(leftList.getItemCount() > 0);
                }
            };

            btnMoveLeft.setEnabled(false);
            btnMoveLeftAll.setEnabled(false);
            btnMoveRight.setEnabled(false);
            btnMoveRightAll.setEnabled(false);
        }

        rightList = createList(this);

        leftList.addSelectionListener(selListener);
        rightList.addSelectionListener(selListener);
        leftList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                moveElements(true);
            }
        });
        rightList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                moveElements(false);
            }
        });
    }

    private Table createList(Composite panel)
    {
        final Table table = new Table(panel, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        table.setLayoutData(gd);
        final TableColumn column = new TableColumn(table, SWT.LEFT);

        table.addListener (SWT.Resize,  new Listener() {
            @Override
            public void handleEvent (Event e) {
                column.setWidth(table.getClientArea().width);
            }
        });

        return table;
    }

    private Button createButton(Composite panel, Image label, SelectionListener listener)
    {
        Button button = new Button(panel, SWT.PUSH);
        button.setImage(label);
        button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        button.addSelectionListener(listener);
        return button;
    }

    private void setListData(Table list, Collection<ELEMENT> elements)
    {
        list.removeAll();
        for (ELEMENT element : elements) {
            createListItem(list, element);
        }
    }

    private void updateControls() {
        if (selListener != null) {
            selListener.widgetSelected(null);
        }
    }

    private TableItem createListItem(Table list, Object element) {
        TableItem item = new TableItem(list, SWT.NONE);
        item.setData(element);
        item.setText(element.toString());

        return item;
    }

    private void moveElements(boolean toRight)
    {
        Table fromTable = toRight ? leftList : rightList;
        Table toTable = toRight ? rightList : leftList;
        Collection<ELEMENT> checkList = toRight ? rightElements : leftElements;
        List<ELEMENT> movedElements = new ArrayList<>();

        for (TableItem item : fromTable.getSelection()) {
            ELEMENT element = (ELEMENT) item.getData();
            movedElements.add(element);
            TableItem newItem = createListItem(toTable, element);
            if (!checkList.contains(element)) {
                newItem.setFont(movedFont);
            }
        }

        fromTable.remove(fromTable.getSelectionIndices());

        updateControls();

        Event event = new Event();
        event.detail = toRight ? MOVE_RIGHT : MOVE_LEFT;
        event.widget = this;
        for (ELEMENT element : movedElements) {
            event.data = element;
            super.notifyListeners(SWT.Modify, event);
        }
    }

}