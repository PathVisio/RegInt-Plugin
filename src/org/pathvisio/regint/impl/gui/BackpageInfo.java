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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bridgedb.AttributeMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.util.Utils;
import org.pathvisio.gui.BackpageTextProvider.BackpageHook;
import org.pathvisio.regint.impl.ResultsObj;

/**
 * The backpage information class from PathVisio, modified to use regulatory
 * interactions loaded by the RI-plugin.
 * 
 * @author Stefan
 */
public class BackpageInfo implements BackpageHook {
	private ResultsObj results;
	private final AttributeMapper attributeMapper;

	public BackpageInfo(AttributeMapper attributeMapper, ResultsObj results) {
		this.results = results;
		this.attributeMapper = attributeMapper;
	}

	public String getHtml(PathwayElement e) {
		Xref xref = e.getXref();
		String text = "";
		String type;
		if (results.getRegulatorMap().containsKey(xref)) {
			type = "Regulator";
		} else {
			type = "Target";
		}
		// type will be displayed in the header, make either "Regulator" or
		// "Target";
		text += "<H1>" + type + " information</H1>";

		if (e.getXref().getId() == null || "".equals(e.getXref().getId())) {
			text += "<font color='red'>Invalid annotation: missing identifier.</font>";
			return text;
		}

		try {
			StringBuilder bpInfo = new StringBuilder("<TABLE border = 1>");

			Map<String, Set<String>> attributes = null;
			if (e.getXref().getDataSource() != null) {
				attributes = attributeMapper.getAttributes(e.getXref());
			} else {
				attributes = new HashMap<String, Set<String>>();
			}
			String[][] table;

			table = new String[][] { { "ID", e.getXref().getId() },
					{ "Symbol", Utils.oneOf(attributes.get("Symbol")) },
					{ "Synonyms", Utils.oneOf(attributes.get("Synonyms")) },
					{ "Description", Utils.oneOf(attributes.get("Description")) },
					{ "Chr", Utils.oneOf(attributes.get("Chromosome")) }, };

			for (String[] row : table) {
				if (!(row[1] == null)) {
					bpInfo.append("<TR><TH>");
					bpInfo.append(row[0]);
					bpInfo.append(":<TH>");
					bpInfo.append(row[1]);
				}
			}
			bpInfo.append("</TABLE>");
			text += bpInfo.toString();
		} catch (IDMapperException ex) {
			text += "Exception occurred, see log for details</br>";
			Logger.log.error("Error fetching backpage info", ex);
		}
		return text;
	}
}
