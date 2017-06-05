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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;
import ru.org.sevn.common.data.SimpleSqliteObjectStore;
import ru.org.sevn.common.data.DBProperty;
import ru.org.sevn.common.data.DBTableProperty;
import ru.org.sevn.jsecure.PassAuth;
import ru.org.sevn.jvert.ExtraUser;

public class ShareHandler implements io.vertx.core.Handler<RoutingContext> {

    //private ChatHandler.MessageStore messageStore = new ChatHandler.SQLiteMessageStore("vertShare.db");
    
    public static class HashMapper implements SimpleSqliteObjectStore.ObjectMapper<Hash> {
        @Override
        public void setStatement(Hash o, String colName, int parameterIndex, PreparedStatement pstmt) throws SQLException {
            switch(colName) {
                case "IS_ON":
                    pstmt.setInt(parameterIndex, o.isIson() ? 1 : 0);
                    break;
                case "CREATE_DATE":
                    pstmt.setDate(parameterIndex, new java.sql.Date(o.getCreateDate().getTime()));
                    break;
                case "CPATH":
                    pstmt.setString(parameterIndex, o.getPath());
                    break;
                case "USERID":
                    pstmt.setString(parameterIndex, o.getUserid());
                    break;
                case "WPATH":
                    pstmt.setString(parameterIndex, o.getWpath());
                    break;
                case "HASHID":
                    pstmt.setString(parameterIndex, o.getHashid());
                    break;
                case "SHARE_MODE":
                    pstmt.setInt(parameterIndex, o.getShareMode());
            }
            
        }
        @Override
        public void mapValues(Hash o, String colName, ResultSet rs) throws SQLException {
            switch(colName) {
                case "ID":
                    o.setId(rs.getLong(colName));
                    break;
                case "IS_ON":
                    o.setIson(rs.getInt(colName) != 0);
                    break;
                case "CREATE_DATE":
                    o.setCreateDate(rs.getDate(colName));
                    break;
                case "CPATH":
                    o.setPath(rs.getString(colName));
                    break;
                case "USERID":
                    o.setUserid(rs.getString(colName));
                    break;
                case "WPATH":
                    o.setWpath(rs.getString(colName));
                    break;
                case "HASHID":
                    o.setHashid(rs.getString(colName));
                    break;
                case "SHARE_MODE":
                    o.setShareMode(rs.getInt(colName));
            }
        }
    }
    
    @DBTableProperty(name = "WEB_HASH")
    public static class Hash {
        @DBProperty(name = "ID", dtype = "INTEGER PRIMARY KEY  AUTOINCREMENT   NOT NULL")
        private long id;
        @DBProperty(name = "IS_ON", dtype = "INTEGER")
        private boolean ison = true;
        @DBProperty(name = "CREATE_DATE", dtype = "DATE NOT NULL")
        private Date createDate = new Date();
        @DBProperty(name = "CPATH", dtype = "TEXT NOT NULL")
        private String path;
        @DBProperty(name = "USERID", dtype = "TEXT NOT NULL")
        private String userid;
        @DBProperty(name = "WPATH", dtype = "TEXT NOT NULL")
        private String wpath;
        @DBProperty(name = "HASHID", dtype = "TEXT NOT NULL")
        private String hashid;
        @DBProperty(name = "SHARE_MODE", dtype = "INTEGER")
        private int shareMode;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public boolean isIson() {
            return ison;
        }

        public void setIson(boolean ison) {
            this.ison = ison;
        }

        public Date getCreateDate() {
            return createDate;
        }

        public void setCreateDate(Date createDate) {
            this.createDate = createDate;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getUserid() {
            return userid;
        }

        public void setUserid(String userid) {
            this.userid = userid;
        }

        public String getWpath() {
            return wpath;
        }

        public void setWpath(String wpath) {
            this.wpath = wpath;
        }

        public String getHashid() {
            return hashid;
        }

        public void setHashid(String hashid) {
            this.hashid = hashid;
        }

        public int getShareMode() {
            return shareMode;
        }

        public void setShareMode(int shareMode) {
            this.shareMode = shareMode;
        }

    }
    
    private final JsonObject config;
    private final SimpleSqliteObjectStore ostore;
    
    public ShareHandler(String wpathDelim, String dirpath, String dirpathOut, SimpleSqliteObjectStore os) {
        this(new JsonObject().put("wpathDelim", wpathDelim).put("dirpath", dirpath).put("dirpathOut", dirpathOut), os);
    }
    
    public ShareHandler(JsonObject config, SimpleSqliteObjectStore os) {
        this.config = config;
        this.ostore = os;
    }
    
    protected String getWpathDelim() {
        return config.getString("wpathDelim");
    }
    
    protected String getDirpath() {
        return config.getString("dirpath");
    }
    
    protected String getDirpathOut() {
        return config.getString("dirpathOut");
    }
    
    @Override
    public void handle(RoutingContext ctx) {
        HttpServerRequest r = ctx.request();
        String shareMode = r.getParam("share1");
        if (shareMode != null) {
            String path = r.path();
            path = path.substring(getWpathDelim().length());
            String pathEncoded = path;
            try {
                path = java.net.URLDecoder.decode(path, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(ShareHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            File file2share = new File(getDirpath(), path);
            if (!file2share.exists()) {
                file2share = new File(getDirpathOut(), path);
            }
            if (file2share.exists()) {
                if ("1".equals(shareMode)) {
                    if (!file2share.isDirectory()) {
                        if (showHashLink(ctx, pathEncoded, file2share.getName(), 1)) {
                            return;
                        }
                        ctx.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                    }
                } else if ("2".equals(shareMode)) {
                    if (showHashLink(ctx, pathEncoded, file2share.getName(), 2)) {
                        return;
                    }
                    ctx.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                }
            }
            ctx.fail(HttpResponseStatus.NOT_FOUND.code());
        } else {
            ctx.next();
        }
    }
    
    private boolean showHashLink(RoutingContext ctx, String path, String fileName, int shareMode) {
        HttpServerRequest r = ctx.request();
        User u = ctx.user();
        if (u instanceof ExtraUser) {
            ExtraUser euser = (ExtraUser)u;
            Hash o = new Hash();
            o.setUserid(euser.getId());
            o.setWpath(getWpathDelim());
            o.setPath(path);
            o.setShareMode(shareMode);
            try {
                PassAuth pa = new PassAuth(o.getWpath());
                Hash exO = null;
                try {
                    Collection exObjs = ostore.getObjects(new String[] {"WPATH", "USERID", "CPATH", "SHARE_MODE"}, new Object[] { o.getWpath(), o.getUserid(), path, o.getShareMode()});
                    if (exObjs.size() > 0) {
                        exO = (Hash)exObjs.stream().findFirst().get();
                    }
                } catch (Exception ex) {
                    Logger.getLogger(ShareHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (exO == null) {
                    o.setHashid(pa.getHashString(o.getUserid(), path));
                    if (ostore.addObject(o) > 0) {
                        showLink(ctx, getUrl(r, o, fileName));
                        return true;
                    }
                } else {
                    showLink(ctx, getUrl(r, exO, fileName));
                    return true;
                }
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(ShareHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return false;
    }
    
    public static String PATH_REF = "ref/";
    private String getUrl(HttpServerRequest r, Hash o, String fileName) {
        return getSchemaUri(r) + getWpathDelim() + PATH_REF + getEncoded(o.getHashid()) + "/" + getEncoded(fileName);
    }
    
    private static String getEncoded(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {}
        return s;
    }
    
    public static String getSchemaUri(HttpServerRequest r) {
        return r.absoluteURI().substring(0, r.absoluteURI().length() - r.uri().length());
    }
    
    private void showLink(RoutingContext ctx, String url) {
        if ("TODO".equals(url)) {
            ctx.fail(HttpResponseStatus.NOT_FOUND.code());
        } else {
            ctx.response().putHeader("content-type", "text/html").end("Share link: <a href='" + url+"'>"+url+"</a>"+
                    "<img src='/qrcode?url="+getEncoded(url)+"'>");
        }
    }
    
//    public static String pathOffset(String path, String mountPoint/*context.mountPoint()*/, String routePath/*context.currentRoute().getPath()*/) {
//        int prefixLen = 0;
//        if (mountPoint != null) {
//            prefixLen = mountPoint.length();
//        }
//        if (routePath != null) {
//            prefixLen += routePath.length();
//            if (routePath.charAt(routePath.length() - 1) == '/') {
//                prefixLen--;
//            }
//        }
//        return prefixLen != 0 ? path.substring(prefixLen) : path;
//    }

}
