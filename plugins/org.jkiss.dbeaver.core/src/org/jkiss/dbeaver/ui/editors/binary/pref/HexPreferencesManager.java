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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
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

    private static final int itemsDisplayed = 9;  // Number of font names displayed in list
    private static final Set<Integer> scalableSizes = new TreeSet<>(
        Arrays.asList(6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 22, 32, 72));

    private static final String TEXT_BOLD = CoreMessages.editor_binary_hex_font_style_bold;
    private static final String TEXT_BOLD_ITALIC = CoreMessages.editor_binary_hex_font_style_bold_italic;
    private static final String TEXT_ITALIC = CoreMessages.editor_binary_hex_font_style_italic;
    private static final String TEXT_REGULAR = CoreMessages.editor_binary_hex_font_style_regular;
    private static final String SAMPLE_TEXT = CoreMessages.editor_binary_hex_sample_text;

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
    private Text textStyle = null;
    private Text textSize = null;
    private List listFont = null;
    private List listStyle = null;
    private List listSize = null;
    private Font sampleFont = null;
    private Text sampleText = null;
    private Combo cmbByteWidth = null;
    private String defWidthValue; 
	private static String[] arrDefValuetoIndex = new String[] { "4", "8", "16" };
    
    private static int fontStyleToInt(String styleString)
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


    private static String fontStyleToString(int style)
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


    HexPreferencesManager(FontData aFontData, String defWidth) {
        sampleFontData = aFontData;
        fontsSorted = new TreeMap<>();
        defWidthValue = defWidth;      
    }


    /**
     * Creates all internal widgets
     */
    private void createComposite()
    {
        composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));

        {
            Group fontGroup = UIUtils.createControlGroup(composite, CoreMessages.editor_binary_hex_froup_font_selection, 3, GridData.FILL_HORIZONTAL, 0);

            Label label = UIUtils.createControlLabel(fontGroup, CoreMessages.editor_binary_hex_label_available_fix_width_fonts);
            GridData gridData = new GridData();
            gridData.horizontalSpan = 3;
            label.setLayoutData(gridData);
            UIUtils.createControlLabel(fontGroup, CoreMessages.editor_binary_hex_label_name);
            UIUtils.createControlLabel(fontGroup, CoreMessages.editor_binary_hex_label_style);
            UIUtils.createControlLabel(fontGroup, CoreMessages.editor_binary_hex_label_size);

            Text textName = new Text(fontGroup, SWT.SINGLE | SWT.BORDER);
            GridData gridData4 = new GridData();
            gridData4.horizontalAlignment = GridData.FILL;
            textName.setLayoutData(gridData4);

            textStyle = new Text(fontGroup, SWT.BORDER);
            GridData gridData5 = new GridData();
            gridData5.horizontalAlignment = GridData.FILL;
            textStyle.setLayoutData(gridData5);
            textStyle.setEnabled(false);

            textSize = new Text(fontGroup, SWT.BORDER);
            GridData gridData6 = new GridData();
            gridData6.horizontalAlignment = GridData.FILL;
            GC gc = new GC(fontGroup);
            int averageCharWidth = gc.getFontMetrics().getAverageCharWidth();
            gc.dispose();
            gridData6.widthHint = averageCharWidth * 6;
            textSize.setLayoutData(gridData6);

            listFont = new List(fontGroup, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
            GridData gridData52 = new GridData();
            gridData52.heightHint = itemsDisplayed * listFont.getItemHeight();
            gridData52.widthHint = averageCharWidth * 40;
            listFont.setLayoutData(gridData52);
            listFont.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    textName.setText(listFont.getSelection()[0]);
                    updateSizeItemsAndGuessSelected();
                    updateAndRefreshSample();
                }
            });

            listStyle = new List(fontGroup, SWT.SINGLE | SWT.BORDER);
            GridData gridData21 = new GridData();
            gridData21.verticalAlignment = GridData.FILL;
            gridData21.widthHint = averageCharWidth * TEXT_BOLD_ITALIC.length() * 2;
            listStyle.setLayoutData(gridData21);
            listStyle.setItems(TEXT_REGULAR, TEXT_BOLD, TEXT_ITALIC, TEXT_BOLD_ITALIC);
            listStyle.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    textStyle.setText(listStyle.getSelection()[0]);
                    updateAndRefreshSample();
                }
            });

            listSize = new List(fontGroup, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
            GridData gridData7 = new GridData();
            gridData7.widthHint = gridData6.widthHint;
            gridData7.heightHint = gridData52.heightHint;
            listSize.setLayoutData(gridData7);
            listSize.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    textSize.setText(listSize.getSelection()[0]);
                    updateAndRefreshSample();
                }
            });

            sampleText = new Text(fontGroup, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY | SWT.BORDER);
            sampleText.setText(SAMPLE_TEXT);
            sampleText.setEditable(false);
            GridData gridData8 = new GridData();
            gridData8.horizontalSpan = 3;
            gridData8.widthHint = gridData52.widthHint + gridData21.widthHint + gridData7.widthHint + 10;
            gridData8.heightHint = 50;
            gridData8.horizontalAlignment = GridData.FILL;
            sampleText.setLayoutData(gridData8);
            sampleText.addDisposeListener(e -> {
                if (sampleFont != null && !sampleFont.isDisposed()) {
                    sampleFont.dispose();
                }
            });
        }

        {
            Group cmpByteSettings = UIUtils.createControlGroup(composite, "Settings", 2, GridData.FILL_HORIZONTAL, 0);
            UIUtils.createControlLabel(cmpByteSettings, "Default width");
            cmbByteWidth = new Combo(cmpByteSettings, SWT.BORDER);
            cmbByteWidth.setItems(arrDefValuetoIndex);
            int index = Arrays.asList(arrDefValuetoIndex).indexOf(defWidthValue);
            cmbByteWidth.select(index);
        }
    }
    
	String getDefWidth() {
		return cmbByteWidth.getText();
	}


    /**
     * Creates the part containing all preferences-editing widgets, that is, ok and cancel
     * buttons are left out so we can call this method from both standalone and plugin.
     *
     * @param aParent composite where preferences will be drawn
     */
    Composite createPreferencesPart(Composite aParent)
    {
        parent = aParent;
        createComposite();
        if (fontsSorted.size() < 1) {
            populateFixedCharWidthFonts();
        } else {
            listFont.setItems(fontsSorted.keySet().toArray(new String[fontsSorted.keySet().size()]));
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


    private FontData getNextFontData()
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
        if (!"".equals(textSize.getText())) { //$NON-NLS-1$
            try {
                size = Integer.parseInt(textSize.getText());
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


    private void populateFixedCharWidthFonts()
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
        DBeaverUI.asyncExec(this::populateFixedCharWidthFontsAsync);
    }


    private void populateFixedCharWidthFontsAsync()
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
                if (!listFont.isDisposed())
                    listFont.setItems(fontsSorted.keySet().toArray(new String[fontsSorted.keySet().size()]));
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
            DBeaverUI.asyncExec(this::populateFixedCharWidthFontsAsync);
        }
    }


    private void refreshSample()
    {
        if (sampleFont != null && !sampleFont.isDisposed()) {
            sampleFont.dispose();
        }
        sampleFont = new Font(Display.getCurrent(), sampleFontData);
        sampleText.setFont(sampleFont);
    }


    private void refreshWidgets()
    {
        if (composite.isDisposed())
            return;

        if (fontsSorted == null || !fontsSorted.containsKey(sampleFontData.getName())) {
            text.setText(CoreMessages.editor_binary_hex_default_font);
        } else {
            text.setText(sampleFontData.getName());
        }
        showSelected(listFont, sampleFontData.getName());

        textStyle.setText(fontStyleToString(sampleFontData.getStyle()));
        listStyle.select(listStyle.indexOf(fontStyleToString(sampleFontData.getStyle())));

        updateSizeItems();
        textSize.setText(Integer.toString(sampleFontData.getHeight()));
        showSelected(listSize, Integer.toString(sampleFontData.getHeight()));

        refreshSample();
    }


    /**
     * Set preferences to show a font. Use null to show default font.
     *
     * @param aFontData the font to be shown.
     */
    void setFontData(FontData aFontData)
    {
        if (aFontData == null)
            aFontData = HexEditControl.DEFAULT_FONT_DATA;
        sampleFontData = aFontData;
        refreshWidgets();
    }
    
   

    private static void showSelected(List aList, String item)
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


    private void updateAndRefreshSample()
    {
        sampleFontData = new FontData(text.getText(), getSize(), fontStyleToInt(textStyle.getText()));
        refreshSample();
    }


    private void updateSizeItems()
    {
        Set<Integer> sizes = fontsSorted.get(text.getText());
        if (sizes == null) {
            listSize.removeAll();
            return;
        }
        String[] items = new String[sizes.size()];
        int i = 0;
        for (Iterator<Integer> j = sizes.iterator(); i < items.length; ++i) items[i] = j.next().toString();
        listSize.setItems(items);
    }


    private void updateSizeItemsAndGuessSelected()
    {
        int lastSize = getSize();
        updateSizeItems();

        int position = 0;
        String[] items = listSize.getItems();
        for (int i = 1; i < items.length; ++i) {
            if (lastSize >= Integer.parseInt(items[i]))
                position = i;
        }
        textSize.setText(items[position]);
        showSelected(listSize, items[position]);
    }
}
