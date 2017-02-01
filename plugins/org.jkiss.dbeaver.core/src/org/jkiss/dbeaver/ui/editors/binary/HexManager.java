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
package org.jkiss.dbeaver.ui.editors.binary;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.editors.binary.dialogs.FindReplaceDialog;
import org.jkiss.dbeaver.ui.editors.binary.dialogs.GoToDialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager of the javahexeditor application, either in its standalone or Eclipse plugin version.
 * Manages creation of widgets, and executes menu actions, like File->Save. Call createEditorPart()
 * before any menu actions.
 *
 * @author Jordi
 */
public class HexManager {

    private BinaryContent content = null;
    private FontData fontData = null;  // when null uses default font
    private Font fontText = null;
    private List<Listener> listOfStatusChangedListeners = null;
    private List<SelectionListener> listOfLongListeners = null;

    // visual controls
    private FindReplaceDialog findDialog = null;
    private GoToDialog goToDialog = null;
    private HexEditControl hexEditControl = null;
    private HexStatusLine statusLine = null;
    private Composite textsParent = null;
    private IMenuListener menuListener;

    public HexEditControl getControl()
    {
        return hexEditControl;
    }

    /**
     * Blocks the caller until the task is finished. Does not block the user interface thread.
     *
     * @param task independent of the user inteface thread (no widgets used)
     */
    public static void blockUntilFinished(Runnable task)
    {
        Thread thread = new Thread(task);
        thread.start();
        Display display = Display.getCurrent();
        final boolean[] pollerEnabled = {false};
        while (thread.isAlive() && !display.isDisposed()) {
            if (!display.readAndDispatch()) {
                // awake periodically so it returns when task has finished
                if (!pollerEnabled[0]) {
                    pollerEnabled[0] = true;
                    display.timerExec(300, new Runnable() {
                        @Override
                        public void run()
                        {
                            pollerEnabled[0] = false;
                        }
                    });
                }
                display.sleep();
            }
        }
    }


    /**
     * Helper method to make a shell come closer to another shell
     *
     * @param fixedShell  where movingShell will get closer to
     * @param movingShell shell to be relocated
     */
    public static void reduceDistance(Shell fixedShell, Shell movingShell)
    {
        Rectangle fixed = fixedShell.getBounds();
        Rectangle moving = movingShell.getBounds();
        int[] fixedLower = {fixed.x, fixed.y};
        int[] fixedHigher = {fixed.x + fixed.width, fixed.y + fixed.height};
        int[] movingLower = {moving.x, moving.y};
        int[] movingSpan = {moving.width, moving.height};

        for (int i = 0; i < 2; ++i) {
            if (movingLower[i] + movingSpan[i] < fixedLower[i])
                movingLower[i] = fixedLower[i] - movingSpan[i] + 10;
            else if (fixedHigher[i] < movingLower[i])
                movingLower[i] = fixedHigher[i] - 10;
        }
        movingShell.setLocation(movingLower[0], movingLower[1]);
    }

    /**
     * Creates editor part of parent application. Can only be called once per Manager object.
     *
     * @param parent composite where the part will be drawn.
     * @throws IllegalStateException when editor part exists already (method called twice or more)
     * @throws NullPointerException  if textsParent is null
     */
    public Composite createEditorPart(Composite parent, int style)
    {
        if (hexEditControl != null) throw new IllegalStateException("Editor part exists already");
        if (parent == null) throw new NullPointerException("Null parent");

        textsParent = parent;
        hexEditControl = new HexEditControl(textsParent, style);
        hexEditControl.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                if (fontText != null && !fontText.isDisposed())
                    fontText.dispose();
            }
        });
        if (fontData != null) {
            fontText = new Font(Display.getCurrent(), fontData);
            hexEditControl.setFont(fontText);
        }

        //hexEditControl.addLongSelectionListener(new ControlSelectionAdapter(ControlSelectionAdapter.UPDATE_POSITION_TEXT));
        hexEditControl.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                if (statusLine != null)
                    statusLine.updateInsertModeText(hexEditControl == null || !hexEditControl.isOverwriteMode());
            }
        });

        if (listOfStatusChangedListeners != null) {
            for (Listener listOfStatusChangedListener : listOfStatusChangedListeners) {
                hexEditControl.addListener(SWT.Modify, listOfStatusChangedListener);
            }
            listOfStatusChangedListeners = null;
        }

        if (listOfLongListeners != null) {
            for (SelectionListener listOfLongListener : listOfLongListeners) {
                hexEditControl.addLongSelectionListener(listOfLongListener);
            }
            listOfLongListeners = null;
        }

        {
            // Context menu
            MenuManager menuManager = new MenuManager();
            menuManager.setRemoveAllWhenShown(true);
            menuManager.addMenuListener(new IMenuListener() {
                @Override
                public void menuAboutToShow(IMenuManager manager)
                {
                    if (menuListener != null) {
                        menuListener.menuAboutToShow(manager);
                    }
                }
            });
            Menu contextMenu = menuManager.createContextMenu(hexEditControl.getHexText());
            hexEditControl.getHexText().setMenu(contextMenu);
            contextMenu = menuManager.createContextMenu(hexEditControl.getPreviewText());
            hexEditControl.getPreviewText().setMenu(contextMenu);
            //getSite().registerContextMenu(menuManager, this);
        }

        return hexEditControl;
    }

    void dispose()
    {
        if (content != null) {
            content.dispose();
        }
    }
    /**
     * Add a listener to changes of the 'dirty', 'insert/overwrite', 'selection' and 'canUndo/canRedo'
     * status
     *
     * @param aListener the listener to be notified of changes
     */
    public void addListener(Listener aListener)
    {
        if (aListener == null) return;

        if (hexEditControl == null) {
            if (listOfStatusChangedListeners == null)
                listOfStatusChangedListeners = new ArrayList<>();
            listOfStatusChangedListeners.add(aListener);
        } else {
            hexEditControl.addListener(SWT.Modify, aListener);
        }
    }


    /**
     * Adds a long selection listener. Events sent to the listener have long start and end points.
     *
     * @param listener the listener
     */
    public void addLongSelectionListener(SelectionListener listener)
    {
        if (listener == null)
            throw new IllegalArgumentException();

        if (hexEditControl == null) {
            if (listOfLongListeners == null)
                listOfLongListeners = new ArrayList<>();
            listOfLongListeners.add(listener);
        } else {
            hexEditControl.addLongSelectionListener(listener);
        }
    }


    /**
     * Tells whether the last action can be redone
     *
     * @return true: an action ca be redone
     */
    public boolean canRedo()
    {
        return hexEditControl != null && hexEditControl.canRedo();
    }


    /**
     * Tells whether the last action can be undone
     *
     * @return true: an action ca be undone
     */
    public boolean canUndo()
    {
        return hexEditControl != null && hexEditControl.canUndo();
    }


    /**
     * Copies selection into clipboard
     */
    public void doCopy()
    {
        if (hexEditControl == null) return;

        hexEditControl.copy();
    }


    /**
     * Cuts selection into clipboard
     */
    public void doCut()
    {
        if (hexEditControl == null) return;

        hexEditControl.cut();
    }


    /**
     * While in insert mode, deletes the selection
     */
    public void doDelete()
    {
        hexEditControl.deleteSelected();
    }


    /**
     * Open find dialog
     */
    public void doFind()
    {
        if (findDialog == null) {
            findDialog = new FindReplaceDialog(textsParent.getShell());
        }
        findDialog.setTarget(hexEditControl);
        findDialog.open();
    }


    /**
     * Open 'go to' dialog
     */
    public void doGoTo()
    {
        if (content.length() < 1L) return;

        if (goToDialog == null)
            goToDialog = new GoToDialog(textsParent.getShell());

        long location = goToDialog.open(content.length() - 1L);
        if (location >= 0L) {
            long button = goToDialog.getButtonPressed();
            if (button == 1)
                hexEditControl.showMark(location);
            else
                hexEditControl.selectBlock(location, location);
        }
    }


    /**
     * Pastes clipboard into editor
     */
    public void doPaste()
    {
        if (hexEditControl == null) return;

        hexEditControl.paste();
    }


    /**
     * Selects all file contents in editor
     */
    public void doSelectAll()
    {
        if (hexEditControl == null) return;

        hexEditControl.selectAll();
    }


    /**
     * Redoes the last undone action
     */
    public void doRedo()
    {
        hexEditControl.redo();
    }


    /**
     * Undoes the last action
     */
    public void doUndo()
    {
        hexEditControl.undo();
    }


    /**
     * Get the binary content
     *
     * @return the content being edited
     */
    public BinaryContent getContent()
    {
        return content;
    }


    /**
     * @see HexEditControl#getSelection()
     */
    public long[] getSelection()
    {
        if (hexEditControl == null) {
            return new long[]{0L, 0L};
        }

        return hexEditControl.getSelection();
    }


    /**
     * Get whether the content has been modified or not
     *
     * @return if changes have been performed
     */
    public boolean isDirty()
    {
        return content != null && content.isDirty();

    }


    /**
     * Tells whether the input is in overwrite or insert mode
     *
     * @return true: overwriting, false: inserting
     */
    public boolean isOverwriteMode()
    {
        return hexEditControl == null || hexEditControl.isOverwriteMode();

    }


    /**
     * Tells whether the input has text selected
     *
     * @return true: text is selected, false: no text selected
     */
    public boolean isTextSelected()
    {
        if (hexEditControl == null) return false;

        long[] selection = hexEditControl.getSelection();

        return selection[0] != selection[1];
    }


    /**
     * Open file for editing
     *
     * @param aFile the file to be edited
     * @throws IOException when a file has no read access
     */
    public void openFile(File aFile, String charset)
        throws IOException
    {
        if (content != null) {
            content.dispose();
            content = null;
        }
        content = new BinaryContent(aFile);  // throws IOException
        hexEditControl.setCharset(charset);
        hexEditControl.setContentProvider(content);
    }

    /**
     * Causes the text areas to have the keyboard focus
     */
    public void setFocus()
    {
        if (hexEditControl != null)
            hexEditControl.setFocus();

        if (statusLine != null) {
            statusLine.updateInsertModeText(hexEditControl == null || !hexEditControl.isOverwriteMode());
            if (hexEditControl != null) {
                if (hexEditControl.isSelected())
                    statusLine.updateSelectionValueText(hexEditControl.getSelection(), hexEditControl.getActualValue());
                else
                    statusLine.updatePositionValueText(hexEditControl.getCaretPos(), hexEditControl.getActualValue());
            } else {
                statusLine.updatePositionValueText(0L, (byte) 0);
            }
        }
    }


    /**
     * Delegates to HexTexts.setSelection(start, end)
     *
     * @see HexEditControl#setSelection(long, long)
     */
    public void setSelection(long start, long end)
    {
        hexEditControl.setSelection(start, end);
    }


    /**
     * Set the editor text font.
     *
     * @param aFont new font to be used; should be a constant char width font. Use null to set to the
     *              default font.
     */
    public void setTextFont(FontData aFont)
    {
        fontData = aFont;
        if (HexEditControl.DEFAULT_FONT_DATA.equals(fontData))
            fontData = null;
        // dispose it after setting new one (StyledTextRenderer 3.448 bug in line 994)
        Font fontToDispose = fontText;
        fontText = null;
        if (hexEditControl != null) {
            if (fontData != null)
                fontText = new Font(Display.getCurrent(), fontData);
            hexEditControl.setFont(fontText);
        }
        if (fontToDispose != null && !fontToDispose.isDisposed())
            fontToDispose.dispose();
    }

    public void setMenuListener(IMenuListener menuListener)
    {
        this.menuListener = menuListener;
    }

}
