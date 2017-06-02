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
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;
import ru.org.sevn.common.data.SimpleSqliteObjectStore;

public class ShareUrlHandler implements io.vertx.core.Handler<RoutingContext> {

    private final JsonObject config;
    private final SimpleSqliteObjectStore ostore;
    
    public ShareUrlHandler(String wpathDelim, String dirpath, SimpleSqliteObjectStore os) {
        this(new JsonObject().put("wpathDelim", wpathDelim).put("dirpath", dirpath), os);
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
        
        try {
            hash = java.net.URLDecoder.decode(hash, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ShareHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ShareHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        ShareHandler.Hash exO = null;
        try {
            Collection exObjs = ostore.getObjects(new String[] {"WPATH", "HASHID" }, new Object[] { getWpathDelim(), hash});
            if (exObjs.size() > 1) {
                ctx.fail(403);
            } else if (exObjs.size() > 0) {
                exO = (ShareHandler.Hash)exObjs.stream().findFirst().get();
                if ( exO.getShareMode() == 1 ) {
                    // send file
                    ctx.response().putHeader("content-type", "text/html").end("Dynamic page for reference to file " + path);
                    
                } else if (exO.getShareMode() == 2) {
                    //new NReachableFSStaticHandlerImpl().setAlwaysAsyncFS(true).setCachingEnabled(false).setDefaultContentEncoding("UTF-8").setAllowRootFileSystemAccess(true).setWebRoot(new File(dirpath).getAbsolutePath())
                    ctx.response().putHeader("content-type", "text/html").end("Dynamic page for reference to content " + path);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(ShareUrlHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        ctx.fail(404);
    }
    
}
