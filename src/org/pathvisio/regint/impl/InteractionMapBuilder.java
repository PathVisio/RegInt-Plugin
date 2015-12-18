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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JProgressBar;
import javax.swing.JTextArea;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.regint.RegIntPlugin;
import org.pathvisio.regint.dialog.LoadFileWizard;
import org.pathvisio.regint.impl.util.ImportInformation;

/**
 * The class that does the actual work on building a map of interactions. Used
 * by the {@link ImportPage} of {@link LoadFileWizard}.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class InteractionMapBuilder {
	// TODO: use this class after progress bar, flickering is fixed
	private RegIntPlugin plugin;
	private int progress;
	private JTextArea progressText;
	private JProgressBar progressSent;
	private ProgressKeeper pk;
	private final int PROGRESS_INTERVAL = 50;

	public InteractionMapBuilder(RegIntPlugin plugin, ProgressKeeper pk, JTextArea progressText,
			JProgressBar progressSent) {
		this.plugin = plugin;
		progress = 0;
		this.pk = pk;
		this.progressSent = progressSent;
		this.progressText = progressText;
	}

	public void addFile(ImportInformation importInformation) throws IOException, IDMapperException {

		progressText.append("Loading " + importInformation.getTxtFile().getName() + "...\n");
		Map<Xref, List<Interaction>> interactions = plugin.getInteractions();
		int RegId = importInformation.getIdColumnReg();
		int TarId = importInformation.getIdColumnTar();
		FileReader fr = new FileReader(importInformation.getTxtFile());
		BufferedReader in = new BufferedReader(fr);
		String line = new String();
		for (int i = 0; i < importInformation.getFirstDataRow(); i++) {
			in.readLine();
		}
		while ((line = in.readLine()) != null) {
			String[] str = line.split(importInformation.getDelimiter());
			if (str.length >= importInformation.getSampleMaxNumCols()) {
				String regulatorString = str[RegId];
				String targetString = str[TarId];

				if (!regulatorString.equals("") && !targetString.equals("")) {
					Xref regulator;
					DataSource dsReg;
					Xref target;
					DataSource dsTar;
					if (importInformation.isSyscodeFixedReg()) {
						dsReg = importInformation.getDataSourceReg();
					} else {
						dsReg = DataSource.getExistingBySystemCode(str[importInformation.getSyscodeColumnReg()]);
					}
					regulator = new Xref(regulatorString, dsReg);
					plugin.addUsedDataSource(dsReg);
					if (importInformation.isSyscodeFixedTar()) {
						dsTar = importInformation.getDataSourceTar();
					} else {
						dsTar = DataSource.getExistingBySystemCode(str[importInformation.getSyscodeColumnTar()]);
					}
					target = new Xref(targetString, dsTar);
					plugin.addUsedDataSource(dsTar);

					Interaction thisInteraction = new Interaction(regulator, target, importInformation.getTxtFile());

					if (importInformation.isPMIDColumnEnabled()) {
						String PMID = str[importInformation.getPMIDColumn()];
						thisInteraction.setPMID(PMID);
					}
					String miscInfo = "<table border=\"1\">";
					for (int i = 0; i < importInformation.getColNames().length; i++) {
						if (i != importInformation.getIdColumnReg() && i != importInformation.getIdColumnTar()) {
							if (!importInformation.isSyscodeFixedReg()
									&& i == importInformation.getSyscodeColumnReg()) {
								// do nothing
							} else if (!importInformation.isSyscodeFixedTar()
									&& i == importInformation.getSyscodeColumnTar()) {
								// do nothing
							} else if (importInformation.isPMIDColumnEnabled()
									&& i == importInformation.getPMIDColumn()) {
								// do nothing
							} else {
								miscInfo += "<tr><td>" + importInformation.getColNames()[i] + "</td><td>" + str[i]
										+ "</td></tr>";
							}
						}
					}
					miscInfo += "</table>";
					if (!miscInfo.equals("<table border=\"1\"></table>")) {
						thisInteraction.setMiscInfo(miscInfo);
					}

					DataSource[] usedDataSourceArray = new DataSource[plugin.getUsedDataSources().size()];
					usedDataSourceArray = plugin.getUsedDataSources().toArray(usedDataSourceArray);
					Set<Xref> crfsReg = new HashSet<Xref>();
					crfsReg.add(regulator);
					Set<Xref> crfsTar = new HashSet<Xref>();
					crfsTar.add(target);
					for (IDMapper aMapper : plugin.getDesktop().getSwingEngine().getGdbManager().getCurrentGdb()
							.getMappers()) {
						Set<Xref> someRegXrefs = aMapper.mapID(regulator, usedDataSourceArray);
						crfsReg.addAll(someRegXrefs);
						Set<Xref> someTarXrefs = aMapper.mapID(target, usedDataSourceArray);
						crfsTar.addAll(someTarXrefs);
					}

					boolean regulatorAlreadyInKeys = false;
					boolean targetAlreadyInKeys = false;
					boolean interactionAlreadyExists = false;
					Xref regulatorInKeys = null;
					Xref targetInKeys = null;
					for (Xref xrefReg : crfsReg) {
						if (interactions.containsKey(xrefReg)) {
							regulatorAlreadyInKeys = true;
							regulatorInKeys = xrefReg;
							for (Interaction anInteraction : interactions.get(xrefReg)) {
								if (crfsTar.contains(anInteraction.getTarget())) {
									interactionAlreadyExists = true;
									if (anInteraction.getPMID().equals("")) {
										anInteraction.setPMID(thisInteraction.getPMID());
									}
									anInteraction.addFile(importInformation.getTxtFile());
								}
							}
						}
					}
					for (Xref xrefTar : crfsTar) {
						if (interactions.containsKey(xrefTar)) {
							targetAlreadyInKeys = true;
							targetInKeys = xrefTar;
							for (Interaction anInteraction : interactions.get(xrefTar)) {
								if (crfsReg.contains(anInteraction.getRegulator())) {
									interactionAlreadyExists = true;
									if (anInteraction.getPMID().equals("")) {
										anInteraction.setPMID(thisInteraction.getPMID());
									}
									anInteraction.addFile(importInformation.getTxtFile());
								}
							}
						}
					}

					if (!interactionAlreadyExists) {
						if (regulatorAlreadyInKeys) {
							if (!targetAlreadyInKeys) {
								thisInteraction = new Interaction(regulatorInKeys, target,
										importInformation.getTxtFile());
								interactions.get(regulatorInKeys).add(thisInteraction);
								List<Interaction> intList = new ArrayList<Interaction>();
								intList.add(thisInteraction);
								interactions.put(target, intList);
							} else {
								thisInteraction = new Interaction(regulatorInKeys, targetInKeys,
										importInformation.getTxtFile());
								interactions.get(regulatorInKeys).add(thisInteraction);
								interactions.get(targetInKeys).add(thisInteraction);
							}
						} else {
							if (!targetAlreadyInKeys) {
								List<Interaction> intListReg = new ArrayList<Interaction>();
								intListReg.add(thisInteraction);
								interactions.put(regulator, intListReg);
								List<Interaction> intListTar = new ArrayList<Interaction>();
								intListTar.add(thisInteraction);
								interactions.put(target, intListTar);
							} else {
								thisInteraction = new Interaction(regulator, targetInKeys,
										importInformation.getTxtFile());
								List<Interaction> intList = new ArrayList<Interaction>();
								intList.add(thisInteraction);
								interactions.put(regulator, intList);
								interactions.get(targetInKeys).add(thisInteraction);
							}
						}
					}
				}
			}
			progress++;
			if (progress % PROGRESS_INTERVAL == 0) {
				progressSent.setValue(progress);
				pk.setProgress(progress);
			}
		}
		in.close();
		plugin.getIntFiles().add(importInformation.getTxtFile());
		progressText.append("Finished loading " + importInformation.getTxtFile().getName() + "\n");
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}
}
