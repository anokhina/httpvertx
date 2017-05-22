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
package ru.org.sevn.jvert.wwwgen;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import ru.org.sevn.jvert.VertxOutputStream;

public class ZipHandler implements io.vertx.core.Handler<RoutingContext> {

    private final JsonObject config;
    
    public ZipHandler(String wpathDelim, String dirpath) {
        this(new JsonObject().put("wpathDelim", wpathDelim).put("dirpath", wpathDelim));
    }
    
    public ZipHandler(JsonObject config) {
        this.config = config;
    }
    
    protected String getWpathDelim() {
        return config.getString("wpathDelim");
    }
    
    protected String getDirpath() {
        return config.getString("dirpath");
    }
    
    @Override
    public void handle(RoutingContext ctx) {
        HttpServerRequest r = ctx.request();
        String zipMode = r.getParam("zip");
        if (zipMode != null) {
            String path = r.path();
            path = path.substring(getWpathDelim().length());
            try {
                path = java.net.URLDecoder.decode(path, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(ZipHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            File dir = new File(getDirpath(), path);
            if (dir.exists()) {
                if (!dir.isDirectory()) {
                    dir = dir.getParentFile();
                }
                if (dir != null) {
                    try {
                        if ("1".equals(zipMode)) {
                            ctx.fail(HttpResponseStatus.FORBIDDEN.code());
                        } else {
                            HttpServerResponse response = ctx.response();
                            String name = dir.getName();
                            String fileName = name + ".zip";
                            String encodeFileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
                            response.setChunked(true)
                                    .putHeader("content-type", "application/zip")
                                    .putHeader("Content-Disposition", "filename=\"" + encodeFileName + "\"");
                            //                                                    .putHeader("Content-Disposition", "form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"");

                            ZipOutputStream zos = new ZipOutputStream(new VertxOutputStream(response));
                            zos.setLevel(Deflater.DEFLATED);
                            for (File fl : dir.listFiles()) {
                                String nm = fl.getName();
                                if (fl.isDirectory()) {
                                } else if (nm.startsWith("_") || nm.startsWith(".")) {
                                } else {
                                    ZipEntry ze = new ZipEntry(nm);
                                    zos.putNextEntry(ze);
                                    zos.write(Files.readAllBytes(fl.toPath()));
                                    zos.closeEntry();
                                }
                            }
                            zos.close();
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(ZipHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                ctx.fail(HttpResponseStatus.NOT_FOUND.code());
            }
        } else {
            ctx.next();
        }
    }

}
