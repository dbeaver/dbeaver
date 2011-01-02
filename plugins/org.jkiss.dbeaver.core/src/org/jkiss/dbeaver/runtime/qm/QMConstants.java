/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.preference.IPreferenceStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * QM log constants
 */
public class QMConstants {

    public static final String PROP_PREFIX = "qm.";

    public static final String PROP_OBJECT_TYPES = PROP_PREFIX + "objectTypes";
    public static final String PROP_QUERY_TYPES = PROP_PREFIX + "queryTypes";
    public static final String PROP_ENTRIES_PER_PAGE = PROP_PREFIX + "maxEntries";
    public static final String PROP_HISTORY_DAYS = PROP_PREFIX + "historyDays";

    public static final String OBJECT_TYPE_SESSION = "session";
    public static final String OBJECT_TYPE_TRANSACTION = "txn";
    public static final String OBJECT_TYPE_SCRIPT = "script";
    public static final String OBJECT_TYPE_QUERY = "query";

}
