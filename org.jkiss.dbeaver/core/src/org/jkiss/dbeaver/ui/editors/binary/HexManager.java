/*
 * binary, a java binary editor
 * Copyright (C) 2006, 2009 Jordi Bergenthal, pestatije(-at_)users.sourceforge.net
 * The official binary site is sourceforge.net/projects/binary
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.jkiss.dbeaver.ui.editors.binary;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.editors.binary.dialogs.FindReplaceDialog;
import org.jkiss.dbeaver.ui.editors.binary.dialogs.GoToDialog;
import org.jkiss.dbeaver.ui.editors.binary.dialogs.SelectBlockDialog;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Manager of the javahexeditor application, either in its standalone or Eclipse plugin version.
 * Manages creation of widgets, and executes menu actions, like File->Save. Call createEditorPart()
 * before any menu actions.
 *
 * @author Jordi
 */
public class HexManager {

    static Log log = LogFactory.getLog(HexManager.class);

    class MySelectionAdapter extends SelectionAdapter {
        static final int PASTE = 1;
        static final int DELETE = 2;
        static final int SELECT_ALL = 3;
        static final int FIND = 4;
        static final int CUT = 10;
        static final int COPY = 11;
        static final int GO_TO = 12;
        static final int GUIDELOCAL = 13;
        static final int GUIDEONLINE = 14;
        static final int PREFERENCES = 16;
        static final int REDO = 17;
        static final int TRIM = 18;
        static final int UNDO = 19;
        static final int UPDATE_POSITION_TEXT = 20;
        static final int SELECT_BLOCK = 21;
        int myAction = -1;

        MySelectionAdapter(int action)
        {
            myAction = action;
        }

        public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
        {
            switch (myAction) {
                case PASTE:
                    doPaste();
                    break;
                case DELETE:
                    doDelete();
                    break;
                case SELECT_ALL:
                    doSelectAll();
                    break;
                case FIND:
                    doFind();
                    break;
                case CUT:
                    doCut();
                    break;
                case COPY:
                    doCopy();
                    break;
                case GO_TO:
                    doGoTo();
                    break;
                case GUIDELOCAL:
                    doOpenUserGuideUrl(false);
                    break;
                case GUIDEONLINE:
                    doOpenUserGuideUrl(true);
                    break;
                case PREFERENCES:
                    doPreferences();
                    break;
                case REDO:
                    doRedo();
                    break;
                case TRIM:
                    doTrim();
                    break;
                case UNDO:
                    doUndo();
                    break;
                case UPDATE_POSITION_TEXT:
                    if (statusLine != null) {
                        if (hexTexts != null) {
                            if (hexTexts.isSelected())
                                statusLine.updateSelectionValueText(hexTexts.getSelection(), hexTexts.getActualValue());
                            else
                                statusLine.updatePositionValueText(hexTexts.getCaretPos(), hexTexts.getActualValue());
                        } else {
                            statusLine.updatePositionValueText(0L, (byte) 0);
                        }
                    }
                    break;
                case SELECT_BLOCK:
                    doSelectBlock();
                    break;
                default:
                    break;
            }
        }
    }


    static final String applicationName = "binary";
    static final String fontExtension = ".font";
    static final String nameExtension = ".name";
    static final String propertiesExtension = ".properties";
    static final String sizeExtension = ".size";
    static final String styleExtension = ".style";
    static final String textCouldNotRead = "Could not read from saved file, try reopening the editor";
    static final String textCouldNotWriteOnFile = "Could not write on file ";
    static final String textFindReplace = "&Find/Replace...\tCtrl+F";
    static final String textIsBeingUsed = "\nis currently being used by the editor.\nCannot overwrite file.";
    static final String textTheFile = "The file ";
    static final String textErrorFatal = "Unexpected fatal error";
    static final String textErrorSave = "Save error";
    static final String textErrorOutOfMemory = "Out of memory error";

    private BinaryContent content = null;
    private List<Object[]> findReplaceFindList = null;
    private List<Object[]> findReplaceReplaceList = null;
    private FontData fontData = null;  // when null uses default font
    private Font fontText = null;
    private File myFile = null;
    private String lastErrorMessage = null;
    private String lastErrorText = null;
    private java.util.List<Listener> listOfStatusChangedListeners = null;
    private java.util.List<SelectionListener> listOfLongListeners = null;
    private PreferencesManager preferences = null;

    // visual controls
    private Shell sShell = null;  //  @jve:decl-index=0:visual-constraint="70,45"
    private FindReplaceDialog findDialog = null;
    private GoToDialog goToDialog = null;
    private SelectBlockDialog selectBlockDialog = null;
    private HexTexts hexTexts = null;
    private StatusLine statusLine = null;
    private Composite textsParent = null;
    private IMenuListener menuListener;

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
        if (hexTexts != null) throw new IllegalStateException("Editor part exists already");
        if (parent == null) throw new NullPointerException("Cannot use null parent");

        textsParent = parent;
        hexTexts = new HexTexts(textsParent, style);
        hexTexts.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                if (fontText != null && !fontText.isDisposed())
                    fontText.dispose();
            }
        });
        if (fontData != null) {
            fontText = new Font(Display.getCurrent(), fontData);
            hexTexts.setFont(fontText);
        }

        hexTexts.addLongSelectionListener(new MySelectionAdapter(MySelectionAdapter.UPDATE_POSITION_TEXT));
        hexTexts.addListener(SWT.Modify, new Listener() {
            public void handleEvent(Event event)
            {
                if (statusLine != null)
                    statusLine.updateInsertModeText(hexTexts == null || !hexTexts.isOverwriteMode());
            }
        });

        if (listOfStatusChangedListeners != null) {
            for (Listener listOfStatusChangedListener : listOfStatusChangedListeners) {
                hexTexts.addListener(SWT.Modify, listOfStatusChangedListener);
            }
            listOfStatusChangedListeners = null;
        }

        if (listOfLongListeners != null) {
            for (SelectionListener listOfLongListener : listOfLongListeners) {
                hexTexts.addLongSelectionListener(listOfLongListener);
            }
            listOfLongListeners = null;
        }

        {
            // Context menu
            MenuManager menuManager = new MenuManager();
            menuManager.setRemoveAllWhenShown(true);
            menuManager.addMenuListener(new IMenuListener() {
                public void menuAboutToShow(IMenuManager manager)
                {
                    if (menuListener != null) {
                        menuListener.menuAboutToShow(manager);
                    }
                }
            });
            Menu contextMenu = menuManager.createContextMenu(hexTexts.getHexText());
            hexTexts.getHexText().setMenu(contextMenu);
            contextMenu = menuManager.createContextMenu(hexTexts.getPreviewText());
            hexTexts.getPreviewText().setMenu(contextMenu);
            //getSite().registerContextMenu(menuManager, this);
        }

        return hexTexts;
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

        if (hexTexts == null) {
            if (listOfStatusChangedListeners == null)
                listOfStatusChangedListeners = new ArrayList<Listener>();
            listOfStatusChangedListeners.add(aListener);
        } else {
            hexTexts.addListener(SWT.Modify, aListener);
        }
    }


    /**
     * Adds a long selection listener. Events sent to the listener have long start and end points.
     *
     * @param listener the listener
     * @see HexTexts.addLongSelectionListener(SelectionListener)
     * @see StyledText#addSelectionListener(org.eclipse.swt.events.SelectionListener)
     */
    public void addLongSelectionListener(SelectionListener listener)
    {
        if (listener == null)
            throw new IllegalArgumentException();

        if (hexTexts == null) {
            if (listOfLongListeners == null)
                listOfLongListeners = new ArrayList<SelectionListener>();
            listOfLongListeners.add(listener);
        } else {
            hexTexts.addLongSelectionListener(listener);
        }
    }


    /**
     * Tells whether the last action can be redone
     *
     * @return true: an action ca be redone
     */
    public boolean canRedo()
    {
        return hexTexts != null && hexTexts.canRedo();
    }


    /**
     * Tells whether the last action can be undone
     *
     * @return true: an action ca be undone
     */
    public boolean canUndo()
    {
        return hexTexts != null && hexTexts.canUndo();
    }


    /**
     * Creates status part of parent application.
     *
     * @param aParent           composite where the part will be drawn
     * @param aParent
     * @param withLeftSeparator so it can be put besides other status items (for plugin)
     * @throws NullPointerException if aParent is null
     */
    public void createStatusPart(Composite aParent, boolean withLeftSeparator)
    {
        if (aParent == null) throw new NullPointerException("Cannot use null parent");

        statusLine = new StatusLine(aParent, SWT.NONE, withLeftSeparator);
        if (hexTexts != null && hexTexts.getEnabled()) {
            statusLine.updateInsertModeText(!hexTexts.isOverwriteMode());
            if (hexTexts.isSelected())
                statusLine.updateSelectionValueText(hexTexts.getSelection(), hexTexts.getActualValue());
            else
                statusLine.updatePositionValueText(hexTexts.getCaretPos(), hexTexts.getActualValue());
        }
    }


    /**
     * Copies selection into clipboard
     */
    public void doCopy()
    {
        if (hexTexts == null) return;

        hexTexts.copy();
    }


    /**
     * Cuts selection into clipboard
     */
    public void doCut()
    {
        if (hexTexts == null) return;

        hexTexts.cut();
    }


    /**
     * While in insert mode, deletes the selection
     */
    public void doDelete()
    {
        hexTexts.deleteSelected();
    }


    /**
     * Open find dialog
     */
    public void doFind()
    {
        if (findDialog == null) {
            findDialog = new FindReplaceDialog(textsParent.getShell());
            if (findReplaceFindList == null) {
                findReplaceFindList = new ArrayList<Object[]>();
                findReplaceReplaceList = new ArrayList<Object[]>();
            }
            findDialog.setFindReplaceLists(findReplaceFindList, findReplaceReplaceList);
        }
        findDialog.setTarget(hexTexts);
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
                hexTexts.showMark(location);
            else
                hexTexts.selectBlock(location, location);
        }
    }

    /**
     * Open 'select block' dialog
     */
    public void doSelectBlock()
    {
        if (content.length() < 1L) return;

        if (selectBlockDialog == null)
            selectBlockDialog = new SelectBlockDialog(textsParent.getShell());
        long start = selectBlockDialog.open(hexTexts.getSelection(), content.length() - 1L);
        long end = selectBlockDialog.getFinalEndResult();
        if ((start >= 0L) && (end >= 0L) && (start != end)) {
            hexTexts.selectBlock(start, end);
        }
    }


    void doOpenUserGuideUrl(boolean online)
    {
        Program browser = Program.findProgram("html");
        if (browser == null) {
            MessageBox box = new MessageBox(sShell, SWT.ICON_WARNING | SWT.OK);
            box.setText("Browser not found");
            box.setMessage("Could not find a browser program to open html files.\n" +
                "Visit binary.sourceforge.net/userGuide.html to see the User Guide.\n");
            box.open();
            return;
        }

        String fileName = "userGuide.html";
        if (online) {
            fileName = "binary.sourceforge.net/" + fileName;
        } else {
            File theFile = new File(fileName);
            if (!theFile.exists()) {
                InputStream inStream = ClassLoader.getSystemResourceAsStream(fileName);
                if (inStream != null) {
                    try {
                        FileOutputStream outStream = new FileOutputStream(theFile);
                        byte[] buffer = new byte[512];
                        int read;
                        try {
                            while ((read = inStream.read(buffer)) > 0) {
                                outStream.write(buffer, 0, read);
                            }
                        }
                        finally {
                            outStream.close();
                        }
                    }
                    catch (IOException e) {
                        // Open browser anyway
                    }
                    try {
                        inStream.close();
                    }
                    catch (IOException e) {
                        // Open browser anyway
                    }
                }
            }
        }
        browser.execute(fileName);
    }


    /**
     * Pastes clipboard into editor
     */
    public void doPaste()
    {
        if (hexTexts == null) return;

        hexTexts.paste();
    }


    void doPreferences()
    {
        if (preferences == null) {
            preferences = new PreferencesManager(fontData == null ? HexTexts.fontDataDefault : fontData);
        }
        if (preferences.openDialog(textsParent.getShell()) == SWT.OK) {
            setTextFont(preferences.getFontData());
            writeNonDefaultFont();
        }
    }


    /**
     * Selects all file contents in editor
     */
    public void doSelectAll()
    {
        if (hexTexts == null) return;

        hexTexts.selectAll();
    }


    /**
     * Redoes the last undone action
     */
    public void doRedo()
    {
        hexTexts.redo();
    }


    /**
     * While in insert mode, trims the selection
     */
    public void doTrim()
    {
        hexTexts.deleteNotSelected();
    }


    /**
     * Undoes the last action
     */
    public void doUndo()
    {
        hexTexts.undo();
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
     * Get error message from last unsuccessful operation
     */
    public String getLastErrorMessage()
    {
        return lastErrorMessage;
    }


    String getLastErrorText()
    {
        return lastErrorText;
    }


    /**
     * @see HexTexts#getSelection()
     */
    public long[] getSelection()
    {
        if (hexTexts == null) {
            return new long[]{0L, 0L};
        }

        return hexTexts.getSelection();
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
        return hexTexts == null || hexTexts.isOverwriteMode();

    }


    /**
     * Tells whether the input has text selected
     *
     * @return true: text is selected, false: no text selected
     */
    public boolean isTextSelected()
    {
        if (hexTexts == null) return false;

        long[] selection = hexTexts.getSelection();

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
        content = new BinaryContent(aFile);  // throws IOException
        myFile = aFile;
        hexTexts.setCharset(charset);
        hexTexts.setContentProvider(content);
    }


    void readNonDefaultFont()
    {
        Properties properties = new Properties();
        try {
            FileInputStream file = new FileInputStream(applicationName + propertiesExtension);
            properties.load(file);
            file.close();
        }
        catch (IOException e) {
            return;
        }

        String name = properties.getProperty(applicationName + fontExtension + nameExtension);
        if (name == null) return;

        String styleString = properties.getProperty(applicationName + fontExtension + styleExtension);
        if (styleString == null) return;
        int style = PreferencesManager.fontStyleToInt(styleString);

        int size;
        try {
            size =
                Integer.parseInt(properties.getProperty(applicationName + fontExtension + sizeExtension));
        }
        catch (NumberFormatException e) {
            return;
        }

        fontData = new FontData(name, size, style);
    }


    void refreshTitleBar()
    {
        StringBuffer title = new StringBuffer();
        if (myFile != null) {
            if (content.isDirty())
                title.append('*');
            title.append(myFile.getName()).append(" - ");
        }
        title.append(applicationName);
        sShell.setText(title.toString());
    }


    /**
     * Reuse the status line control from another manager. Useful for multiple open editors
     *
     * @param other manager to copy its control from
     */
    public void reuseStatusControlFrom(HexManager other)
    {
        statusLine = other.statusLine;
    }


    /**
     * Set Find/Replace combo lists pre-exisitng values
     *
     * @param findList    previous find values
     * @param replaceList previous replace values
     */
    public void setFindReplaceLists(List<Object[]> findList, List<Object[]> replaceList)
    {
        findReplaceFindList = findList;
        findReplaceReplaceList = replaceList;
    }


    /**
     * Causes the text areas to have the keyboard focus
     */
    public void setFocus()
    {
        if (hexTexts != null)
            hexTexts.setFocus();

        if (statusLine != null) {
            statusLine.updateInsertModeText(hexTexts == null || !hexTexts.isOverwriteMode());
            if (hexTexts != null) {
                if (hexTexts.isSelected())
                    statusLine.updateSelectionValueText(hexTexts.getSelection(), hexTexts.getActualValue());
                else
                    statusLine.updatePositionValueText(hexTexts.getCaretPos(), hexTexts.getActualValue());
            } else {
                statusLine.updatePositionValueText(0L, (byte) 0);
            }
        }
    }


    /**
     * Delegates to HexTexts.setSelection(start, end)
     *
     * @see HexTexts#setSelection(long, long)
     */
    public void setSelection(long start, long end)
    {
        hexTexts.setSelection(start, end);
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
        if (HexTexts.fontDataDefault.equals(fontData))
            fontData = null;
        // dispose it after setting new one (StyledTextRenderer 3.448 bug in line 994)
        Font fontToDispose = fontText;
        fontText = null;
        if (hexTexts != null) {
            if (fontData != null)
                fontText = new Font(Display.getCurrent(), fontData);
            hexTexts.setFont(fontText);
        }
        if (fontToDispose != null && !fontToDispose.isDisposed())
            fontToDispose.dispose();
    }

    public void setMenuListener(IMenuListener menuListener)
    {
        this.menuListener = menuListener;
    }

    /**
     * Show an error box with the last error message
     *
     * @param aShell parent of the message box
     */
    public void showErrorBox(Shell aShell)
    {
        MessageBox aMessageBox = new MessageBox(aShell, SWT.ICON_ERROR | SWT.OK);
        aMessageBox.setText(getLastErrorText());
        aMessageBox.setMessage(getLastErrorMessage());
        aMessageBox.open();
    }


    /**
     * Show a file dialog with a save-as message
     *
     * @param aShell parent of the dialog
     */
    public File showSaveAsDialog(Shell aShell, boolean selection)
    {
        FileDialog dialog = new FileDialog(aShell, SWT.SAVE);
        if (selection)
            dialog.setText("Save Selection As");
        else
            dialog.setText("Save As");
        String fileText = dialog.open();
        if (fileText == null) return null;

        File file = new File(fileText);
        if (file.exists() && !showMessageBox(aShell, fileText)) return null;

        return file;
    }


    /**
     * Show a message box with a file-already-exists message
     *
     * @param aShell parent of the dialog
     */
    public boolean showMessageBox(Shell aShell, String file)
    {
        MessageBox aMessageBox = new MessageBox(aShell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
        aMessageBox.setText("File already exists");
        aMessageBox.setMessage("The file " + file + " already exists.\nOverwrite file?");

        return aMessageBox.open() == SWT.YES;
    }


    void writeNonDefaultFont()
    {
        File propertiesFile = new File(applicationName + propertiesExtension);
        if (fontData == null) {
            if (propertiesFile.exists()) {
                if (!propertiesFile.delete()) {
                    log.warn("Could not delete property file '" + propertiesFile.getAbsolutePath() + "'");
                }
            }
            return;
        }

        Properties properties = new Properties();
        properties.setProperty(applicationName + fontExtension + nameExtension, fontData.getName());
        properties.setProperty(applicationName + fontExtension + styleExtension,
                               PreferencesManager.fontStyleToString(fontData.getStyle()));
        properties.setProperty(applicationName + fontExtension + sizeExtension,
                               Integer.toString(fontData.getHeight()));
        try {
            FileOutputStream stream = new FileOutputStream(propertiesFile);
            properties.store(stream, null);
            stream.close();
        }
        catch (IOException e) {
            System.err.println(textCouldNotWriteOnFile + propertiesFile.getPath());
        }
    }
}
