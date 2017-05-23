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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import ru.org.sevn.templ.ClasspathVelocityEngine;
import ru.org.sevn.util.ComplexFilenameFilter;
import ru.org.sevn.util.DirNotHiddenFilenameFilter;
import ru.org.sevn.util.FileNameComparator;
import ru.org.sevn.util.FileUtil;
import ru.org.sevn.util.NotFilenameFilter;
import ru.org.sevn.util.StartWithFilenameFilter;
import ru.org.sevn.utilhtml.UtilHtml;
import ru.org.sevn.utilwt.FileVideoIconSupplier;
import ru.org.sevn.utilwt.ImageUtil;
import ru.org.sevn.winurl.WinUrl;

public class WWWGenHandler implements io.vertx.core.Handler<RoutingContext> {
    
	public static final String[] ICONS_EXT = new String[] {".png", ".jpg", ".jpeg"};
	public static final String[] TXT_EXT = new String[] {".txt"};

    private boolean force = !true;
	private VelocityEngine ve = new VelocityEngine();
    private String logo = "logo.png";
    private String favico = "logo.ico";
    private String css = "css.css";
    private final File dirRoot;
    private final String webpath;
    private final String wpathDelim;

	private FileNameComparator ASC = new FileNameComparator();
	private FileNameComparator DSC = new FileNameComparator(false);
	private DirNotHiddenFilenameFilter dirFileFilter = new DirNotHiddenFilenameFilter();

    private boolean isWinUrl(File f) {
        return f.getName().toLowerCase().endsWith(".url");
    }

    private String getWinUrlText(File f) {
        try {
            WinUrl wurl = WinUrl.parseUrlContent(new String(Files.readAllBytes(f.toPath()),"UTF-8"), f.getName());
            StringBuilder sb = new StringBuilder();
            sb.append("<a href=\"").append(wurl.getSharedText()).append("\">").append(wurl.getSharedSubj()).append("</a>");
            return sb.toString();
        } catch (IOException ex) {
            Logger.getLogger(WWWGenHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
    
	static class HtmlContent {
		private StringBuilder content = new StringBuilder();
		private File file;
		private final Menu menu;
		public HtmlContent(Menu f) {
			menu = f;
		}
	}

    public WWWGenHandler(JsonObject htmlgen, File dirr, String webpath) {
        this(htmlgen.getString("dirtemplates", null), dirr, webpath);
        if (htmlgen.containsKey("logo")) {
            setLogoFile(htmlgen.getString("logo"));
        }
        if (htmlgen.containsKey("css")) {
            setCssFile(htmlgen.getString("css"));
        }
        if (htmlgen.containsKey("favico")) {
            setFaviconFile(htmlgen.getString("favico"));
        }
        if (htmlgen.containsKey("force")) {
            setForce(htmlgen.getBoolean("force"));
        }
    }
    public WWWGenHandler(String resdir, File dirr, String webpath) {
        VelocityEngine ve = new VelocityEngine();
        if (resdir == null || !new File(resdir).exists()) {
            ClasspathVelocityEngine.applyClasspathResourceLoader(ve);
        } else {
            ClasspathVelocityEngine.applyFileResourceLoader(ve, resdir);

        }
        this.ve = ve;
		ve.init();
        dirRoot = dirr;
        this.webpath = webpath;
        wpathDelim = "/"+webpath+"/";
    }
    
    public boolean isForce() {
        return force;
    }

    public WWWGenHandler setForce(boolean force) {
        this.force = force;
        return this;
    }

    public File getLogoFile() {
        return new File(dirRoot, logo);
    }

    public WWWGenHandler setLogoFile(String logoFile) {
        this.logo = logoFile;
        return this;
    }

    public File getCssFile() {
        return new File(dirRoot, css);
    }

    public WWWGenHandler setCssFile(String cssFile) {
        this.css = cssFile;
        return this;
    }

    public File getFaviconFile() {
        return new File(dirRoot, favico);
    }

    public WWWGenHandler setFaviconFile(String faviconFile) {
        this.favico = faviconFile;
        return this;
    }
    
	private String getHref(File root, File img, String title) {
		Template templ = ve.getTemplate("linkTempl.html");
		StringWriter writer = new StringWriter();
		VelocityContext context = new VelocityContext();
		context.put("href", FileUtil.getRelativePath(root, img));
		context.put("title", title);
		templ.merge(context, writer);
		return writer.toString();
	}
	private String getVidHref(File root, File img, HashMap<String, Integer> imgFilesMap) {
		Template templ = ve.getTemplate("videoTempl.html");
		StringWriter writer = new StringWriter();
		VelocityContext context = new VelocityContext();
        File thumbimg = new File(img.getAbsolutePath() + ".png");
        ImageUtil.makeThumbs(new FileVideoIconSupplier(img), thumbimg, 160);
		context.put("href", FileUtil.getRelativePath(root, img));
		context.put("hrefidx", imgFilesMap.get(img.getName()));
		templ.merge(context, writer);
		return writer.toString();
	}
    private String getImgHref(File root, File img, HashMap<String, Integer> imgFilesMap, JsonArray imgFiles) {
		Template templ = ve.getTemplate("imgTempl.html");
		StringWriter writer = new StringWriter();
		VelocityContext context = new VelocityContext();
		
		context.put("href", FileUtil.getRelativePath(root, img));
		context.put("hrefidx", imgFilesMap.get(img.getName()));
		
        context.put("useThumb", false);
        try {
            String imgComment = imgFiles.getJsonObject(imgFilesMap.get(img.getName())).getString("comment");
            context.put("imgComment", imgComment);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        context.put("hrefthumb", FileUtil.getRelativePath(root, img));
		templ.merge(context, writer);
		return writer.toString();
	}
	private String pagination(int page, int pages, File dir) {
		StringWriter writer = new StringWriter();
		if (pages > 1) {
			Template nav = ve.getTemplate("paginationTempl.html");
			VelocityContext context = new VelocityContext();
			context.put("pages", paginationItems(page, pages, dir));
			nav.merge(context, writer);
		}
		return writer.toString();
	}
	private String paginationItems(int page, int pages, File dir) {
		//File file2 = new File(dir, "index"+)
		StringWriter writer = new StringWriter();
		Template nav = ve.getTemplate("paginationItemTempl.html");
		
		if (pages > 0) {
			int li, ri;
			if (pages <= 10) {
				li = -1;
				ri = pages;
			} else {
				li = page - 3 - 1;
				if (li < 0) {
					li = 0;
				}
				ri = li + 5;
			}
			
			for (int i = 0; i < pages; i++) {
				String is = "";
				if (i != 0) {
					is = "" + (i+1);
				}
				VelocityContext context = new VelocityContext();
				if (i != page) {
					context.put("href", "index" + is + ".html");
				}
				context.put("title", "" + (i+1));
				nav.merge(context, writer);
				writer.flush();
				if (i < li) {
					i = li;
					writer.append("...");
				} else if (i > ri && i < pages - 1) {
					i = pages - 2;
					writer.append("...");
				}
			}				
		}
		return writer.toString();
	}
    
	public static Menu makeMenu (String id, File file, DirProperties dp) {
		if (id == null) {
			id = file.getName();
		}
		Menu m = new Menu().setId(id).setFile(file);
		if (dp != null) {
			m.setDirProperties(dp.clone());
            m.getDirProperties().setModify(false);
		}
		File indexFile = new File(file, "index.html");
		if (indexFile.exists()) {
			m.getDirProperties().setHasIndex(true);
			if (indexFile.lastModified() < file.lastModified()) {
				m.getDirProperties().setModify(true);
			} else {
				for (File f : file.listFiles(new NotFilenameFilter("index"))) {
					if (indexFile.lastModified() < f.lastModified()) {
						//System.err.println("+++++++"+f+":"+indexFile);
                        if (f.isDirectory()) {
                            for (File ff: file.listFiles(new NotFilenameFilter("index"))) {
                                if (indexFile.lastModified() < ff.lastModified()) {
                                    m.getDirProperties().setModify(true);
                                    break;
                                }
                            }
                        } else {
                            m.getDirProperties().setModify(true);
                        }
						break;
					}
				}
				
			}
		} else {
			m.getDirProperties().setHasIndex(!true);
		}
		
		m.setIconPath(getExists(file, ".icon", ICONS_EXT));
		m.setTitle(getText(FileUtil.getExistsFile(file, ".title", TXT_EXT)));
		m.setDescription(getText(FileUtil.getExistsFile(file, ".description", TXT_EXT)));
		m.setSinglePage(getExists(file, ".single", TXT_EXT) != null);
		
		m.getDirProperties().setOrder(getText(
				FileUtil.getExistsFile(file, ".order", TXT_EXT),
				m.getDirProperties().getOrder()));
		System.err.println("---- upd--------"+m.getDirProperties().isModify() + ":" + m.isModify() + ":" + m.isModifyRootSibling() + ":" + file+":"+m.getDirProperties().getOrder());
		
		m.getDirProperties().setContentCntMax(
				getTextInt(
						FileUtil.getExistsFile(file, ".contentCntMax", TXT_EXT),
						m.getDirProperties().getContentCntMax()
						));
		m.getDirProperties().setContentCntMaxImg(
				getTextInt(
						FileUtil.getExistsFile(file, ".contentCntMaxImg", TXT_EXT),
						m.getDirProperties().getContentCntMaxImg()
						));
		return m;
	}
    
	public static String getText (File f, String name, String ... extensions) {
		File fl = FileUtil.getExistsFile(f, name, extensions);
		if (fl != null) {
			getText(fl);
		}
		return null;
	}
	public static String getText(File f) {
		return getText(f, null);
	}
	public static int getTextInt(File f, int defVal) {
		String s = getText(f);
		//System.err.println("getTextInt>"+defVal+":"+s+":"+f);
		if (s != null) {
			try {
				int ret = Integer.parseInt(s);
				if (ret > 2 && ret < 1000) {
					return ret;
				}
			} catch (Exception e) {}
		}
		return defVal;
	}
	public static String getText(File f, String defVal) {
		if (f != null) {
			try {
				return new String(Files.readAllBytes(f.toPath()), "UTF-8");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return defVal;
	}
	public static String getExists (File f, String name, String ... extensions) {
		File fl = FileUtil.getExistsFile(f, name, extensions);
		if (fl != null) {
			return fl.getName();
		}
		return null;
	}
    
	public Comparator<File> getFileComparator(String order) {
		if (order != null) {
			if (order.toLowerCase().startsWith("asc")) {
			} else {
				return DSC;
			}
		}
		return ASC;
	}
	private static File[] sort(File[] fileArr, Comparator<File> comparator) {
		Arrays.sort(fileArr, comparator);
		return fileArr;
	}
    
	private void writeJs(File dir, JsonArray imgFiles) {
		Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
					new File(dir, "index.js")
					), "UTF-8"));
			try {
				writer.append("var imgFiles=").append(imgFiles.encodePrettily()).append(";");
			} finally {
				writer.close();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
    
	private String appendContent(HtmlContent pageContent, String content, String lastContent, String contentType) {
		if ("img".equals(contentType) &&  !"img".equals(lastContent)) {
			pageContent.content.append("<div class='photos'>");
		}
		if ("img".equals(lastContent) && ! "img".equals(contentType)) {
			pageContent.content.append("</div>");
		}
		if ("vid".equals(contentType) &&  !"vid".equals(lastContent)) {
			pageContent.content.append("<div class='videos'>");
		}
		if ("vid".equals(lastContent) && ! "vid".equals(contentType)) {
			pageContent.content.append("</div>");
		}
		pageContent.content.append(content);
		if ("text".equals(contentType)) {
			pageContent.content.append("<br>");
		}
		return contentType;
	}
    
	private String makeBreadcrumbs(final Menu m, final String prevpath) {
		if (m == null) return "";
		Menu parent = m.getParent();
		if (parent == null) {
			return "";
		}
		String path = "";
		if (prevpath != null) {
			path = prevpath;
		}
		StringWriter writer = new StringWriter();
		writer.append(makeBreadcrumbs(parent, path + "../"));
		
		Template template = ve.getTemplate("breadcrumbTempl.html");
		VelocityContext context = new VelocityContext();
		context.put("title", m.getAnyTitle());
		if (prevpath != null) {
			context.put("href", path + "index.html");
		} else {
			context.put("hrefcur", path);
        }
		
		template.merge(context, writer);
		
		return writer.toString();
	}
    
	private String applyTemplate(Template nav, HtmlContent content, Menu m) {
		String title = m.getTitle();
		if (title == null) {
			title = m.getId();
		}
		return applyTemplate(nav, content, m, title, FileUtil.getRelativePath(content.file, m.getContentFile()));
	}
	private String applyTemplateDir(Template nav, HtmlContent content, Menu m) {
		return applyTemplate(nav, content, m, "&uArr;", FileUtil.getRelativePath(content.file, new File(m.getFile(), "index.html")));
	}
	private String applyTemplate(Template nav, HtmlContent content, Menu m, String title, String href) {
		StringWriter writer = new StringWriter();
		VelocityContext context = new VelocityContext();
		context.put("href", href);
		context.put("title", title);
		context.put("iconname", m.getIconPath());
		nav.merge(context, writer);
		return writer.toString();
	}
    
	private HtmlContent appendMenus(HtmlContent content, VelocityContext context) {
		Menu mainMenu = Menu.getRoot(content.menu);
		{
			Template nav = ve.getTemplate("navcolumnTempl.html");
			StringBuilder str = new StringBuilder();
			for (Menu m : mainMenu.getMenus()) {
				//System.out.println("++++++++++++"+m.getId()+":"+content.root.hasParent(m)+":"+content.root.getId()+":"+getHref(content.root.getFile(), m.getContentFile()));
				str.append(applyTemplate(nav, content, m)).append("\n");
			}
			context.put("navcolumns", str.toString());
		}
		Menu parent = null;
		if (content.menu != null) {
			parent = content.menu.getParent();
		}
		if (parent != null && mainMenu != parent) {
			Template subnav = ve.getTemplate("subnavcolumnTempl.html");
			StringBuilder str = new StringBuilder();
			if (parent.getMenus().size() > 0 && parent.getLevel() > 1) {
				str.append(applyTemplateDir(subnav, content, parent)).append("\n");
			}
			for(Menu m : parent.getMenus()) {
				//System.out.println("----------"+m.getId()+/*":"+m.getContentFile()+*/":"+getHref(content.menu.getFile(), m.getContentFile(), m.getAnyTitle()));
				str.append(applyTemplate(subnav, content, m)).append("\n");
			}			
			context.put("subnavcolumns", str.toString());
		}
		return content;
	}
    
	private void write(HtmlContent content) {
        Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(content.file), "UTF-8"));
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(">>>>>>>>>>>"+content.file);
		Template template = ve.getTemplate("indexTempl.html");
		VelocityContext context = new VelocityContext();
        context.put("useThumb", false);

		appendMenus(content, context);
		context.put("fakeimg", FileUtil.getRelativePath(content.file, getLogoFile())); 
		
		context.put("pageContent", content.content.toString());
		context.put("breadcrumbs", makeBreadcrumbs(content.menu, null));
		context.put("title", content.menu.getFullTitle());
		
		context.put("cssName", FileUtil.getRelativePath(content.file, getCssFile()));
		
		Menu mainMenu = Menu.getRoot(content.menu);
		if (mainMenu.getContentFile() != null) {
			context.put("navLogoHref", FileUtil.getRelativePath(content.file, mainMenu.getContentFile() ));
		} else
		if (mainMenu.getMenus().size() > 0) {
			Menu mainMenuFirst = mainMenu.getMenus().get(0);
			context.put("navLogoHref", FileUtil.getRelativePath(content.file, mainMenuFirst.getContentFile() ));
		}
		if (getLogoFile().exists()) {
			context.put("navLogo", FileUtil.getRelativePath(content.file, getLogoFile()));
		}
		context.put("favicon", FileUtil.getRelativePath(content.file, getFaviconFile()));
		if (writer != null) {
			template.merge(context, writer);
			try {
				writer.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		content.content = null;
	}
    
    private void makeImagesList(HashMap<String, Integer> imgFilesMap, JsonArray imgFiles, Menu root, FilenameFilter imgFilenameFilter, FilenameFilter vidFilenameFilter, Comparator<File> comparator) {
        File[] filesImg = sort(root.getFile().listFiles(
                new ComplexFilenameFilter(
                        imgFilenameFilter,
                        vidFilenameFilter
                )
        ), comparator);
        int imgFilesIdx = -1;
        for (File f : filesImg) {
            JsonObject jobj = new JsonObject();
            imgFiles.add(jobj);
            imgFilesIdx++;
            String imgComment = "&nbsp;";
            if (imgFilenameFilter.accept(f.getParentFile(), f.getName())) {
                try {
                    imgComment = UtilHtml.getCleanHtmlBodyContent(ImageUtil.getImageUserCommentString(f, "UTF-8"));
                    if (imgComment == null) {
                        imgComment = "&nbsp;";
                    }
                } catch (UnsupportedEncodingException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                jobj.put("tp", 1);
            } else {
                jobj.put("tp", 2);
            }
            jobj.put("comment", imgComment);
            jobj.put("name", f.getName());
            imgFilesMap.put(f.getName(), imgFilesIdx);
        }
        writeJs(root.getFile(), imgFiles);
    }
    private void writeContent(Menu root, HtmlContent content, File[] files, FilenameFilter contentFilenameFilter, FilenameFilter imgFilenameFilter, FilenameFilter vidFilenameFilter, Comparator<File> comparator) {
        int pages = files.length / root.getDirProperties().getContentCntMax();
        int contentCnt = 0;
        int contentCntImg = 0;
        int contentCntVid = 0;
        HashMap<String, Integer> imgFilesMap = new HashMap<>();
        JsonArray imgFiles = new JsonArray();

        makeImagesList(imgFilesMap, imgFiles, root, imgFilenameFilter, vidFilenameFilter, comparator);

        int page = 0;
        for (File f : files) {
            if (imgFilenameFilter.accept(f.getParentFile(), f.getName())) {
                contentCntImg++;
            } else if (contentFilenameFilter.accept(f.getParentFile(), f.getName())) {
                contentCnt++;
            } else if (vidFilenameFilter.accept(f.getParentFile(), f.getName())) {
                contentCntImg++; // ?????
            }
            if (contentCnt >= root.getDirProperties().getContentCntMax()
                    || contentCntImg >= root.getDirProperties().getContentCntMaxImg()
                    || contentCntVid >= root.getDirProperties().getContentCntMaxVid()) {

                contentCnt = 0;
                contentCntImg = 0;
                contentCntVid = 0;
                page++;
            }
        }
        if (contentCnt == 0 && contentCntImg == 0 && contentCntVid == 0) {
        } else {
            pages = page + 1;
        }
        page = 0;
        contentCnt = 0;
        contentCntImg = 0;
        contentCntVid = 0;
        HtmlContent pageContent = content;
        String lastContent = null;
        if (pages > 1) {
            lastContent = appendContent(pageContent, pagination(page, pages, root.getFile()), lastContent, "page");
        }
        for (File f : files) {
            if (imgFilenameFilter.accept(f.getParentFile(), f.getName())) {
                contentCntImg++;
                lastContent = appendContent(pageContent, getImgHref(root.getFile(), f, imgFilesMap, imgFiles), lastContent, "img");
            } else if (contentFilenameFilter.accept(f.getParentFile(), f.getName())) {
                contentCnt++;
                String text2append;
                if (isWinUrl(f)) {
                    text2append = getWinUrlText(f);
                } else {
                    text2append = getText(f);
                }
                lastContent = appendContent(pageContent, text2append, lastContent, "text");
            } else if (vidFilenameFilter.accept(f.getParentFile(), f.getName())) {
                contentCntImg++; //?????????
                lastContent = appendContent(pageContent, getVidHref(root.getFile(), f, imgFilesMap), lastContent, "img");
            }
            if (contentCnt >= root.getDirProperties().getContentCntMax()
                    || contentCntImg >= root.getDirProperties().getContentCntMaxImg()
                    || contentCntVid >= root.getDirProperties().getContentCntMaxVid()) {

                contentCnt = 0;
                contentCntImg = 0;
                contentCntVid = 0;

                lastContent = appendContent(pageContent, pagination(page, pages, root.getFile()), lastContent, "page");
                if (content != pageContent) {
                    write(pageContent);
                }
                page++;
                pageContent = new HtmlContent(root);
                pageContent.file = new File(root.getFile(), "index" + (page + 1) + ".html");
                lastContent = appendContent(pageContent, pagination(page, pages, root.getFile()), lastContent, "page");
            }
        }
        if (pageContent != null && content != pageContent) {
            lastContent = appendContent(pageContent, pagination(page, pages, root.getFile()), lastContent, "page");
            write(pageContent);
        }
        if (content != null) {
            write(content);
        }

    }
    
    private Menu makeRelativeMenu(File rootDir, File file) {
        String rootDirPath = rootDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (filePath.startsWith(rootDirPath)) {
            Menu root = makeMenu(".", rootDir, null);
            if (rootDir.equals(file)) {
                applyMenuContent(root, getContent(root, true, true));
            } else {
                ArrayList<File> parents = new ArrayList<>();
                if (file.isDirectory()) {
                    parents.add(file);
                }
                for (File f = file.getParentFile(); f != null && !f.equals(rootDir) ; f = f.getParentFile()) {
                    parents.add(f);
                }
                Menu p = root;
                int i = parents.size() - 1;
                for (; i >= 0; i--) {
                    File fromPath = parents.get(i);
                    
                    Menu pn = null;

                    Comparator<File> comparator = getFileComparator(p.getDirProperties().getOrder());
                    for(File f : sort(p.getFile().listFiles(dirFileFilter), comparator)) { 
                        if (f.isDirectory()) {
                            Menu m = makeMenu(null, f, p.getDirProperties());
                            //2
                            p.addMenu(m);
                            if (!f.equals(fromPath)) {
                                applyMenuContent(m, getContent(m, false, true));
                            } else {
                                pn = m;
                            }
                        }
                    }
                    if (pn != null) {
                        if (i == 0) {
                            applyMenuContent(pn, getContent(pn, true, true));
                        } else {
                            applyMenuContent(pn, getContent(pn, false, false));
                        }
                    }
                    
                    p = pn;
                }
                return p;
            }
        }
        return null;
    }
    
    private Menu applyMenuContent(Menu m, HtmlContent cnt) {
        if (cnt != null) {
            m.setContentFile(cnt.file);
        }
        return m;
    }
    protected FilenameFilter getContentFilenameFilter() {
		FilenameFilter contentFilenameFilter = new ComplexFilenameFilter(
                new StartWithFilenameFilter("", ".url"),
                new StartWithFilenameFilter("content", ".html"),
                new StartWithFilenameFilter("_content", ".html")
        );
        return contentFilenameFilter;
    }
    protected FilenameFilter getImgFilenameFilter() {
        return new StartWithFilenameFilter("img-", ".jpg");
    }
    protected FilenameFilter getVidFilenameFilter() {
        return new StartWithFilenameFilter("vid-", ".mp4");
    }
    
    public HtmlContent getContent(Menu root, boolean writeIt, boolean addSubMenu) {
        //System.err.println("+++++++++"+writeIt+":"+root.getFile().getAbsolutePath());
        HtmlContent content = null;
        
        Comparator<File> comparator = getFileComparator(root.getDirProperties().getOrder());
        
        HtmlContent subContent = null;
        for(File f : sort(root.getFile().listFiles(dirFileFilter), comparator)) { 
            if (f.isDirectory()) {
                Menu m = makeMenu(null, f, root.getDirProperties());
                //1
                if (addSubMenu) {
                    root.addMenu(m);
                }
                HtmlContent cnt = getContent(m, false, addSubMenu);
                if (cnt != null) {
                    applyMenuContent(m, cnt);
                }
                if (subContent == null) {
                    subContent = cnt;
                }
                if (!addSubMenu && subContent != null) {
                    break;
                }
            }
        }
        
		FilenameFilter contentFilenameFilter = getContentFilenameFilter();
		FilenameFilter imgFilenameFilter = getImgFilenameFilter();
		FilenameFilter vidFilenameFilter = getVidFilenameFilter();
		File[] files = sort(root.getFile().listFiles(
				new ComplexFilenameFilter(
						contentFilenameFilter,
						imgFilenameFilter,
						vidFilenameFilter
						)
			), comparator);
        if (files.length > 0) {
            content = new HtmlContent(root);
            content.file = new File(root.getFile(), "index.html");
            
            if(writeIt && (root.isModifyRootSibling() || force)) {
                writeContent(root, content, files, contentFilenameFilter, imgFilenameFilter, vidFilenameFilter, comparator);
            }
            
        }
        
        if (content == null) {
            HtmlContent emptyContent = new HtmlContent(root);
            emptyContent.file = new File(root.getFile(), "index.html");
            if (subContent == null) {
                content = emptyContent;
            } else {
                content = subContent;
            }
            if(writeIt && (root.isModifyRootSibling() || force)) {
                for (Menu m : root.getMenus()) {
                    File f = m.getContentFile();
                    if (f == null) {
                        f = m.getFile();
                    }
                    appendContent(emptyContent, getHref(root.getFile(), f, m.getAnyTitle()), "text", "text");
                }
                applyMenuContent(emptyContent.menu, content);
                write(emptyContent);
            }
        }
        
        return content;
    }
    
    @Override
    public void handle(RoutingContext ctx) {
        String path = ctx.request().path();
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WWWGenHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (path.endsWith(".html")) {
            path = path.substring(wpathDelim.length());
            File f = new File(dirRoot, path);
            makeRelativeMenu(dirRoot, f.getParentFile());
        }
        ctx.next();
    }
    public void init() {
        /*
        Menu root = makeMenu(".", dirRoot, null);
		fillMenu(root, false);
        fillMenu(root, !false);
                */
    }
    /*
	public HtmlContent fillMenu(Menu root, boolean writeContent) {
		Comparator<File> comparator = getFileComparator(root.getDirProperties().getOrder());
		
		HtmlContent content = null;
		HtmlContent subcontent = null;
		if (!writeContent) {
			for(File f : sort(root.getFile().listFiles(dirFileFilter), comparator)) { 
				if (f.isDirectory()) {
					root.addMenu(makeMenu(null, f, root.getDirProperties()));
				}
			}
		}
		for(Menu m : root.getMenus()) {
			HtmlContent cnt = fillMenu(m, writeContent);
			if (subcontent == null) {
				subcontent = cnt;
			}
		}
		
		int contentCnt = 0;
		int contentCntImg = 0;
		int contentCntVid = 0;
		FilenameFilter contentFilenameFilter = getContentFilenameFilter();
		FilenameFilter imgFilenameFilter = getImgFilenameFilter();
		FilenameFilter vidFilenameFilter = getVidFilenameFilter();
		File[] files = sort(root.getFile().listFiles(
				new ComplexFilenameFilter(
						contentFilenameFilter,
						imgFilenameFilter,
						vidFilenameFilter
						)
			), comparator);
		int pages = files.length / root.getDirProperties().getContentCntMax();
//		if (files.length % contentCntMax > 0) {
//			pages++;
//		}
		JsonArray imgFiles = null;
		HashMap<String, Integer> imgFilesMap = new HashMap<>(); 
		if (writeContent) {
			File[] filesImg = sort(root.getFile().listFiles(
					new ComplexFilenameFilter(
							imgFilenameFilter,
							vidFilenameFilter
							)
				), comparator);
			imgFiles = new JsonArray();
			int imgFilesIdx = -1;
			for (File f: filesImg) {
				JsonObject jobj = new JsonObject();
				imgFiles.add(jobj);
				imgFilesIdx++;
				String imgComment = "&nbsp;";
				if (imgFilenameFilter.accept(f.getParentFile(), f.getName())) {
					try {
						imgComment = UtilHtml.getCleanHtmlBodyContent(ImageUtil.getImageUserCommentString(f, "UTF-8"));
						if (imgComment == null) {
							imgComment = "&nbsp;";
						}
					} catch (UnsupportedEncodingException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					jobj.put("tp", 1); 
				} else {
					jobj.put("tp", 2); 
				}
				jobj.put("comment", imgComment);
				jobj.put("name", f.getName());
				imgFilesMap.put(f.getName(), imgFilesIdx);
			}
			writeJs(root.getFile(), imgFiles);
		}
		int page = 0;
		for(File f : files) {
			if (imgFilenameFilter.accept(f.getParentFile(), f.getName())) {
				contentCntImg++;
			} else if (contentFilenameFilter.accept(f.getParentFile(), f.getName())) {
				contentCnt++;
			} else if (vidFilenameFilter.accept(f.getParentFile(), f.getName())) {
				contentCntImg++; // ?????
			}
			if (contentCnt >= root.getDirProperties().getContentCntMax() || 
				contentCntImg >= root.getDirProperties().getContentCntMaxImg() ||
				contentCntVid >= root.getDirProperties().getContentCntMaxVid()
				) {
				
				contentCnt = 0;
				contentCntImg = 0;
				contentCntVid = 0;
				page++;
			}
		}
		if (contentCnt == 0 && contentCntImg == 0 && contentCntVid == 0) {
		} else {
			pages = page + 1;
		}
		
		page = 0;
		contentCnt = 0;
		contentCntImg = 0;
		contentCntVid = 0;
		HtmlContent pageContent = null;
		String lastContent = null;
		for(File f : files) {
			
			if (content == null) {
				pageContent = new HtmlContent(root);
				pageContent.file = new File(root.getFile(), "index.html");
				content = pageContent;
				if (!writeContent) {
					break;
				}
				if(!root.isModify()) {
					if (!force) {
						return applyContent(root, content);
					}
				}
				if (pages > 1) {
					lastContent = appendContent(pageContent, pagination(page, pages, root.getFile()), lastContent, "page");
				}
			}
			if (writeContent) {
				if (imgFilenameFilter.accept(f.getParentFile(), f.getName())) {
					contentCntImg++;
					lastContent = appendContent(pageContent, getImgHref(root.getFile(), f, imgFilesMap, imgFiles), lastContent, "img");
				} else
				if (contentFilenameFilter.accept(f.getParentFile(), f.getName())) {
					contentCnt++;
					lastContent = appendContent(pageContent, getText(f), lastContent, "text");
				} else
				if (vidFilenameFilter.accept(f.getParentFile(), f.getName())) {
					contentCntImg++; //?????????
					lastContent = appendContent(pageContent, getVidHref(root.getFile(), f, imgFilesMap), lastContent, "img");
				}
				if (contentCnt >= root.getDirProperties().getContentCntMax() || 
					contentCntImg >= root.getDirProperties().getContentCntMaxImg() ||
					contentCntVid >= root.getDirProperties().getContentCntMaxVid() 
					) {
					
					contentCnt = 0;
					contentCntImg = 0;
					contentCntVid = 0;

					lastContent = appendContent(pageContent, pagination(page, pages, root.getFile()), lastContent, "page");
					if (content != pageContent) {
						write(pageContent);
					}
					page++;
					pageContent = new HtmlContent(root);
					pageContent.file = new File(root.getFile(), "index" + (page+1) + ".html");
					lastContent = appendContent(pageContent, pagination(page, pages, root.getFile()), lastContent, "page");
				}
			}
		}
		if (writeContent) {
			if (pageContent != null && content != pageContent) {
				lastContent = appendContent(pageContent, pagination(page, pages, root.getFile()), lastContent, "page");
				write(pageContent);
			}
			if (content != null) {
				write(content);
			}
		}
		HtmlContent emptyContent = null;
		if (content == null) {
			emptyContent = new HtmlContent(root);
			emptyContent.file = new File(root.getFile(), "index.html");
			for (Menu m : root.getMenus()) {
				File f = m.getContentFile();
				if (f == null) {
					f = m.getFile();
				}
				appendContent(emptyContent, getHref(root.getFile(), f, m.getAnyTitle()), "text", "text");
			}
		}
		if (content == null && subcontent != null) {
			content = subcontent;
		}
		if (content == null) {
			content = emptyContent;
			if(!root.isModify()) {
				if (!force) {
					return applyContent(root, content);
				}
			}
		}
		if (writeContent && emptyContent != null) {
			lastContent = appendContent(emptyContent, "\n", null, "text");
			write(emptyContent);
		}
		//TODO
		// up menu
		// selected index - the first
		
		//root.getMenus().clear();
		return applyContent(root, content);
	}
	private HtmlContent applyContent(Menu root, HtmlContent content) {
		root.setContentFile(content.file);
		return content;
	}
*/    
}
