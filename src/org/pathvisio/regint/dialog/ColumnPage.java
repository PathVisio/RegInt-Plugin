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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.pathvisio.desktop.util.RowNumberHeader;
import org.pathvisio.gui.DataSourceModel;
import org.pathvisio.gui.util.PermissiveComboBox;
import org.pathvisio.regint.RegIntPlugin;
import org.pathvisio.regint.impl.util.ColumnTableModel;
import org.pathvisio.regint.impl.util.ImportInformation;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.nexes.wizard.WizardPanelDescriptor;

/**
 * Second page of the {@link LoadFileWizard}, allows users to configure the
 * column settings for each interaction file.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class ColumnPage extends WizardPanelDescriptor {
	public static final String IDENTIFIER = "COLUMN_PAGE";

	private ImportInformation importInformation;

	private ColumnTableModel ctm;
	private JTable tblColumn;

	private JComboBox cbColIdReg;
	private JComboBox cbColIdTar;
	private JComboBox cbColSyscodeReg;
	private JComboBox cbColSyscodeTar;
	private JRadioButton rbFixedNoReg;
	private JRadioButton rbFixedYesReg;
	private JComboBox cbDataSourceReg;
	private JRadioButton rbFixedNoTar;
	private JRadioButton rbFixedYesTar;
	private JComboBox cbDataSourceTar;
	private DataSourceModel mDataSourceReg;
	private DataSourceModel mDataSourceTar;
	private JCheckBox checkPMID;
	private JComboBox cbPMID;
	private JScrollPane bottomPanel;
	private JPanel listPanel;
	private JList jList;
	private JPanel panel;
	private Map<ImportInformation, Boolean> finishedFiles;
	private RegIntPlugin plugin;

	public ColumnPage(RegIntPlugin plugin) {
		super(IDENTIFIER);
		this.plugin = plugin;
	}

	public Object getNextPanelDescriptor() {
		return ImportPage.IDENTIFIER;
	}

	public Object getBackPanelDescriptor() {
		return FilePage.IDENTIFIER;
	}

	protected JPanel createContents() {
		bottomPanel = new JScrollPane();
		listPanel = new JPanel();
		int x = 1;
		ImportInformation[] impInfoArray = new ImportInformation[x];
		jList = new JList(impInfoArray);
		jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		listPanel.add(new JLabel("Select an interaction file to configure:"));
		listPanel.add(jList);
		panel.add(listPanel);
		panel.add(bottomPanel);
		// combined with the default wizard buttons & border fits exactly in 800x600 resolution
		panel.setPreferredSize(new Dimension(764, 482));
		return panel;
	}

	/**
	 * Saves the settings for the previously selected interaction file and
	 * updates the bottom panel to show the settings for the newly selected
	 * interaction file.
	 * 
	 * @param importInformation
	 *            The {@link ImportInformation} of the newly selected
	 *            interaction file
	 */
	private void updateBottomPanel(final ImportInformation importInformation) {
		// save settings for previously selected file
		// checks if the bottomPanel is actually created (not done during instantiation, only after loading files)
		if (rbFixedYesReg != null) {
			plugin.getCurrentFile().setSyscodeFixedReg(rbFixedYesReg.isSelected());
			plugin.getCurrentFile().setSyscodeFixedTar(rbFixedYesTar.isSelected());
			if (rbFixedYesReg.isSelected()) {
				plugin.getCurrentFile().setDataSourceReg(mDataSourceReg.getSelectedDataSource());
			}
			if (rbFixedYesTar.isSelected()) {
				plugin.getCurrentFile().setDataSourceTar(mDataSourceTar.getSelectedDataSource());
			}
			plugin.getCurrentFile().setPMIDColumnEnabled(checkPMID.isSelected());
			if (checkPMID.isSelected()) {
				plugin.getCurrentFile().setPMIDColumn(cbPMID.getSelectedIndex());
			}
		}

		plugin.setCurrentFile(importInformation);
		panel.remove(bottomPanel);
		FormLayout layout = new FormLayout("pref, 7dlu, pref:grow",
				"p, 5dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 15dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 15dlu, p, 3dlu, fill:[100dlu,min]:grow");

		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();

		CellConstraints cc = new CellConstraints();
		rbFixedNoReg = new JRadioButton("Select a column to specify system code for regulators");
		rbFixedYesReg = new JRadioButton("Use the same system code for all rows for regulators");
		rbFixedNoTar = new JRadioButton("Select a column to specify system code for targets");
		rbFixedYesTar = new JRadioButton("Use the same system code for all rows for targets");
		ButtonGroup bgSyscodeColReg = new ButtonGroup();
		bgSyscodeColReg.add(rbFixedNoReg);
		bgSyscodeColReg.add(rbFixedYesReg);
		ButtonGroup bgSyscodeColTar = new ButtonGroup();
		bgSyscodeColTar.add(rbFixedNoTar);
		bgSyscodeColTar.add(rbFixedYesTar);

		cbColIdReg = new JComboBox();
		cbColSyscodeReg = new JComboBox();
		cbColIdTar = new JComboBox();
		cbColSyscodeTar = new JComboBox();

		mDataSourceReg = new DataSourceModel();
		mDataSourceTar = new DataSourceModel();
		cbDataSourceReg = new PermissiveComboBox(mDataSourceReg);
		cbDataSourceTar = new PermissiveComboBox(mDataSourceTar);

		checkPMID = new JCheckBox("Select PubMed ID column");
		cbPMID = new JComboBox();

		ctm = new ColumnTableModel(importInformation);
		tblColumn = new JTable(ctm);
		tblColumn.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tblColumn.setDefaultRenderer(Object.class, ctm.getTableCellRenderer());
		tblColumn.setCellSelectionEnabled(false);

		tblColumn.getTableHeader().addMouseListener(new ColumnPopupListener());
		JTable rowHeader = new RowNumberHeader(tblColumn);
		rowHeader.addMouseListener(new RowPopupListener());
		JScrollPane scrTable = new JScrollPane(tblColumn);

		JViewport jv = new JViewport();
		jv.setView(rowHeader);
		jv.setPreferredSize(rowHeader.getPreferredSize());
		scrTable.setRowHeader(jv);
		builder.add(scrTable, cc.xyw(1, 25, 3));
		builder.addLabel("Current file: " + importInformation.getTxtFile(), cc.xyw(1, 1, 3));

		builder.addLabel("Select primary identifier column for regulators:", cc.xy(1, 3));
		builder.add(cbColIdReg, cc.xy(3, 3));

		builder.add(rbFixedNoReg, cc.xyw(1, 5, 3));
		builder.add(cbColSyscodeReg, cc.xy(3, 7));
		builder.add(rbFixedYesReg, cc.xyw(1, 9, 3));
		builder.add(cbDataSourceReg, cc.xy(3, 11));

		builder.addSeparator("", cc.xyw(1, 12, 3));

		builder.addLabel("Select primary identifier column for targets:", cc.xy(1, 13));
		builder.add(cbColIdTar, cc.xy(3, 13));

		builder.add(rbFixedNoTar, cc.xyw(1, 15, 3));
		builder.add(cbColSyscodeTar, cc.xy(3, 17));
		builder.add(rbFixedYesTar, cc.xyw(1, 19, 3));
		builder.add(cbDataSourceTar, cc.xy(3, 21));

		builder.addSeparator("", cc.xyw(1, 22, 3));

		builder.add(checkPMID, cc.xy(1, 23));
		builder.add(cbPMID, cc.xy(3, 23));

		ActionListener rbActionReg = new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				boolean result = (ae.getSource() == rbFixedYesReg);
				importInformation.setSyscodeFixedReg(result);
				columnPageRefresh();
			}
		};
		rbFixedYesReg.addActionListener(rbActionReg);
		rbFixedNoReg.addActionListener(rbActionReg);

		ActionListener rbActionTar = new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				boolean result = (ae.getSource() == rbFixedYesTar);
				importInformation.setSyscodeFixedTar(result);
				columnPageRefresh();
			}
		};
		rbFixedYesTar.addActionListener(rbActionTar);
		rbFixedNoTar.addActionListener(rbActionTar);

		mDataSourceReg.addListDataListener(new ListDataListener() {
			public void contentsChanged(ListDataEvent arg0) {
				importInformation.setDataSourceReg(mDataSourceReg.getSelectedDataSource());
			}

			public void intervalAdded(ListDataEvent arg0) {
			}

			public void intervalRemoved(ListDataEvent arg0) {
			}
		});
		mDataSourceTar.addListDataListener(new ListDataListener() {
			public void contentsChanged(ListDataEvent arg0) {
				importInformation.setDataSourceTar(mDataSourceTar.getSelectedDataSource());
			}

			public void intervalAdded(ListDataEvent arg0) {
			}

			public void intervalRemoved(ListDataEvent arg0) {
			}
		});

		cbColSyscodeReg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				importInformation.setSysodeColumnReg(cbColSyscodeReg.getSelectedIndex());
				columnPageRefresh();
			}
		});
		cbColIdReg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				importInformation.setIdColumnReg(cbColIdReg.getSelectedIndex());
				columnPageRefresh();
			}
		});
		cbColSyscodeTar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				importInformation.setSysodeColumnTar(cbColSyscodeTar.getSelectedIndex());
				columnPageRefresh();
			}
		});
		cbColIdTar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				importInformation.setIdColumnTar(cbColIdTar.getSelectedIndex());
				columnPageRefresh();
			}
		});

		ActionListener PMIDListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == checkPMID) {
					cbPMID.setEnabled(checkPMID.isSelected());
					importInformation.setPMIDColumnEnabled(checkPMID.isSelected());
					columnPageRefresh();
				} else if (e.getSource() == cbPMID) {
					importInformation.setPMIDColumn(cbPMID.getSelectedIndex());
					columnPageRefresh();
				}
			}
		};
		checkPMID.addActionListener(PMIDListener);
		cbPMID.addActionListener(PMIDListener);
		JScrollPane scroll = new JScrollPane(builder.getPanel());
		bottomPanel.removeAll();
		bottomPanel = scroll;
		bottomPanel.revalidate();

		// create an array of size getSampleMaxNumCols()
		Integer[] cn;
		int max = importInformation.getSampleMaxNumCols();
		cn = new Integer[max];
		for (int i = 0; i < max; ++i)
			cn[i] = i;

		cbColIdReg.setRenderer(new ColumnNameRenderer());
		cbColSyscodeReg.setRenderer(new ColumnNameRenderer());
		cbColIdReg.setModel(new DefaultComboBoxModel(cn));
		cbColSyscodeReg.setModel(new DefaultComboBoxModel(cn));
		cbColIdTar.setRenderer(new ColumnNameRenderer());
		cbColSyscodeTar.setRenderer(new ColumnNameRenderer());
		cbColIdTar.setModel(new DefaultComboBoxModel(cn));
		cbColSyscodeTar.setModel(new DefaultComboBoxModel(cn));

		cbPMID.setRenderer(new ColumnNameRenderer());
		cbPMID.setModel(new DefaultComboBoxModel(cn));

		finishedFiles.put(importInformation, true);

		columnPageRefresh();
		refreshComboBoxes();

		ctm.refresh();

		panel.add(bottomPanel);
		panel.revalidate();
	}

	private class ColumnPopupListener extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
			showPopup(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			showPopup(e);
		}

		int clickedCol;

		private void showPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				JPopupMenu popup;
				popup = new JPopupMenu();
				clickedCol = tblColumn.columnAtPoint(e.getPoint());
				if (clickedCol != importInformation.getSyscodeColumnReg())
					popup.add(new SyscodeColRegAction());
				if (clickedCol != importInformation.getIdColumnReg())
					popup.add(new IdColRegAction());
				if (clickedCol != importInformation.getSyscodeColumnTar())
					popup.add(new SyscodeColTarAction());
				if (clickedCol != importInformation.getIdColumnTar())
					popup.add(new IdColTarAction());
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		private class SyscodeColRegAction extends AbstractAction {
			public SyscodeColRegAction() {
				putValue(Action.NAME, "Regulator SystemCode column");
			}

			public void actionPerformed(ActionEvent arg0) {
				// if id and code column are about to be the same, swap them
				if (clickedCol == importInformation.getIdColumnReg())
					importInformation.setIdColumnReg(importInformation.getSyscodeColumnReg());
				importInformation.setSysodeColumnReg(clickedCol);
				columnPageRefresh();
			}
		}

		private class SyscodeColTarAction extends AbstractAction {
			public SyscodeColTarAction() {
				putValue(Action.NAME, "Target SystemCode column");
			}

			public void actionPerformed(ActionEvent arg0) {
				// if id and code column are about to be the same, swap them
				if (clickedCol == importInformation.getIdColumnTar())
					importInformation.setIdColumnTar(importInformation.getSyscodeColumnTar());
				importInformation.setSysodeColumnTar(clickedCol);
				columnPageRefresh();
			}
		}

		private class IdColRegAction extends AbstractAction {
			public IdColRegAction() {
				putValue(Action.NAME, "Regulator Identifier column");
			}

			public void actionPerformed(ActionEvent arg0) {
				// if id and code column are about to be the same, swap them
				if (clickedCol == importInformation.getSyscodeColumnReg())
					importInformation.setSysodeColumnReg(importInformation.getIdColumnReg());
				importInformation.setIdColumnReg(clickedCol);
				columnPageRefresh();
			}
		}

		private class IdColTarAction extends AbstractAction {
			public IdColTarAction() {
				putValue(Action.NAME, "Target Identifier column");
			}

			public void actionPerformed(ActionEvent arg0) {
				// if id and code column are about to be the same, swap them
				if (clickedCol == importInformation.getSyscodeColumnTar())
					importInformation.setSysodeColumnTar(importInformation.getIdColumnTar());
				importInformation.setIdColumnTar(clickedCol);
				columnPageRefresh();
			}
		}
	}

	private class RowPopupListener extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
			showPopup(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			showPopup(e);
		}

		int clickedRow;

		private void showPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				JPopupMenu popup;
				popup = new JPopupMenu();
				clickedRow = tblColumn.rowAtPoint(e.getPoint());
				popup.add(new DataStartAction());
				popup.add(new HeaderStartAction());
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		private class DataStartAction extends AbstractAction {
			public DataStartAction() {
				putValue(Action.NAME, "First data row");
			}

			public void actionPerformed(ActionEvent arg0) {
				importInformation.setFirstDataRow(clickedRow);
				columnPageRefresh();
			}
		}

		private class HeaderStartAction extends AbstractAction {
			public HeaderStartAction() {
				putValue(Action.NAME, "First header row");
			}

			public void actionPerformed(ActionEvent arg0) {
				importInformation.setFirstHeaderRow(clickedRow);
				columnPageRefresh();
			}
		}

	}

	private void columnPageRefresh() {
		importInformation = plugin.getCurrentFile();
		String error = null;
		if (importInformation.isSyscodeFixedReg()) {
			rbFixedYesReg.setSelected(true);
			cbColSyscodeReg.setEnabled(false);
			cbDataSourceReg.setEnabled(true);
		} else {
			rbFixedNoReg.setSelected(true);
			cbColSyscodeReg.setEnabled(true);
			cbDataSourceReg.setEnabled(false);

			if (importInformation.getIdColumnReg() == importInformation.getSyscodeColumnReg()) {
				error = "Regulator System code column and Id column can't be the same";
			} else if (!importInformation.isSyscodeFixedTar()
					&& importInformation.getSyscodeColumnReg() == importInformation.getSyscodeColumnTar()) {
				error = "Regulator and target system code columns can't be the same";
			} else if (importInformation.getIdColumnTar() == importInformation.getSyscodeColumnReg()) {
				error = "Target ID column and Regulator system code column can't be the same";
			}
		}
		if (importInformation.isSyscodeFixedTar()) {
			rbFixedYesTar.setSelected(true);
			cbColSyscodeTar.setEnabled(false);
			cbDataSourceTar.setEnabled(true);
		} else {
			rbFixedNoTar.setSelected(true);
			cbColSyscodeTar.setEnabled(true);
			cbDataSourceTar.setEnabled(false);

			if (importInformation.getIdColumnTar() == importInformation.getSyscodeColumnTar()) {
				error = "Target System code column and Id column can't be the same";
			} else if (importInformation.getIdColumnReg() == importInformation.getSyscodeColumnTar()) {
				error = "Regulator ID column and Target system code column can't be the same";
			}
		}
		if (importInformation.isPMIDColumnEnabled()) {
			checkPMID.setSelected(true);
			cbPMID.setEnabled(true);

			if (!importInformation.isSyscodeFixedReg()
					&& importInformation.getPMIDColumn() == importInformation.getSyscodeColumnReg()) {
				error = "Regulator system code column and PMID column can't be the same";
			} else if (!importInformation.isSyscodeFixedTar()
					&& importInformation.getPMIDColumn() == importInformation.getSyscodeColumnTar()) {
				error = "Target system code column and PMID column can't be the same";
			} else if (importInformation.getIdColumnReg() == importInformation.getPMIDColumn()) {
				error = "Regulator ID column and PMID column can't be the same";
			} else if (importInformation.getIdColumnTar() == importInformation.getPMIDColumn()) {
				error = "Target ID column and PMID column can't be the same";
			}
		} else {
			checkPMID.setSelected(false);
			cbPMID.setEnabled(false);
		}
		if (importInformation.getIdColumnReg() == importInformation.getIdColumnTar()) {
			error = "Regulator ID column and Target ID column can'tbe the same";
		}

		if (!finishedFiles.containsValue(false)) {
			getWizard().setNextFinishButtonEnabled(error == null);
		}
		getWizard().setErrorMessage(error == null ? "" : error);
		getWizard().setPageTitle("Choose column types");

		ctm.refresh();
	}

	private void refreshComboBoxes() {
		if (importInformation.getSampleMaxNumCols() > 0) {
			mDataSourceReg.setSelectedItem(importInformation.getDataSourceReg());
			cbColIdReg.setSelectedIndex(importInformation.getIdColumnReg());
			mDataSourceTar.setSelectedItem(importInformation.getDataSourceTar());
			cbColIdTar.setSelectedIndex(importInformation.getIdColumnTar());
			cbColSyscodeReg.setSelectedIndex(importInformation.getSyscodeColumnReg());
			cbColSyscodeTar.setSelectedIndex(importInformation.getSyscodeColumnTar());
			cbPMID.setSelectedIndex(importInformation.getPMIDColumn());
		}
	}

	/**
	 * A simple cell Renderer for combo boxes that use the column index integer
	 * as value, but will display the column name String
	 */
	private class ColumnNameRenderer extends JLabel implements ListCellRenderer {
		public ColumnNameRenderer() {
			setOpaque(true);
			setHorizontalAlignment(CENTER);
			setVerticalAlignment(CENTER);
		}

		/*
		 * This method finds the image and text corresponding to the selected
		 * value and returns the label, set up to display the text and image.
		 */
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			// Get the selected index. (The index param isn't
			// always valid, so just use the value.)
			int selectedIndex = ((Integer) value).intValue();

			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}

			String[] cn = importInformation.getColNames();
			String column = cn[selectedIndex];
			setText(column);
			setFont(list.getFont());

			return this;
		}
	}

	public void aboutToDisplayPanel() {
		getWizard().setNextFinishButtonEnabled(false);
		importInformation = plugin.getCurrentFile();
		finishedFiles = new HashMap<ImportInformation, Boolean>();
		for (ImportInformation impInfo : plugin.getImportInformationList()) {
			finishedFiles.put(impInfo, false);
		}

		listPanel.removeAll();
		String[] fileNameArray = new String[plugin.getImportInformationList().size()];
		for (int i = 0; i < plugin.getImportInformationList().size(); i++) {
			fileNameArray[i] = plugin.getImportInformationList().get(i).getTxtFile().getName();
		}
		jList = new JList(fileNameArray);
		jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateBottomPanel(plugin.getImportInformationList().get(jList.getSelectedIndex()));
			}
		});
		listPanel.add(new JLabel("Select an interaction file to configure:"));
		listPanel.add(jList);

		jList.revalidate();
		jList.setSelectedIndex(0);
		listPanel.revalidate();
	}

	@Override
	public void aboutToHidePanel() {
		importInformation.setSyscodeFixedReg(rbFixedYesReg.isSelected());
		importInformation.setSyscodeFixedTar(rbFixedYesTar.isSelected());
		if (rbFixedYesReg.isSelected()) {
			importInformation.setDataSourceReg(mDataSourceReg.getSelectedDataSource());
		}
		if (rbFixedYesTar.isSelected()) {
			importInformation.setDataSourceTar(mDataSourceTar.getSelectedDataSource());
		}
		importInformation.setPMIDColumnEnabled(checkPMID.isSelected());
		if (checkPMID.isSelected()) {
			importInformation.setPMIDColumn(cbPMID.getSelectedIndex());
		}
	}
}
