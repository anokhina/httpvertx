/*******************************************************************************
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
 *******************************************************************************/
package ru.org.sevn.jvert.wwwgen;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Menu {
	private String iconPath;
	private String title;
	private String description;
	private String id;
	private DirProperties dirProperties = new DirProperties();
	private boolean singlePage;
	private Menu parent;
	private File file;
	private File contentFile;
	private List<Menu> menus = new ArrayList<Menu>();
	public String getIconPath() {
		return iconPath;
	}
	public Menu setIconPath(String iconPath) {
		this.iconPath = iconPath;
		return this;
	}
	public String getFullTitle() {
        ArrayList<Menu> mpath = new ArrayList<>();
        Menu r = getRootPath(this, mpath);
        StringBuilder sb = new StringBuilder();
        if (mpath.size() > 1) {
            sb.append(r.getAnyTitle());
            sb.append(" ");
            if (mpath.size() > 2) {
                Menu m = mpath.get(mpath.size() - 2);
                sb.append(m.getAnyTitle());
                sb.append(" ");
            }
        }
        sb.append(this.getAnyTitle());
        return sb.toString();
    }
	public String getAnyTitle() {
		if(title == null) {
			return getId();
		}
		return title;
	}
	public String getTitle() {
		return title;
	}
	public Menu setTitle(String title) {
		this.title = title;
		return this;
	}
	public String getDescription() {
		return description;
	}
	public Menu setDescription(String description) {
		this.description = description;
		return this;
	}
	public String getId() {
		return id;
	}
	public Menu setId(String id) {
		this.id = id;
		return this;
	}
	public Menu getParent() {
		return parent;
	}
	protected Menu setParent(Menu parent) {
		this.parent = parent;
		return this;
	}
	public static Menu getRoot(Menu m) {
		if(m == null) return null;
		if (m.getParent() == null) {
			return m;
		}
		return getRoot(m.getParent());
	}
	public static Menu getRootPath(Menu m, List<Menu> menus) {
		if(m == null) return null;
        menus.add(m);
		if (m.getParent() == null) {
			return m;
		}
		return getRootPath(m.getParent(), menus);
	}
	public void delMenu(Menu m) {
		if (m.getParent() != null && m.getParent() == this) {
			m.setParent(null);
			menus.remove(m);
		}
	}
	public void addMenu(Menu m) {
		if (m.getParent() != null) {
			m.getParent().delMenu(m);
		}
		if (m.getParent() == null) {
			m.setParent(this);
			menus.add(m);
		}
	}
	public boolean hasParent(Menu m) {
		if (m == null || getParent() == null) {
			return false;
		} else
		if (getParent() == m) {
			return true;
		}
		return getParent().hasParent(m);
	}
	public List<Menu> getMenus() {
		return menus;
	}
	public File getFile() {
		return file;
	}
	public Menu setFile(File file) {
		this.file = file;
		return this;
	}
	public boolean isSinglePage() {
		return singlePage;
	}
	public Menu setSinglePage(boolean singlePage) {
		this.singlePage = singlePage;
		return this;
	}
	public Menu setContentFile(File file) {
		contentFile = file;
		return this;
	}
	public File getContentFile() {
		return contentFile;
	}
	public DirProperties getDirProperties() {
		return dirProperties;
	}
	public void setDirProperties(DirProperties dirProperties) {
		this.dirProperties = dirProperties;
	}
	public int getLevel() {
		if (parent == null) {
			return 0;
		}
		return 1 + parent.getLevel();
	}
	
}
