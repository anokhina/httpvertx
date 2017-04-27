/*
 * Copyright 2017 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.org.sevn.jvert;

import io.vertx.core.buffer.Buffer;
import java.io.OutputStream;
import io.vertx.core.streams.WriteStream;
import java.io.IOException;

public class VertxOutputStream extends OutputStream {
    protected final WriteStream<Buffer> response;
    private final byte[] buffer = new byte[8192];
    private int counter = 0;
    
    public VertxOutputStream(WriteStream<Buffer> vertxStream) {
        response = vertxStream;
    }
    
    @Override
    public synchronized void write(final int b) throws IOException {
        buffer[counter++] = (byte) b;

        if (counter >= buffer.length) {
            flush();
        }
    }
    
    @Override
    public void flush() throws IOException {
        super.flush();

        if (counter > 0) {
            byte[] remaining = buffer;

            if (counter < buffer.length) {
                remaining = new byte[counter];

                System.arraycopy(buffer, 0, remaining, 0, counter);
            }

            response.write(Buffer.buffer(remaining));
            counter = 0;
        }
    }
    
    @Override
    public void close() throws IOException {
        flush();

        super.close();
        response.end();
    }
    
}
