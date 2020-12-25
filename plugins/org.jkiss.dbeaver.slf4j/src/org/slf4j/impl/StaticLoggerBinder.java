/*
  DBeaver loopback
 */
package org.slf4j.impl;

import org.jkiss.dbeaver.Log;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * SLF logger binder for dbeaver
 */
public class StaticLoggerBinder implements LoggerFactoryBinder, ILoggerFactory {

    private static final Log log = Log.getLog(StaticLoggerBinder.class);

    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

    public static StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    public ILoggerFactory getLoggerFactory() {
        return this;
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return StaticLoggerBinder.class.getName();
    }

    @Override
    public Logger getLogger(String name) {
        return new SLFLogger(name);
    }

}
