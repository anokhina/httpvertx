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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfoplus;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.impl.AccessTokenImpl;
import io.vertx.ext.auth.oauth2.providers.GoogleAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.handler.impl.OAuth2AuthHandlerImpl;
import io.vertx.ext.web.handler.impl.StaticHandlerImpl;
import io.vertx.ext.web.sstore.LocalSessionStore;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.jcodec.api.awt.FrameGrab;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
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
    
    static class SimpleUserMatcher implements UserMatcher {
        private HashMap<String, Collection> userGroups = new HashMap<>();

        public SimpleUserMatcher(JsonArray arr) {
            for (Object o : arr) {
                if (o instanceof JsonObject) {
                    JsonObject jobj = (JsonObject) o;
                    if (jobj.containsKey("id")) {
                        HashSet<String> grps = new HashSet();
                        try {
                            if (jobj.containsKey("groups")) {
                                for (Object g : jobj.getJsonArray("groups")) {
                                    grps.add(g.toString());
                                }
                            }
                            userGroups.put(jobj.getString("id"), grps);
                        } catch (Exception ex) {
                            Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
        @Override
        public Collection<String> getGroups(User u) {
            if (u instanceof ExtraUser) {
                ExtraUser user = (ExtraUser)u;
                return userGroups.get(user.getId());
            }
            return null;
        }
        
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

        router.route("/logout").handler(context -> {
            context.clearUser();
            context.response().putHeader("location", "/").setStatusCode(302).end();
        });
        
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
            sb.append("Configured paths:").append("<br>");
            for (String wp : websList) {
                sb.append("<a href=\"/").append(wp).append("/index.html\">").append(wp).append("</a>").append("<br>");
                
            }
            ctx.response().putHeader("content-type", "text/html").end(sb.toString()+simpleAnswerString(ctx));
        });
        
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
        if (!thumbimg.exists()) {
            File thumbdir = thumbimg.getParentFile();
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
    
    public static class NReachableFSStaticHandlerImpl extends StaticHandlerImpl {

        private String webRoot = DEFAULT_WEB_ROOT;
        @Override
        public StaticHandler setWebRoot(String webRoot) {
            super.setWebRoot(webRoot);
            this.webRoot = webRoot;
            return this;
        }
        
       @Override
        public void handle(RoutingContext context) {
            try {
                if (new File(webRoot).exists()) {
                    super.handle(context);
                } else {
                    context.fail(NOT_FOUND.code());
                }
            } catch (Exception ex) {
                Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, "Not found: " + webRoot);
                context.fail(NOT_FOUND.code());
            }
        }
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
    
    public static class GroupUserAuthorizer implements UserAuthorizer {
        
        private final Set<String>groups = new HashSet<>();
        
        public GroupUserAuthorizer(JsonArray groups) {
            for(Object o : groups) {
                this.groups.add(o.toString());
            }
        }
        
        @Override
        public boolean isAllowed(User u, RoutingContext rc) {
            if (u instanceof ExtraUser) {
                ExtraUser user = (ExtraUser)u;
                if (groups.size() > 0) {
                    for (String grp : this.groups) {
                        if (user.getGroups().contains(grp)) {
                            return true;
                        }
                    }
                } else {
                    return true;
                }
            }
            return false;
        }
    }
        
    public interface UserAuthorizer  {
        boolean isAllowed(User user, RoutingContext rc);
    }
    public static class UserAuthorizedHandler implements io.vertx.core.Handler<RoutingContext> {
        
        private final io.vertx.core.Handler<RoutingContext> handler;
        private final UserAuthorizer authorizer;
        public UserAuthorizedHandler(UserAuthorizer authorizer, io.vertx.core.Handler<RoutingContext> handler) {
            this.handler = handler;
            this.authorizer = authorizer;
        }

        @Override
        public void handle(RoutingContext rc) {
            if (authorizer.isAllowed(rc.user(), rc)) {
                handler.handle(rc);
            } else {
                rc.fail(403);
            }
        }
    }
    public static class RedirectUnAuthParamPageHandler implements io.vertx.core.Handler<RoutingContext> {
        private final String wpath;
        
        public RedirectUnAuthParamPageHandler (String p) {
            this.wpath = p;
        }
        @Override
        public void handle(RoutingContext ctx) {
            if (ctx.user() == null) {
                if (ctx.request().params().size() > 0) {
                    ctx.reroute(wpath);
                    return;
                }
            }
            ctx.next();        
        }
    }
    public static class ExtraUser implements User {
        private final User user;
        private JsonObject extraData;
        private Set<String> groups = new HashSet<>();
        
        public ExtraUser(User u) {
            user = u;
        }

        @Override
        public User isAuthorised(String string, Handler<AsyncResult<Boolean>> hndlr) {
            user.isAuthorised(string, hndlr);
            return this;
        }

        @Override
        public User clearCache() {
            user.clearCache();
            return this;
        }

        @Override
        public JsonObject principal() {
            return user.principal();
        }

        @Override
        public void setAuthProvider(AuthProvider ap) {
            user.setAuthProvider(ap);
        }
        
        public String getId() {
            if (extraData != null) {
                return extraData.getString("id", null);
            }
            return null;
        }

        public JsonObject getExtraData() {
            return extraData;
        }

        public void setExtraData(JsonObject extraData) {
            this.extraData = extraData;
        }

        public Set<String> getGroups() {
            return groups;
        }

        public void setGroups(Set<String> groups) {
            this.groups = groups;
        }
        
    }    
    public static interface UserMatcher {
        Collection<String> getGroups(User u);
    }
    static class GoogleUserOAuth2AuthHandlerImpl extends OAuth2AuthHandlerImpl {

        private final String appName;
        private UserMatcher userMatcher;
        
        public GoogleUserOAuth2AuthHandlerImpl(OAuth2Auth authProvider, String callbackURL, String appName, UserMatcher um) {
            super(authProvider, callbackURL);
            this.appName = appName;
            this.userMatcher = um;
        }

        @Override
        public void handle(RoutingContext ctx) {
            User user = ctx.user();
            if (user != null) {
                if (user instanceof AccessTokenImpl) {
                    try {
                        upgradeUser((AccessTokenImpl)user, ctx);
                        user = ctx.user();
                    } catch (IOException ex) {
                        Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (user instanceof ExtraUser) {
                    authoriseMe((ExtraUser)user, ctx);
                } else {
                    ctx.fail(403);
                }
            } else {
                super.handle(ctx);
            }
        }
        
        private void upgradeUser(AccessTokenImpl user, RoutingContext context) throws IOException {
            GoogleCredential credential = new GoogleCredential().setAccessToken(user.principal().getString("access_token"));
            Oauth2 oauth2 = new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(), credential).setApplicationName(appName).build();       
            Userinfoplus userinfo = oauth2.userinfo().get().execute();
            ExtraUser euser = new ExtraUser(user);
            euser.setExtraData(new JsonObject(userinfo.toPrettyString()));
            Collection<String> groups = userMatcher.getGroups(euser);
            if (groups != null) {
                euser.getGroups().addAll(groups);
                context.setUser(euser);
                Session session = context.session();
                if (session != null) {
                    // the user has upgraded from unauthenticated to authenticated
                    // session should be upgraded as recommended by owasp
                    session.regenerateId();
                }
            }
        }
        
        protected void authoriseMe(ExtraUser user, RoutingContext context) {
            //super.authorise(user, context);
            context.next();
        }

    }
    //--------------------------
    private static String simpleAnswerString(RoutingContext context) {
        User user = context.user();
        HttpServerRequest r = context.request();

        secureSet(context.response());

        Session session = context.session();

        Integer cnt = session.get("hitcount");
        cnt = (cnt == null ? 0 : cnt) + 1;

        session.put("hitcount", cnt);

        String resp = "<pre>"
                + r.absoluteURI() + "\n"
                + r.uri() + "\n"
                + r.path() + "\n"
                + r.query() + "\n"
                + user + "\n"
                + cnt + "\n"
                + "</pre>";
        System.err.println("*********>" + resp);
        return resp;
    }
    
}
