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

import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

//TODO optimize it
//TODO zip
public class LogHandler implements io.vertx.core.Handler<RoutingContext> {

    private final File dir = new File("log");
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private final SimpleDateFormat sdfYMD = new SimpleDateFormat("yyyyMMdd");
    private final SimpleDateFormat sdfYM = new SimpleDateFormat("yyyyMM");
    private final SimpleDateFormat sdfY = new SimpleDateFormat("yyyy");
    private static final long MAX_LEN = 1024 * 1024 * 2;
    
    private File lastLog;
    private int logidx;
    private final String logPrefix;
    
    public LogHandler() {
        this("");
    }
    
    public LogHandler(String prefix) {
        this.logPrefix = prefix;
    }
    
    @Override
    public void handle(RoutingContext ctx) {
        try {
            JsonObject obj = new JsonObject();
            Date now = new Date();
            String y = sdfY.format(now);
            String ym = sdfYM.format(now);
            String ymd = sdfYMD.format(now);
            File ldir = new File(new File(new File(dir, y), ym), ymd);
            ldir.mkdirs();
            if (lastLog != null) {
                if (lastLog.getParentFile().getAbsolutePath().equals(ldir.getAbsolutePath())) {
                    if (lastLog.length() >= MAX_LEN) {
                        lastLog = null;
                    }
                } else {
                    lastLog = null;
                    logidx = 0;
                }
            }
            while (lastLog == null) {
                logidx++;
                File f = new File(ldir, logPrefix + logidx + ".json");
                if (f.exists()) {
                    if (f.length() < MAX_LEN) {
                        lastLog = f;
                    }
                } else {
                    lastLog = f;
                }
            }
            
            obj.put("date", now.getTime());
            obj.put("dateStr", sdf.format(now));
            
            Session session = ctx.session();
            if (session != null) {
                obj.put("session", session.id());
            }
            
            User user = ctx.user();
            if (user != null) {
                if (user.principal().containsKey("id")) {
                    obj.put("userid", user.principal().getString("id"));
                }  else {
                    obj.put("user", user.principal());
                }
            }

            obj.put("url", ctx.request().absoluteURI());
            SocketAddress addr = ctx.request().remoteAddress();
            if (addr != null) {
                obj.put("host", addr.host());
                obj.put("port", addr.port());
            }
            String outStr = obj.encodePrettily() + ",\n";
            java.nio.file.Files.write(lastLog.toPath(), outStr.getBytes("UTF-8"), 
                    java.nio.file.StandardOpenOption.CREATE, 
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ctx.next();
    }
    
}
