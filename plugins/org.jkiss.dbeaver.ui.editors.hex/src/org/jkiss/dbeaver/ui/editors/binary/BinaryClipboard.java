/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.binary;

import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.StandardConstants;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * A clipboard for binary content. Data up to 4Mbytes is made available as text as well
 *
 * @author Jordi
 */
public class BinaryClipboard {

    private static final Log log = Log.getLog(HexEditControl.class);

    static class FileByteArrayTransfer extends ByteArrayTransfer {

        static final String FORMAT_NAME = "BinaryFileByteArrayTypeName";
        static final int FORMAT_ID = registerType(FORMAT_NAME);

        static final FileByteArrayTransfer instance = new FileByteArrayTransfer();

        private FileByteArrayTransfer()
        {
        }

        static FileByteArrayTransfer getInstance()
        {
            return instance;
        }

        @Override
        public void javaToNative(Object object, TransferData transferData)
        {
            if (object == null || !(object instanceof File)) return;

            if (isSupportedType(transferData)) {
                File myType = (File) object;
                try {
                    // write data to a byte array and then ask super to convert to pMedium
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    DataOutputStream writeOut = new DataOutputStream(out);
                    byte[] buffer = myType.getAbsolutePath().getBytes(Charset.defaultCharset());
                    writeOut.writeInt(buffer.length);
                    writeOut.write(buffer);
                    buffer = out.toByteArray();
                    writeOut.close();

                    super.javaToNative(buffer, transferData);

                }
                catch (IOException e) {
                    log.warn(e);
                }  // copy nothing then
            }
        }

        @Override
        public Object nativeToJava(TransferData transferData)
        {
            if (!isSupportedType(transferData)) return null;

            byte[] buffer = (byte[]) super.nativeToJava(transferData);
            if (buffer == null) {
                return null;
            }

            DataInputStream readIn = new DataInputStream(new ByteArrayInputStream(buffer));
            try {
                int size = readIn.readInt();
                if (size <= 0) {
                    return null;
                }
                byte[] nameBytes = new byte[size];
                if (readIn.read(nameBytes) < size){
                    return null;
                }
                return new File(new String(nameBytes));
            }
            catch (IOException ex) {
                log.warn(ex);
                return null;
            }
        }

        @Override
        protected String[] getTypeNames()
        {
            return new String[]{FORMAT_NAME};
        }

        @Override
        protected int[] getTypeIds()
        {
            return new int[]{FORMAT_ID};
        }
    }


    static class MemoryByteArrayTransfer extends ByteArrayTransfer {
        static final String FORMAT_NAME = "BinaryMemoryByteArrayTypeName";
        static final int FORMAT_ID = registerType(FORMAT_NAME);

        static final MemoryByteArrayTransfer instance = new MemoryByteArrayTransfer();

        private MemoryByteArrayTransfer()
        {
        }

        static MemoryByteArrayTransfer getInstance()
        {
            return instance;
        }

        @Override
        public void javaToNative(Object object, TransferData transferData)
        {
            if (object == null || !(object instanceof byte[])) return;

            if (isSupportedType(transferData)) {
                byte[] buffer = (byte[]) object;
                super.javaToNative(buffer, transferData);
            }
        }

        @Override
        public Object nativeToJava(TransferData transferData)
        {
            Object result = null;
            if (isSupportedType(transferData)) {
                result = super.nativeToJava(transferData);
            }

            return result;
        }

        @Override
        protected String[] getTypeNames()
        {
            return new String[]{FORMAT_NAME};
        }

        @Override
        protected int[] getTypeIds()
        {
            return new int[]{FORMAT_ID};
        }
    }


    private static final File clipboardDir = new File(System.getProperty(StandardConstants.ENV_TMP_DIR, "."));
    private static final File clipboardFile = new File(clipboardDir, "dbeaver-binary-clipboard.tmp");
    private static final long maxClipboardDataInMemory = 4 * 1024 * 1024;  // 4 Megs for byte[], 4 Megs for text

    private final Map<File, Integer> filesReferencesCounter = new HashMap<>();
    private final Clipboard clipboard;

    /**
     * Init system resources for the clipboard
     */
    public BinaryClipboard(Display aDisplay)
    {
        clipboard = new Clipboard(aDisplay);
    }


    /**
     * Dispose system clipboard and file resources
     *
     * @see Clipboard#dispose()
     */
    public void dispose()
        throws IOException
    {
        if (!clipboard.isDisposed()) {
            File lastPaste = (File) clipboard.getContents(FileByteArrayTransfer.getInstance());
            clipboard.dispose();

            if (!clipboardFile.equals(lastPaste))  // null
                emptyClipboardFile();
        }

        for (File aFile : filesReferencesCounter.keySet()) {
            int count = filesReferencesCounter.get(aFile);
            File lock = getLockFromFile(aFile);
            if (updateLock(lock, -count)) {  // lock deleted
                if (!aFile.delete()) {
                    aFile.deleteOnExit();
                }
            }
        }
    }


    void emptyClipboardFile()
    {
        if (clipboardFile.canWrite() && clipboardFile.length() > 0L) {
            try {
                RandomAccessFile file = new RandomAccessFile(clipboardFile, "rw");
                try {
                    file.setLength(0L);
                } finally {
                    ContentUtils.close(file);
                }
            }
            catch (IOException e) {
                log.warn(e);
            }  // ok, leave it alone
        }
    }


    /**
     * Dispose system clipboard resources
     *
     * @see Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable
    {
        dispose();
        super.finalize();
    }


    /**
     * Paste the clipboard contents into a BinaryContent
     */
    public long getContents(BinaryContent content, long start, boolean insert)
    {
        long total = tryGettingFiles(content, start, insert);
        if (total >= 0L) return total;

        total = tryGettingMemoryByteArray(content, start, insert);
        if (total >= 0L) return total;

        total = tryGettingFileByteArray(content, start, insert);
        if (total >= 0L) return total;

        return 0L;
    }


    static File getLockFromFile(File lastPaste)
    {
        String name = lastPaste.getAbsolutePath();
        return new File(name.substring(0, name.length() - 3) + "lock");
    }


    /**
     * Tells whether there is valid data in the clipboard
     *
     * @return true: data is available
     */
    public boolean hasContents()
    {
        TransferData[] available = clipboard.getAvailableTypes();
        for (int i = 0; i < available.length; ++i) {
            if (MemoryByteArrayTransfer.getInstance().isSupportedType(available[i]) ||
                TextTransfer.getInstance().isSupportedType(available[i]) ||
                FileByteArrayTransfer.getInstance().isSupportedType(available[i]) ||
                FileTransfer.getInstance().isSupportedType(available[i]))
                return true;
        }

        return false;
    }


    /**
     * Set the clipboard contents with a BinaryContent
     */
    public void setContents(BinaryContent content, long start, long length)
    {
        if (length < 1L) return;

        Object[] data;
        Transfer[] transfers;
        try {
            if (length <= maxClipboardDataInMemory) {
                byte[] byteArrayData = new byte[(int) length];
                content.get(ByteBuffer.wrap(byteArrayData), start);
                String textData = new String(byteArrayData);
                transfers =
                    new Transfer[]{MemoryByteArrayTransfer.getInstance(), TextTransfer.getInstance()};
                data = new Object[]{byteArrayData, textData};
            } else {
                content.get(clipboardFile, start, length);
                transfers = new Transfer[]{FileByteArrayTransfer.getInstance()};
                data = new Object[]{clipboardFile};
            }
        }
        catch (IOException e) {
            clipboard.setContents(new Object[]{new byte[1]},
                                    new Transfer[]{MemoryByteArrayTransfer.getInstance()});
            clipboard.clearContents();
            emptyClipboardFile();
            return;  // copy nothing then
        }
        clipboard.setContents(data, transfers);
    }


    /*
    * The file is being reference counted. It will be deleted as soon as no binary process is
    * referencing it anymore.
    */
    long tryGettingFileByteArray(BinaryContent content, long start, boolean insert)
    {
        File lastPaste = (File) clipboard.getContents(FileByteArrayTransfer.getInstance());
        if (lastPaste == null) return -1L;
        long total = lastPaste.length();
        if (!insert && total > content.length() - start) return 0L;

        File lock;
        if (clipboardFile.equals(lastPaste)) {
            for (int i = 0; ; ++i) {
                StringBuilder name = new StringBuilder("binaryPasted").append(i);
                lastPaste = new File(clipboardDir, name.toString() + ".tmp");
                lock = new File(clipboardDir, name.append(".lock").toString());
                if (!lock.exists())
                    if (!lastPaste.exists() || lastPaste.delete())
                        break;
            }
            if (lastPaste.exists() || lock.exists()) {
                return 0L;
            }
            if (!clipboardFile.renameTo(lastPaste)) {
                log.warn("Can't rename clipboard temp file");
            }
            clipboard.setContents(
                new Object[]{lastPaste},
                new Transfer[]{FileByteArrayTransfer.getInstance()});
        } else {
            lock = getLockFromFile(lastPaste);
        }
        try {
            if (insert)
                content.insert(lastPaste, start);
            else
                content.overwrite(lastPaste, start);
        }
        catch (IOException e) {
            total = 0L;
        }
        if (total > 0L) {
            try {
                updateLock(lock, 1);
            }
            catch (IOException e) {
                filesReferencesCounter.remove(lastPaste);
                return total;
            }
            Integer value = filesReferencesCounter.put(lastPaste, 1);
            if (value != null)
                filesReferencesCounter.put(lastPaste, value + 1);
        }

        return total;
    }


    long tryGettingFiles(BinaryContent content, long start, boolean insert)
    {
        String[] files = (String[]) clipboard.getContents(FileTransfer.getInstance());
        if (files == null)
            return -1L;

        long total = 0L;
        if (!insert) {
            for (int i = 0; i < files.length; ++i) {
                File file = new File(files[i]);
                total += file.length();
                if (total > content.length() - start) {
                    return 0L;  // would overflow
                }
            }
        }
        total = 0L;
        for (int i = files.length - 1; i >= 0; --i) {  // for some reason they are given in reverse order
            File file = new File(files[i]);
            try {
                file = file.getCanonicalFile();
            }
            catch (IOException e) {
                log.warn(e);
            }  // use non-canonical one then
            boolean success = true;
            try {
                if (insert)
                    content.insert(file, start);
                else
                    content.overwrite(file, start);
            }
            catch (IOException e) {
                success = false;
            }
            if (success) {
                start += file.length();
                total += file.length();
            }
        }

        return total;
    }


    long tryGettingMemoryByteArray(BinaryContent content, long start, boolean insert)
    {
        byte[] byteArray = (byte[]) clipboard.getContents(MemoryByteArrayTransfer.getInstance());
        if (byteArray == null) {
            String text = (String) clipboard.getContents(TextTransfer.getInstance());
            if (text != null) {
                byteArray = text.getBytes(Charset.defaultCharset());
            }
        }
        if (byteArray == null)
            return -1L;

        long total = byteArray.length;
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        if (insert) {
            content.insert(buffer, start);
        } else if (total <= content.length() - start) {
            content.overwrite(buffer, start);
        } else {
            total = 0L;
        }

        return total;
    }


    boolean updateLock(File lock, int references)
        throws IOException
    {
        RandomAccessFile file = new RandomAccessFile(lock, "rw");
        try {
            if (file.length() >= 4)
                references += file.readInt();
            if (references > 0) {
                file.seek(0);
                file.writeInt(references);
            }
        } finally {
            ContentUtils.close(file);
        }
        if (references < 1) {
            if (!lock.delete()) {
                log.warn("Cannot delete lock file '" + lock.getAbsolutePath() + "'");
            }

            return true;
        }

        return false;
    }
}
