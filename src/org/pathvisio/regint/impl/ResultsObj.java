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

import java.util.HashMap;
import java.util.Map;

import org.bridgedb.Xref;
import org.pathvisio.regint.RegIntPlugin;

/**
 * Results object containing all interaction that contain the selected
 * {@link Xref}. Created by the findInteractions method in {@link RegIntPlugin}.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class ResultsObj {

	private Xref selectedXref;
	private Map<Xref, Interaction> regulatorMap;
	private Map<Xref, Interaction> targetMap;

	public ResultsObj(Xref xref) {
		selectedXref = xref;
		regulatorMap = new HashMap<Xref, Interaction>();
		targetMap = new HashMap<Xref, Interaction>();
	}

	/**
	 * @return a map of the regulators targeting the selected Xref, and the
	 *         {@link Interaction}s
	 */
	public Map<Xref, Interaction> getRegulatorMap() {
		return regulatorMap;
	}

	/**
	 * @return a map of the targets regulated by the selected Xref, and the
	 *         {@link Interaction}s
	 */
	public Map<Xref, Interaction> getTargetMap() {
		return targetMap;
	}

	public Xref getSelectedXref() {
		return selectedXref;
	}
}
