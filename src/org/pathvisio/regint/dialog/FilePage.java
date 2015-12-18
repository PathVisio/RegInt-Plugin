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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.bridgedb.IDMapperException;
import org.bridgedb.gui.SimpleFileFilter;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.regint.RegIntPlugin;
import org.pathvisio.regint.impl.util.ImportInformation;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.nexes.wizard.WizardPanelDescriptor;

/**
 * First page of the {@link LoadFileWizard}, allows users to choose the
 * interaction files to load.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class FilePage extends WizardPanelDescriptor implements ActionListener {
	private RegIntPlugin plugin;
	public static final String IDENTIFIER = "FILE_PAGE";
	static final String ACTION_INPUT = "input";
	static final String ACTION_GDB = "gdb";

	private JTextField txtInput;
	private JTextField txtGdb;
	private JButton btnGdb;
	private JButton btnInput;
	private boolean txtFileComplete = false;
	private List<ImportInformation> impInfoList = new ArrayList<ImportInformation>();

	public FilePage(RegIntPlugin plugin) {
		super(IDENTIFIER);
		this.plugin = plugin;
	}

	private void updateTxtFile() {
		String fileName = txtInput.getText();
		String[] buffer = fileName.split("; ");
		if (buffer.length != 0) {
			boolean exists = true;
			for (int i = 0; i < buffer.length; i++) {
				if (exists) {
					if (!buffer[i].equals("")) {
						File file = new File(buffer[i]);
						if (!file.exists()) {
							exists = false;
						}
					}
				}
			}
			if (exists) {
				txtFileComplete = true;
			} else {
				getWizard().setErrorMessage("Specified file to import does not exist");
				txtFileComplete = false;
			}
		}
		getWizard().setNextFinishButtonEnabled(txtFileComplete);

		if (txtFileComplete) {
			getWizard().setErrorMessage(null);
			txtFileComplete = true;
		}
	}

	public void aboutToDisplayPanel() {
		getWizard().setNextFinishButtonEnabled(txtFileComplete);
		getWizard().setPageTitle("Choose file locations");
	}

	public Object getNextPanelDescriptor() {
		return ColumnPage.IDENTIFIER;
	}

	public Object getBackPanelDescriptor() {
		return null;
	}

	protected JPanel createContents() {
		txtInput = new JTextField(40);
		txtGdb = new JTextField(40);
		btnGdb = new JButton("Browse");
		btnInput = new JButton("Browse");

		FormLayout layout = new FormLayout("right:pref, 3dlu, pref, 3dlu, pref", "p, 3dlu, p");

		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();

		CellConstraints cc = new CellConstraints();

		builder.addLabel("Interaction file(s)", cc.xy(1, 1));
		builder.add(txtInput, cc.xy(3, 1));
		builder.add(btnInput, cc.xy(5, 1));
		builder.addLabel("Gene database", cc.xy(1, 3));
		builder.add(txtGdb, cc.xy(3, 3));
		builder.add(btnGdb, cc.xy(5, 3));

		btnInput.addActionListener(this);
		btnInput.setActionCommand(ACTION_INPUT);
		btnGdb.addActionListener(this);
		btnGdb.setActionCommand(ACTION_GDB);

		txtInput.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent arg0) {
				updateTxtFile();
			}

			public void insertUpdate(DocumentEvent arg0) {
				updateTxtFile();
			}

			public void removeUpdate(DocumentEvent arg0) {
				updateTxtFile();
			}

		});
		txtGdb.setText(PreferenceManager.getCurrent().get(GlobalPreference.DB_CONNECTSTRING_GDB));
		return builder.getPanel();
	}

	public void aboutToHidePanel() {
		for (ImportInformation impInfo : impInfoList) {
			impInfo.guessSettings();
		}
		plugin.setImportInformationList(impInfoList);
	}

	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();

		if (ACTION_GDB.equals(action)) {// databases are added to the core, but
										// need to use a different way of using
										// all of them
			File defaultdir = PreferenceManager.getCurrent().getFile(GlobalPreference.DIR_LAST_USED_PGDB);
			JFileChooser jfc = new JFileChooser();
			jfc.setCurrentDirectory(defaultdir);
			jfc.addChoosableFileFilter(new SimpleFileFilter("BridgeDb databases", "*.bridge|*.pgdb", true));
			jfc.setMultiSelectionEnabled(true);
			int result = jfc.showDialog(null, "Select BridgeDb database file(s)");
			String fileNames = "";
			if (result == JFileChooser.APPROVE_OPTION) {
				File[] files = jfc.getSelectedFiles();
				String[] connectionStrings = new String[files.length];
				for (int i = 0; i < files.length; i++) {
					connectionStrings[i] = "idmapper-pgdb:" + files[i].getAbsolutePath();
					fileNames = fileNames + files[i].getAbsolutePath() + "; ";
					txtGdb.setText(fileNames);
				}
				for (String connectString : connectionStrings) {
					if (connectString != null) {
						GdbManager manager = plugin.getDesktop().getSwingEngine().getGdbManager();
						try {
							manager.addMapper(connectString);
						} catch (IDMapperException ex) {
							String msg = "Failed to open database; " + ex.getMessage();
							JOptionPane.showMessageDialog(null,
									"Error: " + msg + "\n\n" + "See the error log for details.", "Error",
									JOptionPane.ERROR_MESSAGE);
							Logger.log.error(msg, ex);
						}
					}
				}
			}
		} else if (ACTION_INPUT.equals(action)) {
			JFileChooser jfc = new JFileChooser();
			jfc.addChoosableFileFilter(new SimpleFileFilter("Interaction files", "*.txt|*.csv|*.tab", true));
			jfc.setMultiSelectionEnabled(true);
			int result = jfc.showDialog(null, "Select interaction file(s)");
			String fileNames = "";
			if (result == JFileChooser.APPROVE_OPTION) {
				File[] files = jfc.getSelectedFiles();
				for (File f : files) {
					ImportInformation importInformation = new ImportInformation();
					try {
						importInformation.setTxtFile(f);
						importInformation.setDelimiter("\t");
					} catch (IOException e1) {
						getWizard().setErrorMessage("Exception while reading file: " + e1.getMessage());
						txtFileComplete = false;
					}
					impInfoList.add(importInformation);
					fileNames = fileNames + f.getAbsolutePath() + "; ";
					txtInput.setText(fileNames);
				}
				updateTxtFile();
				if (impInfoList.size() > 0) {
					plugin.setCurrentFile(impInfoList.get(0));
				}
			}
		}
	}
}
