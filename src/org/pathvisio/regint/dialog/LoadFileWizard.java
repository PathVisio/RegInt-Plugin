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

package org.pathvisio.regint.dialog;

import org.pathvisio.regint.RegIntPlugin;

import com.nexes.wizard.Wizard;

/**
 * The wizard dialog used for loading interaction files
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class LoadFileWizard extends Wizard {

	private FilePage fpd;
	private ColumnPage cpd;
	private ImportPage ipd;

	public LoadFileWizard(RegIntPlugin plugin) {
		fpd = new FilePage(plugin);
		cpd = new ColumnPage(plugin);
		ipd = new ImportPage(plugin);

		getDialog().setTitle("Interaction file loading wizard");

		registerWizardPanel(fpd);
		registerWizardPanel(cpd);
		registerWizardPanel(ipd);

		setCurrentPanel(FilePage.IDENTIFIER);
	}

	public FilePage getFilePage() {
		return fpd;
	}

	public ColumnPage getColumnPage() {
		return cpd;
	}

	public ImportPage getImportPage() {
		return ipd;
	}
}