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

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;
import ru.org.sevn.common.data.SimpleSqliteObjectStore;
import ru.org.sevn.common.mime.Mime;
import ru.org.sevn.jvert.NReachableFSStaticHandlerImpl;
import ru.org.sevn.jvert.VertxOutputStream;

public class ShareUrlHandler implements io.vertx.core.Handler<RoutingContext> {

    //TODO log op https://www.mkyong.com/java/how-to-get-client-ip-address-in-java/
    
    private final JsonObject config;
    private final SimpleSqliteObjectStore ostore;
    
    public ShareUrlHandler(String wpathDelim, String dirpath, String dirpathGen, SimpleSqliteObjectStore os) {
        this(new JsonObject().put("wpathDelim", wpathDelim).put("dirpath", dirpath).put("dirpathGen", dirpathGen), os);
    }
    
    public ShareUrlHandler(JsonObject config, SimpleSqliteObjectStore os) {
        this.config = config;
        this.ostore = os;
    }
    
    protected String getWpathDelim() {
        return config.getString("wpathDelim");
    }
    
    protected String getDirpath() {
        return config.getString("dirpath");
    }
    
    protected String getDirpathGen() {
        return config.getString("dirpathGen");
    }
    
    private static String getDecoded(String s) {
        return getDecoded(s, s);
    }
    private static String getDecoded(String s, String def) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ShareHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return def;
    }
    @Override
    public void handle(RoutingContext ctx) {
        HttpServerRequest r = ctx.request();
        String hashAndPath = r.path();
        hashAndPath = hashAndPath.substring((getWpathDelim()+ShareHandler.PATH_REF).length());
        System.out.println(hashAndPath);
        String hash = hashAndPath;
        String path = "";
        int slash = hash.indexOf("/");
        if (slash > 0) {
            hash = hashAndPath.substring(0, slash);
            path = hashAndPath.substring(slash + 1);
        }
        
        hash = getDecoded(hash, hash);
        path = getDecoded(path, path);
        
        ShareHandler.Hash exO = null;
        try {
            Collection exObjs = ostore.getObjects(ShareHandler.Hash.class, new String[] {"WPATH", "HASHID" }, new Object[] { getWpathDelim(), hash});
            if (exObjs.size() > 1) {
                ctx.fail(403);
            } else if (exObjs.size() > 0) {
                exO = (ShareHandler.Hash)exObjs.stream().findFirst().get();
                if ( exO.getShareMode() == 1 ) {
                    // send file
                    //ctx.response().putHeader("content-type", "text/html").end("Dynamic page for reference to file " + path);
                    File f = new File(getDirpath(), getDecoded(exO.getPath()));
                    if (!f.exists()) {
                        f = new File(getDirpathGen(), getDecoded(exO.getPath())); 
                    }
                    sendFile(ctx, f);
                } else if (exO.getShareMode() == 2) {
                    File f = new File(getDirpath(), getDecoded(exO.getPath()));
                    if (path.length() == 0) {
                        if (!f.exists()) {
                            f = new File(getDirpathGen(), getDecoded(exO.getPath())); 
                        }
                        sendFile(ctx, f);
                    } else {
                        File dir = f.getParentFile();
                        File sndFile = new File(dir, path);
                        if (!sndFile.exists()) {
                            f = new File(getDirpathGen(), getDecoded(exO.getPath())); 
                            dir = f.getParentFile();
                            sndFile = new File(dir, path);
                        }
                        sendFile(ctx, sndFile);//TODO ../..
                    }
                    
                    //new NReachableFSStaticHandlerImpl().setAlwaysAsyncFS(true).setCachingEnabled(false).setDefaultContentEncoding("UTF-8").setAllowRootFileSystemAccess(true).setWebRoot(new File(dirpath).getAbsolutePath())
                    //ctx.response().putHeader("content-type", "text/html").end("Dynamic page for reference to content " + path);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(ShareUrlHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        ctx.fail(404);
    }
    public static void sendFile(RoutingContext ctx, File f) {
        if (f.exists()) {
            String contentType = null;
            try {
                contentType = Files.probeContentType(Paths.get(f.getPath()));
            } catch (IOException ex) {
                Logger.getLogger(HtmlCacheHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (contentType == null) {
                //contentType = Mime.getMimeTypeFile(f.getName());
                contentType = io.vertx.core.http.impl.MimeMapping.getMimeTypeForFilename(f.getName());
            }
            if (contentType != null) {
                ctx.response().putHeader("content-type", contentType);
            }
            ctx.response().sendFile(f.getPath(), res2 -> {
                if (res2.failed()) {
                  ctx.fail(res2.cause());
                }
              });
            /*
            VertxOutputStream vos = new VertxOutputStream(ctx.response());
            try {
                ctx.response().setChunked(true);
                vos.write(Files.readAllBytes(f.toPath()));
                vos.close();
            } catch (IOException ex) {
                Logger.getLogger(HtmlCacheHandler.class.getName()).log(Level.SEVERE, null, ex);
                ctx.fail(ex);
            }
                    */
        }
    }
}
