/*
 * Copyright 2016 ArcBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gwtplatform.processors.tools.outputter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.gwtplatform.processors.tools.domain.Type;
import com.gwtplatform.processors.tools.logger.Logger;

/**
 * A FileObject that wraps both the .java file that will be compiled to a .class file and the .java file that will be
 * sourced by GWT. The first one is only necessary for development purpose and can only be enabled through compiler
 * properties.
 */
class SourcedFileObject implements FileObject {
    private static class MultiFileWriter extends Writer {
        private final Writer prodWriter;
        private final Writer devWriter;

        MultiFileWriter(
                Writer prodWriter,
                Writer devWriter) {
            this.prodWriter = prodWriter;
            this.devWriter = devWriter;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            prodWriter.write(cbuf, off, len);
            devWriter.write(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            prodWriter.flush();
            devWriter.close();
        }

        @Override
        public void close() throws IOException {
            prodWriter.close();
            devWriter.close();
        }
    }

    private static class MultiFileOutputStream extends OutputStream {
        private final OutputStream prodOutputStream;
        private final OutputStream devOutputStream;

        MultiFileOutputStream(
                OutputStream prodOutputStream,
                OutputStream devOutputStream) {
            this.prodOutputStream = prodOutputStream;
            this.devOutputStream = devOutputStream;
        }

        @Override
        public void write(int b) throws IOException {
            prodOutputStream.write(b);
            devOutputStream.write(b);
        }
    }

    private final boolean debug;
    private final FileObject prodFileObject;
    private final FileObject devFileObject;

    SourcedFileObject(
            Logger logger,
            Filer filer,
            Type type) throws IOException {
        debug = logger.isDebugEnabled();
        prodFileObject = filer.createResource(
                StandardLocation.CLASS_OUTPUT,
                type.getPackageName(),
                type.getSimpleName() + ".java");
        devFileObject = debug ? filer.createSourceFile(type.getQualifiedName()) : null;
    }

    @Override
    public URI toUri() {
        return prodFileObject.toUri();
    }

    @Override
    public String getName() {
        return prodFileObject.getName();
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return prodFileObject.openInputStream();
    }

    @SuppressWarnings("resource")
    @Override
    public OutputStream openOutputStream() throws IOException {
        OutputStream prodOutputStream = prodFileObject.openOutputStream();

        return debug ? new MultiFileOutputStream(prodOutputStream, devFileObject.openOutputStream()) : prodOutputStream;
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return prodFileObject.openReader(ignoreEncodingErrors);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return prodFileObject.getCharContent(ignoreEncodingErrors);
    }

    @SuppressWarnings("resource")
    @Override
    public Writer openWriter() throws IOException {
        Writer prodWriter = prodFileObject.openWriter();

        return debug ? new MultiFileWriter(prodWriter, devFileObject.openWriter()) : prodWriter;
    }

    @Override
    public long getLastModified() {
        return prodFileObject.getLastModified();
    }

    @Override
    public boolean delete() {
        return prodFileObject.delete() && devFileObject.delete();
    }
}
