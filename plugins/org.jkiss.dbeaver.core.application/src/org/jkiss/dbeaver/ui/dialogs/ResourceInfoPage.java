/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.net.URI;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;

/**
 * The ResourceInfoPage is the page that shows the basic info about the
 * resource.
 */
public class ResourceInfoPage extends PropertyPage {
    static final Log log = Log.getLog(ResourceInfoPage.class);
    private static String BYTES_LABEL = "{0} Bytes";
    private static String FILE_LABEL = "File";
    private static String FILE_NOT_EXIST_TEXT = "File not exist";
    private static String FILE_TYPE_FORMAT = "File";
    private static String FOLDER_LABEL = "Folder";
    private static String LINKED_FILE_LABEL = "Linked file";
    private static String LINKED_FOLDER_LABEL = "Linked folder";
    private static String VIRTUAL_FOLDER_LABEL = "Virtual folder";
    private static String VIRTUAL_FOLDER_TEXT = "Virtual folder";
    private static String MISSING_PATH_VARIABLE_TEXT = "Undefined path variable";
    private static String NOT_EXIST_TEXT = "Not exist";
    private static String NOT_LOCAL_TEXT = "Not local";
    private static String PROJECT_LABEL = "Project";
    private static String UNKNOWN_LABEL = "Unknown";

    private static String TYPE_TITLE = "Type";
    private static String LOCATION_TITLE = "Location";
    private static String RESOLVED_LOCATION_TITLE = "Resolved location";
    private static String SIZE_TITLE = "Size";
    private static String PATH_TITLE = "Path";
    private static String TIMESTAMP_TITLE = "Last modified";

	private IContentDescription cachedContentDescription;

	private Combo encodingEditor;

	private Text resolvedLocationValue = null;

    // Max value width in characters before wrapping
	private static final int MAX_VALUE_WIDTH = 80;
    private String defEncoding;

    private Composite createBasicInfoGroup(Composite basicInfoComposite, IResource resource) {
		// The group for path
		Label pathLabel = new Label(basicInfoComposite, SWT.NONE);
		pathLabel.setText(PATH_TITLE);
		GridData gd = new GridData();
		gd.verticalAlignment = SWT.TOP;
		pathLabel.setLayoutData(gd);

		// path value label
		Text pathValueText = new Text(basicInfoComposite, SWT.WRAP
				| SWT.READ_ONLY);
		String pathString = TextProcessor.process(resource.getFullPath()
				.toString());
		pathValueText.setText(pathString);
		gd = new GridData();
		gd.widthHint = convertWidthInCharsToPixels(MAX_VALUE_WIDTH);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		pathValueText.setLayoutData(gd);
		pathValueText.setBackground(pathValueText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		// The group for types
		Label typeTitle = new Label(basicInfoComposite, SWT.LEFT);
		typeTitle.setText(TYPE_TITLE);

		Text typeValue = new Text(basicInfoComposite, SWT.LEFT | SWT.READ_ONLY);
		typeValue.setText(getTypeString(resource, getContentDescription(resource)));
		typeValue.setBackground(typeValue.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		if (resource.isLinked() && !resource.isVirtual()) {
			// The group for location
			Label locationTitle = new Label(basicInfoComposite, SWT.LEFT);
			locationTitle.setText(LOCATION_TITLE);
			gd = new GridData();
			gd.verticalAlignment = SWT.TOP;
			locationTitle.setLayoutData(gd);

			Composite locationComposite = new Composite(basicInfoComposite, SWT.NULL);
            GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			locationComposite.setLayout(layout);
			gd = new GridData();
			gd.widthHint = convertWidthInCharsToPixels(MAX_VALUE_WIDTH);
			gd.grabExcessHorizontalSpace = true;
			gd.verticalAlignment = SWT.TOP;
			gd.horizontalAlignment = GridData.FILL;
			locationComposite.setLayoutData(gd);

            Text locationValue = new Text(locationComposite, SWT.WRAP | SWT.READ_ONLY);
			String locationStr = TextProcessor.process(getLocationText(resource));
			locationValue.setText(locationStr);
			gd = new GridData();
			gd.widthHint = convertWidthInCharsToPixels(MAX_VALUE_WIDTH);
			gd.grabExcessHorizontalSpace = true;
			gd.verticalAlignment = SWT.TOP;
			gd.horizontalAlignment = GridData.FILL;
			locationValue.setLayoutData(gd);
			locationValue.setBackground(locationValue.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

			// displayed in all cases since the link can be changed to a path variable any time by the user in this dialog
			Label resolvedLocationTitle = new Label(basicInfoComposite, SWT.LEFT);
			resolvedLocationTitle.setText(RESOLVED_LOCATION_TITLE);
			gd = new GridData();
			gd.verticalAlignment = SWT.TOP;
			resolvedLocationTitle.setLayoutData(gd);

			resolvedLocationValue = new Text(basicInfoComposite, SWT.WRAP | SWT.READ_ONLY);
			resolvedLocationValue.setText(getResolvedLocationText(resource));
			gd = new GridData();
			gd.widthHint = convertWidthInCharsToPixels(MAX_VALUE_WIDTH);
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalAlignment = GridData.FILL;
			resolvedLocationValue.setLayoutData(gd);
			resolvedLocationValue.setBackground(resolvedLocationValue
					.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		} else {
			if (!resource.isVirtual()) {
				// The group for location
				Label locationTitle = new Label(basicInfoComposite, SWT.LEFT);
				locationTitle.setText(LOCATION_TITLE);
				gd = new GridData();
				gd.verticalAlignment = SWT.TOP;
				locationTitle.setLayoutData(gd);

				Text locationValue = new Text(basicInfoComposite, SWT.WRAP
						| SWT.READ_ONLY);
				String locationStr = TextProcessor.process(
                    getLocationText(resource));
				locationValue.setText(locationStr);
				gd = new GridData();
				gd.widthHint = convertWidthInCharsToPixels(MAX_VALUE_WIDTH);
				gd.grabExcessHorizontalSpace = true;
				gd.horizontalAlignment = GridData.FILL;
				locationValue.setLayoutData(gd);
				locationValue.setBackground(locationValue.getDisplay()
						.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			}
		}
		if (resource.getType() == IResource.FILE) {
			// The group for size
			Label sizeTitle = new Label(basicInfoComposite, SWT.LEFT);
			sizeTitle.setText(SIZE_TITLE);

			Text sizeValue = new Text(basicInfoComposite, SWT.LEFT | SWT.READ_ONLY);
			sizeValue.setText(getSizeString(resource));
			gd = new GridData();
			gd.widthHint = convertWidthInCharsToPixels(MAX_VALUE_WIDTH);
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalAlignment = GridData.FILL;
			sizeValue.setLayoutData(gd);
			sizeValue.setBackground(sizeValue.getDisplay().getSystemColor(
					SWT.COLOR_WIDGET_BACKGROUND));
		}

		Label timeStampLabel = new Label(basicInfoComposite, SWT.NONE);
		timeStampLabel.setText(TIMESTAMP_TITLE);

		// timeStamp value label
		Text timeStampValue = new Text(basicInfoComposite, SWT.READ_ONLY);
		timeStampValue.setText(
            getDateStringValue(resource));
		timeStampValue.setBackground(timeStampValue.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		timeStampValue.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

		return basicInfoComposite;
	}

	protected Control createContents(Composite parent) {

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(),
				"Resource properties");

		// layout the page
		IResource resource = getElement().getAdapter(
				IResource.class);
		
		if (resource == null) {
			Label label = new Label(parent, SWT.NONE);
			label.setText("No Resource");
			return label;
		}
		
		// top level group
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

        Composite encodingComposite = new Composite(composite, SWT.NULL);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        encodingComposite.setLayout(layout);
        encodingComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		createBasicInfoGroup(encodingComposite, resource);

        {
            defEncoding = GeneralUtils.getDefaultFileEncoding();

            UIUtils.createControlLabel(encodingComposite, "Encoding");
            if (resource instanceof IFile) {
                try {
                    defEncoding = ((IFile) resource).getCharset();
                } catch (CoreException e) {
                    log.error(e);
                }
            }
            encodingEditor = UIUtils.createEncodingCombo(encodingComposite, defEncoding);
        }

		Dialog.applyDialogFont(composite);

		return composite;
	}

	private IContentDescription getContentDescription(IResource resource) {
		if (resource.getType() != IResource.FILE) {
			return null;
		}

		if (cachedContentDescription == null) {
			try {
				cachedContentDescription = ((IFile) resource)
						.getContentDescription();
			} catch (CoreException e) {
				// silently ignore
			}
		}
		return cachedContentDescription;
	}

	protected void performDefaults() {

		IResource resource = getElement().getAdapter(IResource.class);

        if (resource == null) {
            return;
        }

		if (encodingEditor != null) {
			encodingEditor.setText(defEncoding);
		}
	}

	/**
	 * Apply the read only state and the encoding to the resource.
	 */
	public boolean performOk() {
        IResource resource = getElement().getAdapter(IResource.class);

        if (resource == null) {
            return true;
        }
		try {
            // This must be invoked after the 'derived' property has been set,
            // because it may influence the place where encoding is stored.
            if (encodingEditor != null && !encodingEditor.getText().equals(defEncoding)) {
                String newEncoding = encodingEditor.getText();
                applyEncoding(resource, newEncoding, new NullProgressMonitor());
                defEncoding = newEncoding;
            }

		} catch (CoreException exception) {
			ErrorDialog.openError(getShell(),
                "Error", exception.getLocalizedMessage(), exception.getStatus());
			return false;
		}

		return true;
	}

    private void applyEncoding(IResource resource, String encoding, IProgressMonitor monitor) throws CoreException {
        if (resource instanceof IFile) {
            ((IFile) resource).setCharset(encoding, monitor);
        } else if (resource instanceof IContainer) {
            for (IResource child : ((IContainer) resource).members()) {
                applyEncoding(child, encoding, monitor);
            }
        }
    }

    private static String getContentTypeString(IContentDescription description) {
        if (description != null) {
            IContentType contentType = description.getContentType();
            if (contentType != null) {
                return contentType.getName();
            }
        }
        return null;
    }

    public static String getDateStringValue(IResource resource) {
        // don't access the file system for closed projects (bug 151089)
        if (!isProjectAccessible(resource)) {
            return UNKNOWN_LABEL;
        }

        URI location = resource.getLocationURI();
        if (location == null) {
            if (resource.isLinked()) {
                return MISSING_PATH_VARIABLE_TEXT;
            }
            return NOT_EXIST_TEXT;
        }

        IFileInfo info = getFileInfo(location);
        if (info == null) {
            return UNKNOWN_LABEL;
        }

        if (info.exists()) {
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG,
                DateFormat.MEDIUM);
            return format.format(new Date(info.getLastModified()));
        }
        return NOT_EXIST_TEXT;
    }

    public static IFileInfo getFileInfo(URI location) {
        if (location.getScheme() == null)
            return null;
        IFileStore store = getFileStore(location);
        if (store == null) {
            return null;
        }
        return store.fetchInfo();
    }

    public static IFileStore getFileStore(URI uri) {
        try {
            return EFS.getStore(uri);
        } catch (CoreException e) {
            log.error(e);
            return null;
        }
    }

    /**
     * Get the location of a resource
     *
     * @param resource
     * @return String the text to display the location
     */
    public static String getLocationText(IResource resource) {
        if (resource.isVirtual())
            return VIRTUAL_FOLDER_TEXT;
        URI resolvedLocation = resource.getLocationURI();
        URI location = resolvedLocation;
        boolean isLinked = resource.isLinked();
        if (isLinked) {
            location = resource.getRawLocationURI();
        }
        if (location == null) {
            return NOT_EXIST_TEXT;
        }

        if (resolvedLocation.getScheme() == null)
            return location.toString();

        IFileStore store = getFileStore(resolvedLocation);
        // don't access the file system for closed projects (bug 151089)
        boolean isPathVariable = isPathVariable(resource);
        if (isProjectAccessible(resource) && !isPathVariable) {
            // No path variable used. Display the file not exist message
            // in the location. Fixes bug 33318.
            if (store == null) {
                return UNKNOWN_LABEL;
            }
            if (!store.fetchInfo().exists()) {
                return NLS.bind(FILE_NOT_EXIST_TEXT, store.toString());
            }
        }
        if (isLinked && isPathVariable) {
            String tmp = org.eclipse.core.filesystem.URIUtil.toPath(resource.getRawLocationURI()).toOSString();
            return resource.getPathVariableManager().convertToUserEditableFormat(tmp, true);
        }
        if (store != null) {
            return store.toString();
        }
        return location.toString();
    }

    public static String getResolvedLocationText(IResource resource) {
        URI location = resource.getLocationURI();
        if (location == null) {
            if (resource.isLinked()) {
                return MISSING_PATH_VARIABLE_TEXT;
            }

            return NOT_EXIST_TEXT;
        }

        if (location.getScheme() == null)
            return UNKNOWN_LABEL;

        IFileStore store = getFileStore(location);
        if (store == null) {
            return UNKNOWN_LABEL;
        }

        // don't access the file system for closed projects (bug 151089)
        if (isProjectAccessible(resource) && !store.fetchInfo().exists()) {
            return NLS.bind(FILE_NOT_EXIST_TEXT, store.toString());
        }

        return store.toString();
    }

    public static String getSizeString(IResource resource) {
        if (resource.getType() != IResource.FILE) {
            return ""; //$NON-NLS-1$
        }

        IFile file = (IFile) resource;

        URI location = file.getLocationURI();
        if (location == null) {
            if (file.isLinked()) {
                return MISSING_PATH_VARIABLE_TEXT;
            }

            return NOT_EXIST_TEXT;
        }

        IFileInfo info = getFileInfo(location);
        if (info == null) {
            return UNKNOWN_LABEL;
        }

        if (info.exists()) {
            return NLS.bind(BYTES_LABEL, NumberFormat.getInstance().format(
                info.getLength()));
        }

        return NOT_EXIST_TEXT;
    }

    public static String getTypeString(IResource resource,
                                       IContentDescription description) {

        if (resource.getType() == IResource.FILE) {
            if (resource.isLinked()) {
                return LINKED_FILE_LABEL;
            }

            if (resource instanceof IFile) {
                String contentType = getContentTypeString(description);
                if (contentType != null) {
                    return MessageFormat.format(FILE_TYPE_FORMAT, contentType);
                }
            }
            return FILE_LABEL;
        }

        if (resource.getType() == IResource.FOLDER) {
            if (resource.isVirtual()) {
                return VIRTUAL_FOLDER_LABEL;
            }
            if (resource.isLinked()) {
                return LINKED_FOLDER_LABEL;
            }

            return FOLDER_LABEL;
        }

        if (resource.getType() == IResource.PROJECT) {
            return PROJECT_LABEL;
        }

        // Should not be possible
        return UNKNOWN_LABEL;
    }

    private static boolean isPathVariable(IResource resource) {
        if (!resource.isLinked()) {
            return false;
        }

        URI resolvedLocation = resource.getLocationURI();
        if (resolvedLocation == null) {
            // missing path variable
            return true;
        }
        URI rawLocation = resource.getRawLocationURI();
        return !resolvedLocation.equals(rawLocation);

    }

    private static boolean isProjectAccessible(IResource resource) {
        IProject project = resource.getProject();
        return project != null && project.isAccessible();
    }

}
