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

import io.vertx.ext.web.RoutingContext;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.org.sevn.jvert.VertxOutputStream;

public class HtmlCacheHandler implements io.vertx.core.Handler<RoutingContext> {

    private final String dirpathHtmlCache;
    private final String wpathDelim;
    private final String dirpath;
    
    public HtmlCacheHandler(String dirpathHtmlCache, String wpathDelim, String dirpath) {
        this.dirpath = dirpath;
        this.dirpathHtmlCache = dirpathHtmlCache;
        this.wpathDelim = wpathDelim;
    }
    
    @Override
    public void handle(RoutingContext ctx) {
        File dirpathHtmlCacheFile = new File(dirpathHtmlCache);;
        if (dirpathHtmlCacheFile.exists() && dirpathHtmlCacheFile.canWrite()) {
        } else {
            Logger.getLogger(HtmlCacheHandler.class.getName()).log(Level.SEVERE, "Can''t cache html into {0}", dirpathHtmlCacheFile.getAbsolutePath());
            dirpathHtmlCacheFile = null;
        }
        String path = ctx.request().path();
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HtmlCacheHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (path.endsWith(".html")) {
            path = path.substring(wpathDelim.length());
            File f = new File(dirpath, path);
            if (f.exists()) { //TODO cache refactor

                String contentType = null;
                try {
                    contentType = Files.probeContentType(Paths.get(f.getPath()));
                } catch (IOException ex) {
                    Logger.getLogger(HtmlCacheHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (contentType != null) {
                    ctx.response().putHeader("content-type", contentType);
                    VertxOutputStream vos = new VertxOutputStream(ctx.response());
                    try {
                        ctx.response().setChunked(true);
                        File cache = null;
                        if (dirpathHtmlCacheFile != null) {
                            cache = new File(dirpathHtmlCacheFile, path);
                        }
                        String fileCont;
                        if (cache != null && cache.exists()) {
                            fileCont = new String(Files.readAllBytes(cache.toPath()), "UTF-8");
                        } else {
                            fileCont = new String(Files.readAllBytes(f.toPath()), "UTF-8");
                            fileCont = fileCont.replace("<!--${__htmlCmtE__}", "");
                            fileCont = fileCont.replace("${__htmlCmtB__}-->", "<!--");
                            if (cache != null) {
                                File parFile = cache.getParentFile();
                                try {
                                    if (!parFile.exists()) {
                                        parFile.mkdirs();
                                    }
                                    Files.write(cache.toPath(), fileCont.getBytes("UTF-8"));
                                } catch (IOException ex1) {
                                    Logger.getLogger(HtmlCacheHandler.class.getName()).log(Level.SEVERE, "Can't cache html into " + cache.getAbsolutePath(), ex1);
                                }
                            }
                        }
                        vos.write(fileCont.getBytes("UTF-8"));
                        vos.close();
                    } catch (IOException ex) {
                        Logger.getLogger(HtmlCacheHandler.class.getName()).log(Level.SEVERE, null, ex);
                        ctx.fail(ex);
                    }

                    return;
                }

            }
        }
        ctx.next();
    }

}
