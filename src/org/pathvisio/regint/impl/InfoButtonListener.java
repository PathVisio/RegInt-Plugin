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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import org.bridgedb.Xref;
import org.pathvisio.regint.RegIntPlugin;

/**
 * Called when the (i) button next to the regulators/targets in the
 * {@link RegIntTab} is clicked.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class InfoButtonListener implements MouseListener {

	private Xref xref;
	private ResultsObj results;
	private RegIntPlugin plugin;

	public InfoButtonListener(Xref xref, ResultsObj results, RegIntPlugin plugin) {
		this.xref = xref;
		this.results = results;
		this.plugin = plugin;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		plugin.updateBackpage(xref, results);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}
}
