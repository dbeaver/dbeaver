/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.DBPNamedObject;

import java.text.MessageFormat;

/**
 * Custom combo editor
 */
public class CustomComboBoxCellEditor extends CellEditor {

	private String[] items;
	private CCombo comboBox;

	private static final int defaultStyle = SWT.NONE;

	public CustomComboBoxCellEditor() {
		setStyle(defaultStyle);
	}

	public CustomComboBoxCellEditor(Composite parent, String[] items) {
		this(parent, items, defaultStyle);
	}

	public CustomComboBoxCellEditor(Composite parent, String[] items, int style) {
		super(parent, style);
		setItems(items);
	}

	/**
	 * Returns the list of choices for the combo box
	 *
	 * @return the list of choices for the combo box
	 */
	public String[] getItems() {
		return this.items;
	}

	/**
	 * Sets the list of choices for the combo box
	 *
	 * @param items
	 *            the list of choices for the combo box
	 */
	public void setItems(String[] items) {
		Assert.isNotNull(items);
		this.items = items;
		populateComboBoxItems();
	}

	protected Control createControl(Composite parent) {

		comboBox = new CCombo(parent, getStyle());
        //comboBox.setEditable((getStyle() & SWT.READ_ONLY) == 0);
		comboBox.setFont(parent.getFont());
        comboBox.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		populateComboBoxItems();

		comboBox.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				keyReleaseOccured(e);
			}
		});

		comboBox.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent event) {
				applyEditorValueAndDeactivate();
			}
			public void widgetSelected(SelectionEvent event) {
			}
		});

		comboBox.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE
						|| e.detail == SWT.TRAVERSE_RETURN) {
					e.doit = false;
				}
			}
		});

		comboBox.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				CustomComboBoxCellEditor.this.focusLost();
			}
		});
		return comboBox;
	}

	protected Object doGetValue() {
		return comboBox.getText();
	}

	/*
	 * (non-Javadoc) Method declared on CellEditor.
	 */
	protected void doSetFocus() {
		comboBox.setFocus();
        fireEnablementChanged(DELETE);
        fireEnablementChanged(COPY);
        fireEnablementChanged(CUT);
        fireEnablementChanged(PASTE);
	}

	public LayoutData getLayoutData() {
		LayoutData layoutData = super.getLayoutData();
		if ((comboBox == null) || comboBox.isDisposed()) {
			layoutData.minimumWidth = 60;
		} else {
			// make the comboBox 10 characters wide
			GC gc = new GC(comboBox);
			layoutData.minimumWidth = (gc.getFontMetrics()
					.getAverageCharWidth() * 10) + 10;
			gc.dispose();
		}
		return layoutData;
	}

	/**
	 * The <code>ComboBoxCellEditor</code> implementation of this
	 * <code>CellEditor</code> framework method accepts a zero-based index of
	 * a selection.
	 *
	 * @param value
	 *            the zero-based index of the selection wrapped as an
	 *            <code>Integer</code>
	 */
	protected void doSetValue(Object value) {
		Assert.isTrue(comboBox != null && (value instanceof String || value instanceof DBPNamedObject));
        if (value instanceof DBPNamedObject) {
            comboBox.setText(((DBPNamedObject) value).getName());
        } else {
		    comboBox.setText(CommonUtils.toString(value));
        }
	}

	/**
	 * Updates the list of choices for the combo box for the current control.
	 */
	private void populateComboBoxItems() {
		if (comboBox != null && items != null) {
			comboBox.removeAll();
			for (int i = 0; i < items.length; i++) {
				comboBox.add(items[i], i);
			}

			setValueValid(true);
		}
	}

	/**
	 * Applies the currently selected value and deactivates the cell editor
	 */
	void applyEditorValueAndDeactivate() {
		// must set the selection before getting value
		Object newValue = doGetValue();
		markDirty();
		boolean isValid = isCorrect(newValue);
		setValueValid(isValid);

		if (!isValid) {
            setErrorMessage(MessageFormat.format(getErrorMessage(), comboBox.getText() ));
		}

		fireApplyEditorValue();
		deactivate();
	}

	protected void focusLost() {
		if (isActivated()) {
			applyEditorValueAndDeactivate();
		}
	}

	protected void keyReleaseOccured(KeyEvent keyEvent) {
		if (keyEvent.character == '\u001b') { // Escape character
			fireCancelEditor();
		} else if (keyEvent.character == SWT.TAB) { // tab key
			applyEditorValueAndDeactivate();
        } else if (keyEvent.character == SWT.DEL) { // tab key
            comboBox.setText("");
            keyEvent.doit = false;
		}
	}

    public boolean isCopyEnabled() {
        return comboBox != null && !comboBox.isDisposed();
    }

    public boolean isCutEnabled() {
        return comboBox != null && !comboBox.isDisposed();
    }

    public boolean isDeleteEnabled() {
        return comboBox != null && !comboBox.isDisposed();
    }

    public boolean isPasteEnabled() {
        return comboBox != null && !comboBox.isDisposed();
    }

    public void performCopy() {
        comboBox.copy();
    }

    public void performCut() {
        comboBox.cut();
    }

    public void performDelete() {
        comboBox.setText("");
    }

    public void performPaste() {
        comboBox.paste();
    }

}
