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

package org.pathvisio.regint.impl.gui;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.gui.BackpageTextProvider.BackpageHook;
import org.pathvisio.regint.RegIntPlugin;
import org.pathvisio.regint.impl.ResultsObj;
import org.pathvisio.regint.impl.preferences.RegIntPreferences;

/**
 * The class that displays the table of files that the selected interaction
 * occurs in.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class BackpageFileTable implements BackpageHook {
	private ResultsObj results;
	private RegIntPlugin plugin;
	private Set<Xref> crfs;

	public BackpageFileTable(ResultsObj results, RegIntPlugin plugin) {
		this.results = results;
		this.plugin = plugin;
	}

	@Override
	public String getHtml(PathwayElement e) {
		Xref selectedXref = results.getSelectedXref();
		Xref xref = e.getXref();
		String text = "";
		text += "<H1>" + e.getTextLabel() +" (" + xref.getDataSource().getSystemCode() + ": " + xref.getId() + ")</H1>";
		text += "<table border=\"1\">";
		try {
			DataSource[] usedDataSourceArray = new DataSource[plugin.getUsedDataSources().size()];
			usedDataSourceArray = plugin.getUsedDataSources().toArray(usedDataSourceArray);
			crfs = new HashSet<Xref>();
			crfs.add(selectedXref);
			for (IDMapper mapper : plugin.getDesktop().getSwingEngine().getGdbManager().getCurrentGdb().getMappers()) {
				Set<Xref> someXrefs = mapper.mapID(selectedXref, usedDataSourceArray);
				crfs.addAll(someXrefs);
			}
		} catch (IDMapperException idme) {
			idme.printStackTrace();
		}
		for (Xref xr : crfs) {
			if (results.getRegulatorMap() != null && !results.getRegulatorMap().isEmpty()
					&& results.getRegulatorMap().containsKey(xref)) {
				if (results.getRegulatorMap().get(xref).getTarget().equals(xr)) {
					for (File intFile : results.getRegulatorMap().get(xref).getFiles()) {
						if (RegIntPreferences.getPreferences().getSelectedIntFiles().contains(intFile)) {
							text += "<tr><td>From file: </td><td>" + intFile.getName() + "</td></tr>";
						}
					}
				}
			}
			if (results.getTargetMap() != null && !results.getTargetMap().isEmpty()
					&& results.getTargetMap().containsKey(xref)) {
				if (results.getTargetMap().get(xref).getRegulator().equals(xr)) {
					for (File intFile : results.getTargetMap().get(xref).getFiles()) {
						if (RegIntPreferences.getPreferences().getSelectedIntFiles().contains(intFile)) {
							text += "<tr><td>From file: </td><td>" + intFile.getName() + "</td></tr>";
						}
					}
				}
			}
		}
		text += "</table>";
		return text;
	}

}
