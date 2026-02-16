/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.utils;

import org.jkiss.dbeaver.utils.ContentUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Tests for {@link ContentUtils} covering text detection, ASCII checking,
 * file operations, content length calculation, and close safety.
 */
public class ContentUtilsTest {

    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("contentutils-test");
    }

    @After
    public void tearDown() {
        ContentUtils.deleteFileRecursive(tempDir);
    }

    // ========== isTextMime ==========

    @Test
    public void testIsTextMimeWithTextTypes() {
        assertTrue(ContentUtils.isTextMime("text/plain"));
        assertTrue(ContentUtils.isTextMime("text/html"));
        assertTrue(ContentUtils.isTextMime("text/xml"));
        assertTrue(ContentUtils.isTextMime("TEXT/PLAIN"));
    }

    @Test
    public void testIsTextMimeWithNonTextTypes() {
        assertFalse(ContentUtils.isTextMime("application/json"));
        assertFalse(ContentUtils.isTextMime("image/png"));
        assertFalse(ContentUtils.isTextMime("application/octet-stream"));
    }

    @Test
    public void testIsTextMimeWithNull() {
        assertFalse(ContentUtils.isTextMime(null));
    }

    @Test
    public void testIsTextMimeWithEmptyString() {
        assertFalse(ContentUtils.isTextMime(""));
    }

    // ========== isTextValue ==========

    @Test
    public void testIsTextValueWithNull() {
        assertFalse(ContentUtils.isTextValue(null));
    }

    @Test
    public void testIsTextValueWithString() {
        assertTrue(ContentUtils.isTextValue("Hello World"));
        assertTrue(ContentUtils.isTextValue(""));
    }

    @Test
    public void testIsTextValueWithStringBuilder() {
        assertTrue(ContentUtils.isTextValue(new StringBuilder("test")));
    }

    @Test
    public void testIsTextValueWithAsciiBytes() {
        assertTrue(ContentUtils.isTextValue(new byte[]{'H', 'e', 'l', 'l', 'o'}));
    }

    // ========== isAsciiText ==========

    @Test
    public void testIsAsciiTextWithValidAscii() {
        assertTrue(ContentUtils.isAsciiText("Hello World 123!@#".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    public void testIsAsciiTextWithNull() {
        assertFalse(ContentUtils.isAsciiText(null));
    }

    @Test
    public void testIsAsciiTextWithEmptyArray() {
        assertFalse(ContentUtils.isAsciiText(new byte[0]));
    }

    @Test
    public void testIsAsciiTextWithControlCharacters() {
        assertFalse(ContentUtils.isAsciiText(new byte[]{0x01, 0x02}));
    }

    @Test
    public void testIsAsciiTextWithHighBytes() {
        assertFalse(ContentUtils.isAsciiText(new byte[]{(byte) 0x80, (byte) 0xFF}));
    }

    // ========== calculateContentLength ==========

    @Test
    public void testCalculateContentLengthFromReader() throws IOException {
        String testContent = "Hello, DBeaver!";
        Reader reader = new StringReader(testContent);
        long length = ContentUtils.calculateContentLength(reader);
        assertEquals(testContent.length(), length);
    }

    @Test
    public void testCalculateContentLengthEmptyReader() throws IOException {
        Reader reader = new StringReader("");
        long length = ContentUtils.calculateContentLength(reader);
        assertEquals(0, length);
    }

    @Test
    public void testCalculateContentLengthLargeContent() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50000; i++) {
            sb.append('A');
        }
        Reader reader = new StringReader(sb.toString());
        long length = ContentUtils.calculateContentLength(reader);
        assertEquals(50000, length);
    }

    // ========== makeTempFile ==========

    @Test
    public void testMakeTempFile() throws IOException {
        Path tempFile = ContentUtils.makeTempFile(tempDir, "test", "txt");
        assertNotNull(tempFile);
        assertTrue(Files.exists(tempFile));
        assertTrue(tempFile.getFileName().toString().startsWith("test-"));
        assertTrue(tempFile.getFileName().toString().endsWith(".txt"));
    }

    @Test
    public void testMakeTempFileWithEmptyNameUsesDefault() throws IOException {
        Path tempFile = ContentUtils.makeTempFile(tempDir, "", "dat");
        assertNotNull(tempFile);
        assertTrue(Files.exists(tempFile));
        assertTrue(tempFile.getFileName().toString().startsWith("tmp-"));
    }

    // ========== deleteFileRecursive ==========

    @Test
    public void testDeleteFileRecursiveSingleFile() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.createFile(file);
        assertTrue(Files.exists(file));
        assertTrue(ContentUtils.deleteFileRecursive(file));
        assertFalse(Files.exists(file));
    }

    @Test
    public void testDeleteFileRecursiveDirectory() throws IOException {
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectory(subDir);
        Files.createFile(subDir.resolve("file1.txt"));
        Files.createFile(subDir.resolve("file2.txt"));
        Path nested = subDir.resolve("nested");
        Files.createDirectory(nested);
        Files.createFile(nested.resolve("deep.txt"));

        assertTrue(ContentUtils.deleteFileRecursive(subDir));
        assertFalse(Files.exists(subDir));
    }

    @Test
    public void testDeleteFileRecursiveNonExistent() {
        Path nonExistent = tempDir.resolve("does-not-exist");
        // Should not throw, returns true since deleteIfExists returns true for non-existent
        assertTrue(ContentUtils.deleteFileRecursive(nonExistent));
    }

    // ========== close ==========

    @Test
    public void testCloseWithNull() {
        // Should not throw NPE
        ContentUtils.close(null);
    }

    @Test
    public void testCloseWithValidCloseable() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[]{1, 2, 3});
        ContentUtils.close(bais);
        // Should not throw
    }

    @Test
    public void testCloseWithFailingCloseable() {
        Closeable failingCloseable = () -> {
            throw new IOException("Test exception");
        };
        // Should not throw, just log warning
        ContentUtils.close(failingCloseable);
    }

    // ========== readToString ==========

    @Test
    public void testReadToString() throws IOException {
        String expected = "Hello DBeaver UTF-8 content";
        byte[] bytes = expected.getBytes(StandardCharsets.UTF_8);
        InputStream is = new ByteArrayInputStream(bytes);
        String result = ContentUtils.readToString(is, StandardCharsets.UTF_8);
        assertEquals(expected, result);
    }

    @Test
    public void testReadToStringEmpty() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        String result = ContentUtils.readToString(is, StandardCharsets.UTF_8);
        assertEquals("", result);
    }
}
