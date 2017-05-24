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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import ru.org.sevn.common.util.JaxbMarshallerManager;
import ru.org.sevn.jvert.VertxOutputStream;
import ru.org.sevn.rss.Rss;
import ru.org.sevn.rss.RssChanel;
import ru.org.sevn.rss.RssItem;
import ru.org.sevn.util.ComplexAndFilenameFilter;
import ru.org.sevn.util.ComplexFilenameFilter;
import ru.org.sevn.util.DirNotHiddenFilenameFilter;
import ru.org.sevn.util.FileUtil;

public class RssVerticle extends AbstractVerticle implements Runnable {
    
    public static final String FILE_NAME = "rss.xml";
    public static final String FILE_NAME_HTML = "index.html";
    
    private String schema;
    private final File dir;
    private final File dirOut;
    private final File fileOut;
    private final File fileOutHtml;
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final long initialDelay;
    private final long period;
    private final TimeUnit unit;
    private long myLastUpdated;

    private FilenameFilter contentFilenameFilter;
	private FilenameFilter imgFilenameFilter;
	private FilenameFilter vidFilenameFilter;
	private FilenameFilter contentFilter;
    private DirNotHiddenFilenameFilter dirFilter = new DirNotHiddenFilenameFilter();
	private FilenameFilter lastUpdatedFilter = (dir, name) -> {
        File idxFile = new File(dir, "index.html");
        File chFile = new File(dir, name);
        if (idxFile.exists()) {
            return (chFile.lastModified() > myLastUpdated && chFile.lastModified() > idxFile.lastModified());
        } else {
            return true;
        }
    };
        
    private Rss rss = new Rss();
    private RssChanel rssChanel;
    private boolean changed = false;
    
    private final WWWGenHandler genHandler;
    
    public RssVerticle(File f, File o, WWWGenHandler filterSupplier) {
        this(f, o, filterSupplier, 0, 5, TimeUnit.SECONDS);
    }
    public RssVerticle(File f, File o, WWWGenHandler filterSupplier, long initialDelay, long period, TimeUnit unit) {
        this.genHandler = filterSupplier;
        dir = f;
        dirOut = o;
        this.initialDelay = initialDelay;
        this.period = period;
        this.unit = unit;
        fileOut = new File(dirOut, FILE_NAME);
        fileOutHtml = new File(dirOut, FILE_NAME_HTML);

        {
            rssChanel = new RssChanel();
            rssChanel.setLastBuildDate(new Date());
            rss.getChanel().add(rssChanel);
        }
        if(fileOut.exists()) {
            myLastUpdated = fileOut.lastModified(); //TODO can loose some updates
            rss = JaxbMarshallerManager.unmarshal(Rss.class, fileOut, rss);
            if (rss.getChanel().size() > 0) {
                rssChanel = rss.getChanel().get(0);
            } else {
                rss.getChanel().add(rssChanel);
            }
        }
        
        {
            File d = genHandler.getDirRoot();
            Menu m = genHandler.makeRootMenu(d);
            String lnk = schema + this.genHandler.getWebpath() + FILE_NAME;
            String title = m.getAnyTitle(); //WWWGenHandler.getText(FileUtil.getExistsFile(d, WWWGenHandler.FILE_NAME_TITLE, WWWGenHandler.TXT_EXT), d.getName());
            String dsc = WWWGenHandler.getText(FileUtil.getExistsFile(d, WWWGenHandler.FILE_NAME_DESCR, WWWGenHandler.TXT_EXT), "");

            rssChanel.setTitle(title);
            rssChanel.setLink(lnk);
            rssChanel.setDescription(dsc);
        }
        
		contentFilenameFilter = filterSupplier.makeImgFilenameFilter();
		imgFilenameFilter = filterSupplier.makeVidFilenameFilter();
		vidFilenameFilter = filterSupplier.makeContentFilenameFilter();
        contentFilter = new ComplexAndFilenameFilter(new ComplexFilenameFilter(
						contentFilenameFilter,
						imgFilenameFilter,
						vidFilenameFilter
						), lastUpdatedFilter);
        
    }
    public void start(Future<Void> startFuture) {
        executor.scheduleAtFixedRate(this, initialDelay, period, unit);
    }    

    @Override
    public void run() {
        long newLastUpdated = new Date().getTime();
        //System.err.println("Find updates for rss " + dir.getAbsolutePath());
        if (drillDirs(dir)) {
            if (writeMe() > 0) {
                myLastUpdated = newLastUpdated;
            }
        }
    }
    
    private boolean drillDirs(File dir) {
        boolean ret = false;
        System.err.println("find>"+dir.getAbsolutePath());
        if (schema != null && dir.exists()) {
            File[] files = dir.listFiles(contentFilter);
            if (files.length > 0) {
                addDir(dir, files);
                ret = true;
            }
            for (File f : dir.listFiles(dirFilter)) {
                if (drillDirs(f)) {
                    ret = true;
                }
            }
            if (!new File(dir, "index.html").exists()) {
                Menu m = this.genHandler.makeRelativeMenu(this.genHandler.getDirRoot(), dir);
            }
        }
        return ret;
    }
    private void addDir(File d, File[] files) {
        changed = true;
        System.err.println("Found updates for rss in " + d.getAbsolutePath());
        long maxLastUpdated = 0;
        for (File f : files) {
            maxLastUpdated = Math.max(maxLastUpdated, f.lastModified());
        }
        //TODO
        Menu m = this.genHandler.makeRelativeMenu(this.genHandler.getDirRoot(), d);
        
        String lnk = schema + this.genHandler.getWebpath() + FileUtil.getRelativePath(this.genHandler.getDirRoot(), d);
        String title = WWWGenHandler.getText(FileUtil.getExistsFile(d, WWWGenHandler.FILE_NAME_TITLE, WWWGenHandler.TXT_EXT), d.getName());
        String dsc = WWWGenHandler.getText(FileUtil.getExistsFile(d, WWWGenHandler.FILE_NAME_DESCR, WWWGenHandler.TXT_EXT), "");
        
        RssItem ri = new RssItem();
        ri.setTitle(title);
        ri.setDescription(dsc);
        ri.setLink(lnk);
        ri.setPubDate(new Date(maxLastUpdated));
        rssChanel.getItem().add(ri);
        
//        String outStr = d.getAbsolutePath();
//        try {
//            Files.write(fileOut.toPath(), outStr.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//        } catch (IOException ex) {
//            Logger.getLogger(RssVerticle.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
    private long writeMe() {
        if (!dirOut.exists()) {
            dirOut.mkdirs();
        }
        if (changed) {
            rssChanel.setLastBuildDate(new Date());

            try {
                JaxbMarshallerManager.marshal(rss, fileOut);
                this.genHandler.write(getHtmlContent());
            } catch (JAXBException ex) {
                Logger.getLogger(RssVerticle.class.getName()).log(Level.SEVERE, null, ex);
            }
            return fileOut.lastModified();
        }
        return 0;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
        String lnk = schema + this.genHandler.getWebpath() + FILE_NAME;
        rssChanel.setLink(lnk);
    }
    
    public File getFileOut() {
        return this.fileOut;
    }
    public File getFileOutHtml() {
        return this.fileOutHtml;
    }
    
    //TODO
    private static void sendFile(File f, RoutingContext ctx) {
        if (f.exists()) {
            String contentType = null;
            try {
                contentType = Files.probeContentType(Paths.get(f.getPath()));
            } catch (IOException ex) {
                Logger.getLogger(HtmlCacheHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (contentType != null) {
                try {
                    String fileCont = new String(Files.readAllBytes(f.toPath()), "UTF-8");

                    ctx.response().putHeader("content-type", contentType);
                    VertxOutputStream vos = new VertxOutputStream(ctx.response());
                    
                    ctx.response().setChunked(true);
                    vos.write(fileCont.getBytes("UTF-8"));
                    vos.close();
                    return;
                } catch (IOException ex) {
                    Logger.getLogger(HtmlCacheHandler.class.getName()).log(Level.SEVERE, null, ex);
                    ctx.fail(ex);
                }
            }
        }
        ctx.fail(404);
    }
    private io.vertx.core.Handler<RoutingContext> rssHandler = ctx -> {
        sendFile(getFileOut(), ctx);
    };
    private io.vertx.core.Handler<RoutingContext> rssHtmlHandler = ctx -> {
        sendFile(getFileOutHtml(), ctx);
    };
    
    public io.vertx.core.Handler<RoutingContext> getRssHandler() {
        return rssHandler;
    }
    public io.vertx.core.Handler<RoutingContext> getRssHtmlHandler() {
        return rssHtmlHandler;
    }

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private WWWGenHandler.HtmlContent getHtmlContent() {
        File f = new File(genHandler.getDirRoot(), "rss/index.html");
        Menu m = genHandler.makeRelativeMenu(genHandler.getDirRoot(), f);
        WWWGenHandler.HtmlContent content = new WWWGenHandler.HtmlContent(Menu.getRoot(m));
        content.setFileOut(this.fileOutHtml);
        content.setFile(f);
        for(RssItem ri : rssChanel.getItem()) {
            String pref = null;
            //TODO sort by date
            if (ri.getPubDate() != null) {
                pref = dateFormat.format(ri.getPubDate())+"&nbsp;";
            }
            content.getContent().append(genHandler.getHref(ri.getLink(), ri.getTitle(), pref)).append("<br>");
        }
        return content;
    }
}
