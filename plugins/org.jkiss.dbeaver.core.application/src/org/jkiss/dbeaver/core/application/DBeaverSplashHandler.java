
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
package org.jkiss.dbeaver.core.application;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.splash.BasicSplashHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * @since 3.3
 * 
 */
public class DBeaverSplashHandler extends BasicSplashHandler {

	public static final int TOTAL_LOADING_TASKS = 20;
	private static DBeaverSplashHandler instance;

    public static IProgressMonitor getActiveMonitor()
    {
        if (instance == null) {
            return null;
        } else {
            return instance.getBundleProgressMonitor();
        }
    }

    private Font normalFont;
    private Font boldFont;

    public DBeaverSplashHandler()
    {
        instance = this;
    }

    @Override
    public void init(Shell splash) {
        super.init(splash);

        try {
            initVisualization();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        getBundleProgressMonitor().beginTask("Loading", TOTAL_LOADING_TASKS);
    }

    private void initVisualization() {
        String progressRectString = null, messageRectString = null, foregroundColorString = null,
            versionCoordString = null, versionInfoSizeString = null, versionInfoColorString = null;
        final IProduct product = Platform.getProduct();
        if (product != null) {
            progressRectString = product.getProperty(IProductConstants.STARTUP_PROGRESS_RECT);
            messageRectString = product.getProperty(IProductConstants.STARTUP_MESSAGE_RECT);
            foregroundColorString = product.getProperty(IProductConstants.STARTUP_FOREGROUND_COLOR);
            versionCoordString = product.getProperty("versionInfoCoord");
            versionInfoSizeString = product.getProperty("versionInfoSize");
            versionInfoColorString = product.getProperty("versionInfoColor");
        }

        setProgressRect(StringConverter.asRectangle(progressRectString, new Rectangle(275, 300, 280, 10)));
        setMessageRect(StringConverter.asRectangle(messageRectString, new Rectangle(275,275,280,25)));
        final Point versionCoord = StringConverter.asPoint(versionCoordString, new Point(485, 215));
        final int versionInfoSize = StringConverter.asInt(versionInfoSizeString, 22);
        final RGB versionInfoRGB = StringConverter.asRGB(versionInfoColorString, new RGB(255,255,255));

        int foregroundColorInteger = 0xD2D7FF;
        try {
			if (foregroundColorString != null) {
				foregroundColorInteger = Integer.parseInt(foregroundColorString, 16);
			}
        } catch (Exception ex) {
            // ignore
        }

        setForeground(
			new RGB(
				(foregroundColorInteger & 0xFF0000) >> 16,
                (foregroundColorInteger & 0xFF00) >> 8,
                foregroundColorInteger & 0xFF));

        normalFont = getContent().getFont();
        //boldFont = UIUtils.makeBoldFont(normalFont);
        FontData[] fontData = normalFont.getFontData();
        fontData[0].setStyle(fontData[0].getStyle() | SWT.BOLD);
        fontData[0].setHeight(versionInfoSize);
        boldFont = new Font(normalFont.getDevice(), fontData[0]);

        final Color versionColor = new Color(getContent().getDisplay(), versionInfoRGB);

        getContent().addPaintListener(e -> {
            String productVersion = "";
            if (product != null) {
                productVersion = GeneralUtils.getPlainVersion();
            }
            //String osVersion = Platform.getOS() + " " + Platform.getOSArch();
            if (boldFont != null) {
                e.gc.setFont(boldFont);
            }
            e.gc.setForeground(versionColor);
            e.gc.drawText(productVersion, versionCoord.x, versionCoord.y, true);
            //e.gc.drawText(osVersion, 115, 200, true);
            e.gc.setFont(normalFont);
        });
    }

    @Override
    public void dispose()
    {
        super.dispose();
        if (boldFont != null) {
            boldFont.dispose();
            boldFont = null;
        }
        instance = null;
    }

	public static void showMessage(String message) {
		if (message == null || message.isEmpty() || message.startsWith(">")) {
			return;
		}
		IProgressMonitor activeMonitor = getActiveMonitor();
		if (activeMonitor != null) {
			activeMonitor.setTaskName(message);
			activeMonitor.worked(1);
		}
	}

    /*
     private final static int F_LABEL_HORIZONTAL_INDENT = 175;

     private final static int F_BUTTON_WIDTH_HINT = 80;

     private final static int F_TEXT_WIDTH_HINT = 175;

     private final static int F_COLUMN_COUNT = 3;

     private Composite fCompositeLogin;

     private Text fTextUsername;

     private Text fTextPassword;

     private Button fButtonOK;

     private Button fButtonCancel;

     private boolean fAuthenticated;

     */
/**
	 * 
	 */
/*
	public DBeaverSplashHandler() {
		fCompositeLogin = null;
		fTextUsername = null;
		fTextPassword = null;
		fButtonOK = null;
		fButtonCancel = null;
		fAuthenticated = false;
	}

    public void init(final Shell splash) {
		// Store the shell
		super.init(splash);
		// Configure the shell layout
		configureUISplash();
		// Create UI
		createUI();		
		// Create UI listeners
		createUIListeners();
		// Force the splash screen to layout
		splash.layout(true);
		// Keep the splash screen visible and prevent the RCP application from 
		// loading until the close button is clicked.
		doEventLoop();
	}
	
	private void doEventLoop() {
		Shell splash = getSplash();
		while (fAuthenticated == false) {
			if (splash.getDisplay().readAndDispatch() == false) {
				splash.getDisplay().sleep();
			}
		}
	}

	private void createUIListeners() {
		// Create the OK button listeners
		createUIListenersButtonOK();
		// Create the cancel button listeners
		createUIListenersButtonCancel();
	}

	private void createUIListenersButtonCancel() {
		fButtonCancel.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleButtonCancelWidgetSelected();
			}
		});		
	}

	private void handleButtonCancelWidgetSelected() {
		// Abort the loading of the RCP application
		getSplash().getDisplay().close();
		System.exit(0);		
	}
	
	private void createUIListenersButtonOK() {
		fButtonOK.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleButtonOKWidgetSelected();
			}
		});				
	}

	private void handleButtonOKWidgetSelected() {
		String username = fTextUsername.getText();
		String password = fTextPassword.getText();
		// Aunthentication is successful if a user provides any username and
		// any password
		if ((username.length() > 0) &&
				(password.length() > 0)) {
			fAuthenticated = true;
		} else {
			MessageDialog.openError(
					getSplash(),
					"Authentication Failed",  //NON-NLS-1
					"A username and password must be specified to login.");  //NON-NLS-1
		}
	}
	
	private void createUI() {
		// Create the login panel
		createUICompositeLogin();
		// Create the blank spanner
		createUICompositeBlank();
		// Create the user name label
		createUILabelUserName();
		// Create the user name text widget
		createUITextUserName();
		// Create the password label
		createUILabelPassword();
		// Create the password text widget
		createUITextPassword();
		// Create the blank label
		createUILabelBlank();
		// Create the OK button
		createUIButtonOK();
		// Create the cancel button
		createUIButtonCancel();
	}		
	
	private void createUIButtonCancel() {
		// Create the button
		fButtonCancel = new Button(fCompositeLogin, SWT.PUSH);
		fButtonCancel.setText("Cancel"); //NON-NLS-1
		// Configure layout data
		GridData data = new GridData(SWT.NONE, SWT.NONE, false, false);
		data.widthHint = F_BUTTON_WIDTH_HINT;	
		data.verticalIndent = 10;
		fButtonCancel.setLayoutData(data);
	}

	private void createUIButtonOK() {
		// Create the button
		fButtonOK = new Button(fCompositeLogin, SWT.PUSH);
		fButtonOK.setText("OK"); //NON-NLS-1
		// Configure layout data
		GridData data = new GridData(SWT.NONE, SWT.NONE, false, false);
		data.widthHint = F_BUTTON_WIDTH_HINT;
		data.verticalIndent = 10;
		fButtonOK.setLayoutData(data);
	}

	private void createUILabelBlank() {
		Label label = new Label(fCompositeLogin, SWT.NONE);
		label.setVisible(false);
	}

	private void createUITextPassword() {
		// Create the text widget
		int style = SWT.PASSWORD | SWT.BORDER;
		fTextPassword = new Text(fCompositeLogin, style);
		// Configure layout data
		GridData data = new GridData(SWT.NONE, SWT.NONE, false, false);
		data.widthHint = F_TEXT_WIDTH_HINT;
		data.horizontalSpan = 2;
		fTextPassword.setLayoutData(data);		
	}

	private void createUILabelPassword() {
		// Create the label
		Label label = new Label(fCompositeLogin, SWT.NONE);
		label.setText("&Password:"); //NON-NLS-1
		// Configure layout data
		GridData data = new GridData();
		data.horizontalIndent = F_LABEL_HORIZONTAL_INDENT;
		label.setLayoutData(data);					
	}

	private void createUITextUserName() {
		// Create the text widget
		fTextUsername = new Text(fCompositeLogin, SWT.BORDER);
		// Configure layout data
		GridData data = new GridData(SWT.NONE, SWT.NONE, false, false);
		data.widthHint = F_TEXT_WIDTH_HINT;
		data.horizontalSpan = 2;
		fTextUsername.setLayoutData(data);		
	}

	private void createUILabelUserName() {
		// Create the label
		Label label = new Label(fCompositeLogin, SWT.NONE);
		label.setText("&User Name:"); //NON-NLS-1
		// Configure layout data
		GridData data = new GridData();
		data.horizontalIndent = F_LABEL_HORIZONTAL_INDENT;
		label.setLayoutData(data);		
	}

	private void createUICompositeBlank() {
		Composite spanner = new Composite(fCompositeLogin, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = F_COLUMN_COUNT;
		spanner.setLayoutData(data);
	}

	private void createUICompositeLogin() {
		// Create the composite
		fCompositeLogin = new Composite(getSplash(), SWT.BORDER);
		GridLayout layout = new GridLayout(F_COLUMN_COUNT, false);
		fCompositeLogin.setLayout(layout);		
	}

	private void configureUISplash() {
		// Configure layout
		FillLayout layout = new FillLayout(); 
		getSplash().setLayout(layout);
		// Force shell to inherit the splash background
		getSplash().setBackgroundMode(SWT.INHERIT_DEFAULT);
	}
*/

}
