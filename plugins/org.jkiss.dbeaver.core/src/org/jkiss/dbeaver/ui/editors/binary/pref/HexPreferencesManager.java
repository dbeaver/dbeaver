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
package org.jkiss.dbeaver.ui.editors.binary.pref;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.List;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.binary.HexEditControl;

import java.util.*;

/**
 * Manager of all preferences-editing widgets, with an optional standalone dialog.
 *
 * @author Jordi
 */
public class HexPreferencesManager {

    static final int itemsDisplayed = 9;  // Number of font names displayed in list
    static final java.util.Set<Integer> scalableSizes = new TreeSet<>(
        Arrays.asList(6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 22, 32, 72));

    private static final String TEXT_BOLD = CoreMessages.editor_binary_hex_font_style_bold;
    private static final String TEXT_BOLD_ITALIC = CoreMessages.editor_binary_hex_font_style_bold_italic;
    private static final String TEXT_ITALIC = CoreMessages.editor_binary_hex_font_style_italic;
    private static final String TEXT_REGULAR = CoreMessages.editor_binary_hex_font_style_regular;
    public static final String SAMPLE_TEXT = CoreMessages.editor_binary_hex_sample_text;

    private java.util.List<FontData> fontsListCurrent = null;
    private java.util.List<FontData> fontsNonScalable = null;
    private java.util.List<FontData> fontsScalable = null;
    private GC fontsGc = null;
    private java.util.Set<String> fontsRejected = null;
    private java.util.Map<String, Set<Integer>> fontsSorted = null;
    private FontData sampleFontData = null;

    private Composite composite = null;
    private Composite parent = null;
    private Text text = null;
    private Text text1 = null;
    private Text text2 = null;
    private List list = null;
    private List list1 = null;
    private List list2 = null;
    private Font sampleFont = null;
    private Text sampleText = null;

    static int fontStyleToInt(String styleString)
    {
        int style = SWT.NORMAL;
        if (TEXT_BOLD.equals(styleString))
            style = SWT.BOLD;
        else if (TEXT_ITALIC.equals(styleString))
            style = SWT.ITALIC;
        else if (TEXT_BOLD_ITALIC.equals(styleString))
            style = SWT.BOLD | SWT.ITALIC;

        return style;
    }


    static String fontStyleToString(int style)
    {
        switch (style) {
            case SWT.BOLD:
                return TEXT_BOLD;
            case SWT.ITALIC:
                return TEXT_ITALIC;
            case SWT.BOLD | SWT.ITALIC:
                return TEXT_BOLD_ITALIC;
            default:
                return TEXT_REGULAR;
        }
    }


    public HexPreferencesManager(FontData aFontData)
    {
        sampleFontData = aFontData;
        fontsSorted = new TreeMap<>();
    }


    /**
     * Creates all internal widgets
     */
    void createComposite()
    {
        composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FillLayout());

        Group group = new Group(composite, SWT.NONE);
        group.setText(CoreMessages.editor_binary_hex_froup_font_selection);
        group.setVisible(true);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        group.setLayout(gridLayout);

        Label label = UIUtils.createControlLabel(group, CoreMessages.editor_binary_hex_label_available_fix_width_fonts);
        label.setVisible(true);
        GridData gridData = new GridData();
        gridData.horizontalSpan = 3;
        label.setLayoutData(gridData);
        UIUtils.createControlLabel(group, CoreMessages.editor_binary_hex_label_name);
        UIUtils.createControlLabel(group, CoreMessages.editor_binary_hex_label_style);
        UIUtils.createControlLabel(group, CoreMessages.editor_binary_hex_label_size);

        text = new Text(group, SWT.SINGLE | SWT.BORDER);
        GridData gridData4 = new GridData();
        gridData4.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        text.setLayoutData(gridData4);
        text1 = new Text(group, SWT.BORDER);
        GridData gridData5 = new GridData();
        gridData5.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        text1.setLayoutData(gridData5);
        text1.setEnabled(false);
        text2 = new Text(group, SWT.BORDER);
        GridData gridData6 = new GridData();
        gridData6.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        GC gc = new GC(group);
        int averageCharWidth = gc.getFontMetrics().getAverageCharWidth();
        gc.dispose();
        gridData6.widthHint = averageCharWidth * 6;
        text2.setLayoutData(gridData6);

        list = new List(group, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
        GridData gridData52 = new GridData();
        gridData52.heightHint = itemsDisplayed * list.getItemHeight();
        gridData52.widthHint = averageCharWidth * 40;
        list.setLayoutData(gridData52);
        list.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                text.setText(list.getSelection()[0]);
                updateSizeItemsAndGuessSelected();
                updateAndRefreshSample();
            }
        });

        list1 = new List(group, SWT.SINGLE | SWT.BORDER);
        GridData gridData21 = new GridData();
        gridData21.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData21.widthHint = averageCharWidth * TEXT_BOLD_ITALIC.length() * 2;
        list1.setLayoutData(gridData21);
        list1.setItems(new String[]{TEXT_REGULAR, TEXT_BOLD, TEXT_ITALIC, TEXT_BOLD_ITALIC});
        list1.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                text1.setText(list1.getSelection()[0]);
                updateAndRefreshSample();
            }
        });

        list2 = new List(group, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
        GridData gridData7 = new GridData();
        gridData7.widthHint = gridData6.widthHint;
        gridData7.heightHint = gridData52.heightHint;
        list2.setLayoutData(gridData7);
        list2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                text2.setText(list2.getSelection()[0]);
                updateAndRefreshSample();
            }
        });
        sampleText = new Text(group, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY | SWT.BORDER);
        sampleText.setText(SAMPLE_TEXT);
        sampleText.setEditable(false);
        GridData gridData8 = new GridData();
        gridData8.horizontalSpan = 3;
        gridData8.widthHint = gridData52.widthHint + gridData21.widthHint + gridData7.widthHint +
            gridLayout.horizontalSpacing * 2;
        gridData8.heightHint = 50;
        gridData8.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        sampleText.setLayoutData(gridData8);
        sampleText.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                if (sampleFont != null && !sampleFont.isDisposed()) {
                    sampleFont.dispose();
                }
            }
        });
    }


    /**
     * Creates the part containing all preferences-editing widgets, that is, ok and cancel
     * buttons are left out so we can call this method from both standalone and plugin.
     *
     * @param aParent composite where preferences will be drawn
     */
    public Composite createPreferencesPart(Composite aParent)
    {
        parent = aParent;
        createComposite();
        if (fontsSorted.size() < 1) {
            populateFixedCharWidthFonts();
        } else {
            list.setItems(fontsSorted.keySet().toArray(new String[fontsSorted.keySet().size()]));
            refreshWidgets();
        }

        return composite;
    }


    /**
     * Get the preferred font data
     *
     * @return a copy of the preferred font data
     */
    public FontData getFontData()
    {
        return new FontData(
            sampleFontData.getName(),
            sampleFontData.getHeight(),
            sampleFontData.getStyle());
    }


    FontData getNextFontData()
    {
        if (fontsListCurrent.size() == 0) {
            fontsListCurrent = fontsScalable;
        }
        FontData aData = fontsListCurrent.get(0);
        fontsListCurrent.remove(0);
        while (fontsRejected.contains(aData.getName()) && fontsScalable.size() > 0) {
            if (fontsListCurrent.size() == 0) {
                fontsListCurrent = fontsScalable;
            }
            aData = fontsListCurrent.get(0);
            fontsListCurrent.remove(0);
        }

        return aData;
    }


    int getSize()
    {
        int size = 0;
        if (!"".equals(text2.getText())) { //$NON-NLS-1$
            try {
                size = Integer.parseInt(text2.getText());
            }
            catch (NumberFormatException e) {
                // do nothing
            }  // was not a number, keep it 0
        }
        // bugfix: HexText's raw array overflows when font is very small and window very big
        // very small sizes would compromise responsiveness in large windows, and they are too small
        // to see anyway
        if (size == 1 || size == 2) size = 3;

        return size;
    }


    void populateFixedCharWidthFonts()
    {
        fontsNonScalable = new ArrayList<>(Arrays.asList(Display.getCurrent().getFontList(null, false)));
        fontsScalable = new ArrayList<>(Arrays.asList(Display.getCurrent().getFontList(null, true)));
        if (fontsNonScalable.size() == 0 && fontsScalable.size() == 0) {
            fontsNonScalable = null;
            fontsScalable = null;

            return;
        }
        fontsListCurrent = fontsNonScalable;
        fontsRejected = new HashSet<>();
        fontsGc = new GC(parent);
        DBeaverUI.asyncExec(new Runnable() {
            @Override
            public void run() {
                populateFixedCharWidthFontsAsync();
            }
        });
    }


    void populateFixedCharWidthFontsAsync()
    {
        FontData fontData = getNextFontData();
        if (!fontsRejected.contains(fontData.getName())) {
            boolean isScalable = fontsListCurrent == fontsScalable;
            int height = 10;
            if (!isScalable) height = fontData.getHeight();
            Font font = new Font(Display.getCurrent(), fontData.getName(), height, SWT.NORMAL);
            fontsGc.setFont(font);
            int width = fontsGc.getAdvanceWidth((char) 0x020);
            boolean isFixedWidth = true;
            for (int j = 0x021; j < 0x0100 && isFixedWidth; ++j) {
                if (((char)j) == '.' && j != '.') continue;
                if (width != fontsGc.getAdvanceWidth((char) j)) isFixedWidth = false;
            }
            font.dispose();
            if (isFixedWidth) {
                if (isScalable) {
                    fontsSorted.put(fontData.getName(), scalableSizes);
                } else {
                    Set<Integer> heights = fontsSorted.get(fontData.getName());
                    if (heights == null) {
                        heights = new TreeSet<>();
                        fontsSorted.put(fontData.getName(), heights);
                    }
                    heights.add(fontData.getHeight());
                }
                if (!list.isDisposed())
                    list.setItems(fontsSorted.keySet().toArray(new String[fontsSorted.keySet().size()]));
                refreshWidgets();
            } else {
                fontsRejected.add(fontData.getName());
            }
        }
        if (fontsNonScalable.size() == 0 && fontsScalable.size() == 0) {
            if (!parent.isDisposed()) fontsGc.dispose();
            fontsGc = null;
            fontsNonScalable = fontsScalable = fontsListCurrent = null;
            fontsRejected = null;
        } else {
            DBeaverUI.asyncExec(new Runnable() {
                @Override
                public void run() {
                    populateFixedCharWidthFontsAsync();
                }
            });
        }
    }


    void refreshSample()
    {
        if (sampleFont != null && !sampleFont.isDisposed()) {
            sampleFont.dispose();
        }
        sampleFont = new Font(Display.getCurrent(), sampleFontData);
        sampleText.setFont(sampleFont);
    }


    void refreshWidgets()
    {
        if (composite.isDisposed())
            return;

        if (fontsSorted == null || !fontsSorted.containsKey(sampleFontData.getName())) {
            text.setText(CoreMessages.editor_binary_hex_default_font);
        } else {
            text.setText(sampleFontData.getName());
        }
        showSelected(list, sampleFontData.getName());

        text1.setText(fontStyleToString(sampleFontData.getStyle()));
        list1.select(list1.indexOf(fontStyleToString(sampleFontData.getStyle())));

        updateSizeItems();
        text2.setText(Integer.toString(sampleFontData.getHeight()));
        showSelected(list2, Integer.toString(sampleFontData.getHeight()));

        refreshSample();
    }


    /**
     * Set preferences to show a font. Use null to show default font.
     *
     * @param aFontData the font to be shown.
     */
    public void setFontData(FontData aFontData)
    {
        if (aFontData == null)
            aFontData = HexEditControl.DEFAULT_FONT_DATA;
        sampleFontData = aFontData;
        refreshWidgets();
    }


    static void showSelected(List aList, String item)
    {
        int selected = aList.indexOf(item);
        if (selected >= 0) {
            aList.setSelection(selected);
            aList.setTopIndex(Math.max(0, selected - itemsDisplayed + 1));
        } else {
            aList.deselectAll();
            aList.setTopIndex(0);
        }
    }


    void updateAndRefreshSample()
    {
        sampleFontData = new FontData(text.getText(), getSize(), fontStyleToInt(text1.getText()));
        refreshSample();
    }


    void updateSizeItems()
    {
        Set<Integer> sizes = fontsSorted.get(text.getText());
        if (sizes == null) {
            list2.removeAll();
            return;
        }
        String[] items = new String[sizes.size()];
        int i = 0;
        for (Iterator<Integer> j = sizes.iterator(); i < items.length; ++i) items[i] = j.next().toString();
        list2.setItems(items);
    }


    void updateSizeItemsAndGuessSelected()
    {
        int lastSize = getSize();
        updateSizeItems();

        int position = 0;
        String[] items = list2.getItems();
        for (int i = 1; i < items.length; ++i) {
            if (lastSize >= Integer.parseInt(items[i]))
                position = i;
        }
        text2.setText(items[position]);
        showSelected(list2, items[position]);
    }
}
