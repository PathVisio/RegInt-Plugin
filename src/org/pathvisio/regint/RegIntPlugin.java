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

package org.pathvisio.regint;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.core.ApplicationEvent;
import org.pathvisio.core.Engine.ApplicationEventListener;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.view.GeneProduct;
import org.pathvisio.core.view.SelectionBox.SelectionEvent;
import org.pathvisio.core.view.SelectionBox.SelectionListener;
import org.pathvisio.core.view.VPathway;
import org.pathvisio.core.view.VPathwayElement;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;
import org.pathvisio.regint.dialog.LoadFileWizard;
import org.pathvisio.regint.dialog.PreferenceDialog;
import org.pathvisio.regint.impl.Interaction;
import org.pathvisio.regint.impl.ResultsObj;
import org.pathvisio.regint.impl.gui.RegIntTab;
import org.pathvisio.regint.impl.preferences.RegIntPreferences;
import org.pathvisio.regint.impl.util.ImportInformation;

/**
 * The RegulatoryInteractionPlugin that integrates regulatory interactions into
 * pathway analysis.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class RegIntPlugin implements Plugin, ApplicationEventListener, SelectionListener, ChangeListener {

	private PvDesktop desktop;
	private RegIntPlugin plugin;

	// menu items added by the plugin
	private JMenu regIntMenu;
	private JMenuItem menuLoad;
	private JMenuItem menuSettings;

	// components to create the tab
	private RegIntTab regIntTab;
	private JTabbedPane sidebarTabbedPane;

	// xref that the user selected in the pathway
	private PathwayElement selectedElem;

	// interaction files that are loaded by the user
	private ArrayList<File> interactionFiles;

	// interactions for all xrefs in the interaction files
	private Map<Xref, List<Interaction>> interactions;
	// all used datasources in the different interaction file
	private List<DataSource> usedDataSources;

	// import dialog data
	private List<ImportInformation> importInformationList;
	private ImportInformation currentFile;

	public void init(final PvDesktop desktop) {
		this.desktop = desktop;
		plugin = this;
		interactionFiles = new ArrayList<File>();
		interactions = new HashMap<Xref, List<Interaction>>();
		importInformationList = new ArrayList<ImportInformation>();
		usedDataSources = new ArrayList<DataSource>();
		regIntMenu = new JMenu("RegInt Plugin");
		registerMenuItems();
		createSidePanel();
	}

	private void registerMenuItems() {
		menuLoad = new JMenuItem("Load interaction file(s)");
		menuLoad.addActionListener(new MenuActionListener());
				
		menuSettings = new JMenuItem("Preferences");
		menuSettings.addActionListener(new PreferencesActionListener());
		
		regIntMenu.add(menuLoad);
		regIntMenu.add(menuSettings);
		desktop.registerSubMenu("Plugins", regIntMenu);
	}

	private void createSidePanel() {
		regIntTab = new RegIntTab(plugin);
		sidebarTabbedPane = desktop.getSideBarTabbedPane();
		desktop.getSwingEngine().getEngine().addApplicationEventListener(this);
		sidebarTabbedPane.add("RegInt Plugin", regIntTab);
		sidebarTabbedPane.addChangeListener(this);
	}

	class MenuActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			final LoadFileWizard lfw = new LoadFileWizard(plugin);
			lfw.showModalDialog(desktop.getSwingEngine().getFrame());
		}
	}

	private class PreferencesActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			PreferenceDialog pref = new PreferenceDialog(desktop, plugin);
			pref.setVisible(true);
		}
	}

	@Override
	public void done() {
		desktop.unregisterSubMenu("Plugins", regIntMenu);
		sidebarTabbedPane.remove(regIntTab);
	}

	@Override
	public void applicationEvent(ApplicationEvent e) {
		switch (e.getType()) {
		case VPATHWAY_CREATED: {
			StringBuffer geneIDs = new StringBuffer();
			((VPathway) e.getSource()).addSelectionListener(this);
			for (VPathwayElement o : ((VPathway) e.getSource()).getDrawingObjects()) {
				if (o instanceof GeneProduct) {
					String geneID = ((GeneProduct) o).getPathwayElement().getTextLabel();
					geneIDs.append(' ');
					geneIDs.append(geneID);
				}
			}
		}
			break;
		case VPATHWAY_DISPOSED: {
			((VPathway) e.getSource()).removeSelectionListener(this);
		}
		default:
			break;
		}
	}

	public void selectionEvent(SelectionEvent e) {
		if (interactions.size() > 0) {
			switch (e.type) {
			case SelectionEvent.OBJECT_ADDED:
				if (e.selection.size() == 1) {
					Iterator<VPathwayElement> it = e.selection.iterator();
					VPathwayElement o = it.next();

					if (o instanceof GeneProduct) {
						selectedElem = ((GeneProduct) o).getPathwayElement();
						if (selectedElem != null && selectedElem.getDataSource() != null) {
							if (sidebarTabbedPane.getSelectedComponent().equals(regIntTab)) {
								regIntTab.updatePathwayPanel(selectedElem);
								regIntTab.revalidate();
								regIntTab.repaint();
							}
						} else {
							regIntTab.setPathwayPanelText(
									"<html><br>&nbsp;&nbsp;&nbsp;&nbsp;DataNode does not have an identifier.</html>");
						}
					}
				}
				break;
			case SelectionEvent.SELECTION_CLEARED:
				selectedElem = null;
				regIntTab.setPathwayPanelText("<html><br>&nbsp;&nbsp;&nbsp;&nbsp;No Selection.</html>");
				regIntTab.revalidate();
				break;
			}
		} else {
			regIntTab.setPathwayPanelText("<html><br>&nbsp;&nbsp;&nbsp;&nbsp;No interaction files loaded.</html>");
		}
	}

	/**
	 * Looks through the interactions map to find interactions that contain the
	 * specified {@link Xref}.
	 * 
	 * @return a results object with the interactions containing the Xref
	 */
	public ResultsObj findInteractions(Xref currentXref) throws IDMapperException {
		ResultsObj results = null;
		String connectionString = PreferenceManager.getCurrent().get(GlobalPreference.DB_CONNECTSTRING_GDB);
		if (connectionString.equals("idmapper-pgdb:none")) {
			JOptionPane.showMessageDialog(desktop.getFrame(),
					"Please select a Gene Database first.\n(Data > Select Gene Database)");
		} else {
			DataSource[] usedDataSourceArray = new DataSource[getUsedDataSources().size()];
			usedDataSourceArray = getUsedDataSources().toArray(usedDataSourceArray);
			Set<Xref> crfs = new HashSet<Xref>();
			crfs.add(currentXref);
			for (IDMapper mapper : getDesktop().getSwingEngine().getGdbManager().getCurrentGdb().getMappers()) {
				Set<Xref> someXrefs = mapper.mapID(currentXref, usedDataSourceArray);
				crfs.addAll(someXrefs);
			}
			for (Xref xref : crfs) {
				if (interactions.containsKey(xref)) {
					results = checkInteractions(xref);
				}
			}
		}
		return results;
	}

	private ResultsObj checkInteractions(Xref xref) {
		ResultsObj results = new ResultsObj(xref);

		for (Interaction interaction : interactions.get(xref)) {
			for (File file : interaction.getFiles()) {
				if (isFileSelected(file)) {
					addInteractionPartners(interaction, xref, results);
				}
			}
		}
		return results;
	}

	private void addInteractionPartners(Interaction interaction, Xref xref, ResultsObj obj) {
		if (interaction.getRegulator().equals(xref)) {
			for (Interaction anInteraction : interactions.get(interaction.getTarget())) {
				if (anInteraction.getRegulator().equals(xref)) {
					obj.getTargetMap().put(interaction.getTarget(), anInteraction);
				}
			}
		} else if (interaction.getTarget().equals(xref)) {
			for (Interaction anInteraction : interactions.get(interaction.getRegulator())) {
				if (anInteraction.getTarget().equals(xref)) {
					obj.getRegulatorMap().put(interaction.getRegulator(), anInteraction);
				}
			}
		}
	}

	private boolean isFileSelected(File file) {
		if (RegIntPreferences.getPreferences().getSelectedIntFiles().contains(file)) {
			return true;
		}
		return false;
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		if (sidebarTabbedPane.getSelectedComponent().equals(regIntTab)) {
			if (selectedElem != null && selectedElem.getDataSource() != null) {
				regIntTab.updatePathwayPanel(selectedElem);
				regIntTab.revalidate();
				regIntTab.repaint();
			}
		}
	}

	// SETTERS & GETTERS

	public ArrayList<File> getIntFiles() {
		return interactionFiles;
	}

	public Map<Xref, List<Interaction>> getInteractions() {
		return interactions;
	}

	public void updateBackpage(final Xref xref, ResultsObj results) {
		regIntTab.updateBackpagePanel(xref, results);
	}

	public PvDesktop getDesktop() {
		return desktop;
	}

	public List<DataSource> getUsedDataSources() {
		return usedDataSources;
	}

	public void addUsedDataSource(DataSource ds) {
		if (!usedDataSources.contains(ds)) {
			usedDataSources.add(ds);
		}
	}

	public void removeUsedDataSource(DataSource ds) {
		if (usedDataSources.contains(ds)) {
			usedDataSources.remove(ds);
		}
	}

	public List<ImportInformation> getImportInformationList() {
		return importInformationList;
	}

	public void setImportInformationList(List<ImportInformation> importInformationList) {
		this.importInformationList = importInformationList;
	}

	public ImportInformation getCurrentFile() {
		return currentFile;
	}

	public void setCurrentFile(ImportInformation currentFile) {
		this.currentFile = currentFile;
	}
}
