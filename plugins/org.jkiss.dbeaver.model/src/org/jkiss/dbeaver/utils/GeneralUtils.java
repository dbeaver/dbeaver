/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.utils;

import org.eclipse.core.internal.runtime.AdapterManager;
import org.eclipse.core.runtime.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.bundle.ModelActivator;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.impl.app.ApplicationDescriptor;
import org.jkiss.dbeaver.model.impl.app.ApplicationRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.utils.Base64;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * General non-ui utility methods
 */
public class GeneralUtils {
    private static final Log log = Log.getLog(GeneralUtils.class);

    public static final Pattern URI_SCHEMA_PATTERN = Pattern.compile("([a-zA-Z0-9-_]+:).+");

    public static final String UTF8_ENCODING = StandardCharsets.UTF_8.name();
    public static final String DEFAULT_ENCODING = UTF8_ENCODING;

    public static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;
    public static final Charset DEFAULT_FILE_CHARSET = UTF8_CHARSET;

    public static final String DEFAULT_TIMESTAMP_PATTERN = "yyyyMMddHHmm";
    public static final String DEFAULT_DATE_PATTERN = "yyyyMMdd";
    public static final String RESOURCE_NAME_FORBIDDEN_SYMBOLS_REGEX = "(?U)[^/:'\"\\\\<>|?*]+";

    public static final String[] byteToHex = new String[256];
    public static final char[] nibbleToHex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final char[] HEX_CHAR_TABLE = {
        '0', '1', '2', '3',
        '4', '5', '6', '7',
        '8', '9', 'a', 'b',
        'c', 'd', 'e', 'f'
    };

    public static final String PROP_TRUST_STORE = "javax.net.ssl.trustStore"; //$NON-NLS-1$
    public static final String PROP_TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType"; //$NON-NLS-1$
    public static final String VALUE_TRUST_STORE_TYPE_WINDOWS = "WINDOWS-ROOT"; //$NON-NLS-1$
    public static final String EMPTY_ENV_VARIABLE_VALUE = "''";

    static {
        // Compose byte to hex map
        for (int i = 0; i < 256; ++i) {
            byteToHex[i] = Character.toString(nibbleToHex[i >>> 4]) + nibbleToHex[i & 0x0f];
        }
    }

    private static final Pattern VAR_PATTERN = Pattern.compile(
        "(\\$\\{([\\w\\.\\-]+)(\\:[^\\$\\{\\}]+)?\\})", Pattern.CASE_INSENSITIVE);

    /**
     * Default encoding (UTF-8)
     */
    public static String getDefaultFileEncoding() {
        return UTF8_ENCODING;
    }

    public static String getDefaultLocalFileEncoding() {
        return System.getProperty(StandardConstants.ENV_FILE_ENCODING, getDefaultFileEncoding());
    }

    public static String getDefaultConsoleEncoding() {
        String consoleEncoding = System.getProperty(StandardConstants.ENV_CONSOLE_ENCODING);
        if (CommonUtils.isEmpty(consoleEncoding)) {
            consoleEncoding = System.getProperty(StandardConstants.ENV_FILE_ENCODING);
        }
        if (CommonUtils.isEmpty(consoleEncoding)) {
            consoleEncoding = getDefaultFileEncoding();
        }
        return consoleEncoding;
    }

    public static String getDefaultLineSeparator() {
        return System.getProperty(StandardConstants.ENV_LINE_SEPARATOR, "\n");
    }

    public static void writeBytesAsHex(Writer out, byte[] buf, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            byte b = buf[off + i];
            int v = b & 0xFF;
            out.write(HEX_CHAR_TABLE[v >>> 4]);
            out.write(HEX_CHAR_TABLE[v & 0xF]);
        }
    }

    public static String convertToString(byte[] bytes, int offset, int length) {
        if (length == 0) {
            return "";
        }
        char[] chars = new char[length];
        for (int i = offset; i < offset + length; i++) {
            int b = bytes[i];
            if (b < 0) b = 256 + b;
            if (b < 32 || (b >= 0x7F && b <= 0xA0)) b = 32;
            chars[i - offset] = (char) b;
        }
        return new String(chars);
    }

    /**
     * Converts string to byte array.
     * This is loosy algorithm because it gets only first byte from each char.
     */
    public static byte[] convertToBytes(String strValue) {
        int length = strValue.length();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            int c = strValue.charAt(i) & 255;
            if (c > 127) {
                c = -(c - 127);
            }
            bytes[i] = (byte) c;
        }
        return bytes;
    }

    public static Object makeDisplayString(Object object) {
        if (object == null) {
            return ""; //$NON-NLS-1$
        }
        if (object instanceof Number) {
            return NumberFormat.getInstance().format(object);
        }
        Class<?> eClass = object.getClass();
        if (eClass.isArray()) {
            if (eClass == byte[].class)
                return Arrays.toString((byte[]) object);
            else if (eClass == short[].class)
                return Arrays.toString((short[]) object);
            else if (eClass == int[].class)
                return Arrays.toString((int[]) object);
            else if (eClass == long[].class)
                return Arrays.toString((long[]) object);
            else if (eClass == char[].class)
                return Arrays.toString((char[]) object);
            else if (eClass == float[].class)
                return Arrays.toString((float[]) object);
            else if (eClass == double[].class)
                return Arrays.toString((double[]) object);
            else if (eClass == boolean[].class)
                return Arrays.toString((boolean[]) object);
            else { // element is an array of object references
                return Arrays.deepToString((Object[]) object);
            }
        }
        return object;
    }

    public static Object convertString(String value, Class<?> valueType) {
        try {
            if (CommonUtils.isEmpty(value)) {
                return null;
            }
            if (valueType == null || CharSequence.class.isAssignableFrom(valueType)) {
                return value;
            } else if (valueType == Boolean.class || valueType == Boolean.TYPE) {
                return Boolean.valueOf(value);
            } else if (valueType == Long.class) {
                return Long.valueOf(normalizeIntegerString(value));
            } else if (valueType == Long.TYPE) {
                return Long.parseLong(normalizeIntegerString(value));
            } else if (valueType == Integer.class) {
                return Integer.valueOf(normalizeIntegerString(value));
            } else if (valueType == Integer.TYPE) {
                return Integer.parseInt(normalizeIntegerString(value));
            } else if (valueType == Short.class) {
                return Short.valueOf(normalizeIntegerString(value));
            } else if (valueType == Short.TYPE) {
                return Short.parseShort(normalizeIntegerString(value));
            } else if (valueType == Byte.class) {
                return Byte.valueOf(normalizeIntegerString(value));
            } else if (valueType == Byte.TYPE) {
                return Byte.parseByte(normalizeIntegerString(value));
            } else if (valueType == Double.class) {
                return Double.valueOf(value);
            } else if (valueType == Double.TYPE) {
                return Double.parseDouble(value);
            } else if (valueType == Float.class) {
                return Float.valueOf(value);
            } else if (valueType == Float.TYPE) {
                return Float.parseFloat(value);
            } else if (valueType == BigInteger.class) {
                return new BigInteger(normalizeIntegerString(value));
            } else if (valueType == BigDecimal.class) {
                return new BigDecimal(value);
            } else {
                return value;
            }
        } catch (RuntimeException e) {
            log.error("Error converting value", e);
            return value;
        }
    }

    private static String normalizeIntegerString(String value) {
        int divPos = value.lastIndexOf('.');
        return divPos == -1 ? value : value.substring(0, divPos);
    }

    @NotNull
    public static IStatus makeInfoStatus(String message) {
        return new Status(
            IStatus.INFO,
            ModelPreferences.PLUGIN_ID,
            message,
            null);
    }

    @NotNull
    public static IStatus makeErrorStatus(String message) {
        return new Status(
            IStatus.ERROR,
            ModelPreferences.PLUGIN_ID,
            message,
            null);
    }

    @NotNull
    public static IStatus makeErrorStatus(String message, Throwable e) {
        return new Status(
            IStatus.ERROR,
            ModelPreferences.PLUGIN_ID,
            message,
            e);
    }

    @NotNull
    public static String getProductTitle() {
        return getProductName() + " " + getPlainVersion();
    }

    @NotNull
    public static String getLongProductTitle() {
        return getProductName() + " " + getProductVersion();
    }

    @NotNull
    public static String getProductName() {
        ApplicationDescriptor application = ApplicationRegistry.getInstance().getApplication();
        if (application != null) {
            return ApplicationRegistry.getInstance().getApplication().getName();
        }
        final IProduct product = Platform.getProduct();
        if (product != null) {
            return product.getName();
        }
        return "DBeaver";
    }

    @NotNull
    public static Version getProductVersion() {
        ApplicationDescriptor application = ApplicationRegistry.getInstance().getApplication();
        if (application != null) {
            return application.getContributorBundle().getVersion();
        }
        final IProduct product = Platform.getProduct();
        if (product == null) {
            return ModelActivator.getInstance().getBundle().getVersion();
        }
        return product.getDefiningBundle().getVersion();
    }

    @NotNull
    public static String getPlainVersion(String versionStr) {
        try {
            Version version = new Version(versionStr);
            return version.getMajor() + "." + version.getMinor() + "." + version.getMicro();
        } catch (Exception e) {
            return versionStr;
        }
    }

    @NotNull
    public static String getPlainVersion() {
        Version version = getProductVersion();
        return version.getMajor() + "." + version.getMinor() + "." + version.getMicro();
    }

    @NotNull
    public static String getMajorVersion() {
        Version version = getProductVersion();
        return version.getMajor() + "." + version.getMinor();
    }

    @NotNull
    public static Date getProductReleaseDate() {
        Bundle definingBundle = null;
        ApplicationDescriptor application = ApplicationRegistry.getInstance().getApplication();
        if (application != null) {
            definingBundle = application.getContributorBundle();
        } else {
            final IProduct product = Platform.getProduct();
            if (product != null) {
                definingBundle = product.getDefiningBundle();
            }
        }
        if (definingBundle == null) {
            return new Date();
        }

        final Dictionary<String, String> headers = definingBundle.getHeaders();
        final String releaseDate = headers.get("Bundle-Release-Date");
        if (releaseDate != null) {
            try {
                return new SimpleDateFormat(DEFAULT_DATE_PATTERN).parse(releaseDate);
            } catch (ParseException e) {
                log.debug(e);
            }
        }
        final String buildTime = headers.get("Build-Time");
        if (buildTime != null) {
            try {
                return new SimpleDateFormat(DEFAULT_TIMESTAMP_PATTERN).parse(buildTime);
            } catch (ParseException e) {
                log.debug(e);
            }
        }

        // Failed to get valid date from product bundle
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2017);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    @Nullable
    public static Date getProductBuildTime() {
        Bundle definingBundle = null;
        ApplicationDescriptor application = ApplicationRegistry.getInstance().getApplication();
        if (application != null) {
            definingBundle = application.getContributorBundle();
        } else {
            final IProduct product = Platform.getProduct();
            if (product != null) {
                definingBundle = product.getDefiningBundle();
            }
        }
        if (definingBundle == null) {
            return null;
        }

        final Dictionary<String, String> headers = definingBundle.getHeaders();
        final String buildTime = headers.get("Build-Time");
        if (buildTime != null) {
            try {
                return new SimpleDateFormat(DEFAULT_TIMESTAMP_PATTERN).parse(buildTime);
            } catch (ParseException e) {
                log.debug(e);
            }
        }
        return null;
    }

    @NotNull
    public static String getProductEarlyAccessURL() {
        return Platform.getProduct().getProperty("earlyAccessURL");
    }

    public static String getExpressionParseMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return e.getClass().getName();
        }
        int divPos = message.indexOf('@');
        return divPos == -1 ? message : message.substring(divPos + 1);
    }

    public static String trimAllWhitespaces(String str) {
        int len = str.length();
        int st = 0;
        while (st < len && isWhitespaceExt(str.charAt(st))) {
            st++;
        }
        while (st < len && isWhitespaceExt(str.charAt(len - 1))) {
            len--;
        }
        return ((st > 0) || (len < str.length() )) ?
            str.substring(st, len) : str;
    }

    public static boolean isWhitespaceExt(char c) {
        return c <= ' ' || c == 0x160;
    }

    public interface IParameterHandler {
        boolean setParameter(String name, String value);
    }

    public static class MapResolver implements IVariableResolver {
        private final Map<String, Object> variables;

        public MapResolver(Map<String, Object> variables) {
            this.variables = variables;
        }

        @Override
        public String get(String name) {
            Object value = variables.get(name);
            return value == null ? null : CommonUtils.toString(value);
        }
    }

    public static String replaceSystemPropertyVariables(String string) {
        if (string == null) {
            return null;
        }
        return replaceVariables(string, System::getProperty);
    }

    @NotNull
    public static String variablePattern(String name) {
        return "${" + name + "}";
    }

    public static boolean isVariablePattern(String pattern) {
        return pattern.startsWith("${") && pattern.endsWith("}");
    }

    @NotNull
    public static String generateVariablesLegend(@NotNull String[][] vars) {
        String[] varPatterns = new String[vars.length];
        int patternMaxLength = 0;
        for (int i = 0; i < vars.length; i++) {
            varPatterns[i] = GeneralUtils.variablePattern(vars[i][0]);
            patternMaxLength = Math.max(patternMaxLength, varPatterns[i].length());
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < vars.length; i++) {
            text.append(varPatterns[i]);
            // Indent
            for (int k = 0; k < patternMaxLength - varPatterns[i].length(); k++) {
                text.append(' ');
            }
            text.append(" - ").append(vars[i][1]).append("\n");
        }
        return text.toString();
    }

    /**
     * recursively iterates through all variables and returns root
     **/
    @Nullable
    public static String extractVariableName(@NotNull String variablePattern) {
        Matcher matcher = VAR_PATTERN.matcher(variablePattern);
        String name = null;
        String s = variablePattern;
        while (matcher.find()) {
            name = matcher.group(2);
            s = substituteVariable(s, matcher, "");
            matcher = VAR_PATTERN.matcher(s);
        }
        return name;
    }

    @NotNull
    public static String replaceVariables(@NotNull String string, IVariableResolver resolver) {
        return replaceVariables(string, resolver, false);
    }

    @NotNull
    public static String replaceVariables(@NotNull String string, IVariableResolver resolver, boolean isUpperCaseVarName) {
        if (CommonUtils.isEmpty(string)) {
            return string;
        }
        // We save resolved vars here to avoid resolve recursive cycles
        Map<String, String> resolvedVars = null;
        try {
            Matcher matcher = VAR_PATTERN.matcher(string);
            int pos = 0;
            while (matcher.find(pos)) {
                pos = matcher.end();
                String matchedName = matcher.group(2);
                String varName = isUpperCaseVarName ? matchedName.toUpperCase(Locale.ENGLISH) : matchedName;
                String varValue;
                if (resolvedVars != null) {
                    varValue = resolvedVars.get(varName);
                    if (varValue != null) {
                        string = substituteVariable(string, matcher, varValue);
                        matcher = VAR_PATTERN.matcher(string);
                        pos = 0;
                        continue;
                    }
                }
                varValue = resolver.get(varName);
                if (varValue == null) {
                    varValue = matcher.group(3);
                    if (varValue != null && varValue.startsWith(":")) {
                        varValue = varValue.substring(1);
                    }
                }
                if (varValue != null) {
                    if (resolvedVars == null) {
                        resolvedVars = new HashMap<>();
                        if (EMPTY_ENV_VARIABLE_VALUE.equals(varValue)) {
                            varValue = "";
                        }
                        resolvedVars.put(varName, varValue);
                    }
                    string = substituteVariable(string, matcher, varValue);
                    matcher = VAR_PATTERN.matcher(string);
                    pos = 0;
                }
            }
            return string;
        } catch (Exception e) {
            log.warn("Error matching regex", e);
            return string;
        }
    }

    @NotNull
    private static String substituteVariable(@NotNull String string, @NotNull Matcher matcher, @NotNull String varValue) {
        if (matcher.start() == 0 && matcher.end() >= string.length() - 1) {
            return varValue;
        } else {
            return string.substring(0, matcher.start()) + varValue + string.substring(matcher.end());
        }
    }

    public static IStatus makeExceptionStatus(Throwable ex) {
        return makeExceptionStatus(IStatus.ERROR, ex);
    }

    public static IStatus makeExceptionStatus(int severity, Throwable ex) {
        return makeExceptionStatus(severity, ex, false);
    }

    public static IStatus transformExceptionsToStatus(@NotNull List<Throwable> exceptions) {

        if (exceptions.isEmpty()) {
            return new Status(IStatus.ERROR, (Class<?>) null, "Empty exceptions list");
        }
        Set<String> exceptionMessageSet = new HashSet<>();
        IStatus prev = null;
        for (Throwable exception : exceptions) {
            String message = exception.getMessage();
            if (prev == null) {
                exceptionMessageSet.add(message);
                prev = new Status(
                    IStatus.ERROR,
                    ModelPreferences.PLUGIN_ID,
                    message,
                    null);
            } else {
                if (exceptionMessageSet.contains(message)) {
                    continue;
                }
                prev = new MultiStatus(ModelPreferences.PLUGIN_ID,
                    0,
                    new IStatus[]{prev},
                    message,
                    null);
            }
        }
        return prev;
    }

    private static IStatus makeExceptionStatus(int severity, Throwable ex, boolean nested) {
        if (ex instanceof InvocationTargetException) {
            ex = ((InvocationTargetException) ex).getTargetException();
        }
        if (ex instanceof CoreException) {
            return ((CoreException) ex).getStatus();
        }
        // Skip chain of nested DBExceptions. Show only last message
        while (ex.getCause() != null && ex.getMessage() != null && ex.getMessage().equals(ex.getCause().getMessage())) {
            ex = ex.getCause();
        }
        Throwable cause = ex.getCause();
        SQLException nextError = null;
        if (ex instanceof SQLException) {
            nextError = ((SQLException) ex).getNextException();
        } else if (cause instanceof SQLException) {
            nextError = ((SQLException) cause).getNextException();
        }
        if (cause == null && nextError == null) {
            return new Status(
                severity,
                ModelPreferences.PLUGIN_ID,
                getExceptionMessage(ex),
                ex);
        } else {
            if (nextError != null) {
                List<IStatus> errorChain = new ArrayList<>();
                if (cause != null) {
                    errorChain.add(makeExceptionStatus(severity, cause, true));
                }
                for (SQLException error = nextError; error != null; error = error.getNextException()) {
                    errorChain.add(new Status(
                        severity,
                        ModelPreferences.PLUGIN_ID,
                        getExceptionMessage(error)));
                }
                return new MultiStatus(
                    ModelPreferences.PLUGIN_ID,
                    0,
                    errorChain.toArray(new IStatus[0]),
                    getExceptionMessage(ex),
                    ex);
            } else {
                // Pass null exception to avoid dups in error message.
                // Real exception stacktrace will be passed in the root cause
                return new MultiStatus(
                    ModelPreferences.PLUGIN_ID,
                    0,
                    new IStatus[]{makeExceptionStatus(severity, cause, true)},
                    getExceptionMessage(ex),
                    !nested ? null : ex);
            }
        }
    }

    public static IStatus makeExceptionStatus(String message, Throwable ex) {
        return makeExceptionStatus(IStatus.ERROR, message, ex);
    }

    public static IStatus makeExceptionStatus(int severity, String message, Throwable ex) {
        if (CommonUtils.equalObjects(message, ex.getMessage())) {
            return makeExceptionStatus(severity, ex);
        }
        return new MultiStatus(
            ModelPreferences.PLUGIN_ID,
            0,
            new IStatus[]{makeExceptionStatus(severity, ex)},
            message,
            null);
    }

    public static IStatus getRootStatus(IStatus status) {
        IStatus[] children = status.getChildren();
        if (children == null || children.length == 0) {
            return status;
        } else {
            return getRootStatus(children[0]);
        }
    }

    public static String getStatusText(IStatus status) {
        StringBuilder text = new StringBuilder(status.getMessage());
        IStatus[] children = status.getChildren();
        if (children != null && children.length > 0) {
            for (IStatus child : children) {
                text.append("\n").append(getStatusText(child));
            }
        }
        return text.toString();
    }

    /**
     * Returns first non-null and non-empty message from this exception or it's cause
     */
    public static String getFirstMessage(Throwable ex) {
        for (Throwable e = ex; e != null; e = e.getCause()) {
            String message = e.getMessage();
            if (!CommonUtils.isEmpty(message)) {
                return message;
            }
        }
        return null;
    }

    public static String getExceptionMessage(@NotNull Throwable ex) {
/*
        StringBuilder msg = new StringBuilder(*/
        /*CommonUtils.getShortClassName(ex.getClass())*//*
);
        msg.append(ex.getClass().getSimpleName());
        if (ex.getMessage() != null) {
            msg.append(": ").append(ex.getMessage());
        }
        return msg.toString().trim();
*/
        try {
            ex.getClass().getDeclaredMethod("toString");
            return ex.toString();
        } catch (NoSuchMethodException e) {
            return ex.getMessage();
        }
    }

    @NotNull
    public static String serializeObject(@NotNull Object object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream os = new ObjectOutputStream(baos)) {
                os.writeObject(object);
            }
            return Base64.encode(baos.toByteArray());
        } catch (Throwable e) {
            log.warn("Error serializing object [" + object + "]", e);
            return "";
        }
    }

    public static Object deserializeObject(String text) {
        try {
            final byte[] bytes = Base64.decode(text);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            try (ObjectInputStream is = new ObjectInputStream(bais)) {
                return is.readObject();
            }
        } catch (Throwable e) {
            log.warn("Error deserializing object [" + text + "]", e);
            return null;
        }
    }

    public static Path getMetadataFolder() {
        Path workspacePath;
        if (!DBWorkbench.isPlatformStarted()) {
            log.debug("Platform not initialized: metadata folder may be not set");
            try {
                workspacePath = RuntimeUtils.getLocalPathFromURL(Platform.getInstanceLocation().getURL());
            } catch (IOException e) {
                throw new IllegalStateException("Can't parse workspace location URL", e);
            }
        } else {
            DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
            if (workspace == null) {
                log.warn("Metadata is read before workspace initialization");
                try {
                    workspacePath = RuntimeUtils.getLocalPathFromURL(Platform.getInstanceLocation().getURL());
                } catch (IOException e) {
                    throw new IllegalStateException("Can't parse workspace location URL", e);
                }
            } else {
                workspacePath = workspace.getAbsolutePath();
            }
        }
        Path metaDir = getMetadataFolder(workspacePath);
        if (!Files.exists(metaDir)) {
            try {
                Files.createDirectories(metaDir);
            } catch (IOException e) {
                return Platform.getLogFileLocation().toFile().toPath();
            }
        }
        return metaDir;
    }

    public static Path getMetadataFolder(Path workspaceFolder) {
        return workspaceFolder.resolve(DBPWorkspace.METADATA_FOLDER);
    }

    // Workaround for broken URLs.
    // In some cases we get file path from URI and it looks like file:/c:/path with spaces/
    // Thus we can't parse it as URL or URI (because of spaces and special characters)
    // and we can't parse it as file (because of file:/ prefix - it fail on Windows at least)
    // So we remove schema prefix if present and convert path to URI.
    @NotNull
    public static URI makeURIFromFilePath(@NotNull String path) throws URISyntaxException {
        Matcher matcher = URI_SCHEMA_PATTERN.matcher(path);
        if (matcher.matches()) {
            String plainPath = path.substring(matcher.end(1));
            if (RuntimeUtils.isWindows()) {
                // Trim leading slashes on Windows
                while (plainPath.startsWith("/") && plainPath.indexOf(':') >= 0)
                    plainPath = plainPath.substring(1);
            }
            return Path.of(plainPath).toUri();
        }
        return Path.of(path).toUri();
    }

    /////////////////////////////////////////////////////////////////////////
    // Adapters
    // Copy-pasted from org.eclipse.core.runtime.Adapters to support Eclipse Mars (#46667)

    public static <T> T adapt(Object sourceObject, Class<T> adapter, boolean allowActivation) {
        if (sourceObject == null) {
            return null;
        }
        if (adapter.isInstance(sourceObject)) {
            return adapter.cast(sourceObject);
        }

        if (sourceObject instanceof IAdaptable adaptable) {
            T result = adaptable.getAdapter(adapter);
            if (result != null) {
                // Sanity-check
                if (!adapter.isInstance(result)) {
                    throw new AssertionFailedException(adaptable.getClass().getName() + ".getAdapter(" + adapter.getName() + ".class) returned " //$NON-NLS-1$//$NON-NLS-2$
                        + result.getClass().getName() + " that is not an instance the requested type"); //$NON-NLS-1$
                }
                return result;
            }
        }

        // If the source object is a platform object then it's already tried calling AdapterManager.getAdapter,
        // so there's no need to try it again.
        if ((sourceObject instanceof PlatformObject) && !allowActivation) {
            return null;
        }

        String adapterId = adapter.getName();
        Object result = queryAdapterManager(sourceObject, adapterId, allowActivation);
        if (result != null) {
            // Sanity-check
            if (!adapter.isInstance(result)) {
                throw new AssertionFailedException("An adapter factory for " //$NON-NLS-1$
                    + sourceObject.getClass().getName() + " returned " + result.getClass().getName() //$NON-NLS-1$
                    + " that is not an instance of " + adapter.getName()); //$NON-NLS-1$
            }
            return adapter.cast(result);
        }

        return null;
    }

    public static <T> T adapt(Object sourceObject, Class<T> adapter) {
        return adapt(sourceObject, adapter, true);
    }

    public static Object queryAdapterManager(Object sourceObject, String adapterId, boolean allowActivation) {
        Object result;
        AdapterManager adapterManager = AdapterManager.getDefault();
        if (adapterManager == null) {
            return null;
        }
        if (allowActivation) {
            result = adapterManager.loadAdapter(sourceObject, adapterId);
        } else {
            result = adapterManager.getAdapter(sourceObject, adapterId);
        }
        return result;
    }

    public static byte[] getBytesFromUUID(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        return bb.array();
    }

    public static UUID getUUIDFromBytes(byte[] bytes) throws IllegalArgumentException {
        if (bytes.length < 16) {
            throw new IllegalArgumentException("UUID length must be at least 16 bytes (actual length = " + bytes.length + ")");
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return new UUID(byteBuffer.getLong(), byteBuffer.getLong());
    }

    public static UUID getMixedEndianUUIDFromBytes(byte[] bytes) {
        ByteBuffer source = ByteBuffer.wrap(bytes);
        ByteBuffer target = ByteBuffer.allocate(16).
            order(ByteOrder.LITTLE_ENDIAN).
            putInt(source.getInt()).
            putShort(source.getShort()).
            putShort(source.getShort()).
            order(ByteOrder.BIG_ENDIAN).
            putLong(source.getLong());
        target.rewind();
        return new UUID(target.getLong(), target.getLong());
    }

    /**
     * Validates the resource name, only if the application is running in desktop mode.
     *
     * @param name the resource name to validate
     * @throws DBException if the resource name is invalid
     */
    public static void validateResourceName(String name) throws DBException {
        if (!DBWorkbench.isDistributed() && !DBWorkbench.getPlatform().getApplication().isMultiuser()) {
            return;
        }
        validateResourceNameUnconditionally(name);
    }

    /**
     * Validates the resource name unconditionally.
     *
     * @param name resource name to validate
     * @throws DBException if resource name is invalid
     */
    public static void validateResourceNameUnconditionally(String name) throws DBException {
        if (name.startsWith(".")) {
            throw new DBException("Resource name '" + name + "' can't start with dot");
        }

        String forbiddenSymbols = name.replaceAll(RESOURCE_NAME_FORBIDDEN_SYMBOLS_REGEX, "");
        if (CommonUtils.isNotEmpty(forbiddenSymbols)) {
            String forbiddenExplain = forbiddenSymbols.chars()
                .mapToObj(c -> Character.toString((char) c))
                .collect(Collectors.joining(" "));
            throw new DBException("Resource name '" + name + "' contains illegal characters:  " + forbiddenExplain);
        }
    }

    /**
     * Normalizes line endings by converting Windows ({@code \\r\n}) and
     * macOS ({@code \r}) line endings to Unix ({@code \n}) line endings.
     *
     * @param text the text to normalize
     * @return the normalized text
     */
    @NotNull
    public static String normalizeLineEndings(@NotNull String text) {
        return text.replaceAll("(\r\n)|\r", "\n");
    }
}
