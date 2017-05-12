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

public class DirProperties {
	private int contentCntMax = 2;
	private int contentCntMaxImg = 2;
	private int contentCntMaxVid = 2;
	private String order;
	private boolean singlePage;
	private boolean modify;
	public boolean isModify() {
		return modify;
	}
	public void setModify(boolean modify) {
		this.modify = modify;
	}
	public DirProperties clone() {
		DirProperties ret = new DirProperties();
		ret.contentCntMax = contentCntMax;
		ret.contentCntMaxImg = contentCntMaxImg;
		ret.order = order;
		ret.singlePage = singlePage;
		ret.modify = modify;
		return ret;
	}
	public int getContentCntMax() {
		return contentCntMax;
	}
	public void setContentCntMax(int contentCntMax) {
		this.contentCntMax = contentCntMax;
	}
	public int getContentCntMaxImg() {
		return contentCntMaxImg;
	}
	public void setContentCntMaxImg(int contentCntMaxImg) {
		this.contentCntMaxImg = contentCntMaxImg;
	}
	public String getOrder() {
		return order;
	}
	public void setOrder(String order) {
		this.order = order;
	}
	public boolean isSinglePage() {
		return singlePage;
	}
	public void setSinglePage(boolean singlePage) {
		this.singlePage = singlePage;
	}
	public int getContentCntMaxVid() {
		return contentCntMaxVid;
	}
	public void setContentCntMaxVid(int contentCntVid) {
		this.contentCntMaxVid = contentCntVid;
	}
}