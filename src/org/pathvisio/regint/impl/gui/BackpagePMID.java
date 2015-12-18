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
import org.pathvisio.regint.impl.ResultsObj;

/**
 * The class that displays the PubMed ID of the selected interaction, which
 * functions as a linkout to the PubMed publication.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class BackpagePMID implements BackpageHook {
	private ResultsObj results;

	public BackpagePMID(ResultsObj results) {
		this.results = results;
	}

	@Override
	public String getHtml(PathwayElement e) {
		Xref xref = e.getXref();
		String text = "";
		if (results.getRegulatorMap() != null && !results.getRegulatorMap().isEmpty()
				&& results.getRegulatorMap().containsKey(xref)
				&& !results.getRegulatorMap().get(xref).getPMID().equals("")) {
			text = "<br /><a href=\"http://www.ncbi.nlm.nih.gov/pubmed?term="
					+ results.getRegulatorMap().get(xref).getPMID() + "\">PubMed</a>";
		}
		if (results.getTargetMap() != null && !results.getTargetMap().isEmpty()
				&& results.getTargetMap().containsKey(xref) && !results.getTargetMap().get(xref).getPMID().equals("")) {
			text = "<br /><a href=\"http://www.ncbi.nlm.nih.gov/pubmed?term="
					+ results.getTargetMap().get(xref).getPMID() + "\">PubMed</a>";
		}
		return text;
	}

}
