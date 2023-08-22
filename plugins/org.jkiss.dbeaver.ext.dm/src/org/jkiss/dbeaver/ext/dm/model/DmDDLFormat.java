package org.jkiss.dbeaver.ext.dm.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.utils.CommonUtils;

public enum DmDDLFormat {
	FULL("Full DDL", true, true, true), NO_STORAGE("No storage information", false, true, true),
	COMPACT("Compact form", false, false, false);

	private final String title;
	private final boolean showStorage;
	private final boolean showSegments;
	private final boolean showTablespace;

	private static final Log log = org.jkiss.dbeaver.Log.getLog(DmDDLFormat.class);

	private DmDDLFormat(String title, boolean showStorage, boolean showSegments, boolean showTablespace)
    {
        this.showTablespace = showTablespace;
        this.showSegments = showSegments;
        this.showStorage = showStorage;
        this.title = title;
    }

	public String getTitle() {
		return title;
	}

	public boolean isShowStorage() {
		return showStorage;
	}

	public boolean isShowSegments() {
		return showSegments;
	}

	public boolean isShowTablespace() {
		return showTablespace;
	}

	public static DmDDLFormat getCurrentFormat(DmDataSource dataSource) {
		String ddlFormatString = dataSource.getContainer().getPreferenceStore()
				.getString(DmConstants.PREF_KEY_DDL_FORMAT);
		if (!CommonUtils.isEmpty(ddlFormatString)) {
			try {
				return DmDDLFormat.valueOf(ddlFormatString);
			} catch (IllegalArgumentException e) {
				log.error(e);
			}
		}
		return DmDDLFormat.FULL;
	}
}
