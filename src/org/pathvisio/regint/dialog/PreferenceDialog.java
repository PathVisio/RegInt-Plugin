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

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.regint.RegIntPlugin;
import org.pathvisio.regint.impl.preferences.RegIntPreferences;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The dialog that allows users to set preferences for the {@link RegIntPlugin}.
 * Uses {@link RegIntPreferences}.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class PreferenceDialog extends JDialog {
	private String[] sortOptions = { "Alphabetically", "By number of occurrences in interaction files" };
	private JButton saveButton = new JButton("Save");
	private JButton cancelButton = new JButton("Cancel");
	private JDialog dialog;
	private JPanel mainPanel;
	private JComboBox sortBox;
	private JList jList;
	private RegIntPlugin plugin;
	private PvDesktop desktop;

	public PreferenceDialog(PvDesktop desktop, RegIntPlugin plugin) {
		super(desktop.getFrame(), "RegInt preferences", true);
		this.plugin = plugin;
		this.desktop = desktop;
		dialog = this;

		init();
		dialog.add(addContent());

		dialog.pack();
	}

	private JComponent addContent() {
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				RegIntPreferences.getPreferences().setSort(sortBox.getSelectedIndex());
				if( jList != null && jList.isSelectionEmpty()) {
					RegIntPreferences.getPreferences().setSelectedIntFileIndices(jList.getSelectedIndices());
					LinkedHashSet<File> setIntFiles = new LinkedHashSet<File>();
					for (int i : jList.getSelectedIndices()) {
						setIntFiles.add(plugin.getIntFiles().get(i));
					}
					RegIntPreferences.getPreferences().setSelectedIntFiles(setIntFiles);
				}
				dialog.setVisible(false);
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
			}
		});
		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		buttonPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);

		FormLayout layout = new FormLayout("5dlu, pref, 3dlu, pref, 5dlu", "5dlu, pref, 5dlu, pref, 5dlu");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		sortBox = new JComboBox(sortOptions);

		sortBox.setSelectedIndex(RegIntPreferences.getPreferences().getSort());

		if (plugin.getIntFiles() != null && !plugin.getIntFiles().isEmpty()) {
			ArrayList<File> intFiles = plugin.getIntFiles();
			ArrayList<String> intFileNames = new ArrayList<String>();
			String[] fileNameArray = new String[intFiles.size()];
			for (File intfile : intFiles) {
				intFileNames.add(intfile.toString());
			}
			intFileNames.toArray(fileNameArray);
			if (RegIntPreferences.getPreferences().getSelectedIntFileIndices() == null) {
				int[] selectedIntFileIndices = new int[intFiles.size()];
				for (int i = 0; i < intFiles.size(); i++) {
					selectedIntFileIndices[i] = i;
				}
				RegIntPreferences.getPreferences().setSelectedIntFileIndices(selectedIntFileIndices);
			}
			jList = new JList(fileNameArray);
			jList.setLayoutOrientation(JList.VERTICAL);
			jList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			jList.setSelectedIndices(RegIntPreferences.getPreferences().getSelectedIntFileIndices());

			builder.add(jList, cc.xy(4, 4));
		} else {
			JTextField textField = new JTextField("No interaction files loaded");
			textField.setEditable(false);
			builder.add(textField, cc.xy(4, 4));
		}

		builder.addLabel("Sorting method", cc.xy(2, 2));
		builder.addLabel("Select interaction files to use", cc.xy(2, 4));
		builder.add(sortBox, cc.xy(4, 2));

		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		mainPanel.add(builder.getPanel(), BorderLayout.CENTER);
		return mainPanel;
	}

	private void init() {
		Dimension parentSize = desktop.getFrame().getSize();
		Point p = desktop.getFrame().getLocation();
		dialog.setLocation(p.x + ((parentSize.width - this.getSize().width) / 2),
				p.y + ((parentSize.height - this.getSize().height) / 2));
	}
}