package org.jkiss.dbeaver.ext.yashandb.debug.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @Author yangmeng on 2023/3/20 17:51
 */
public class YashanDBUtil {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public static String toJson(Object o){
        return gson.toJson(o);
    }
}
