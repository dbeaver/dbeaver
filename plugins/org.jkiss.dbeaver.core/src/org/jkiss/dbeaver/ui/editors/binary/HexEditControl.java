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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;


/**
 * A binary file editor, composed of two synchronized displays: an hexadecimal and a basic ascii char
 * display. The file size has no effect on the memory footprint of the editor. It has binary, ascii
 * and unicode find functionality. Use addListener(SWT.Modify, Listener) to listen to changes of the
 * 'dirty', 'overwrite/insert', 'selection' and 'canUndo/canRedo' status.
 *
 * @author Jordi
 */
public class HexEditControl extends Composite {

    private static final Log log = Log.getLog(HexEditControl.class);

    public static final String DEFAULT_FONT_NAME = "Courier New"; //$NON-NLS-1$"
    public static final FontData DEFAULT_FONT_DATA = new FontData(DEFAULT_FONT_NAME, 10, SWT.NORMAL);

    static final Color COLOR_BLUE = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
    static final Color COLOR_LIGHT_SHADOW = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
    static final Color COLOR_NORMAL_SHADOW = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);

    /**
     * Map of displayed chars. Chars that cannot be displayed correctly are changed for a '.' char.
     * There are differences on which chars can correctly be displayed in each operating system,
     * charset encoding, or font system.
     */
    static String headerRow = null;
    static final byte[] hexToNibble = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1, -1, -1, -1, -1,
        10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 10, 11, 12, 13, 14, 15};
    static final int maxScreenResolution = 1920;
    static final int minCharSize = 5;
    static final int SET_TEXT = 0;
    static final int SHIFT_FORWARD = 1;  // frame
    static final int SHIFT_BACKWARD = 2;

    static final int BYTE_WIDTH_16 = 16; 
    static final int BYTE_WIDTH_8 = 8;
    static final int BYTE_WIDTH_4 = 4;
    static final int BYTE_WIDTH_2 = 2;

    static {
        // Compose header row
        StringBuilder rowChars = new StringBuilder();
        for (int i = 0; i < maxScreenResolution / minCharSize / 3; ++i)
            rowChars.append(GeneralUtils.byteToHex[i & 0x0ff]).append(' ');
        headerRow = rowChars.toString().toUpperCase();
    }

    public final char[] byteToChar = new char[256];

    private boolean readOnly;
    private int charsForFileSizeAddress = 0;
    private String charset = null;
    private boolean delayedInQueue = false;
    private Runnable delayedWaiting = null;
    private boolean dragging = false;
    private int fontCharWidth = -1;
    private List<Integer> highlightRangesInScreen = null;
    private List<Long> mergeChangeRanges = null;
    private List<Integer> mergeHighlightRanges = null;
    private int mergeIndexChange = -2;
    private int mergeIndexHighlight = -2;
    private boolean mergeRangesIsBlue = false;
    private boolean mergeRangesIsHighlight = false;
    private int mergeRangesPosition = -1;
    private final int charsForAddress;  // Files up to 16 Ters: 11 binary digits + ':'
    private int bytesPerLine;
    private boolean caretStickToStart = false;  // stick to end
    private BinaryClipboard myClipboard = null;
    private BinaryContent content = null;
    private long endPosition = 0L;
    private BinaryTextFinder finder = null;
    private boolean isInserting = true;
    private KeyListener keyAdapter = new ControlKeyAdapter();
    private int lastFocusedTextArea = -1;  // 1 or 2;
    private long lastLocationPosition = -1L;
    private List<SelectionListener> longSelectionListeners = null;
    private long previousFindEnd = -1;
    private boolean previousFindIgnoredCase = false;
    private String previousFindString = null;
    private boolean previousFindStringWasHex = false;
    private int previousLine = -1;
    private long previousRedrawStart = -1;
    private long startPosition = 0L;
    private long textAreasStart = -1L;
    private int upANibble = 0;  // always 0 or 1
    private int numberOfLines = 16;
    private int numberOfLines_1 = numberOfLines - 1;
    private boolean stopSearching = false;
    private byte[] tmpRawBuffer = new byte[maxScreenResolution / minCharSize / 3 * maxScreenResolution / minCharSize];
    private int verticalBarFactor = 0;

    // visual components
    private Color colorCaretLine = null;
    private Color colorHighlight = null;
    private Font fontCurrent = null;  // disposed externally
    private Font fontDefault = null;  // disposed internally
    private GridData textGridData = null;
    private GridData previewGridData = null;
    private GC styledText1GC = null;
    private GC styledText2GC = null;
    private Text linesTextSeparator = null;
    private StyledText linesText = null;

    private StyledText hexHeaderText = null;
    private StyledText hexText = null;

    private Text previewTextSeparator = null;
    private StyledText previewText = null;

    /**
     * Get long selection start and end points. Helper method for long selection listeners.
     * The start point is formed by event.width as the most significant int and event.x as the least
     * significant int. The end point is similarly formed by event.height and event.y
     */
    public static long[] getLongSelection(SelectionEvent event)
    {
        return new long[]{((long) event.width) << 32 | (event.x & 0x0ffffffffL),
            ((long) event.height) << 32 | (event.y & 0x0ffffffffL)};
    }

    /**
     * Converts a hex String to byte[]. Will convert full bytes only, odd number of hex characters will
     * have a leading '0' added. Big endian.
     *
     * @param hexString an hex string (ie. "0fdA1").
     * @return the byte[] value of the hex string
     */
    public static byte[] hexStringToByte(String hexString)
    {
        if ((hexString.length() & 1) == 1)  // nibbles promote to a full byte
            hexString = '0' + hexString;
        byte[] tmp = new byte[hexString.length() / 2];
        for (int i = 0; i < tmp.length; ++i) {
            String hexByte = hexString.substring(i * 2, i * 2 + 2);
            tmp[i] = (byte) Integer.parseInt(hexByte, 16);
        }

        return tmp;
    }

    private class ControlKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e)
        {
            switch (e.keyCode) {
                case SWT.ARROW_UP:
                case SWT.ARROW_DOWN:
                case SWT.ARROW_LEFT:
                case SWT.ARROW_RIGHT:
                case SWT.END:
                case SWT.HOME:
                case SWT.PAGE_UP:
                case SWT.PAGE_DOWN:
                    boolean selection = startPosition != endPosition;
                    boolean ctrlKey = (e.stateMask & SWT.CONTROL) != 0;
                    if ((e.stateMask & SWT.SHIFT) != 0) {  // shift mod2
                        long newPos = doNavigateKeyPressed(ctrlKey, e.keyCode, getCaretPos(), false);
                        shiftStartAndEnd(newPos);
                    } else {  // if no modifier or control or alt
                        endPosition = startPosition = doNavigateKeyPressed(
                            ctrlKey,
                            e.keyCode,
                            getCaretPos(),
                            e.widget == hexText && !isInserting);
                        caretStickToStart = false;
                    }
                    ensureCaretIsVisible();
                    Runnable delayed = new Runnable() {
                        @Override
                        public void run()
                        {
                            redrawTextAreas(false);
                            runnableEnd();
                        }
                    };
                    runnableAdd(delayed);
                    notifyLongSelectionListeners();
//                    if (selection != (startPosition != endPosition))
//                        notifyListeners(SWT.Modify, null);
                    e.doit = false;
                    break;
                case SWT.INSERT:
                    if ((e.stateMask & SWT.MODIFIER_MASK) == 0) {
                        redrawCaret(true);
                    } else if (!readOnly && e.stateMask == SWT.SHIFT) {
                        paste();
                    } else if (e.stateMask == SWT.CONTROL) {
                        copy();
                    }
                    break;
                case 'a':
                    if (e.stateMask == SWT.CONTROL)  // control mod1
                        selectAll();
                    break;
                case 'c':
                    if (e.stateMask == SWT.CONTROL)  // control mod1
                        copy();
                    break;
                case 'v':
                    if (!readOnly && e.stateMask == SWT.CONTROL)  // control mod1
                        paste();
                    break;
                case 'x':
                    if (!readOnly && e.stateMask == SWT.CONTROL)  // control mod1
                        cut();
                    break;
                case 'y':
                    if (!readOnly && e.stateMask == SWT.CONTROL)  // control mod1
                        redo();
                    break;
                case 'z':
                    if (!readOnly && e.stateMask == SWT.CONTROL)  // control mod1
                        undo();
                    break;
                default:
                    break;
            }
        }
    }

    private class ControlMouseAdapter extends MouseAdapter {
        int charLen;

        public ControlMouseAdapter(boolean hexContent)
        {
            charLen = 1;
            if (hexContent) charLen = 3;
        }

        @Override
        public void mouseDown(MouseEvent e)
        {
            if (e.button == 1)
                dragging = true;
            int textOffset;
            try {
                textOffset = ((StyledText) e.widget).getOffsetAtLocation(new Point(e.x, e.y));
            } catch (IllegalArgumentException ex) {
                textOffset = ((StyledText) e.widget).getCharCount();
            }
            int byteOffset = textOffset / charLen;
            ((StyledText) e.widget).setTopIndex(0);
            if (e.button == 1 && (e.stateMask & SWT.MODIFIER_MASK & ~SWT.SHIFT) == 0) {// no modif or shift
                if ((e.stateMask & SWT.MODIFIER_MASK) == 0) {
                    caretStickToStart = false;
                    startPosition = endPosition = textAreasStart + byteOffset;
                } else {  // shift
                    shiftStartAndEnd(textAreasStart + byteOffset);
                }
                refreshCaretsPosition();
                setFocus();
                refreshSelections();
                //notifyListeners(SWT.Modify, null);
                notifyLongSelectionListeners();
            }
        }

        @Override
        public void mouseUp(MouseEvent e)
        {
            if (e.button == 1)
                dragging = false;
        }
    }

    private class ControlPaintAdapter implements PaintListener {
        boolean hexContent = false;

        ControlPaintAdapter(boolean isHexText)
        {
            hexContent = isHexText;
        }

        @Override
        public void paintControl(PaintEvent event)
        {
            event.gc.setForeground(COLOR_LIGHT_SHADOW);
            int lineWidth = 1;
            int charLen = 1;
            int rightHalfWidth = 0;  // is 1, but better to tread on leftmost char pixel than rightmost one
            if (hexContent) {
                lineWidth = fontCharWidth;
                charLen = 3;
                rightHalfWidth = (lineWidth + 1) / 2;  // line spans to both sides of its position
            }
            event.gc.setLineWidth(lineWidth);
            for (int block = BYTE_WIDTH_16; block <= bytesPerLine; block += BYTE_WIDTH_16) {
                int xPos = (charLen * block) * fontCharWidth - rightHalfWidth;
                event.gc.drawLine(xPos, event.y, xPos, event.y + event.height);
            }
        }
    }

    private class ControlSelectionAdapter extends SelectionAdapter implements SelectionListener {
        int charLen;

        public ControlSelectionAdapter(boolean hexContent)
        {
            charLen = 1;
            if (hexContent) charLen = 3;
        }

        @Override
        public void widgetSelected(SelectionEvent e)
        {
            if (!dragging)
                return;

            boolean selection = startPosition != endPosition;
            int lower = e.x / charLen;
            int higher = e.y / charLen;
            int caretPos = ((StyledText) e.widget).getCaretOffset() / charLen;
            caretStickToStart = caretPos < higher || caretPos < lower;
            if (lower > higher) {
                lower = higher;
                higher = e.x / charLen;
            }

            select(textAreasStart + lower, textAreasStart + higher);
//            if (selection != (startPosition != endPosition))
//                notifyListeners(SWT.Modify, null);

            redrawTextAreas(false);
        }
    }


    private class ControlTraverseAdapter implements TraverseListener {
        @Override
        public void keyTraversed(TraverseEvent e)
        {
            if (e.detail == SWT.TRAVERSE_TAB_NEXT)
                e.doit = true;
        }
    }

    private class ControlVerifyKeyAdapter implements VerifyKeyListener {
        @Override
        public void verifyKey(VerifyEvent e)
        {
            if (readOnly) {
                return;
            }
            if ((e.character == SWT.DEL || e.character == SWT.BS) && isInserting) {
                if (!deleteSelected()) {
                    if (e.character == SWT.BS) {
                        startPosition += upANibble;
                        if (startPosition > 0L) {
                            content.delete(startPosition - 1L, 1L);
                            endPosition = --startPosition;
                        }
                    } else {  // e.character == SWT.DEL
                        content.delete(startPosition, 1L);
                    }
                    ensureWholeScreenIsVisible();
                    ensureCaretIsVisible();
                    Runnable delayed = new Runnable() {
                        @Override
                        public void run()
                        {
                            redrawTextAreas(true);
                            runnableEnd();
                        }
                    };
                    runnableAdd(delayed);
                    updateScrollBar();

                    notifyListeners(SWT.Modify, null);
                    notifyLongSelectionListeners();
                }
                upANibble = 0;
            } else {
                doModifyKeyPressed(e);
            }

            e.doit = false;
        }
    }

    public HexEditControl(final Composite parent, int style)
    {
        this(parent, style, 12, 16);
    }

    /**
     * Create a binary text editor
     *
     * @param parent parent in the widget hierarchy
     * @param style  not used for the moment
     */
    public HexEditControl(final Composite parent, int style, int charsForAddress, int bytesPerLine)
    {
        super(parent, style | SWT.V_SCROLL);

        this.readOnly = (style & SWT.READ_ONLY) != 0;
        this.charsForAddress = charsForAddress;
        this.bytesPerLine = bytesPerLine;
        this.colorCaretLine = new Color(Display.getCurrent(), 232, 242, 254);  // very light blue
        this.colorHighlight = new Color(Display.getCurrent(), 255, 248, 147);  // mellow yellow
        this.highlightRangesInScreen = new ArrayList<>();

        this.myClipboard = new BinaryClipboard(parent.getDisplay());
        this.longSelectionListeners = new ArrayList<>();
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                colorCaretLine.dispose();
                colorHighlight.dispose();
                if (fontDefault != null && !fontDefault.isDisposed())
                    fontDefault.dispose();
                try {
                    myClipboard.dispose();
                } catch (IOException ex) {
                    log.warn("Can't cleanup clipboard temporary data");
                }
            }
        });
        initialize();
        this.lastFocusedTextArea = 1;
        this.previousLine = -1;
    }

    public BinaryTextFinder getFinder()
    {
        return finder;
    }

    /**
     * compose byte-to-char map
     */
    private void composeByteToCharMap()
    {
        if (charset == null || previewText == null) return;

        CharsetDecoder decoder = Charset.forName(charset).newDecoder().
            onMalformedInput(CodingErrorAction.REPLACE).
            onUnmappableCharacter(CodingErrorAction.REPLACE).
            replaceWith(".");
        ByteBuffer bb = ByteBuffer.allocate(1);
        CharBuffer cb = CharBuffer.allocate(1);
        for (int i = 0; i < 256; ++i) {
            if (i < 0x20 || i == 0x7f) {
                byteToChar[i] = (char) (160 + i);
            } else {
                bb.clear();
                bb.put((byte) i);
                bb.rewind();
                cb.clear();
                decoder.reset();
                decoder.decode(bb, cb, true);
                decoder.flush(cb);
                cb.rewind();
                char decoded = cb.get();
                // neither font metrics nor graphic context work for charset 8859-1 chars between 128 and
                // 159
                // It works too slow. Dumn with it.
                byteToChar[i] = decoded;
            }
        }
    }

    public void setCharset(String name)
    {
        if (CommonUtils.isEmpty(name)) {
            name = GeneralUtils.getDefaultFileEncoding();
        }
        charset = name;
        composeByteToCharMap();
    }

    public StyledText getHexText()
    {
        return hexText;
    }

    public StyledText getPreviewText()
    {
        return previewText;
    }

    /**
     * redraw the caret with respect of Inserting/Overwriting mode
     */
    public void redrawCaret(boolean focus)
    {
        drawUnfocusedCaret(false);
        setCaretsSize(focus ? (!isInserting) : isInserting);
        if (isInserting && upANibble != 0) {
            upANibble = 0;
            refreshCaretsPosition();
            if (focus) setFocus();
        } else {
            drawUnfocusedCaret(true);
        }
//        if (focus) notifyListeners(SWT.Modify, null);
    }

    /**
     * Adds a long selection listener. Events sent to the listener have long start and end points.
     * The start point is formed by event.width as the most significant int and event.x as the least
     * significant int. The end point is similarly formed by event.height and event.y
     * A listener can obtain the long selection with this code: getLongSelection(SelectionEvent)
     * long start = ((long)event.width) << 32 | (event.x & 0x0ffffffffL)
     * Similarly for the end point:
     * long end = ((long)event.height) << 32 | (event.y & 0x0ffffffffL)
     *
     * @param listener the listener
     * @see StyledText#addSelectionListener(org.eclipse.swt.events.SelectionListener)
     */
    public void addLongSelectionListener(SelectionListener listener)
    {
        if (listener == null)
            throw new IllegalArgumentException();

        if (!longSelectionListeners.contains(listener))
            longSelectionListeners.add(listener);
    }

    /**
     * This method initializes composite
     */
    private void initialize() {
        GridLayout gridLayout1 = new GridLayout();
        gridLayout1.numColumns = 3;
        gridLayout1.marginHeight = 0;
        gridLayout1.verticalSpacing = 0;
        gridLayout1.horizontalSpacing = 0;
        gridLayout1.marginWidth = 0;
        setLayout(gridLayout1);

        FocusListener myFocusAdapter = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                drawUnfocusedCaret(false);
                lastFocusedTextArea = 1;
                if (e.widget == previewText)
                    lastFocusedTextArea = 2;
                DBeaverUI.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        drawUnfocusedCaret(true);
                    }
                });
            }
        };
        Caret defaultCaret;
        Caret nonDefaultCaret;

        {
            // Lines
            Composite linesColumn = new Composite(this, SWT.NONE);
            GridLayout columnLayout = new GridLayout();
            columnLayout.marginHeight = 0;
            columnLayout.verticalSpacing = 1;
            columnLayout.horizontalSpacing = 0;
            columnLayout.marginWidth = 0;
            linesColumn.setLayout(columnLayout);
            //linesColumn.setBackground(COLOR_LIGHT_SHADOW);
            GridData gridDataColumn = new GridData(SWT.BEGINNING, SWT.FILL, false, true);
            linesColumn.setLayoutData(gridDataColumn);

            GridData gridDataTextSeparator = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
            gridDataTextSeparator.widthHint = 10;
            linesTextSeparator = new Text(linesColumn, SWT.SEPARATOR);
            linesTextSeparator.setEnabled(false);
            linesTextSeparator.setBackground(COLOR_LIGHT_SHADOW);
            linesTextSeparator.setLayoutData(gridDataTextSeparator);

            linesText = new StyledText(linesColumn, SWT.MULTI | SWT.READ_ONLY);
            linesText.setEditable(false);
            linesText.setEnabled(false);
            //linesText.setBackground(COLOR_LIGHT_SHADOW);
            //linesText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
            fontDefault = new Font(Display.getCurrent(), DEFAULT_FONT_DATA);
            fontCurrent = fontDefault;
            linesText.setFont(fontCurrent);
            GC styledTextGC = new GC(linesText);
            fontCharWidth = styledTextGC.getFontMetrics().getAverageCharWidth();
            styledTextGC.dispose();
            GridData gridDataAddresses = new GridData(SWT.BEGINNING, SWT.FILL, false, true);
            gridDataAddresses.heightHint = numberOfLines * linesText.getLineHeight();
            linesText.setLayoutData(gridDataAddresses);
            setAddressesGridDataWidthHint();
            linesText.setContent(new DisplayedContent(charsForAddress, numberOfLines));
        }

        {
            // Hex
            Composite hexColumn = new Composite(this, SWT.NONE);
            GridLayout column1Layout = new GridLayout();
            column1Layout.marginHeight = 0;
            column1Layout.verticalSpacing = 1;
            column1Layout.horizontalSpacing = 0;
            column1Layout.marginWidth = 0;
            hexColumn.setLayout(column1Layout);
            //hexColumn.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            GridData gridDataColumn1 = new GridData(SWT.BEGINNING, SWT.FILL, false, true);
            hexColumn.setLayoutData(gridDataColumn1);

            Composite hexHeaderGroup = new Composite(hexColumn, SWT.NONE);
            //hexHeaderGroup.setBackground(COLOR_LIGHT_SHADOW);
            GridLayout column1HeaderLayout = new GridLayout();
            column1HeaderLayout.marginHeight = 0;
            column1HeaderLayout.marginWidth = 0;
            hexHeaderGroup.setLayout(column1HeaderLayout);
            GridData gridDataColumn1Header = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
            hexHeaderGroup.setLayoutData(gridDataColumn1Header);

            GridData gridData = new GridData();
            gridData.horizontalIndent = 1;
            hexHeaderText = new StyledText(hexHeaderGroup, SWT.SINGLE | SWT.READ_ONLY);
            hexHeaderText.setEditable(false);
            hexHeaderText.setEnabled(false);
            hexHeaderText.setBackground(COLOR_LIGHT_SHADOW);
            //hexHeaderText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
            hexHeaderText.setLayoutData(gridData);
            hexHeaderText.setFont(fontCurrent);
            refreshHeader();

            hexText = new StyledText(hexColumn, SWT.MULTI);
            hexText.setFont(fontCurrent);
            if (readOnly) {
                hexText.setEditable(false);
            }
            styledText1GC = new GC(hexText);
            int width = bytesPerLine * 3 * fontCharWidth;
            textGridData = new GridData();
            textGridData.horizontalIndent = 1;
            textGridData.verticalAlignment = SWT.FILL;
            textGridData.widthHint = hexText.computeTrim(0, 0, width, 0).width;
            textGridData.grabExcessVerticalSpace = true;
            hexText.setLayoutData(textGridData);
            hexText.addKeyListener(keyAdapter);
            hexText.addFocusListener(myFocusAdapter);
            hexText.addMouseListener(new ControlMouseAdapter(true));
            hexText.addPaintListener(new ControlPaintAdapter(true));
            hexText.addTraverseListener(new ControlTraverseAdapter());
            hexText.addVerifyKeyListener(new ControlVerifyKeyAdapter());
            hexText.setContent(new DisplayedContent(bytesPerLine * 3, numberOfLines));
            hexText.setDoubleClickEnabled(false);
            hexText.addSelectionListener(new ControlSelectionAdapter(true));
            // StyledText.setCaretOffset() version 3.448 bug resets the caret size if using the default one,
            // so we use not the default one.
            defaultCaret = hexText.getCaret();
            nonDefaultCaret = new Caret(defaultCaret.getParent(), defaultCaret.getStyle());
            nonDefaultCaret.setBounds(defaultCaret.getBounds());
            hexText.setCaret(nonDefaultCaret);
        }

        {
            // Preview
            Composite previewColumn = new Composite(this, SWT.NONE);
            GridLayout column2Layout = new GridLayout();
            column2Layout.marginHeight = 0;
            column2Layout.verticalSpacing = 1;
            column2Layout.horizontalSpacing = 0;
            column2Layout.marginWidth = 0;
            previewColumn.setLayout(column2Layout);
            //previewColumn.setBackground(hexText.getBackground());
            GridData gridDataColumn2 = new GridData(SWT.FILL, SWT.FILL, true, true);
            previewColumn.setLayoutData(gridDataColumn2);

            GridData gridDataTextSeparator2 = new GridData();
            gridDataTextSeparator2.horizontalAlignment = SWT.FILL;
            gridDataTextSeparator2.verticalAlignment = SWT.FILL;
            gridDataTextSeparator2.grabExcessHorizontalSpace = true;
            previewTextSeparator = new Text(previewColumn, SWT.SEPARATOR);
            previewTextSeparator.setEnabled(false);
            previewTextSeparator.setBackground(COLOR_LIGHT_SHADOW);
            previewTextSeparator.setLayoutData(gridDataTextSeparator2);
            makeFirstRowSameHeight();

            previewText = new StyledText(previewColumn, SWT.MULTI);
            previewText.setFont(fontCurrent);
            if (readOnly) {
                previewText.setEditable(false);
            }
            int width = bytesPerLine * fontCharWidth + 1;  // one pixel for caret in last linesColumn
            previewGridData = new GridData();
            previewGridData.verticalAlignment = SWT.FILL;
            previewGridData.widthHint = previewText.computeTrim(0, 0, width, 0).width;
            previewGridData.grabExcessVerticalSpace = true;
            previewText.setLayoutData(previewGridData);
            previewText.addKeyListener(keyAdapter);
            previewText.addFocusListener(myFocusAdapter);
            previewText.addMouseListener(new ControlMouseAdapter(false));
            previewText.addPaintListener(new ControlPaintAdapter(false));
            previewText.addTraverseListener(new ControlTraverseAdapter());
            previewText.addVerifyKeyListener(new ControlVerifyKeyAdapter());
            previewText.setContent(new DisplayedContent(bytesPerLine, numberOfLines));
            previewText.setDoubleClickEnabled(false);
            previewText.addSelectionListener(new ControlSelectionAdapter(false));
            // StyledText.setCaretOffset() version 3.448 bug resets the caret size if using the default one,
            // so we use not the default one.
            defaultCaret = previewText.getCaret();
            nonDefaultCaret = new Caret(defaultCaret.getParent(), defaultCaret.getStyle());
            nonDefaultCaret.setBounds(defaultCaret.getBounds());
            previewText.setCaret(nonDefaultCaret);
            styledText2GC = new GC(previewText);
            setCharset(null);
        }

        super.setFont(fontCurrent);
        ScrollBar vertical = getVerticalBar();
        vertical.setSelection(0);
        vertical.setMinimum(0);
        vertical.setIncrement(1);
        vertical.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                e.doit = false;
                long previousStart = textAreasStart;
                textAreasStart =
                    (((long) getVerticalBar().getSelection()) << verticalBarFactor) * (long) bytesPerLine;
                if (previousStart == textAreasStart) return;

                Runnable delayed = new Runnable() {
                    @Override
                    public void run()
                    {
                        redrawTextAreas(false);
                        setFocus();
                        runnableEnd();
                    }
                };
                runnableAdd(delayed);
            }
        });
        updateScrollBar();
        addMouseListener(new org.eclipse.swt.events.MouseAdapter() {
            @Override
            public void mouseDown(org.eclipse.swt.events.MouseEvent e)
            {
                setFocus();
            }
        });
        addControlListener(new org.eclipse.swt.events.ControlAdapter() {
            @Override
            public void controlResized(org.eclipse.swt.events.ControlEvent e)
            {
                updateTextsMetrics();
            }
        });
        addDisposeListener(new org.eclipse.swt.events.DisposeListener() {
            @Override
            public void widgetDisposed(org.eclipse.swt.events.DisposeEvent e)
            {
                if (content != null)
                    content.dispose();
            }
        });
    }


    /**
     * Tells whether the last action can be redone
     *
     * @return true: an action ca be redone
     */
    public boolean canRedo()
    {
        return content != null && content.canRedo();
    }


    /**
     * Tells whether the last action can be undone
     *
     * @return true: an action ca be undone
     */
    public boolean canUndo()
    {
        return content != null && content.canUndo();
    }


    /**
     * Copies the selection into the clipboard. If nothing is selected leaves the clipboard with its
     * current contents. The clipboard will hold text data (for pasting into a text editor) and binary
     * data (internal for HexText). Text data is limited to 4Mbytes, binary data is limited by disk space.
     */
    public void copy()
    {
        if (startPosition >= endPosition) return;

        myClipboard.setContents(content, startPosition, endPosition - startPosition);
    }


    StringBuilder cookAddresses(long address, int limit)
    {
        StringBuilder theText = new StringBuilder();
        for (int i = 0; i < limit; i += bytesPerLine, address += bytesPerLine) {
            boolean indenting = true;
            for (int j = (charsForAddress - 2) * 4; j > 0; j -= 4) {
                int nibble = ((int) (address >>> j)) & 0x0f;
                if (nibble != 0)
                    indenting = false;
                if (indenting) {
                    if (j >= (charsForFileSizeAddress * 4))
                        theText.append(' ');
                    else
                        theText.append('0');
                } else {
                    theText.append(GeneralUtils.nibbleToHex[nibble]);
                }
            }
            theText.append(GeneralUtils.nibbleToHex[((int) address) & 0x0f]).append(':');
        }

        return theText;
    }


    StringBuilder cookTexts(boolean isHexOutput, int length)
    {
        if (length > tmpRawBuffer.length) length = tmpRawBuffer.length;
        StringBuilder result;

        if (isHexOutput) {
            result = new StringBuilder(length * 3);
            for (int i = 0; i < length; ++i) {
                result.append(GeneralUtils.byteToHex[tmpRawBuffer[i] & 0x0ff]).append(' ');
            }
        } else {
            result = new StringBuilder(length);
            for (int i = 0; i < length; ++i) {
                result.append(byteToChar[tmpRawBuffer[i] & 0x0ff]);
            }
        }

        return result;
    }


    /**
     * Calls copy();deleteSelected();
     */
    public void cut()
    {
        copy();
        deleteSelected();
    }


    /**
     * While in insert mode, deletes the selection
     *
     * @return did delete something
     */
    public boolean deleteSelected()
    {
        if (!handleSelectedPreModify()) {
            return false;
        }
        upANibble = 0;
        ensureWholeScreenIsVisible();
        restoreStateAfterModify();

        return true;
    }


    void doModifyKeyPressed(KeyEvent event)
    {
        char aChar = event.character;
        if (aChar == '\0' || aChar == '\b' || aChar == '\u007f' || event.stateMask == SWT.CTRL ||
            event.widget == hexText && ((event.stateMask & SWT.MODIFIER_MASK) != 0 ||
                aChar < '0' || aChar > '9' && aChar < 'A' || aChar > 'F' && aChar < 'a' || aChar > 'f')) {
            return;
        }
        boolean origInserting = isInserting;
        if (getCaretPos() == content.length() && !isInserting) {
//            ensureCaretIsVisible();
//            redrawTextAreas(false);
//            return;
            isInserting = true;
        }
        try {
            handleSelectedPreModify();
            try {
                if (isInserting) {
                    if (event.widget == previewText) {
                        content.insert((byte) aChar, getCaretPos());
                    } else if (upANibble == 0) {
                        content.insert((byte) (hexToNibble[aChar - '0'] << 4), getCaretPos());
                    } else {
                        content.overwrite(hexToNibble[aChar - '0'], 4, 4, getCaretPos());
                    }
                } else {
                    if (event.widget == previewText) {
                        content.overwrite((byte) aChar, getCaretPos());
                    } else {
                        content.overwrite(hexToNibble[aChar - '0'], upANibble * 4, 4, getCaretPos());
                    }
                    content.get(ByteBuffer.wrap(tmpRawBuffer, 0, 1), null, getCaretPos());
                    int offset = (int) (getCaretPos() - textAreasStart);
                    hexText.replaceTextRange(offset * 3, 2, GeneralUtils.byteToHex[tmpRawBuffer[0] & 0x0ff]);
                    hexText.setStyleRange(new StyleRange(offset * 3, 2, COLOR_BLUE, null));
                    previewText.replaceTextRange(
                        offset,
                        1,
                        Character.toString(byteToChar[tmpRawBuffer[0] & 0x0ff]));
                    previewText.setStyleRange(new StyleRange(offset, 1, COLOR_BLUE, null));
                }
            } catch (IOException e) {
                log.warn(e);
            }
            startPosition = endPosition = incrementPosWithinLimits(getCaretPos(), event.widget == hexText);
            Runnable delayed = new Runnable() {
                @Override
                public void run()
                {
                    ensureCaretIsVisible();
                    redrawTextAreas(false);
                    if (isInserting) {
                        updateScrollBar();
                        redrawTextAreas(true);
                    }
                    refreshSelections();
                    runnableEnd();
                }
            };
            runnableAdd(delayed);
            notifyListeners(SWT.Modify, null);
            notifyLongSelectionListeners();
        } finally {
            isInserting = origInserting;
        }
    }


    private long doNavigateKeyPressed(boolean ctrlKey, int keyCode, long oldPos, boolean countNibbles)
    {
        if (!countNibbles)
            upANibble = 0;
        switch (keyCode) {
            case SWT.ARROW_UP:
                if (oldPos >= bytesPerLine) oldPos -= bytesPerLine;
                break;

            case SWT.ARROW_DOWN:
                if (oldPos <= content.length() - bytesPerLine) oldPos += bytesPerLine;
                if (countNibbles && oldPos == content.length()) upANibble = 0;
                break;

            case SWT.ARROW_LEFT:
                if (countNibbles && (oldPos > 0 || oldPos == 0 && upANibble > 0)) {
                    if (upANibble == 0) --oldPos;
                    upANibble ^= 1;  // 1->0, 0->1
                }
                if (!countNibbles && oldPos > 0)
                    --oldPos;
                break;

            case SWT.ARROW_RIGHT:
                oldPos = incrementPosWithinLimits(oldPos, countNibbles);
                break;

            case SWT.END:
                if (ctrlKey) {
                    oldPos = content.length();
                } else {
                    oldPos = oldPos - oldPos % bytesPerLine + bytesPerLine - 1L;
                    if (oldPos >= content.length()) oldPos = content.length();
                }
                upANibble = 0;
                if (countNibbles && oldPos < content.length()) upANibble = 1;
                break;

            case SWT.HOME:
                if (ctrlKey) {
                    oldPos = 0;
                } else {
                    oldPos = oldPos - oldPos % bytesPerLine;
                }
                upANibble = 0;
                break;

            case SWT.PAGE_UP:
                if (oldPos >= bytesPerLine) {
                    oldPos = oldPos - bytesPerLine * numberOfLines_1;
                    if (oldPos < 0L)
                        oldPos = (oldPos + bytesPerLine * numberOfLines_1) % bytesPerLine;
                }
                break;

            case SWT.PAGE_DOWN:
                if (oldPos <= content.length() - bytesPerLine) {
                    oldPos = oldPos + bytesPerLine * numberOfLines_1;
                    if (oldPos > content.length())
                        oldPos = oldPos -
                            ((oldPos - 1 - content.length()) / bytesPerLine + 1) * bytesPerLine;
                }
                if (countNibbles && oldPos == content.length()) upANibble = 0;
                break;
        }

        return oldPos;
    }


    void drawUnfocusedCaret(boolean visible)
    {
        if (hexText.isDisposed()) return;

        GC unfocusedGC;
        Caret unfocusedCaret;
        int chars = 0;
        int shift = 0;
        if (lastFocusedTextArea == 1) {
            unfocusedCaret = previewText.getCaret();
            unfocusedGC = styledText2GC;
        } else {
            unfocusedCaret = hexText.getCaret();
            unfocusedGC = styledText1GC;
            chars = 1;
            if (hexText.getCaretOffset() % 3 == 1)
                shift = -1;
        }
        if (unfocusedCaret.getVisible()) {
            Rectangle unfocused = unfocusedCaret.getBounds();
            unfocusedGC.setForeground(visible ? COLOR_NORMAL_SHADOW : colorCaretLine);
            unfocusedGC.drawRectangle(unfocused.x + shift * unfocused.width, unfocused.y,
                unfocused.width << chars, unfocused.height - 1);
        }
    }


    void ensureCaretIsVisible()
    {
        long caretPos = getCaretPos();
        long posInLine = caretPos % bytesPerLine;

        if (textAreasStart > caretPos) {
            textAreasStart = caretPos - posInLine;
        } else if (textAreasStart + bytesPerLine * numberOfLines < caretPos ||
            textAreasStart + bytesPerLine * numberOfLines == caretPos &&
                caretPos != content.length()) {
            textAreasStart = caretPos - posInLine - bytesPerLine * numberOfLines_1;
            if (caretPos == content.length() && posInLine == 0)
                textAreasStart = caretPos - bytesPerLine * numberOfLines;
            if (textAreasStart < 0L) textAreasStart = 0L;
        } else {

            return;
        }
        getVerticalBar().setSelection((int) ((textAreasStart / bytesPerLine) >>> verticalBarFactor));
    }


    private void ensureWholeScreenIsVisible()
    {
        if (textAreasStart + bytesPerLine * numberOfLines > content.length())
            textAreasStart = content.length() - (content.length() - 1L) % bytesPerLine - 1L -
                bytesPerLine * numberOfLines_1;

        if (textAreasStart < 0L)
            textAreasStart = 0L;
    }


    /**
     * Performs a find on the text and sets the selection accordingly.
     * The find starts at the current caret position.
     *
     * @param findString    the literal to find
     * @param isHexString   consider the literal as an hex string (ie. "0fdA1"). Used for binary finds.
     *                      Will search full bytes only, odd number of hex characters will have a leading '0' added.
     * @param searchForward look for matches after current position
     * @param ignoreCase    match upper case with lower case characters
     * @return whether a match was found
     */
    public boolean findAndSelect(String findString, boolean isHexString, boolean searchForward,
                                 boolean ignoreCase)
        throws IOException
    {
        return findAndSelectInternal(findString, isHexString, searchForward, ignoreCase, true);
    }


    private boolean findAndSelectInternal(String findString, boolean isHexString, boolean searchForward,
                                          boolean ignoreCase, boolean updateGui)
        throws IOException
    {
        if (findString == null) return true;

        initFinder(findString, isHexString, searchForward, ignoreCase);
        final Object[] result = new Object[2];
        HexManager.blockUntilFinished(new Runnable() {
            @Override
            public void run()
            {
                try {
                    result[0] = finder.getNextMatch();
                } catch (IOException e) {
                    result[1] = e;
                }
            }
        });
        if (result[1] != null) {
            throw (IOException) result[1];
        }
        Object[] vector = (Object[]) result[0];
        if (vector != null && vector.length > 1 && vector[0] != null && vector[1] != null) {
            startPosition = (Long) vector[0];
            caretStickToStart = false;
            if (updateGui) {
                setSelection(startPosition, startPosition + (Integer) vector[1]);
            } else {
                select(startPosition, startPosition + (Integer) vector[1]);
            }
            previousFindEnd = getCaretPos();

            return true;
        }

        return false;
    }


    /**
     * Get caret position in file, which can be out of view
     *
     * @return the current caret position
     */
    public long getCaretPos()
    {
        if (caretStickToStart)
            return startPosition;
        else
            return endPosition;
    }

    public byte getActualValue()
    {
        return getValue(getCaretPos());
    }

    public byte getValue(long pos)
    {
        try {
            content.get(ByteBuffer.wrap(tmpRawBuffer, 0, 1), null, pos);
        } catch (IOException e) {
            log.warn(e);
        }
        return tmpRawBuffer[0];
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


    private void getHighlightRangesInScreen(long start, int length)
    {
        highlightRangesInScreen.clear();
        if (lastLocationPosition >= start && lastLocationPosition < start + length) {
            highlightRangesInScreen.add((int) (lastLocationPosition - textAreasStart));
            highlightRangesInScreen.add(1);
        }
    }


    /**
     * Gets the selection start and end points as long values
     *
     * @return 2 elements long array, first one the start point (inclusive), second one the end point
     *         (exclusive)
     */
    public long[] getSelection()
    {
        return new long[]{startPosition, endPosition};
    }

    public boolean isSelected()
    {
        return (startPosition != endPosition);
    }

    boolean handleSelectedPreModify()
    {
        if (startPosition == endPosition || !isInserting) return false;

        content.delete(startPosition, endPosition - startPosition);
        endPosition = startPosition;

        return true;
    }


    long incrementPosWithinLimits(long oldPos, boolean countNibbles)
    {
        if (oldPos < content.length())
            if (countNibbles) {
                if (upANibble > 0) ++oldPos;
                upANibble ^= 1;  // 1->0, 0->1
            } else {
                ++oldPos;
            }

        return oldPos;
    }


    private void initFinder(String findString, boolean isHexString, boolean searchForward,
                            boolean ignoreCase)
    {
        if (!searchForward)
            caretStickToStart = true;
        if (finder == null || !findString.equals(previousFindString) ||
            isHexString != previousFindStringWasHex || ignoreCase != previousFindIgnoredCase) {
            previousFindString = findString;
            previousFindStringWasHex = isHexString;
            previousFindIgnoredCase = ignoreCase;

            if (isHexString) {
                finder = new BinaryTextFinder(hexStringToByte(findString), content);
            } else {
                finder = new BinaryTextFinder(findString, content);
                if (ignoreCase)
                    finder.setCaseSensitive(false);
            }
            finder.setNewStart(getCaretPos());
        }
        if (previousFindEnd != getCaretPos()) {
            finder.setNewStart(getCaretPos());
        }
        finder.setDirectionForward(searchForward);
    }


    /**
     * Tells whether the input is in overwrite or insert mode
     *
     * @return true: overwriting, false: inserting
     */
    public boolean isOverwriteMode()
    {
        return !isInserting;
    }


    void makeFirstRowSameHeight()
    {
        ((GridData) linesTextSeparator.getLayoutData()).heightHint =
            hexHeaderText.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        ((GridData) previewTextSeparator.getLayoutData()).heightHint =
            hexHeaderText.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
    }


    /**
     * Merge ranges of changes in file with ranges of highlighted elements.
     * Finds lowest range border, finds next lowest range border. That's the first result. Keeps going
     * until last range border.
     *
     * @return list of StyleRanges, each with a style of type 'changed', 'highlighted', or both.
     */
    List<StyleRange> mergeRanges(List<Long> changeRanges, List<Integer> highlightRanges)
    {
        if (!mergerInit(changeRanges, highlightRanges)) {
            return null;
        }
        List<StyleRange> result = new ArrayList<>();
        mergerNext();
        int start = mergeRangesPosition;
        boolean blue = mergeRangesIsBlue;
        boolean highlight = mergeRangesIsHighlight;
        while (mergerNext()) {
            if (blue || highlight) {
                result.add(new StyleRange(start, mergeRangesPosition - start, blue ? COLOR_BLUE : null,
                    highlight ? colorHighlight : null));
            }
            start = mergeRangesPosition;
            blue = mergeRangesIsBlue;
            highlight = mergeRangesIsHighlight;
        }

        return result;
    }


    boolean mergerCatchUps()
    {
        boolean withinRange = false;
        if (mergeChangeRanges != null && mergeChangeRanges.size() > mergeIndexChange) {
            withinRange = true;
            if (mergerPosition(true) < mergeRangesPosition) {
                ++mergeIndexChange;
            }
        }
        if (mergeHighlightRanges != null && mergeHighlightRanges.size() > mergeIndexHighlight) {
            withinRange = true;
            if (mergerPosition(false) < mergeRangesPosition) {
                ++mergeIndexHighlight;
            }
        }

        return withinRange;
    }


    /**
     * Initialise merger variables
     *
     * @return whether the parameters hold any data
     */
    boolean mergerInit(List<Long> changeRanges, List<Integer> highlightRanges)
    {
        if ((changeRanges == null || changeRanges.size() < 2) &&
            (highlightRanges == null || highlightRanges.size() < 2)) {
            return false;
        }
        this.mergeChangeRanges = changeRanges;
        this.mergeHighlightRanges = highlightRanges;
        mergeRangesIsBlue = false;
        mergeRangesIsHighlight = false;
        mergeRangesPosition = -1;
        mergeIndexChange = 0;
        mergeIndexHighlight = 0;

        return true;
    }


    int mergerMinimumInChangesHighlights()
    {
        int change = Integer.MAX_VALUE;
        if (mergeChangeRanges != null && mergeChangeRanges.size() > mergeIndexChange) {
            change = mergerPosition(true);
        }
        int highlight = Integer.MAX_VALUE;
        if (mergeHighlightRanges != null && mergeHighlightRanges.size() > mergeIndexHighlight) {
            highlight = mergerPosition(false);
        }
        int result = Math.min(change, highlight);
        if (change == result) {
            mergeRangesIsBlue = (mergeIndexChange & 1) == 0;
        }
        if (highlight == result) {
            mergeRangesIsHighlight = (mergeIndexHighlight & 1) == 0;
        }

        return result;
    }


    boolean mergerNext()
    {
        ++mergeRangesPosition;
        if (!mergerCatchUps()) {
            return false;
        }
        mergeRangesPosition = mergerMinimumInChangesHighlights();

        return true;
    }


    int mergerPosition(boolean changesNotHighlights)
    {
        int result;
        if (changesNotHighlights) {
            result = (int) (mergeChangeRanges.get(mergeIndexChange & 0xfffffffe) -
                textAreasStart);
            if ((mergeIndexChange & 1) == 1) {
                result = (int) Math.min(bytesPerLine * numberOfLines,
                    result + mergeChangeRanges.get(mergeIndexChange));
            }
        } else {
            result = mergeHighlightRanges.get(mergeIndexHighlight & 0xfffffffe);
            if ((mergeIndexHighlight & 1) == 1) {
                result += mergeHighlightRanges.get(mergeIndexHighlight);
            }
        }

        return result;
    }


    void notifyLongSelectionListeners()
    {
        if (longSelectionListeners.isEmpty()) return;

        Event basicEvent = new Event();
        basicEvent.widget = this;
        SelectionEvent anEvent = new SelectionEvent(basicEvent);
        anEvent.width = (int) (startPosition >>> 32);
        anEvent.x = (int) startPosition;
        anEvent.height = (int) (endPosition >>> 32);
        anEvent.y = (int) endPosition;

        for (SelectionListener aListener : longSelectionListeners) {
            aListener.widgetSelected(anEvent);
        }
    }


    /**
     * Pastes the clipboard content. The result depends on which insertion mode is currently active:
     * Insert mode replaces the selection with the DND.CLIPBOARD clipboard contents or, if there is no
     * selection, inserts at the current caret offset.
     * Overwrite mode replaces contents at the current caret offset, unless pasting would overflow the
     * content length, in which case does nothing.
     */
    public void paste()
    {
        if (!myClipboard.hasContents()) return;

        handleSelectedPreModify();
        long caretPos = getCaretPos();
        long total = myClipboard.getContents(content, caretPos, isInserting);
        startPosition = caretPos;
        endPosition = caretPos + total;
        caretStickToStart = false;
        redrawTextAreas(true);
        restoreStateAfterModify();
    }


    /**
     * Redoes the last undone action
     */
    public void redo()
    {
        undo(false);
    }


    void redrawTextAreas(int mode, StringBuilder newText, StringBuilder resultHex, StringBuilder resultChar,
                         List<StyleRange> viewRanges)
    {
        hexText.getCaret().setVisible(false);
        previewText.getCaret().setVisible(false);
        if (mode == SET_TEXT) {
            linesText.getContent().setText(newText.toString());
            hexText.getContent().setText(resultHex.toString());
            previewText.getContent().setText(resultChar.toString());
            previousLine = -1;
        } else {
            boolean forward = mode == SHIFT_FORWARD;
            linesText.setRedraw(false);
            hexText.setRedraw(false);
            previewText.setRedraw(false);
            ((DisplayedContent) linesText.getContent()).shiftLines(newText.toString(), forward);
            ((DisplayedContent) hexText.getContent()).shiftLines(resultHex.toString(), forward);
            ((DisplayedContent) previewText.getContent()).shiftLines(resultChar.toString(), forward);
            linesText.setRedraw(true);
            hexText.setRedraw(true);
            previewText.setRedraw(true);
            if (previousLine >= 0 && previousLine < numberOfLines)
                previousLine += newText.length() / charsForAddress * (forward ? 1 : -1);
            if (previousLine < -1 || previousLine >= numberOfLines)
                previousLine = -1;
        }
        if (viewRanges != null) {
            for (StyleRange styleRange : viewRanges) {
                previewText.setStyleRange(styleRange);
                styleRange = (StyleRange) styleRange.clone();
                styleRange.start *= 3;
                styleRange.length *= 3;
                hexText.setStyleRange(styleRange);
            }
        }
    }


    void redrawTextAreas(boolean fromScratch)
    {
        if (content == null || hexText.isDisposed()) return;

        long newLinesStart = textAreasStart;
        int linesShifted = numberOfLines;
        int mode = SET_TEXT;
        if (!fromScratch && previousRedrawStart >= 0L) {
            long lines = (textAreasStart - previousRedrawStart) / bytesPerLine;
            if (Math.abs(lines) < numberOfLines) {
                mode = lines > 0L ? SHIFT_BACKWARD : SHIFT_FORWARD;
                linesShifted = Math.abs((int) lines);
                if (linesShifted < 1) {
                    refreshSelections();
                    refreshCaretsPosition();

                    return;
                }
                if (mode == SHIFT_BACKWARD)
                    newLinesStart = textAreasStart + (numberOfLines - (int) lines) * bytesPerLine;
            }
        }
        previousRedrawStart = textAreasStart;

        StringBuilder newText = cookAddresses(newLinesStart, linesShifted * bytesPerLine);

        List<Long> changeRanges = new ArrayList<>();
        int actuallyRead;
        try {
            actuallyRead = content.get(ByteBuffer.wrap(tmpRawBuffer, 0, linesShifted * bytesPerLine),
                changeRanges, newLinesStart);
        } catch (IOException e) {
            actuallyRead = 0;
        }
        StringBuilder resultHex = cookTexts(true, actuallyRead);
        StringBuilder resultChar = cookTexts(false, actuallyRead);
        getHighlightRangesInScreen(newLinesStart, linesShifted * bytesPerLine);
        List<StyleRange> viewRanges = mergeRanges(changeRanges, highlightRangesInScreen);
        redrawTextAreas(mode, newText, resultHex, resultChar, viewRanges);
        refreshSelections();
        refreshCaretsPosition();
    }


    private void refreshCaretsPosition()
    {
        drawUnfocusedCaret(false);
        long caretLocation = getCaretPos() - textAreasStart;
        if (caretLocation >= 0L && caretLocation < bytesPerLine * numberOfLines ||
            getCaretPos() == content.length() && caretLocation == bytesPerLine * numberOfLines) {
            int tmp = (int) caretLocation;
            if (tmp == bytesPerLine * numberOfLines) {
                hexText.setCaretOffset(tmp * 3 - 1);
                previewText.setCaretOffset(tmp);
            } else {
                hexText.setCaretOffset(tmp * 3 + upANibble);
                previewText.setCaretOffset(tmp);
            }
            int line = hexText.getLineAtOffset(hexText.getCaretOffset());
            if (line != previousLine) {
                if (previousLine >= 0 && previousLine < numberOfLines) {
                    hexText.setLineBackground(previousLine, 1, null);
                    previewText.setLineBackground(previousLine, 1, null);
                }
                hexText.setLineBackground(line, 1, colorCaretLine);
                previewText.setLineBackground(line, 1, colorCaretLine);
                previousLine = line;
            }
            hexText.getCaret().setVisible(true);
            previewText.getCaret().setVisible(true);
            DBeaverUI.asyncExec(new Runnable() {
                @Override
                public void run() {
                    drawUnfocusedCaret(true);
                }
            });
        } else {
            hexText.getCaret().setVisible(false);
            previewText.getCaret().setVisible(false);
        }
    }


    void refreshHeader()
    {
        hexHeaderText.setText(headerRow.substring(0, Math.min(bytesPerLine * 3, headerRow.length())));
    }


    void refreshSelections()
    {
        if (startPosition >= endPosition ||
            startPosition > textAreasStart + bytesPerLine * numberOfLines ||
            endPosition <= textAreasStart)
            return;

        long startLocation = startPosition - textAreasStart;
        if (startLocation < 0L) startLocation = 0L;
        int intStart = (int) startLocation;

        long endLocation = endPosition - textAreasStart;
        if (endLocation > bytesPerLine * numberOfLines)
            endLocation = bytesPerLine * numberOfLines;
        int intEnd = (int) endLocation;

        if (caretStickToStart) {
            int tmp = intStart;
            intStart = intEnd;
            intEnd = tmp;
        }

        hexText.setSelection(intStart * 3, intEnd * 3);
        hexText.setTopIndex(0);
        previewText.setSelection(intStart, intEnd);
        previewText.setTopIndex(0);
    }


    /**
     * Replaces the selection. The result depends on which insertion mode is currently active:
     * Insert mode replaces the selection with the replaceString or, if there is no selection, inserts
     * at the current caret offset.
     * Overwrite mode replaces contents at the current selection start.
     *
     * @param replaceString the new string
     * @param isHexString   consider the literal as an hex string (ie. "0fdA1"). Used for binary finds.
     *                      Will replace full bytes only, odd number of hex characters will have a leading '0' added.
     */
    public void replace(String replaceString, boolean isHexString)
    {
        handleSelectedPreModify();
        byte[] replaceData = replaceString.getBytes(Charset.defaultCharset());
        if (isHexString) {
            replaceData = hexStringToByte(replaceString);
        }
        ByteBuffer newSelection = ByteBuffer.wrap(replaceData);
        if (isInserting) {
            content.insert(newSelection, startPosition);
        } else {
            newSelection.limit((int) Math.min(newSelection.limit(), content.length() - startPosition));
            content.overwrite(newSelection, startPosition);
        }
        endPosition = startPosition + newSelection.limit() - newSelection.position();
        caretStickToStart = false;
        redrawTextAreas(true);
        restoreStateAfterModify();
    }


    /**
     * Replaces all occurrences of findString with replaceString.
     * The find starts at the current caret position.
     *
     * @param findString         the literal to find
     * @param isFindHexString    consider the literal as an hex string (ie. "0fdA1"). Used for binary finds.
     *                           Will search full bytes only, odd number of hex characters will have a leading '0' added.
     * @param searchForward      look for matches after current position
     * @param ignoreCase         match upper case with lower case characters
     * @param replaceString      the new string
     * @param isReplaceHexString consider the literal as an hex string (ie. "0fdA1"). Used for binary
     *                           finds. Will replace full bytes only, odd number of hex characters will have a leading '0' added.
     * @return number of replacements
     */
    public int replaceAll(String findString, boolean isFindHexString, boolean searchForward,
                          boolean ignoreCase, String replaceString, boolean isReplaceHexString)
        throws IOException
    {
        int result = 0;
        stopSearching = false;
        while (!stopSearching &&
            findAndSelectInternal(findString, isFindHexString, searchForward, ignoreCase, false)) {
            ++result;
            replace(replaceString, isReplaceHexString);
        }
        if (result > 0) {
            setSelection(getSelection()[0], getSelection()[1]);
        }

        return result;
    }


    void restoreStateAfterModify()
    {
        ensureCaretIsVisible();
        redrawTextAreas(true);
        updateScrollBar();

        notifyListeners(SWT.Modify, null);
        notifyLongSelectionListeners();
    }


    void runnableAdd(Runnable delayed)
    {
        if (delayedInQueue) {
            delayedWaiting = delayed;
        } else {
            delayedInQueue = true;
            DBeaverUI.asyncExec(delayed);
        }
    }


    void runnableEnd()
    {
        if (delayedWaiting != null) {
            DBeaverUI.asyncExec(delayedWaiting);
            delayedWaiting = null;
        } else {
            delayedInQueue = false;
        }
    }


    /**
     * Sets the selection to the entire text. Caret remains either at the selection start or end
     */
    public void selectAll()
    {
        select(0L, content.length());
        refreshSelections();
    }

    /**
     * Sets the selection from start to end.
     */
    public void selectBlock(long start, long end)
    {
        select(start, end);
        refreshSelections();
        showMark(start);
    }

    void select(long start, long end)
    {
        upANibble = 0;
        boolean selection = startPosition != endPosition;
        startPosition = 0L;
        if (start > 0L) {
            startPosition = start;
            if (startPosition > content.length()) startPosition = content.length();
        }

        endPosition = startPosition;
        if (end > startPosition) {
            endPosition = end;
            if (endPosition > content.length()) endPosition = content.length();
        }

        notifyLongSelectionListeners();
    }

    void setAddressesGridDataWidthHint()
    {
        ((GridData) linesText.getLayoutData()).widthHint = charsForAddress * fontCharWidth;
    }

    void setCaretsSize(boolean insert)
    {
        isInserting = insert;
        int width = 0;
        int height = hexText.getCaret().getSize().y;
        if (!isInserting)
            width = fontCharWidth;

        hexText.getCaret().setSize(width, height);
        previewText.getCaret().setSize(width, height);
    }


    /**
     * Sets the content to be displayed. Replacing an existing content keeps the display area in the
     * same position, but only if it falls within the new content's limits.
     *
     * @param aContent the content to be displayed
     */
    public void setContentProvider(BinaryContent aContent)
    {
        if (isDisposed()) {
            return;
        }
        boolean firstContent = content == null;
        if (!firstContent) {
            content.dispose();
        }
        content = aContent;
        finder = null;
        if (content != null) {
            content.setActionsHistory();

            if (firstContent || endPosition > content.length() || textAreasStart >= content.length()) {
                textAreasStart = startPosition = endPosition = 0L;
                caretStickToStart = false;
            }

            charsForFileSizeAddress = Long.toHexString(content.length()).length();

        }

        updateScrollBar();
        redrawTextAreas(true);
        notifyLongSelectionListeners();
        notifyListeners(SWT.Modify, null);
    }

    public void setContent(byte[] data, String charset)
    {
        BinaryContent binaryContent = new BinaryContent();
        if (charset != null) {
            setCharset(charset);
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        binaryContent.insert(byteBuffer, 0);

        setContentProvider(binaryContent);
    }


    /**
     * Causes the receiver to have the keyboard focus. Within Eclipse, never call setFocus() before
     * the workbench has called EditorActionBarContributor.setActiveEditor()
     *
     * @see Composite#setFocus()
     */
    @Override
    public boolean setFocus()
    {
        redrawCaret(false);
        if (lastFocusedTextArea == 1)
            return hexText.setFocus();
        else
            return previewText.setFocus();
    }


    /**
     * @throws IllegalArgumentException if font height is 1 or 2
     * @see Control#setFont(org.eclipse.swt.graphics.Font)
     *      Font height must not be 1 or 2.
     */
    @Override
    public void setFont(Font font)
    {
        // bugfix: HexText's raw array overflows when font is very small and window very big
        // very small sizes would compromise responsiveness in large windows, and they are too small
        // to see anyway
        if (font != null) {
            int newSize = UIUtils.getFontHeight(font);
            if (newSize == 1 || newSize == 2)
                throw new IllegalArgumentException("Font size is " + newSize + ", too small");
        }

        fontCurrent = font;
        if (fontCurrent == null) {
            fontCurrent = fontDefault;
        }
        super.setFont(fontCurrent);
        hexHeaderText.setFont(fontCurrent);
        hexHeaderText.pack(true);
        GC gc = new GC(hexHeaderText);
        fontCharWidth = gc.getFontMetrics().getAverageCharWidth();
        gc.dispose();
        makeFirstRowSameHeight();
        linesText.setFont(fontCurrent);
        setAddressesGridDataWidthHint();
        linesText.pack(true);
        hexText.setFont(fontCurrent);
        hexText.pack(true);
        previewText.setFont(fontCurrent);
        previewText.pack(true);
        updateTextsMetrics();
        layout();
        setCaretsSize(isInserting);
    }


    /**
     * Sets the selection. The caret may change position but stays at the same selection point (if it
     * was at the start of the selection it will move to the new start, otherwise to the new end point).
     * The new selection is made visible
     *
     * @param start inclusive start selection char
     * @param end   exclusive end selection char
     */
    public void setSelection(long start, long end)
    {
        select(start, end);
        ensureCaretIsVisible();
        redrawTextAreas(false);
    }


    void shiftStartAndEnd(long newPos)
    {
        if (caretStickToStart) {
            startPosition = Math.min(newPos, endPosition);
            endPosition = Math.max(newPos, endPosition);
        } else {
            endPosition = Math.max(newPos, startPosition);
            startPosition = Math.min(newPos, startPosition);
        }
        caretStickToStart = endPosition != newPos;
    }


    /**
     * Shows the position on screen.
     *
     * @param position where relocation should go
     */
    public void showMark(long position)
    {
        lastLocationPosition = position;
        if (position < 0) return;

        position = position - position % bytesPerLine;
        textAreasStart = position;
        if (numberOfLines > 2)
            textAreasStart = position - (numberOfLines / 2) * bytesPerLine;
        ensureWholeScreenIsVisible();
        redrawTextAreas(true);
//	setFocus();
        updateScrollBar();
    }


    /**
     * Stop findAndSelect() or replaceAll() calls. Long running searches can be stopped from another
     * thread.
     */
    public void stopSearching()
    {
        stopSearching = true;
        if (finder != null) {
            finder.stopSearching();
        }
    }


    long totalNumberOfLines()
    {
        long result = 1L;
        if (content != null) {
            if (bytesPerLine > 0) {
                result = (content.length() - 1L) / bytesPerLine + 1L;
            }
        }

        return result;
    }


    /**
     * Undoes the last action
     */
    public void undo()
    {
        undo(true);
    }


    void undo(boolean previousAction)
    {
        long[] selection = previousAction ? content.undo() : content.redo();
        if (selection == null) return;

        upANibble = 0;
        startPosition = selection[0];
        endPosition = selection[1];
        caretStickToStart = false;
        ensureWholeScreenIsVisible();
        restoreStateAfterModify();
    }


    void updateNumberOfLines()
    {
        int height = getClientArea().height - hexHeaderText.computeSize(SWT.DEFAULT, SWT.DEFAULT, false).y;

        numberOfLines = height / linesText.getLineHeight();
        if (numberOfLines < 1)
            numberOfLines = 1;

        numberOfLines_1 = numberOfLines - 1;

        ((DisplayedContent) linesText.getContent()).setDimensions(charsForAddress, numberOfLines);
        ((DisplayedContent) hexText.getContent()).setDimensions(bytesPerLine * 3, numberOfLines);
        ((DisplayedContent) previewText.getContent()).setDimensions(bytesPerLine, numberOfLines);
    }


    private void updateScrollBar()
    {
        ScrollBar vertical = getVerticalBar();
        long max = totalNumberOfLines();
        verticalBarFactor = 0;
        while (max > Integer.MAX_VALUE) {
            max >>>= 1;
            ++verticalBarFactor;
        }
        vertical.setMaximum((int) max);
        vertical.setSelection((int) ((textAreasStart / bytesPerLine) >>> verticalBarFactor));
        vertical.setPageIncrement(numberOfLines_1);
        vertical.setThumb(numberOfLines);
    }


    void updateTextsMetrics()
    {
        int width = getClientArea().width - linesText.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        int displayedNumberWidth = fontCharWidth * 4;  // hexText and previewText
        int commonWidth = width / displayedNumberWidth;
        
        if (commonWidth >= BYTE_WIDTH_16) {
			bytesPerLine = 0;
			int comPart = commonWidth / BYTE_WIDTH_16;
			int remPart = commonWidth % BYTE_WIDTH_16;
			if (remPart >= BYTE_WIDTH_8) {
				bytesPerLine = BYTE_WIDTH_8;
				remPart = remPart % BYTE_WIDTH_8;
			} else if (remPart >= BYTE_WIDTH_4) {
				bytesPerLine = BYTE_WIDTH_4;
				remPart = remPart % BYTE_WIDTH_4;
			} else if (remPart >= BYTE_WIDTH_2) {
				bytesPerLine = BYTE_WIDTH_2;
			}
			bytesPerLine += comPart * BYTE_WIDTH_16;
		} else if (commonWidth >= BYTE_WIDTH_8 && commonWidth < BYTE_WIDTH_16) {
			bytesPerLine = BYTE_WIDTH_8;
		} else if (commonWidth >= BYTE_WIDTH_4 && commonWidth < BYTE_WIDTH_8) {
			bytesPerLine = BYTE_WIDTH_4;
		} else {
			bytesPerLine = BYTE_WIDTH_2;
		}

        textGridData.widthHint = hexText.computeTrim(0, 0, bytesPerLine * 3 * fontCharWidth, 100).width;
        previewGridData.widthHint = previewText.computeTrim(0, 0, bytesPerLine * fontCharWidth, 100).width;
        updateNumberOfLines();
        changed(new Control[]{hexHeaderText, linesText, hexText, previewText});
        updateScrollBar();
        refreshHeader();
        textAreasStart = (((long) getVerticalBar().getSelection()) * bytesPerLine) << verticalBarFactor;
        redrawTextAreas(true);
    }

}
