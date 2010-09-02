/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.binary;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

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

    static final Log log = LogFactory.getLog(HexEditControl.class);
    /**
     * Map of displayed chars. Chars that cannot be displayed correctly are changed for a '.' char.
     * There are differences on which chars can correctly be displayed in each operating system,
     * charset encoding, or font system.
     */
    public static final char[] byteToChar = new char[256];
    public static final String[] byteToHex = new String[256];
    static final int charsForAddress = 12;  // Files up to 16 Ters: 11 binary digits + ':'
    static final Color colorBlue = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
    static final Color colorLightShadow =
        Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
    static final Color colorNormalShadow =
        Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
    static final FontData fontDataDefault = new FontData("Courier New", 10, SWT.NORMAL);
    static String headerRow = null;
    static final byte[] hexToNibble = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1, -1, -1, -1, -1,
                                       10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                                       -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 10, 11, 12, 13, 14, 15};
    static final int maxScreenResolution = 1920;
    static final int minCharSize = 5;
    static final char[] nibbleToHex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    static final int SET_TEXT = 0;
    static final int SHIFT_FORWARD = 1;  // frame
    static final int SHIFT_BACKWARD = 2;

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
    private int myBytesPerLine = 16;
    private boolean myCaretStickToStart = false;  // stick to end
    private BinaryClipboard myClipboard = null;
    private BinaryContent content = null;
    private long myEnd = 0L;
    private Finder finder = null;
    private boolean myInserting = false;
    private KeyListener myKeyAdapter = new MyKeyAdapter();
    private int myLastFocusedTextArea = -1;  // 1 or 2;
    private long myLastLocationPosition = -1L;
    private List<SelectionListener> myLongSelectionListeners = null;
    private long myPreviousFindEnd = -1;
    private boolean myPreviousFindIgnoredCase = false;
    private String myPreviousFindString = null;
    private boolean myPreviousFindStringWasHex = false;
    private int myPreviousLine = -1;
    private long myPreviousRedrawStart = -1;
    private long myStart = 0L;
    private long myTextAreasStart = -1L;
    private final MyTraverseAdapter myTraverseAdapter = new MyTraverseAdapter();
    private int myUpANibble = 0;  // always 0 or 1
    private final MyVerifyKeyAdapter myVerifyKeyAdapter = new MyVerifyKeyAdapter();
    private int numberOfLines = 16;
    private int numberOfLines_1 = numberOfLines - 1;
    private boolean stopSearching = false;
    private byte[] tmpRawBuffer = new byte[maxScreenResolution / minCharSize / 3 * maxScreenResolution /
        minCharSize];
    private int verticalBarFactor = 0;

    // visual components
    private Color colorCaretLine = null;
    private Color colorHighlight = null;
    private Font fontCurrent = null;  // disposed externally
    private Font fontDefault = null;  // disposed internally
    private GridData gridData5 = null;
    private GridData gridData6 = null;
    private GC styledText1GC = null;
    private GC styledText2GC = null;
    private Text linesTextSeparator = null;
    private StyledText linesText = null;

    private StyledText hexHeaderText = null;
    private StyledText hexText = null;

    private Text previewTextSeparator = null;
    private StyledText previewText = null;

    @Override
    public void dispose() {
        super.dispose();
    }

    public Finder getFinder()
    {
        return finder;
    }

    /**
     * compose byte-to-hex map
     */
    private void composeByteToHexMap()
    {
        for (int i = 0; i < 256; ++i) {
            byteToHex[i] = Character.toString(nibbleToHex[i >>> 4]) + nibbleToHex[i & 0x0f];
        }
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
                byteToChar[i] = '.';
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
/*
                String text = previewText.getText();
                previewText.setText("|" + decoded);
                if (previewText.getLocationAtOffset(2).x - previewText.getLocationAtOffset(1).x <
                    previewText.getLocationAtOffset(1).x - previewText.getLocationAtOffset(0).x) {
                    decoded = '.';
                }
                previewText.setText(text);
*/
                byteToChar[i] = decoded;
            }
        }
    }

    /**
     * compose header row
     */
    private void composeHeaderRow()
    {
        StringBuffer rowChars = new StringBuffer();
        for (int i = 0; i < maxScreenResolution / minCharSize / 3; ++i)
            rowChars.append(byteToHex[i & 0x0ff]).append(' ');
        headerRow = rowChars.toString().toUpperCase();
    }


    public String getCharset()
    {
        return charset;
    }

    public String getSystemCharset()
    {
        return System.getProperty("file.encoding", "utf-8");
//	return Charset.defaultCharset().toString();
    }

    public void setCharset(String name)
    {
        if ((name == null) || (name.length() == 0))
            name = getSystemCharset();
        charset = name;
        composeByteToCharMap();
    }

    /**
     * Get long selection start and end points. Helper method for long selection listeners.
     * The start point is formed by event.width as the most significant int and event.x as the least
     * significant int. The end point is similarly formed by event.height and event.y
     *
     * @param selectionEvent an event with long selection start and end points
     * @see addLongSelectionListener(org.eclipse.swt.events.SelectionListener)
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


    private class MyKeyAdapter extends KeyAdapter {
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
                    boolean selection = myStart != myEnd;
                    boolean ctrlKey = (e.stateMask & SWT.CONTROL) != 0;
                    if ((e.stateMask & SWT.SHIFT) != 0) {  // shift mod2
                        long newPos = doNavigateKeyPressed(ctrlKey, e.keyCode, getCaretPos(), false);
                        shiftStartAndEnd(newPos);
                    } else {  // if no modifier or control or alt
                        myEnd = myStart = doNavigateKeyPressed(ctrlKey, e.keyCode, getCaretPos(),
                                                               e.widget == hexText && !myInserting);
                        myCaretStickToStart = false;
                    }
                    ensureCaretIsVisible();
                    Runnable delayed = new Runnable() {
                        public void run()
                        {
                            redrawTextAreas(false);
                            runnableEnd();
                        }
                    };
                    runnableAdd(delayed);
                    notifyLongSelectionListeners();
                    if (selection != (myStart != myEnd))
                        notifyListeners(SWT.Modify, null);
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


    private class MyMouseAdapter extends MouseAdapter {
        int charLen;

        public MyMouseAdapter(boolean hexContent)
        {
            charLen = 1;
            if (hexContent) charLen = 3;
        }

        public void mouseDown(MouseEvent e)
        {
            if (e.button == 1)
                dragging = true;
            int textOffset;
            try {
                textOffset = ((StyledText) e.widget).getOffsetAtLocation(new Point(e.x, e.y));
            }
            catch (IllegalArgumentException ex) {
                textOffset = ((StyledText) e.widget).getCharCount();
            }
            int byteOffset = textOffset / charLen;
            ((StyledText) e.widget).setTopIndex(0);
            if (e.button == 1 && (e.stateMask & SWT.MODIFIER_MASK & ~SWT.SHIFT) == 0) {// no modif or shift
                if ((e.stateMask & SWT.MODIFIER_MASK) == 0) {
                    myCaretStickToStart = false;
                    myStart = myEnd = myTextAreasStart + byteOffset;
                } else {  // shift
                    shiftStartAndEnd(myTextAreasStart + byteOffset);
                }
                refreshCaretsPosition();
                setFocus();
                refreshSelections();
                notifyListeners(SWT.Modify, null);
                notifyLongSelectionListeners();
            }
        }

        public void mouseUp(MouseEvent e)
        {
            if (e.button == 1)
                dragging = false;
        }
    }


    private class MyPaintAdapter implements PaintListener {
        boolean hexContent = false;

        MyPaintAdapter(boolean isHexText)
        {
            hexContent = isHexText;
        }

        public void paintControl(PaintEvent event)
        {
            event.gc.setForeground(colorLightShadow);
            int lineWidth = 1;
            int charLen = 1;
            int rightHalfWidth = 0;  // is 1, but better to tread on leftmost char pixel than rightmost one
            if (hexContent) {
                lineWidth = fontCharWidth;
                charLen = 3;
                rightHalfWidth = (lineWidth + 1) / 2;  // line spans to both sides of its position
            }
            event.gc.setLineWidth(lineWidth);
            for (int block = 8; block <= myBytesPerLine; block += 8) {
                int xPos = (charLen * block) * fontCharWidth - rightHalfWidth;
                event.gc.drawLine(xPos, event.y, xPos, event.y + event.height);
            }
        }
    }


    private class MySelectionAdapter extends SelectionAdapter implements SelectionListener {
        int charLen;

        public MySelectionAdapter(boolean hexContent)
        {
            charLen = 1;
            if (hexContent) charLen = 3;
        }

        public void widgetSelected(SelectionEvent e)
        {
            if (!dragging)
                return;

            boolean selection = myStart != myEnd;
            int lower = e.x / charLen;
            int higher = e.y / charLen;
            int caretPos = ((StyledText) e.widget).getCaretOffset() / charLen;
            myCaretStickToStart = caretPos < higher || caretPos < lower;
            if (lower > higher) {
                lower = higher;
                higher = e.x / charLen;
            }

            select(myTextAreasStart + lower, myTextAreasStart + higher);
            if (selection != (myStart != myEnd))
                notifyListeners(SWT.Modify, null);

            redrawTextAreas(false);
        }
    }


    private class MyTraverseAdapter implements TraverseListener {
        public void keyTraversed(TraverseEvent e)
        {
            if (e.detail == SWT.TRAVERSE_TAB_NEXT)
                e.doit = true;
        }
    }


    private class MyVerifyKeyAdapter implements VerifyKeyListener {
        public void verifyKey(VerifyEvent e)
        {
//System.out.println("int:"+(int)e.character+", char:"+e.character+", keycode:"+e.keyCode);
            if (readOnly) {
                return;
            }
            if ((e.character == SWT.DEL || e.character == SWT.BS) && myInserting) {
                if (!deleteSelected()) {
                    if (e.character == SWT.BS) {
                        myStart += myUpANibble;
                        if (myStart > 0L) {
                            content.delete(myStart - 1L, 1L);
                            myEnd = --myStart;
                        }
                    } else {  // e.character == SWT.DEL
                        content.delete(myStart, 1L);
                    }
                    ensureWholeScreenIsVisible();
                    ensureCaretIsVisible();
                    Runnable delayed = new Runnable() {
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
                myUpANibble = 0;
            } else {
                doModifyKeyPressed(e);
            }

            e.doit = false;
        }
    }


    /**
     * Create a binary text editor
     *
     * @param parent parent in the widget hierarchy
     * @param style  not used for the moment
     */
    public HexEditControl(final Composite parent, int style)
    {
        super(parent, style | SWT.V_SCROLL);

        this.readOnly = (style & SWT.READ_ONLY) != 0;
        colorCaretLine = new Color(Display.getCurrent(), 232, 242, 254);  // very light blue
        colorHighlight = new Color(Display.getCurrent(), 255, 248, 147);  // mellow yellow
        highlightRangesInScreen = new ArrayList<Integer>();

        composeByteToHexMap();
        composeHeaderRow();

        myClipboard = new BinaryClipboard(parent.getDisplay());
        myLongSelectionListeners = new ArrayList<SelectionListener>();
        addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                colorCaretLine.dispose();
                colorHighlight.dispose();
                if (fontDefault != null && !fontDefault.isDisposed())
                    fontDefault.dispose();
                try {
                    myClipboard.dispose();
                }
                catch (IOException ex) {
                    log.warn("Could not cleanup clipboard temporary data");
                }
            }
        });
        initialize();
        myLastFocusedTextArea = 1;
        myPreviousLine = -1;
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
        setCaretsSize(focus ? (!myInserting) : myInserting);
        if (myInserting && myUpANibble != 0) {
            myUpANibble = 0;
            refreshCaretsPosition();
            if (focus) setFocus();
        } else {
            drawUnfocusedCaret(true);
        }
        if (focus) notifyListeners(SWT.Modify, null);
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
     * @see getLongSelection(SelectionEvent)
     */
    public void addLongSelectionListener(SelectionListener listener)
    {
        if (listener == null)
            throw new IllegalArgumentException();

        if (!myLongSelectionListeners.contains(listener))
            myLongSelectionListeners.add(listener);
    }


    /**
     * This method initializes composite
     */
    private void initialize()
    {
        GridLayout gridLayout1 = new GridLayout();
        gridLayout1.numColumns = 3;
        gridLayout1.marginHeight = 0;
        gridLayout1.verticalSpacing = 0;
        gridLayout1.horizontalSpacing = 0;
        gridLayout1.marginWidth = 0;
        setLayout(gridLayout1);

        Composite linesColumn = new Composite(this, SWT.NONE);
        GridLayout columnLayout = new GridLayout();
        columnLayout.marginHeight = 0;
        columnLayout.verticalSpacing = 1;
        columnLayout.horizontalSpacing = 0;
        columnLayout.marginWidth = 0;
        linesColumn.setLayout(columnLayout);
        linesColumn.setBackground(colorLightShadow);
        GridData gridDataColumn = new GridData(SWT.BEGINNING, SWT.FILL, false, true);
        linesColumn.setLayoutData(gridDataColumn);

        GridData gridDataTextSeparator = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        gridDataTextSeparator.widthHint = 10;
        linesTextSeparator = new Text(linesColumn, SWT.SEPARATOR);
        linesTextSeparator.setEnabled(false);
        linesTextSeparator.setBackground(colorLightShadow);
        linesTextSeparator.setLayoutData(gridDataTextSeparator);

        linesText = new StyledText(linesColumn, SWT.MULTI | SWT.READ_ONLY);
        linesText.setEditable(false);
        linesText.setEnabled(false);
        linesText.setBackground(colorLightShadow);
        linesText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
        fontDefault = new Font(Display.getCurrent(), fontDataDefault);
        fontCurrent = fontDefault;
        linesText.setFont(fontCurrent);
        GC styledTextGC = new GC(linesText);
        fontCharWidth = styledTextGC.getFontMetrics().getAverageCharWidth();
        styledTextGC.dispose();
        GridData gridDataAddresses = new GridData(SWT.BEGINNING, SWT.FILL, false, true);
        gridDataAddresses.heightHint = numberOfLines * linesText.getLineHeight();
        linesText.setLayoutData(gridDataAddresses);
        setAddressesGridDataWidthHint();
        linesText.setContent(new DisplayedContent(linesText, charsForAddress, numberOfLines));

        Composite hexColumn = new Composite(this, SWT.NONE);
        GridLayout column1Layout = new GridLayout();
        column1Layout.marginHeight = 0;
        column1Layout.verticalSpacing = 1;
        column1Layout.horizontalSpacing = 0;
        column1Layout.marginWidth = 0;
        hexColumn.setLayout(column1Layout);
        hexColumn.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        GridData gridDataColumn1 = new GridData(SWT.BEGINNING, SWT.FILL, false, true);
        hexColumn.setLayoutData(gridDataColumn1);

        Composite hexHeaderGroup = new Composite(hexColumn, SWT.NONE);
        hexHeaderGroup.setBackground(colorLightShadow);
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
        hexHeaderText.setBackground(colorLightShadow);
        hexHeaderText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
        hexHeaderText.setLayoutData(gridData);
        hexHeaderText.setFont(fontCurrent);
        refreshHeader();

        hexText = new StyledText(hexColumn, SWT.MULTI);
        hexText.setFont(fontCurrent);
        if (readOnly) {
            hexText.setEditable(false);
        }
        styledText1GC = new GC(hexText);
        int width = myBytesPerLine * 3 * fontCharWidth;
        gridData5 = new GridData();
        gridData5.horizontalIndent = 1;
        gridData5.verticalAlignment = SWT.FILL;
        gridData5.widthHint = hexText.computeTrim(0, 0, width, 0).width;
        gridData5.grabExcessVerticalSpace = true;
        hexText.setLayoutData(gridData5);
        hexText.addKeyListener(myKeyAdapter);
        FocusListener myFocusAdapter = new FocusAdapter() {
            public void focusGained(FocusEvent e)
            {
                drawUnfocusedCaret(false);
                myLastFocusedTextArea = 1;
                if (e.widget == previewText)
                    myLastFocusedTextArea = 2;
                getDisplay().asyncExec(new Runnable() {
                    public void run()
                    {
                        drawUnfocusedCaret(true);
                    }
                });
            }
        };
        hexText.addFocusListener(myFocusAdapter);
        hexText.addMouseListener(new MyMouseAdapter(true));
        hexText.addPaintListener(new MyPaintAdapter(true));
        hexText.addTraverseListener(myTraverseAdapter);
        hexText.addVerifyKeyListener(myVerifyKeyAdapter);
        hexText.setContent(new DisplayedContent(hexText, myBytesPerLine * 3, numberOfLines));
        hexText.setDoubleClickEnabled(false);
        hexText.addSelectionListener(new MySelectionAdapter(true));
        // StyledText.setCaretOffset() version 3.448 bug resets the caret size if using the default one,
        // so we use not the default one.
        Caret defaultCaret = hexText.getCaret();
        Caret nonDefaultCaret = new Caret(defaultCaret.getParent(), defaultCaret.getStyle());
        nonDefaultCaret.setBounds(defaultCaret.getBounds());
        hexText.setCaret(nonDefaultCaret);

        Composite previewColumn = new Composite(this, SWT.NONE);
        GridLayout column2Layout = new GridLayout();
        column2Layout.marginHeight = 0;
        column2Layout.verticalSpacing = 1;
        column2Layout.horizontalSpacing = 0;
        column2Layout.marginWidth = 0;
        previewColumn.setLayout(column2Layout);
        previewColumn.setBackground(hexText.getBackground());
        GridData gridDataColumn2 = new GridData(SWT.FILL, SWT.FILL, true, true);
        previewColumn.setLayoutData(gridDataColumn2);

        GridData gridDataTextSeparator2 = new GridData();
        gridDataTextSeparator2.horizontalAlignment = SWT.FILL;
        gridDataTextSeparator2.verticalAlignment = SWT.FILL;
        gridDataTextSeparator2.grabExcessHorizontalSpace = true;
        previewTextSeparator = new Text(previewColumn, SWT.SEPARATOR);
        previewTextSeparator.setEnabled(false);
        previewTextSeparator.setBackground(colorLightShadow);
        previewTextSeparator.setLayoutData(gridDataTextSeparator2);
        makeFirstRowSameHeight();

        previewText = new StyledText(previewColumn, SWT.MULTI);
        previewText.setFont(fontCurrent);
        if (readOnly) {
            previewText.setEditable(false);
        }
        width = myBytesPerLine * fontCharWidth + 1;  // one pixel for caret in last linesColumn
        gridData6 = new GridData();
        gridData6.verticalAlignment = SWT.FILL;
        gridData6.widthHint = previewText.computeTrim(0, 0, width, 0).width;
        gridData6.grabExcessVerticalSpace = true;
        previewText.setLayoutData(gridData6);
        previewText.addKeyListener(myKeyAdapter);
        previewText.addFocusListener(myFocusAdapter);
        previewText.addMouseListener(new MyMouseAdapter(false));
        previewText.addPaintListener(new MyPaintAdapter(false));
        previewText.addTraverseListener(myTraverseAdapter);
        previewText.addVerifyKeyListener(myVerifyKeyAdapter);
        previewText.setContent(new DisplayedContent(previewText, myBytesPerLine, numberOfLines));
        previewText.setDoubleClickEnabled(false);
        previewText.addSelectionListener(new MySelectionAdapter(false));
        // StyledText.setCaretOffset() version 3.448 bug resets the caret size if using the default one,
        // so we use not the default one.
        defaultCaret = previewText.getCaret();
        nonDefaultCaret = new Caret(defaultCaret.getParent(), defaultCaret.getStyle());
        nonDefaultCaret.setBounds(defaultCaret.getBounds());
        previewText.setCaret(nonDefaultCaret);
        styledText2GC = new GC(previewText);
        setCharset(null);

        super.setFont(fontCurrent);
        ScrollBar vertical = getVerticalBar();
        vertical.setSelection(0);
        vertical.setMinimum(0);
        vertical.setIncrement(1);
        vertical.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e)
            {
                e.doit = false;
                long previousStart = myTextAreasStart;
                myTextAreasStart =
                    (((long) getVerticalBar().getSelection()) << verticalBarFactor) * (long) myBytesPerLine;
                if (previousStart == myTextAreasStart) return;

                Runnable delayed = new Runnable() {
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
            public void mouseDown(org.eclipse.swt.events.MouseEvent e)
            {
                setFocus();
            }
        });
        addControlListener(new org.eclipse.swt.events.ControlAdapter() {
            public void controlResized(org.eclipse.swt.events.ControlEvent e)
            {
                updateTextsMetrics();
            }
        });
        addDisposeListener(new org.eclipse.swt.events.DisposeListener() {
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
        if (myStart >= myEnd) return;

        myClipboard.setContents(content, myStart, myEnd - myStart);
    }


    StringBuffer cookAddresses(long address, int limit)
    {
        StringBuffer theText = new StringBuffer();
        for (int i = 0; i < limit; i += myBytesPerLine, address += myBytesPerLine) {
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
                    theText.append(nibbleToHex[nibble]);
                }
            }
            theText.append(nibbleToHex[((int) address) & 0x0f]).append(':');
        }

        return theText;
    }


    StringBuffer cookTexts(boolean isHexOutput, int length)
    {
        if (length > tmpRawBuffer.length) length = tmpRawBuffer.length;
        StringBuffer result;

        if (isHexOutput) {
            result = new StringBuffer(length * 3);
            for (int i = 0; i < length; ++i) {
                result.append(byteToHex[tmpRawBuffer[i] & 0x0ff]).append(' ');
            }
        } else {
            result = new StringBuffer(length);
            for (int i = 0; i < length; ++i) {
                result.append(byteToChar[tmpRawBuffer[i] & 0x0ff]);
            }
        }

        return result;
    }


    /**
     * Calls copy();deleteSelected();
     *
     * @see copy(), deleteSelected()
     */
    public void cut()
    {
        copy();
        deleteSelected();
    }


    /**
     * While in insert mode, trims the selection
     *
     * @return did delete something
     */
    public boolean deleteNotSelected()
    {
        if (!myInserting || myStart < 1L && myEnd >= content.length()) return false;

        content.delete(myEnd, content.length() - myEnd);
        content.delete(0L, myStart);
        myStart = 0L;
        myEnd = content.length();

        myUpANibble = 0;
        ensureWholeScreenIsVisible();
        restoreStateAfterModify();

        return true;
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
        myUpANibble = 0;
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

        if (getCaretPos() == content.length() && !myInserting) {
            ensureCaretIsVisible();
            redrawTextAreas(false);
            return;
        }
        handleSelectedPreModify();
        try {
            if (myInserting) {
                if (event.widget == previewText) {
                    content.insert((byte) aChar, getCaretPos());
                } else if (myUpANibble == 0) {
                    content.insert((byte) (hexToNibble[aChar - '0'] << 4), getCaretPos());
                } else {
                    content.overwrite(hexToNibble[aChar - '0'], 4, 4, getCaretPos());
                }
            } else {
                if (event.widget == previewText) {
                    content.overwrite((byte) aChar, getCaretPos());
                } else {
                    content.overwrite(hexToNibble[aChar - '0'], myUpANibble * 4, 4, getCaretPos());
                }
                content.get(ByteBuffer.wrap(tmpRawBuffer, 0, 1), null, getCaretPos());
                int offset = (int) (getCaretPos() - myTextAreasStart);
                hexText.replaceTextRange(offset * 3, 2, byteToHex[tmpRawBuffer[0] & 0x0ff]);
                hexText.setStyleRange(new StyleRange(offset * 3, 2, colorBlue, null));
                previewText.replaceTextRange(offset, 1,
                                             Character.toString(byteToChar[tmpRawBuffer[0] & 0x0ff]));
                previewText.setStyleRange(new StyleRange(offset, 1, colorBlue, null));
            }
        }
        catch (IOException e) {
            log.warn(e);
        }
        myStart = myEnd = incrementPosWithinLimits(getCaretPos(), event.widget == hexText);
        Runnable delayed = new Runnable() {
            public void run()
            {
                ensureCaretIsVisible();
                redrawTextAreas(false);
                if (myInserting) {
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
    }


    private long doNavigateKeyPressed(boolean ctrlKey, int keyCode, long oldPos, boolean countNibbles)
    {
        if (!countNibbles)
            myUpANibble = 0;
        switch (keyCode) {
            case SWT.ARROW_UP:
                if (oldPos >= myBytesPerLine) oldPos -= myBytesPerLine;
                break;

            case SWT.ARROW_DOWN:
                if (oldPos <= content.length() - myBytesPerLine) oldPos += myBytesPerLine;
                if (countNibbles && oldPos == content.length()) myUpANibble = 0;
                break;

            case SWT.ARROW_LEFT:
                if (countNibbles && (oldPos > 0 || oldPos == 0 && myUpANibble > 0)) {
                    if (myUpANibble == 0) --oldPos;
                    myUpANibble ^= 1;  // 1->0, 0->1
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
                    oldPos = oldPos - oldPos % myBytesPerLine + myBytesPerLine - 1L;
                    if (oldPos >= content.length()) oldPos = content.length();
                }
                myUpANibble = 0;
                if (countNibbles && oldPos < content.length()) myUpANibble = 1;
                break;

            case SWT.HOME:
                if (ctrlKey) {
                    oldPos = 0;
                } else {
                    oldPos = oldPos - oldPos % myBytesPerLine;
                }
                myUpANibble = 0;
                break;

            case SWT.PAGE_UP:
                if (oldPos >= myBytesPerLine) {
                    oldPos = oldPos - myBytesPerLine * numberOfLines_1;
                    if (oldPos < 0L)
                        oldPos = (oldPos + myBytesPerLine * numberOfLines_1) % myBytesPerLine;
                }
                break;

            case SWT.PAGE_DOWN:
                if (oldPos <= content.length() - myBytesPerLine) {
                    oldPos = oldPos + myBytesPerLine * numberOfLines_1;
                    if (oldPos > content.length())
                        oldPos = oldPos -
                            ((oldPos - 1 - content.length()) / myBytesPerLine + 1) * myBytesPerLine;
                }
                if (countNibbles && oldPos == content.length()) myUpANibble = 0;
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
        if (myLastFocusedTextArea == 1) {
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
            unfocusedGC.setForeground(visible ? colorNormalShadow : colorCaretLine);
            unfocusedGC.drawRectangle(unfocused.x + shift * unfocused.width, unfocused.y,
                                      unfocused.width << chars, unfocused.height - 1);
        }
    }


    void ensureCaretIsVisible()
    {
        long caretPos = getCaretPos();
        long posInLine = caretPos % myBytesPerLine;

        if (myTextAreasStart > caretPos) {
            myTextAreasStart = caretPos - posInLine;
        } else if (myTextAreasStart + myBytesPerLine * numberOfLines < caretPos ||
            myTextAreasStart + myBytesPerLine * numberOfLines == caretPos &&
                caretPos != content.length()) {
            myTextAreasStart = caretPos - posInLine - myBytesPerLine * numberOfLines_1;
            if (caretPos == content.length() && posInLine == 0)
                myTextAreasStart = caretPos - myBytesPerLine * numberOfLines;
            if (myTextAreasStart < 0L) myTextAreasStart = 0L;
        } else {

            return;
        }
        getVerticalBar().setSelection((int) ((myTextAreasStart / myBytesPerLine) >>> verticalBarFactor));
    }


    private void ensureWholeScreenIsVisible()
    {
        if (myTextAreasStart + myBytesPerLine * numberOfLines > content.length())
            myTextAreasStart = content.length() - (content.length() - 1L) % myBytesPerLine - 1L -
                myBytesPerLine * numberOfLines_1;

        if (myTextAreasStart < 0L)
            myTextAreasStart = 0L;
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
            public void run()
            {
                try {
                    result[0] = finder.getNextMatch();
                }
                catch (IOException e) {
                    result[1] = e;
                }
            }
        });
        if (result[1] != null) {
            throw (IOException) result[1];
        }
        Object[] vector = (Object[]) result[0];
        if (vector != null && vector.length > 1 && vector[0] != null && vector[1] != null) {
            myStart = (Long) vector[0];
            myCaretStickToStart = false;
            if (updateGui) {
                setSelection(myStart, myStart + (Integer) vector[1]);
            } else {
                select(myStart, myStart + (Integer) vector[1]);
            }
            myPreviousFindEnd = getCaretPos();

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
        if (myCaretStickToStart)
            return myStart;
        else
            return myEnd;
    }

    public byte getActualValue()
    {
        return getValue(getCaretPos());
    }

    public byte getValue(long pos)
    {
        try {
            content.get(ByteBuffer.wrap(tmpRawBuffer, 0, 1), null, pos);
        }
        catch (IOException e) {
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
        if (myLastLocationPosition >= start && myLastLocationPosition < start + length) {
            highlightRangesInScreen.add((int) (myLastLocationPosition - myTextAreasStart));
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
        return new long[]{myStart, myEnd};
    }

    public boolean isSelected()
    {
        return (myStart != myEnd);
    }

    boolean handleSelectedPreModify()
    {
        if (myStart == myEnd || !myInserting) return false;

        content.delete(myStart, myEnd - myStart);
        myEnd = myStart;

        return true;
    }


    long incrementPosWithinLimits(long oldPos, boolean countNibbles)
    {
        if (oldPos < content.length())
            if (countNibbles) {
                if (myUpANibble > 0) ++oldPos;
                myUpANibble ^= 1;  // 1->0, 0->1
            } else {
                ++oldPos;
            }

        return oldPos;
    }


    private void initFinder(String findString, boolean isHexString, boolean searchForward,
                            boolean ignoreCase)
    {
        if (!searchForward)
            myCaretStickToStart = true;
        if (finder == null || !findString.equals(myPreviousFindString) ||
            isHexString != myPreviousFindStringWasHex || ignoreCase != myPreviousFindIgnoredCase) {
            myPreviousFindString = findString;
            myPreviousFindStringWasHex = isHexString;
            myPreviousFindIgnoredCase = ignoreCase;

            if (isHexString) {
                finder = new Finder(hexStringToByte(findString), content);
            } else {
                finder = new Finder(findString, content);
                if (ignoreCase)
                    finder.setCaseSensitive(false);
            }
            finder.setNewStart(getCaretPos());
        }
        if (myPreviousFindEnd != getCaretPos()) {
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
        return !myInserting;
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
        List<StyleRange> result = new ArrayList<StyleRange>();
        mergerNext();
        int start = mergeRangesPosition;
        boolean blue = mergeRangesIsBlue;
        boolean highlight = mergeRangesIsHighlight;
        while (mergerNext()) {
            if (blue || highlight) {
                result.add(new StyleRange(start, mergeRangesPosition - start, blue ? colorBlue : null,
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
                myTextAreasStart);
            if ((mergeIndexChange & 1) == 1) {
                result = (int) Math.min(myBytesPerLine * numberOfLines,
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
        if (myLongSelectionListeners.isEmpty()) return;

        Event basicEvent = new Event();
        basicEvent.widget = this;
        SelectionEvent anEvent = new SelectionEvent(basicEvent);
        anEvent.width = (int) (myStart >>> 32);
        anEvent.x = (int) myStart;
        anEvent.height = (int) (myEnd >>> 32);
        anEvent.y = (int) myEnd;

        for (SelectionListener aListener : myLongSelectionListeners) {
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
        long total = myClipboard.getContents(content, caretPos, myInserting);
        myStart = caretPos;
        myEnd = caretPos + total;
        myCaretStickToStart = false;
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


    void redrawTextAreas(int mode, StringBuffer newText, StringBuffer resultHex, StringBuffer resultChar,
                         List<StyleRange> viewRanges)
    {
        hexText.getCaret().setVisible(false);
        previewText.getCaret().setVisible(false);
        if (mode == SET_TEXT) {
            linesText.getContent().setText(newText.toString());
            hexText.getContent().setText(resultHex.toString());
            previewText.getContent().setText(resultChar.toString());
            myPreviousLine = -1;
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
            if (myPreviousLine >= 0 && myPreviousLine < numberOfLines)
                myPreviousLine += newText.length() / charsForAddress * (forward ? 1 : -1);
            if (myPreviousLine < -1 || myPreviousLine >= numberOfLines)
                myPreviousLine = -1;
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

        long newLinesStart = myTextAreasStart;
        int linesShifted = numberOfLines;
        int mode = SET_TEXT;
        if (!fromScratch && myPreviousRedrawStart >= 0L) {
            long lines = (myTextAreasStart - myPreviousRedrawStart) / myBytesPerLine;
            if (Math.abs(lines) < numberOfLines) {
                mode = lines > 0L ? SHIFT_BACKWARD : SHIFT_FORWARD;
                linesShifted = Math.abs((int) lines);
                if (linesShifted < 1) {
                    refreshSelections();
                    refreshCaretsPosition();

                    return;
                }
                if (mode == SHIFT_BACKWARD)
                    newLinesStart = myTextAreasStart + (numberOfLines - (int) lines) * myBytesPerLine;
            }
        }
        myPreviousRedrawStart = myTextAreasStart;

        StringBuffer newText = cookAddresses(newLinesStart, linesShifted * myBytesPerLine);

        List<Long> changeRanges = new ArrayList<Long>();
        int actuallyRead;
        try {
            actuallyRead = content.get(ByteBuffer.wrap(tmpRawBuffer, 0, linesShifted * myBytesPerLine),
                                         changeRanges, newLinesStart);
        }
        catch (IOException e) {
            actuallyRead = 0;
        }
        StringBuffer resultHex = cookTexts(true, actuallyRead);
        StringBuffer resultChar = cookTexts(false, actuallyRead);
        getHighlightRangesInScreen(newLinesStart, linesShifted * myBytesPerLine);
        List<StyleRange> viewRanges = mergeRanges(changeRanges, highlightRangesInScreen);
        redrawTextAreas(mode, newText, resultHex, resultChar, viewRanges);
        refreshSelections();
        refreshCaretsPosition();
    }


    private void refreshCaretsPosition()
    {
        drawUnfocusedCaret(false);
        long caretLocation = getCaretPos() - myTextAreasStart;
        if (caretLocation >= 0L && caretLocation < myBytesPerLine * numberOfLines ||
            getCaretPos() == content.length() && caretLocation == myBytesPerLine * numberOfLines) {
            int tmp = (int) caretLocation;
            if (tmp == myBytesPerLine * numberOfLines) {
                hexText.setCaretOffset(tmp * 3 - 1);
                previewText.setCaretOffset(tmp);
            } else {
                hexText.setCaretOffset(tmp * 3 + myUpANibble);
                previewText.setCaretOffset(tmp);
            }
            int line = hexText.getLineAtOffset(hexText.getCaretOffset());
            if (line != myPreviousLine) {
                if (myPreviousLine >= 0 && myPreviousLine < numberOfLines) {
                    hexText.setLineBackground(myPreviousLine, 1, null);
                    previewText.setLineBackground(myPreviousLine, 1, null);
                }
                hexText.setLineBackground(line, 1, colorCaretLine);
                previewText.setLineBackground(line, 1, colorCaretLine);
                myPreviousLine = line;
            }
            hexText.getCaret().setVisible(true);
            previewText.getCaret().setVisible(true);
            getDisplay().asyncExec(new Runnable() {
                public void run()
                {
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
        hexHeaderText.setText(headerRow.substring(0, Math.min(myBytesPerLine * 3, headerRow.length())));
    }


    void refreshSelections()
    {
        if (myStart >= myEnd ||
            myStart > myTextAreasStart + myBytesPerLine * numberOfLines ||
            myEnd <= myTextAreasStart)
            return;

        long startLocation = myStart - myTextAreasStart;
        if (startLocation < 0L) startLocation = 0L;
        int intStart = (int) startLocation;

        long endLocation = myEnd - myTextAreasStart;
        if (endLocation > myBytesPerLine * numberOfLines)
            endLocation = myBytesPerLine * numberOfLines;
        int intEnd = (int) endLocation;

        if (myCaretStickToStart) {
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
     * Removes the specified selection listener
     *
     * @see StyledText#removeSelectionListener(org.eclipse.swt.events.SelectionListener)
     */
    public void removeLongSelectionListener(SelectionListener listener)
    {
        if (listener == null)
            throw new IllegalArgumentException();

        myLongSelectionListeners.remove(listener);
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
        byte[] replaceData = replaceString.getBytes();
        if (isHexString) {
            replaceData = hexStringToByte(replaceString);
        }
        ByteBuffer newSelection = ByteBuffer.wrap(replaceData);
        if (myInserting) {
            content.insert(newSelection, myStart);
        } else {
            newSelection.limit((int) Math.min(newSelection.limit(), content.length() - myStart));
            content.overwrite(newSelection, myStart);
        }
        myEnd = myStart + newSelection.limit() - newSelection.position();
        myCaretStickToStart = false;
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
            Display.getCurrent().asyncExec(delayed);
        }
    }


    void runnableEnd()
    {
        if (delayedWaiting != null) {
            Display.getCurrent().asyncExec(delayedWaiting);
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
        myUpANibble = 0;
        boolean selection = myStart != myEnd;
        myStart = 0L;
        if (start > 0L) {
            myStart = start;
            if (myStart > content.length()) myStart = content.length();
        }

        myEnd = myStart;
        if (end > myStart) {
            myEnd = end;
            if (myEnd > content.length()) myEnd = content.length();
        }

        notifyLongSelectionListeners();
        if (selection != (myStart != myEnd))
            notifyListeners(SWT.Modify, null);
    }


    void setAddressesGridDataWidthHint()
    {
        ((GridData) linesText.getLayoutData()).widthHint = charsForAddress * fontCharWidth;
    }


    void setCaretsSize(boolean insert)
    {
        myInserting = insert;
        int width = 0;
        int height = hexText.getCaret().getSize().y;
        if (!myInserting)
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
        boolean firstContent = content == null;
        if (!firstContent) {
            content.dispose();
        }
        content = aContent;
        finder = null;
        if (content != null) {
            content.setActionsHistory();
        }

        if (firstContent || myEnd > content.length() || myTextAreasStart >= content.length()) {
            myTextAreasStart = myStart = myEnd = 0L;
            myCaretStickToStart = false;
        }

        charsForFileSizeAddress = Long.toHexString(content.length()).length();

        updateScrollBar();
        redrawTextAreas(true);
        notifyLongSelectionListeners();
        notifyListeners(SWT.Modify, null);
    }


    /**
     * Causes the receiver to have the keyboard focus. Within Eclipse, never call setFocus() before
     * the workbench has called EditorActionBarContributor.setActiveEditor()
     *
     * @see Composite#setFocus()
     */
    public boolean setFocus()
    {
        redrawCaret(false);
        if (myLastFocusedTextArea == 1)
            return hexText.setFocus();
        else
            return previewText.setFocus();
    }


    /**
     * @throws IllegalArgumentException if font height is 1 or 2
     * @see Control#setFont(org.eclipse.swt.graphics.Font)
     *      Font height must not be 1 or 2.
     */
    public void setFont(Font font)
    {
        // bugfix: HexText's raw array overflows when font is very small and window very big
        // very small sizes would compromise responsiveness in large windows, and they are too small
        // to see anyway
        if (font != null) {
            int newSize = font.getFontData()[0].getHeight();
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
        setCaretsSize(myInserting);
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
        if (myCaretStickToStart) {
            myStart = Math.min(newPos, myEnd);
            myEnd = Math.max(newPos, myEnd);
        } else {
            myEnd = Math.max(newPos, myStart);
            myStart = Math.min(newPos, myStart);
        }
        myCaretStickToStart = myEnd != newPos;
    }


    /**
     * Shows the position on screen.
     *
     * @param position where relocation should go
     */
    public void showMark(long position)
    {
        myLastLocationPosition = position;
        if (position < 0) return;

        position = position - position % myBytesPerLine;
        myTextAreasStart = position;
        if (numberOfLines > 2)
            myTextAreasStart = position - (numberOfLines / 2) * myBytesPerLine;
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
            result = (content.length() - 1L) / myBytesPerLine + 1L;
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

        myUpANibble = 0;
        myStart = selection[0];
        myEnd = selection[1];
        myCaretStickToStart = false;
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
        ((DisplayedContent) hexText.getContent()).setDimensions(myBytesPerLine * 3, numberOfLines);
        ((DisplayedContent) previewText.getContent()).setDimensions(myBytesPerLine, numberOfLines);
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
        vertical.setSelection((int) ((myTextAreasStart / myBytesPerLine) >>> verticalBarFactor));
        vertical.setPageIncrement(numberOfLines_1);
        vertical.setThumb(numberOfLines);
    }


    void updateTextsMetrics()
    {
        int width = getClientArea().width - linesText.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        int displayedNumberWidth = fontCharWidth * 4;  // hexText and previewText
        myBytesPerLine = (width / displayedNumberWidth) & 0xfffffff8;  // 0, 8, 16, 24, etc.
        if (myBytesPerLine < 16)
            myBytesPerLine = 16;
        gridData5.widthHint = hexText.computeTrim(0, 0,
                                                      myBytesPerLine * 3 * fontCharWidth, 100).width;
        gridData6.widthHint = previewText.computeTrim(0, 0,
                                                      myBytesPerLine * fontCharWidth, 100).width;
        updateNumberOfLines();
        changed(new Control[]{hexHeaderText, linesText, hexText, previewText});
        updateScrollBar();
        refreshHeader();
        myTextAreasStart = (((long) getVerticalBar().getSelection()) * myBytesPerLine) << verticalBarFactor;
        redrawTextAreas(true);
    }

}
