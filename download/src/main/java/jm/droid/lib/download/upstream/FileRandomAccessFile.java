/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jm.droid.lib.download.upstream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * The FileOutputStream implemented using {@link RandomAccessFile}.
 */

public class FileRandomAccessFile implements FileOutputStream {
    private final BufferedOutputStream out;
    private final FileDescriptor fd;
    private final RandomAccessFile randomAccess;

    public FileRandomAccessFile(File file) throws IOException {
        randomAccess = new RandomAccessFile(file, "rw");
        fd = randomAccess.getFD();
        out = new BufferedOutputStream(new java.io.FileOutputStream(randomAccess.getFD()));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flushAndSync() throws IOException {
        out.flush();
        fd.sync();
    }

    @Override
    public void close() throws IOException {
        out.close();
        randomAccess.close();
    }

    @Override
    public void seek(long offset) throws IOException {
        randomAccess.seek(offset);
    }

    @Override
    public void setLength(long totalBytes) throws IOException {
        randomAccess.setLength(totalBytes);
    }
}
