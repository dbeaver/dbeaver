/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.utils;

import java.io.*;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Some IO helper functions
 */
public final class IOUtils {

	public static final int		DEFAULT_BUFFER_SIZE		= 16384;

    public static void close(Closeable closeable)
    {
        try {
            closeable.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void fastCopy(final InputStream src, final OutputStream dest) throws IOException {
        fastCopy(src, dest, DEFAULT_BUFFER_SIZE);
    }

    public static void fastCopy(final InputStream src, final OutputStream dest, int bufferSize) throws IOException {
        final ReadableByteChannel inputChannel = Channels.newChannel(src);
        final WritableByteChannel outputChannel = Channels.newChannel(dest);
        fastCopy(inputChannel, outputChannel, bufferSize);
    }

    public static void fastCopy(final ReadableByteChannel src, final WritableByteChannel dest, int bufferSize) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

        while(src.read(buffer) != -1) {
            buffer.flip();
            dest.write(buffer);
            buffer.compact();
        }

        buffer.flip();

        while(buffer.hasRemaining()) {
            dest.write(buffer);
        }
    }

	public static void copyStream(
		java.io.InputStream inputStream,
		java.io.OutputStream outputStream)
		throws IOException
	{
		copyStream(inputStream, outputStream, DEFAULT_BUFFER_SIZE);
	}
	/**
		Read entire input stream and writes all data to output stream
		then closes input and flushed output
	*/
	public static void copyStream(
		java.io.InputStream inputStream,
		java.io.OutputStream outputStream,
		int bufferSize)
		throws IOException
	{
        try {
            byte[] writeBuffer = new byte[bufferSize];
            for (int br = inputStream.read(writeBuffer); br != -1; br = inputStream.read(writeBuffer)) {
                outputStream.write(writeBuffer, 0, br);
            }
            outputStream.flush();
        }
        finally {
    		// Close input stream
            inputStream.close();
        }
	}

	/**
		Read entire input stream portion and writes it data to output stream
	*/
	public static void copyStreamPortion(
		java.io.InputStream inputStream,
		java.io.OutputStream outputStream,
		int portionSize,
		int bufferSize)
		throws IOException
	{
		if (bufferSize > portionSize) {
			bufferSize = portionSize;
		}
		byte[] writeBuffer = new byte[bufferSize];
		int totalRead = 0;
		while (totalRead < portionSize) {
			int bytesToRead = bufferSize;
			if (bytesToRead > portionSize - totalRead) {
				bytesToRead = portionSize - totalRead;
			}
			int bytesRead = inputStream.read(writeBuffer, 0, bytesToRead);
			outputStream.write(writeBuffer, 0, bytesRead);
			totalRead += bytesRead;
		}

		// Close input stream
		outputStream.flush();
	}

	public static String toString(File file) throws IOException {
		try (Reader reader = new FileReader(file)) {
			StringWriter writer = new StringWriter();
			copyText(reader, writer, DEFAULT_BUFFER_SIZE);
			return writer.toString();
		}
	}

	/**
		Read entire reader content and writes it to writer
		then closes reader and flushed output.
	*/
	public static void copyText(
		java.io.Reader reader,
		java.io.Writer writer,
		int bufferSize)
		throws IOException
	{
		char[] writeBuffer = new char[bufferSize];
		for (int br = reader.read(writeBuffer); br != -1; br = reader.read(writeBuffer)) {
			writer.write(writeBuffer, 0, br);
		}
		writer.flush();
	}

	public static void copyText(
		java.io.Reader reader,
		java.io.Writer writer)
		throws IOException
	{
		copyText(reader, writer, DEFAULT_BUFFER_SIZE);
	}

	public static int readStreamToBuffer(
		java.io.InputStream inputStream,
		byte[] buffer)
		throws IOException
	{
		int totalRead = 0;
		while (totalRead != buffer.length) {
			int br = inputStream.read(buffer, totalRead, buffer.length - totalRead);
			if (br == -1) {
				break;
			}
			totalRead += br;
		}
		return totalRead;
	}

	public static String readLine(java.io.InputStream input)
		throws IOException
	{
		StringBuilder linebuf = new StringBuilder();
		for (int b = input.read(); b != '\n'; b = input.read()) {
			if (b == -1) {
				if (linebuf.length() == 0) {
					return null;
				} else {
					break;
				}
			}
			if (b != '\r') {
				linebuf.append((char)b);
			}
		}
		return linebuf.toString();
	}

	public static String readFullLine(java.io.InputStream input)
		throws IOException
	{
		StringBuilder linebuf = new StringBuilder();
		for (int b = input.read(); ; b = input.read()) {
			if (b == -1) {
				if (linebuf.length() == 0) {
					return null;
				} else {
					break;
				}
			}
			linebuf.append((char)b);
			if (b == '\n') {
				break;
			}
		}
		return linebuf.toString();
	}

	public static int findFreePort(int minPort, int maxPort) {
        int portRange = Math.abs(maxPort - minPort);
        while (true) {
            int portNum = minPort + SecurityUtils.getRandom().nextInt(portRange);
            try {
                ServerSocket socket = new ServerSocket(portNum);
                try {
                    socket.close();
                } catch (IOException e) {
                    // just skip
                }
                return portNum;
            } catch (IOException e) {
                // Port is busy
            }
        }
    }
}
