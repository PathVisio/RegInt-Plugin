// RegInt Plugin for PathVisio
// Visualize regulatory interactions in the side panel of PathVisio
// Copyright 2015 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.pathvisio.regint.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bridgedb.Xref;

/**
 * Contains data about a biological interaction between a regulator and a
 * target.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class Interaction {

	private Xref regulator;
	private Xref target;
	private List<File> files = new ArrayList<File>();
	private String PMID = "";

	// TODO: maybe read misc info as a map -> key = column header, value = data
	private String miscInfo = "";

	public Interaction(Xref regulator, Xref target, File file) {
		this.regulator = regulator;
		this.target = target;
		files.add(file);
	}

	public Xref getRegulator() {
		return regulator;
	}

	public void setRegulator(Xref regulator) {
		this.regulator = regulator;
	}

	public Xref getTarget() {
		return target;
	}

	public void setTarget(Xref target) {
		this.target = target;
	}

	public List<File> getFiles() {
		return files;
	}

	public void addFile(File file) {
		if (!files.contains(file)) {
			this.files.add(file);
		}
	}

	public void removeFile(File file) {
		if (files.contains(file)) {
			files.remove(file);
		}
	}

	public String getPMID() {
		return PMID;
	}

	public void setPMID(String PMID) {
		this.PMID = PMID;
	}

	public String getMiscInfo() {
		return miscInfo;
	}

	public void setMiscInfo(String miscInfo) {
		this.miscInfo = miscInfo;
	}
}
