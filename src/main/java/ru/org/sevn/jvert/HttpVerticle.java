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

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.providers.GoogleAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.FormLoginHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.RedirectAuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.jcodec.api.awt.FrameGrab;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
import ru.org.sevn.jsecure.PassAuth;
import ru.org.sevn.utilwt.ImageUtil;

public class HttpVerticle extends AbstractVerticle {
    private static final int KB = 1024;
    private static final int MB = 1024 * KB;
    private boolean useSsl = true;
    private String hostName = "localhost";
    private int port = 1945;
    private JsonObject config;
    private String sslCertPathCer = "cert-04.cer";
    private String sslKeyPathPem = "key-04.pem";
    private String appName = "SV-www";
    private JsonArray webs;
    private SimpleUserMatcher userMatcher;
    private String saltPrefix = "salt";

    public boolean isUseSsl() {
        return useSsl;
    }

    public String getHostName() {
        return hostName;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getAppName() {
        return appName;
    }
    
    protected Path getConfigJsonPath() {
        return new java.io.File(System.getProperty("user.home") + "/sevn-http-vert.json").toPath();
    }
    
    protected String getSslCertPathCer() {
        return sslCertPathCer;
    }
    protected String getSslKeyPathPem() {
        return sslKeyPathPem;
    }
    
    protected void configService() {
        try {
            config = new JsonObject(new String(Files.readAllBytes(getConfigJsonPath()), "UTF-8"));
            if (config.containsKey("auth")) {
                try {
                    JsonObject auth = config.getJsonObject("auth", null);
                    if (auth != null) {
                        saltPrefix = auth.getString("salt", "salt");
                    }
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("webs")) {
                try {
                    webs = config.getJsonArray("webs", null);
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("users")) {
                try {
                    userMatcher = new SimpleUserMatcher(config.getJsonArray("users", null));
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("hostName")) {
                try {
                    hostName = config.getString("hostName");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("port")) {
                try {
                    port = config.getInteger("port");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("useSsl")) {
                try {
                    useSsl = config.getBoolean("useSsl");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("sslCertPathCer")) {
                try {
                    sslCertPathCer = config.getString("sslCertPathCer");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("sslKeyPathPem")) {
                try {
                    sslKeyPathPem = config.getString("sslKeyPathPem");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("appName")) {
                try {
                    appName = config.getString("appName");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        } catch (java.nio.file.NoSuchFileException e1) {
            Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, e1.getClass().getName() + ": " + e1.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    protected String getConfigAuthJsonFilename() {
        if (isUseSsl()) {
            return "googletrue.json";
        } else {
            return "google.json";
        }
    }
    
    protected Path getConfigAuthJsonPath() {
        return new java.io.File(System.getProperty("user.home") + "/" + getConfigAuthJsonFilename()).toPath();
    }
    
    private AuthProvider newAuthProviderGoogle() {
        try {
            String fileContent = new String(Files.readAllBytes(getConfigAuthJsonPath()), "UTF-8");
            JsonObject serviceAccountJson = new JsonObject(fileContent);
            JsonObject web = serviceAccountJson.getJsonObject("web");
            OAuth2Auth authProvider = GoogleAuth.create(vertx, web.getString("client_id"), web.getString("client_secret"));
            return authProvider;
        } catch (IOException ex) {
            Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    protected String getAuthUrl() {
        String hostname = getHostName();
        String host = "http://"+hostname+":";
        if (isUseSsl()) {
            host = "https://"+hostname+":";
        }
        return host + getPort();
    }
    
    protected void setUpRouter(Router router) {
        router.route().handler(CookieHandler.create());
        router.route().handler(BodyHandler.create().setBodyLimit(50 * MB));
        SessionHandler sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));
        sessionHandler.setCookieHttpOnlyFlag(true);
        if (isUseSsl()) { //TODO
            sessionHandler.setCookieSecureFlag(true);
        }
        router.route().handler(sessionHandler);
    }
    
    
    private void redirect(HttpServerRequest r, String location) {
        r.response().setChunked(false);
        r.response().setStatusCode(302);
        r.response().putHeader("Location", location);
        r.response().end();
    }
    
    @Override
    public void start() throws Exception {
        configService();
        
        Router router = Router.router(vertx);
        setUpRouter(router);
        
        OAuth2Auth authProvider = (OAuth2Auth) newAuthProviderGoogle();
        router.route().handler(UserSessionHandler.create(authProvider));
        
        OAuth2AuthHandler oauth2 = new GoogleUserOAuth2AuthHandlerImpl(authProvider, getAuthUrl(), getAppName(), userMatcher);
        oauth2.addAuthority("profile");
        oauth2.setupCallback(router.get("/auth"));

        FileAuthProvider fileAuthProvider = new FileAuthProvider(userMatcher, new PassAuth(saltPrefix));
        router.route("/www/login").handler(RedirectAuthHandler.create(fileAuthProvider,"/www/loginpage.html"));
        router.route("/www/login").handler(new WebpathHandler());
        router.route("/www/loginauth").handler(FormLoginHandler.create(fileAuthProvider));
                
        router.route("/logout").handler(context -> {
            context.clearUser();
            context.response().putHeader("location", "/").setStatusCode(302).end();
        });
        
        
        {
            String servPath = "/www/logingoogle/";
            router.route(servPath+"*").handler(new RedirectUnAuthParamPageHandler(servPath));
            router.route(servPath+"*").handler(oauth2);
            router.route(servPath+"*").handler(new WebpathHandler());
        }
        
        {
            String servPath = "/chat/";
            router.route(servPath+"*").handler(new RedirectUnAuthParamPageHandler(servPath));
            router.route(servPath+"*").handler(oauth2);
            router.route(servPath+"*").handler(new ChatHandler());
        }
        //TODO refresh settings
        
        final ArrayList<String> websList = new ArrayList<>();
        if (webs != null) {
            for (Object o : webs) {
                if (o instanceof JsonObject) {
                    JsonObject jobj = (JsonObject)o;
                    if (jobj.containsKey("webpath") && jobj.containsKey("dirpath") && jobj.containsKey("groups")) {
                        try {
                            String webpath = jobj.getString("webpath");
                            String dirpath = jobj.getString("dirpath");
                            String dirpathThumb = jobj.getString("dirpathThumb");
                            String dirpathThumbBig = jobj.getString("dirpathThumbBig");
                            String dirpathHtmlCache = jobj.getString("dirpathHtmlCache");
                            JsonArray groups = jobj.getJsonArray("groups");
                            GroupUserAuthorizer authorizer = new GroupUserAuthorizer(groups);
                            String wpath = "/"+webpath;
                            String wpathDelim = "/"+webpath+"/";

                            router.route(wpath+"/*").handler(new RedirectUnAuthParamPageHandler(wpath + "/"));
                            router.route(wpath+"/*").handler(oauth2);
                            router.route(wpath).handler(ctx -> {
                                redirect(ctx.request(), wpath + "/index.html");
                            });
                            router.route(wpath+"/dynamic/*").handler(
                                    new UserAuthorizedHandler(authorizer, ctx -> {
                                        ctx.response().putHeader("content-type", "text/html").end("Dynamic page for " + webpath);
                                    })
                            );
                            router.route(wpath+"/logout/*").handler(ctx -> {
                                ctx.clearUser();
                                ctx.response().putHeader("location", "/").setStatusCode(302).end();
                            });
                            router.route(wpath+"/*").handler( new UserAuthorizedHandler(authorizer, ctx -> {
                                HttpServerRequest r = ctx.request();
                                String zipMode = r.getParam("zip");
                                if (zipMode != null) {
                                    String path = r.path();
                                    path = path.substring(wpathDelim.length());
                                    try {
                                        path = java.net.URLDecoder.decode(path, "UTF-8");
                                    } catch (UnsupportedEncodingException ex) {
                                        Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    File dir = new File(dirpath, path);
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
                                                    for(File fl : dir.listFiles()) {
                                                        String nm = fl.getName(); 
                                                        if (fl.isDirectory()) {}
                                                        else if (nm.startsWith("_") || nm.startsWith(".")) {}
                                                        else {
                                                            ZipEntry ze = new ZipEntry(nm);
                                                            zos.putNextEntry(ze);
                                                            zos.write(Files.readAllBytes(fl.toPath()));
                                                            zos.closeEntry();
                                                        }
                                                    }
                                                    zos.close();
                                                }
                                            } catch (IOException ex) {
                                                Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                        }
                                    } else {
                                        ctx.fail(HttpResponseStatus.NOT_FOUND.code());
                                    }
                                } else {
                                    ctx.next();
                                }
                            }));
                            router.route(wpath+"/*").handler(
                                    new UserAuthorizedHandler(authorizer, ctx -> {
                                        HttpServerRequest r = ctx.request();
                                        String imgThmb = r.params().get("imgThmb");
                                        if (imgThmb != null) {
                                            
                                            String path = r.path();
                                            try {
                                                path = java.net.URLDecoder.decode(r.path(), "UTF-8");
                                            } catch (UnsupportedEncodingException ex) {
                                                Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                            
                                            int height = 0;
                                            String dirpathTh = "";
                                            String newRoute = "";
                                            String thumbName = path.substring(wpathDelim.length());
                                            if ("sm".equals(imgThmb)) {
                                                height = 160;
                                                dirpathTh = dirpathThumb;
                                                newRoute = wpath + "/thumb/" + thumbName;
                                            } else if ("bg".equals(imgThmb)) {
                                                height = 736;
                                                dirpathTh = dirpathThumbBig;
                                                newRoute = wpath + "/thumbg/" + thumbName;
                                            }
                                            if (height > 0) {
                                                final File img = new File(dirpath, thumbName);
                                                if (img.exists()) {
                                                    try {
                                                        String contentType = Files.probeContentType(img.toPath());
                                                        if (contentType != null && contentType.startsWith("image")) {
                                                            File thumbFile = new File(dirpathTh, thumbName);
                                                            if (makeThumbs( new ImageIconSupplier() {

                                                                @Override
                                                                public ImageIcon getImageIcon() {
                                                                    return new ImageIcon(img.getPath());
                                                                }
                                                            }, thumbFile, height) != null) {
                                                                ctx.reroute(newRoute);
                                                                return;
                                                            }
                                                        } else if (contentType != null && contentType.startsWith("video")) {
                                                            thumbName += ".png";
                                                            newRoute += ".png";
                                                            File thumbFile = new File(dirpathTh, thumbName);
                                                            if (makeThumbs( new ImageIconSupplier() {

                                                                @Override
                                                                public ImageIcon getImageIcon() {
                                                                    try {
                                                                        FileChannelWrapper grabch = NIOUtils.readableFileChannel(img);
                                                                        BufferedImage frame = null;
                                                                        try { 
                                                                            FrameGrab grab = new FrameGrab(grabch);
                                                                            for (int i = 0; i < 50; i++) {
                                                                                grab.seekToFrameSloppy(50);
                                                                                try {
                                                                                    frame = grab.getFrame();
                                                                                } catch (Exception e) {
                                                                                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                                                                                }
                                                                            }
                                                                        } finally {
                                                                            NIOUtils.closeQuietly(grabch);
                                                                        }
                                                                        if (frame != null) {
                                                                            return new ImageIcon(frame);
                                                                        }
                                                                    } catch (Exception ex) {
                                                                        Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                                                                    }
                                                                    return null;
                                                                }
                                                            }, thumbFile, height) != null) {
                                                                ctx.reroute(newRoute);
                                                                return;
                                                            }
                                                        }
                                                    } catch (IOException ex) {
                                                        Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                                                    }
                                                }
                                            }
                                            //TODO cache image refactor
                                        }
                                        ctx.next();
                                    })
                            );
                            router.route(wpath+"/thumb/*").handler(
                                    new UserAuthorizedHandler(authorizer, 
                                        new NReachableFSStaticHandlerImpl().setAlwaysAsyncFS(true).setCachingEnabled(false).setDefaultContentEncoding("UTF-8").setAllowRootFileSystemAccess(true).setWebRoot(new File(dirpathThumb).getAbsolutePath())
                                    )
                            );
                            router.route(wpath+"/thumbg/*").handler(
                                    new UserAuthorizedHandler(authorizer, 
                                        new NReachableFSStaticHandlerImpl().setAlwaysAsyncFS(true).setCachingEnabled(false).setDefaultContentEncoding("UTF-8").setAllowRootFileSystemAccess(true).setWebRoot(new File(dirpathThumbBig).getAbsolutePath())
                                    )
                            );
                            if (dirpathHtmlCache == null) {
                                Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, "Can't cache html");                                
                            } else {
                                router.route(wpath+"/*").handler(
                                        new UserAuthorizedHandler(authorizer, ctx -> {
                                            File dirpathHtmlCacheFile = new File(dirpathHtmlCache);;
                                            if (dirpathHtmlCacheFile.exists() && dirpathHtmlCacheFile.canWrite()) {
                                            } else {
                                                Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, "Can''t cache html into {0}", dirpathHtmlCacheFile.getAbsolutePath());
                                                dirpathHtmlCacheFile = null;
                                            }
                                            String path = ctx.request().path();
                                            try {
                                                path = java.net.URLDecoder.decode(path, "UTF-8");
                                            } catch (UnsupportedEncodingException ex) {
                                                Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                            if (path.endsWith(".html")) {
                                                path = path.substring(wpathDelim.length());
                                                File f = new File(dirpath, path);
                                                if (f.exists()) { //TODO cache refactor

                                                    String contentType = null;
                                                    try {
                                                        contentType = Files.probeContentType(Paths.get(f.getPath()));
                                                    } catch (IOException ex) {
                                                        Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
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
                                                                        Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, "Can't cache html into " + cache.getAbsolutePath(), ex1);
                                                                    }       
                                                                }
                                                            }
                                                            vos.write(fileCont.getBytes("UTF-8"));
                                                            vos.close();
                                                        } catch (IOException ex) {
                                                            Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                                                            ctx.fail(ex);
                                                        }

                                                        return;
                                                    }

                                                }
                                            }
                                            ctx.next();
                                        })
                                );
                            }
                            router.route(wpath+"/*").handler(
                                    new UserAuthorizedHandler(authorizer, 
                                        new NReachableFSStaticHandlerImpl().setAlwaysAsyncFS(true).setCachingEnabled(false).setDefaultContentEncoding("UTF-8").setAllowRootFileSystemAccess(true).setWebRoot(new File(dirpath).getAbsolutePath())
                                    )
                            );
                            websList.add(webpath);
                        } catch (Exception ex) {
                            Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
        //TODO add dynamic pages
        
        router.get("/").handler(ctx -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Server is running").append("<br>");
            String shemaPath = getSchemaUri(ctx.request());
            {
                String wp = shemaPath + "/www/logingoogle";
                String wpStr = "Google";
                sb.append("<a href=\"").append(wp).append("\">").append(wpStr).append("</a>").append("<br>");
            }
            {
                String wp = shemaPath + "/www/login";
                String wpStr = "Local";
                sb.append("<a href=\"").append(wp).append("\">").append(wpStr).append("</a>").append("<br>");
            }
            ctx.response().putHeader("content-type", "text/html").end(sb.toString()+loggedUserString(ctx));
        });
        
        router.get("/admin").handler(ctx -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Configured paths:").append("<br>");
            for (String wp : websList) {
                sb.append("<a href=\"/").append(wp).append("/index.html\">").append(wp).append("</a>").append("<br>");
                
            }
            ctx.response().putHeader("content-type", "text/html").end(sb.toString()+simpleAnswerString(ctx));
        });
        
        //webroot
        router.route("/www/*").handler(StaticHandler.create().setCachingEnabled(false));
        
        router.get("/*").failureHandler(ctx -> {
            //TODO error template
            int statusCode = ctx.statusCode();
            HttpServerResponse response = ctx.response();
            response.putHeader("content-type", "text/plain");
            response.setChunked(false);
            response.setStatusCode(statusCode).end("Not accessible");
        });
        
        startServer(router);
    }

    public static interface ImageIconSupplier {
        ImageIcon getImageIcon();
    }
    private static File makeThumbs(ImageIconSupplier imgSupplier, File thumbimg, int height) {
        //BufferedImage frame = FrameGrab.getFrame(new File("/Users/jovi/Movies/test.mp4"), i);
        File ret = null;
        // TODO Exception
        if (!thumbimg.exists()) {
            File thumbdir = thumbimg.getParentFile();
            // TODO Exception
            if (!thumbdir.exists()) {
                thumbdir.mkdirs();
            }
            
            System.err.println("generate thumb>>>" + thumbimg);
            try {
                ImageIcon ii = ImageUtil.getScaledImageIconHeight(imgSupplier.getImageIcon(), height, false);
                ImageIO.write(ImageUtil.getBufferedImage(ii), "png", thumbimg);
                ret = thumbimg;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            ret = thumbimg;
        }
        return ret;
    }
    
            
    protected void startServer(Router router) {
        HttpServerOptions options = new HttpServerOptions();
        if (isUseSsl()) {
            setSslCerts(options);
        }
        vertx.createHttpServer(options).requestHandler(router::accept)
                .listen(getPort());
    }
    
    protected void setSslCerts(HttpServerOptions options) {
        PemKeyCertOptions pemOptions = new PemKeyCertOptions();
        pemOptions.setKeyPath(getSslKeyPathPem()).setCertPath(getSslCertPathCer());
        options.setSsl(true);
        options.setPemKeyCertOptions(pemOptions);
    }
    public static void secureSet(HttpServerResponse response) {
        response
                // do not allow proxies to cache the data
                .putHeader("Cache-Control", "no-store, no-cache")
                // prevents Internet Explorer from MIME - sniffing a
                // response away from the declared content-type
                .putHeader("X-Content-Type-Options", "nosniff")
                // Strict HTTPS (for about ~6Months)
                .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
                // IE8+ do not allow opening of attachments in the context of this resource
                .putHeader("X-Download-Options", "noopen")
                // enable XSS for IE
                .putHeader("X-XSS-Protection", "1; mode=block")
                // deny frames
                .putHeader("X-FRAME-OPTIONS", "DENY");
    }
    
        

//--------------------------
    private static String loggedUserString(RoutingContext context) {
        User user = context.user();
        if (user instanceof ExtraUser) {
            return ((ExtraUser)user).fullInfo().encodePrettily();
        } else if (user != null) {
            return "some authenticated user";
        }
        return "";
    }

    private static String simpleAnswerString(RoutingContext context) {
        HttpServerRequest r = context.request();

        secureSet(context.response());

        Session session = context.session();

        Integer cnt = session.get("hitcount");
        cnt = (cnt == null ? 0 : cnt) + 1;

        session.put("hitcount", cnt);

        String resp = "<pre>"
                + r.absoluteURI() + "\n"
                + getSchemaPath(r) + "\n"
                + r.uri() + "\n"
                + r.path() + "\n"
                + r.query() + "\n"
                + loggedUserString(context) + "\n"
                + cnt + "\n"
                + "</pre>";
        System.err.println("*********>" + resp);
        return resp;
    }
    
    public static String getSchemaUri(HttpServerRequest r) {
        return r.absoluteURI().substring(0, r.absoluteURI().length() - r.uri().length());
    }
    public static String getSchemaPath(HttpServerRequest r) {
        return getSchemaUri(r) + r.path();
    }
    
}
