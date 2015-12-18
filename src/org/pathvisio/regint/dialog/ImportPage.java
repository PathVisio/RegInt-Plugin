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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.core.debug.StopWatch;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.core.util.ProgressKeeper.ProgressEvent;
import org.pathvisio.core.util.ProgressKeeper.ProgressListener;
import org.pathvisio.regint.RegIntPlugin;
import org.pathvisio.regint.impl.Interaction;
import org.pathvisio.regint.impl.InteractionMapBuilder;
import org.pathvisio.regint.impl.preferences.RegIntPreferences;
import org.pathvisio.regint.impl.util.ImportInformation;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.nexes.wizard.WizardPanelDescriptor;

/**
 * Third page of the {@link LoadFileWizard}, loads the interaction files
 * selected and configured in the {@link FilePage} and {@link ColumnPage}, and
 * uses {@link InteractionMapBuilder} to add them to the map of interactions.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class ImportPage extends WizardPanelDescriptor implements ProgressListener {
	public static final String IDENTIFIER = "IMPORT_PAGE";
	private final int PROGRESS_INTERVAL = 50;
	
	private RegIntPlugin plugin;
	
	public ImportPage(RegIntPlugin plugin) {
		super(IDENTIFIER);
		this.plugin = plugin;
	}

	public Object getNextPanelDescriptor() {
		return FINISH;
	}

	public Object getBackPanelDescriptor() {
		return ColumnPage.IDENTIFIER;
	}

	private JProgressBar progressSent;
	private JTextArea progressText;
	private ProgressKeeper pk;
	private JLabel lblTask;
	private StopWatch stopwatch;
	private int progress;

	@Override
	public void aboutToCancel() {
		// let the progress keeper know that the user pressed cancel.
		pk.cancel();
	}

	protected JPanel createContents() {
		FormLayout layout = new FormLayout("fill:[100dlu,min]:grow", "pref, pref, fill:pref:grow");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();

		pk = new ProgressKeeper((int) 1E6);

		pk.addListener(this);
		progressSent = new JProgressBar(0, pk.getTotalWork());
		builder.append(progressSent);
		builder.nextLine();
		lblTask = new JLabel();
		builder.append(lblTask);

		progressText = new JTextArea();
		progressText.setEditable(false);

		progress = 0;

		builder.append(new JScrollPane(progressText));
		return builder.getPanel();
	}

	public void setProgressValue(int i) {
		progressSent.setValue(i);
	}

	public void setProgressText(String msg) {
		progressText.setText(msg);
	}

	public void aboutToDisplayPanel() {
		// imb = new InteractionMapBuilder(plugin, pk, progressText,
		// progressSent);

		getWizard().setPageTitle("Load interaction file(s)");

		int x = 0;
		// get total number of lines for all interaction files
		for (ImportInformation impInfo : plugin.getImportInformationList()) {
			try {
				InputStream is = new BufferedInputStream(new FileInputStream(impInfo.getTxtFile()));
				try {
					byte[] c = new byte[1024];
					int readChars = 0;
					while ((readChars = is.read(c)) != -1) {
						for (int i = 0; i < readChars; ++i) {
							if (c[i] == '\n')
								++x;
						}
					}
				} finally {
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		pk = new ProgressKeeper(x);
		progressSent.setMaximum(x);

		setProgressValue(0);
		setProgressText("");

		getWizard().setNextFinishButtonEnabled(false);
		getWizard().setBackButtonEnabled(false);
	}

	public void displayingPanel() {
		SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				pk.setTaskName("Loading interaction file(s)");
				try {
					stopwatch = new StopWatch();
					stopwatch.start();
					for (ImportInformation importInformation : plugin.getImportInformationList()) {
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
										dsReg = DataSource
												.getExistingBySystemCode(str[importInformation.getSyscodeColumnReg()]);
									}
									regulator = new Xref(regulatorString, dsReg);
									plugin.addUsedDataSource(dsReg);
									if (importInformation.isSyscodeFixedTar()) {
										dsTar = importInformation.getDataSourceTar();
									} else {
										dsTar = DataSource
												.getExistingBySystemCode(str[importInformation.getSyscodeColumnTar()]);
									}
									target = new Xref(targetString, dsTar);
									plugin.addUsedDataSource(dsTar);

									Interaction thisInteraction = new Interaction(regulator, target,
											importInformation.getTxtFile());

									if (importInformation.isPMIDColumnEnabled()) {
										String PMID = str[importInformation.getPMIDColumn()];
										thisInteraction.setPMID(PMID);
									}
									String miscInfo = "<table border=\"1\">";
									for (int i = 0; i < importInformation.getColNames().length; i++) {
										if (i != importInformation.getIdColumnReg()
												&& i != importInformation.getIdColumnTar()) {
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
												miscInfo += "<tr><td>" + importInformation.getColNames()[i]
														+ "</td><td>" + str[i] + "</td></tr>";
											}
										}
									}
									miscInfo += "</table>";
									if (!miscInfo.equals("<table border=\"1\"></table>")) {
										thisInteraction.setMiscInfo(miscInfo);
									}

									DataSource[] usedDataSourceArray = new DataSource[plugin.getUsedDataSources()
											.size()];
									usedDataSourceArray = plugin.getUsedDataSources().toArray(usedDataSourceArray);
									Set<Xref> crfsReg = new HashSet<Xref>();
									crfsReg.add(regulator);
									Set<Xref> crfsTar = new HashSet<Xref>();
									crfsTar.add(target);
									for (IDMapper aMapper : plugin.getDesktop().getSwingEngine().getGdbManager()
											.getCurrentGdb().getMappers()) {
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
					progressText.append("Added " + progress + " entries in " + stopwatch.stop() + "ms\n");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (IDMapperException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			public void done() {
				progressSent.setValue(pk.getTotalWork());
				pk.finished();
				pk.setTaskName("Finished");
				getWizard().setNextFinishButtonEnabled(true);
				getWizard().setBackButtonEnabled(true);

				LinkedHashSet<File> files = new LinkedHashSet<File>();
				for (ImportInformation impInfo : plugin.getImportInformationList()) {
					files.add(impInfo.getTxtFile());
				}
				RegIntPreferences.getPreferences().setSelectedIntFiles(files);
			}
		};
		sw.execute();
	}

	public void progressEvent(ProgressEvent e) {
		switch (e.getType()) {
		case ProgressEvent.FINISHED:
			progressSent.setValue(pk.getTotalWork());
		case ProgressEvent.TASK_NAME_CHANGED:
			// TODO: fix, doesn't update the label text
			lblTask.setText(pk.getTaskName());
			break;
		case ProgressEvent.REPORT:
			progressText.append(e.getProgressKeeper().getReport() + "\n");
			break;
		case ProgressEvent.PROGRESS_CHANGED:
			progressSent.setValue(pk.getProgress());
			break;
		}
	}
}