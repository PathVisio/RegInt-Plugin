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

import org.bridgedb.Xref;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.gui.BackpageTextProvider.BackpageHook;
import org.pathvisio.regint.dialog.LoadFileWizard;
import org.pathvisio.regint.impl.ResultsObj;

/**
 * The class that displays the miscellaneous information about a regulatory
 * interaction, as loaded from an interaction file by {@link LoadFileWizard}.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class BackpageMiscInfo implements BackpageHook {
	private ResultsObj results;

	public BackpageMiscInfo(ResultsObj results) {
		this.results = results;
	}

	@Override
	public String getHtml(PathwayElement e) {
		Xref xref = e.getXref();
		String text = "";
		if (results.getRegulatorMap() != null && !results.getRegulatorMap().isEmpty()
				&& results.getRegulatorMap().containsKey(xref)) {
			text = "<H1>Miscellaneous Information</H1>" + results.getRegulatorMap().get(xref).getMiscInfo();
		} else if (results.getTargetMap() != null && !results.getTargetMap().isEmpty()
				&& results.getTargetMap().containsKey(xref)) {
			text = "<H1>Miscellaneous Information</H1>" + results.getTargetMap().get(xref).getMiscInfo();
		}
		if (!text.equals("<H1>Miscellaneous Information</H1>")) {
			return text;
		} else {
			return "";
		}
	}

}
