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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.bridgedb.Xref;
import org.pathvisio.core.model.LineStyle;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.ShapeType;
import org.pathvisio.core.view.VPathway;
import org.pathvisio.desktop.gex.BackpageExpression;
import org.pathvisio.gui.BackpagePane;
import org.pathvisio.gui.BackpageTextProvider;
import org.pathvisio.gui.SwingEngine;
import org.pathvisio.gui.view.VPathwaySwing;
import org.pathvisio.regint.RegIntPlugin;
import org.pathvisio.regint.dialog.PreferenceDialog;
import org.pathvisio.regint.impl.InfoButtonListener;
import org.pathvisio.regint.impl.Interaction;
import org.pathvisio.regint.impl.ResultsObj;
import org.pathvisio.regint.impl.preferences.RegIntPreferences;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The tab in the PathVisio side panel that belongs to the RegInt Plugin. It is
 * split it two parts. The top part contains the list of interaction partners of
 * the selected {@link PathwayElement} in the form of a {@link Pathway} object
 * to support data visualization. The bottom part contains the
 * {@link BackpageHook}s that have more information about the selected
 * interaction.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class RegIntTab extends JSplitPane {

	private RegIntPlugin plugin;
	private JPanel pathwayPanel = new JPanel();
	private JPanel backpagePanel = new JPanel();
	private ImageIcon icon = createImageIcon("/i.gif", "information icon");
	private RegIntTab tab;
	private CellConstraints cc = new CellConstraints();
	private int y;
	private int x;
	private SwingEngine swingEngine;

	public RegIntTab(RegIntPlugin plugin) {
		super(JSplitPane.VERTICAL_SPLIT);
		tab = this;
		this.plugin = plugin;
		pathwayPanel.setLayout(new BorderLayout());
		backpagePanel.setLayout(new BoxLayout(backpagePanel, BoxLayout.PAGE_AXIS));
		swingEngine = plugin.getDesktop().getSwingEngine();
		JScrollPane pathwayScroll = new JScrollPane(pathwayPanel);
		pathwayScroll.getVerticalScrollBar().setUnitIncrement(20);
		JScrollPane backpageScroll = new JScrollPane(backpagePanel);
		backpageScroll.getVerticalScrollBar().setUnitIncrement(20);
		setTopComponent(pathwayScroll);
		setBottomComponent(backpageScroll);
		setOneTouchExpandable(true);
		setDividerLocation(400);
	}

	/**
	 * Updates the bottom part of the {@link RegIntTab} with information about the
	 * Xref selected in the top part of RIPTab and its interaction
	 * 
	 * @param xref
	 *            one of the interaction partners (displayed in the top part of
	 *            the RIPTAB) of the Xref selected in the main pathway
	 * @param results
	 *            the results object from updating the top part of the RIPTab by
	 *            selecting an Xref in the main pathway
	 */
	public void updateBackpagePanel(Xref xref, ResultsObj results) {
		backpagePanel.removeAll();
		backpagePanel.setLayout(new BoxLayout(backpagePanel, BoxLayout.PAGE_AXIS));

		PathwayElement e = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		e.setDataSource(xref.getDataSource());
		e.setElementID(xref.getId());
		BackpageTextProvider bpt = new BackpageTextProvider();

		bpt.addBackpageHook(new BackpageInfo(swingEngine.getGdbManager().getCurrentGdb(), results));
		// TODO: fix file table, doesn't always show files
		bpt.addBackpageHook(new BackpageFileTable(results, plugin));
		bpt.addBackpageHook(new BackpagePMID(results));
		// TODO: change this to use multiple BridgeDb databases?
		bpt.addBackpageHook(new BackpageTextProvider.BackpageXrefs(swingEngine.getGdbManager().getCurrentGdb()));
		bpt.addBackpageHook(new BackpageExpression(plugin.getDesktop().getGexManager()));
		bpt.addBackpageHook(new BackpageMiscInfo(results));
		BackpagePane bpp = new BackpagePane(bpt, swingEngine.getEngine());
		bpp.setInput(e);

		bpp.addHyperlinkListener(new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
					try {
						Desktop.getDesktop().browse(e.getURL().toURI());
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (URISyntaxException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		backpagePanel.add(bpp);

		backpagePanel.revalidate();
		backpagePanel.repaint();
	}

	/**
	 * Updates the top part of the {@link RegIntTab} with a list of interaction
	 * partners of the selected Xref
	 * 
	 * @param xref
	 *            The {@link Xref} from the selected pathway element
	 */
	public void updatePathwayPanel(PathwayElement elm) {
		Xref xref = elm.getXref();
		backpagePanel.removeAll();

		JLabel loading = new JLabel("<html><br>&nbsp;&nbsp;&nbsp;&nbsp;Loading...</html>", JLabel.LEFT);
		loading.setVerticalAlignment(JLabel.TOP);
		pathwayPanel.removeAll();
		pathwayPanel.setLayout(new BorderLayout());
		pathwayPanel.setBackground(Color.WHITE);
		pathwayPanel.add(loading, BorderLayout.CENTER);
		pathwayPanel.revalidate();

		try {
			ResultsObj results = plugin.findInteractions(xref);
			if (results != null) {
				FormLayout layout = new FormLayout("5dlu, 123px, 3dlu, pref, 3dlu, pref, 5dlu",
						tab.getRowLayout(results.getRegulatorMap().size(), results.getTargetMap().size()));

				PanelBuilder builder = new PanelBuilder(layout);
				builder.setDefaultDialogBorder();

				CellConstraints cc = new CellConstraints();

				builder.addSeparator("", cc.xyw(1, 1, 7));
				y = 2;
				int y2 = y;
				if (results.getRegulatorMap().size() > 0) {
					builder.addLabel("Regulators:", cc.xy(2, y));
					y = y + 2;
					y2 = y;
					JScrollPane sourceScroll = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
							ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
					sourceScroll.setMinimumSize(new Dimension(50, 50));
					VPathwaySwing vPathwaySwing = createPathway(sourceScroll, results.getRegulatorMap(), results, builder);
					sourceScroll.add(vPathwaySwing);
					builder.add(sourceScroll, cc.xywh(2, y2, 1, (2 * results.getRegulatorMap().size())));
				}

				if (results.getTargetMap().size() > 0) {
					y++;
					builder.addLabel("Targets:", cc.xy(2, y));
					y = y + 2;
					y2 = y;
					JScrollPane targetScroll = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
							ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
					targetScroll.setMinimumSize(new Dimension(50, 50));
					VPathwaySwing vPathwaySwing = createPathway(targetScroll, results.getTargetMap(), results, builder);
					targetScroll.add(vPathwaySwing);
					builder.add(targetScroll, cc.xywh(2, y2, 1, (2 * results.getTargetMap().size())));
				}
				JPanel panel = builder.getPanel();
				pathwayPanel.removeAll();

				JLabel selectedLabel = new JLabel("<html><br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<B>Selected: "
						+ elm.getTextLabel() + " (" + xref.getDataSource().getSystemCode() + ": " + xref.getId() + ")</B></html>");
				pathwayPanel.setLayout(new BorderLayout());
				pathwayPanel.add(selectedLabel, BorderLayout.NORTH);
				pathwayPanel.add(panel, BorderLayout.CENTER);
				pathwayPanel.setBackground(Color.WHITE);
				pathwayPanel.revalidate();
				pathwayPanel.repaint();
			} else {
				setPathwayPanelText("<html><br>&nbsp;&nbsp;&nbsp;&nbsp;No results found.</html>");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setPathwayPanelText(String text) {
		pathwayPanel.removeAll();
		JLabel label = new JLabel(text, JLabel.LEFT);
		label.setVerticalAlignment(JLabel.TOP);
		pathwayPanel.setLayout(new BorderLayout());
		pathwayPanel.add(label, BorderLayout.CENTER);
		pathwayPanel.revalidate();
	}

	/**
	 * Creates the list of interaction partners displayed in the top part of the
	 * {@link RegIntTab}
	 * 
	 * @param parent
	 *            The {@link JScrollPane} containing either the regulators or
	 *            the targets of the selected element
	 * @param map
	 *            The map of interaction partners, either regulators or targets,
	 *            also found in results
	 * @param results
	 *            The entire {@link ResultsObj}. Must have map as either its
	 *            regulator or target map
	 * @param builder
	 *            The {@link PanelBuilder} that will include this pathway
	 * @return the list of interaction partners (in pathway form for expression
	 *         visualization)
	 */
	private VPathwaySwing createPathway(JScrollPane parent, Map<Xref, Interaction> map, ResultsObj results, PanelBuilder builder) {
		VPathwaySwing vPathwaySwing = new VPathwaySwing(parent);
		VPathway vPathway = vPathwaySwing.createVPathway();
		vPathway.setEditMode(false);
		Pathway sourcePw = new Pathway();
		Map<Xref, Interaction> sorted = sortXrefs(map);
		x = 0;
		for (Xref xref : sorted.keySet()) {
			PathwayElement pwe = createPathwayELement(xref);
			sourcePw.add(pwe);

			JLabel linkout = new JLabel(icon);
			JPanel linkoutPane = new JPanel();
			linkout.addMouseListener(new InfoButtonListener(xref, results, plugin));
			linkoutPane.add(linkout);
			linkoutPane.setCursor(new Cursor(Cursor.HAND_CURSOR));
			linkoutPane.setVisible(true);

			List<File> uniqueFiles = new ArrayList<File>();
			for (File intFile : sorted.get(xref).getFiles()) {
				if (!uniqueFiles.contains(intFile)
						&& RegIntPreferences.getPreferences().getSelectedIntFiles().contains(intFile)) {
					uniqueFiles.add(intFile);
				}
			}
			builder.addLabel(uniqueFiles.size() + "/" + RegIntPreferences.getPreferences().getSelectedIntFiles().size(),
					cc.xy(6, y));

			builder.add(linkoutPane, cc.xy(4, y));
			y = y + 2;
			x++;
		}
		vPathway.fromModel(sourcePw);
		vPathway.setSelectionEnabled(false);
		vPathway.addVPathwayListener(plugin.getDesktop().getVisualizationManager());
		return vPathwaySwing;
	}

	private PathwayElement createPathwayELement(Xref xref) {
		PathwayElement pwe = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		pwe.setDataSource(xref.getDataSource());
		pwe.setElementID(xref.getId());
		pwe.setTextLabel(xref.toString());
		pwe.setTransparent(false);
		pwe.setColor(Color.BLACK);
		pwe.setShapeType(ShapeType.RECTANGLE);
		pwe.setLineStyle(LineStyle.SOLID);
		pwe.setInitialSize();
		pwe.setMWidth(120);
		pwe.setMCenterX(60);
		pwe.setMCenterY((35 * x) + 14);
		return pwe;
	}

	/**
	 * Sorts the list of interaction partners. Alphabetical sorting is the
	 * default, but can be set to sort by number of occurrences in
	 * {@link PreferenceDialog}.
	 * 
	 * @param map
	 *            The interactions to sort
	 * @return The sorted interactions
	 */
	private Map<Xref, Interaction> sortXrefs(Map<Xref, Interaction> map) {
		Map<Xref, Interaction> sorted = new LinkedHashMap<Xref, Interaction>();
		if (RegIntPreferences.getPreferences().getSort() == RegIntPreferences.BY_NUMBER_OF_OCCURRENCES) {
			List<ObjectsToSort> list = new ArrayList<ObjectsToSort>();

			for (Xref xref : map.keySet()) {
				ObjectsToSort obj = new ObjectsToSort(xref, map.get(xref).getFiles().size());
				list.add(obj);
			}

			Collections.sort(list);

			for (ObjectsToSort obj : list) {
				sorted.put(obj.getXref(), map.get(obj.getXref()));
			}
		} else {
			sorted = new TreeMap<Xref, Interaction>(map);// alphabetical sorting
															// as default
		}
		return sorted;
	}

	private String getRowLayout(int source, int target) {
		String formLayoutString = "20dlu";
		if (source > 0) {
			for (int i = 0; i <= source; i++) {
				formLayoutString = formLayoutString + ", pref, 3dlu";
			}
		}
		if (target > 0) {
			formLayoutString = formLayoutString + ", 7dlu, pref, 3dlu";
			for (int i = 0; i < target; i++) {
				formLayoutString = formLayoutString + ", pref, 3dlu";
			}
		}
		return formLayoutString;
	}

	/** Returns an ImageIcon, or null if the path was invalid. */
	protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	class ObjectsToSort implements Comparable<ObjectsToSort> {

		private Xref xref;
		private Integer files = 0;

		public ObjectsToSort(Xref xref, Integer files) {
			this.xref = xref;
			this.files = files;
		}

		public Xref getXref() {
			return xref;
		}

		public void setXref(Xref xref) {
			this.xref = xref;
		}

		public Integer getFiles() {
			return files;
		}

		public void setFiles(Integer files) {
			this.files = files;
		}

		@Override
		public int compareTo(ObjectsToSort o) {
			return o.getFiles().compareTo(files);
		}

	}

}