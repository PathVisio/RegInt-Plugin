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

package org.pathvisio.regint.impl.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bridgedb.DataSource;
import org.bridgedb.DataSourcePatterns;
import org.pathvisio.core.debug.Logger;

/**
 * Adjusted version of the ImportInformation class from {@link GexImportWizard}
 * containing settings for interaction files
 */
public class ImportInformation {

	private File txtFile;

	/**
	 * Sets the text file containing the interaction data
	 * 
	 * @param aTxtFile
	 *            {@link File} to set
	 */
	public void setTxtFile(File aTxtFile) throws IOException {
		if (!aTxtFile.equals(txtFile)) {
			txtFile = aTxtFile;
			readSample();
			interpretSample();
		}
	}

	/**
	 * Get the private {@link File} txtFile
	 * 
	 * @return {@link File} object pointing to the text file that contains the
	 *         interaction data
	 */
	public File getTxtFile() {
		return txtFile;
	}

	private List<String> errorList = new ArrayList<String>();

	/**
	 * Returns a list of errors made during importing data, the same list as
	 * saved in the error file (.ex.txt)
	 */
	public List<String> getErrorList() {
		return errorList;
	}

	/**
	 * An error has been reported during importing data. The message is added to
	 * the list of errors.
	 */
	public void addError(String message) {
		errorList.add(message);
	}

	private int firstDataRow = 1;

	/**
	 * linenumber (first line is 0) of the line where the data begins
	 */
	public int getFirstDataRow() {
		return firstDataRow;
	}

	/**
	 * linenumber (first line is 0) of the line where the data begins
	 */
	public void setFirstDataRow(int value) {
		assert (value >= 0);
		firstDataRow = value;
		if (firstHeaderRow > firstDataRow) {
			firstHeaderRow = firstDataRow;
		}
	}

	private int firstHeaderRow;

	/**
	 * linenumber (first line is 0) of the line where the header begins.
	 */
	public int getFirstHeaderRow() {
		return firstHeaderRow;
	}

	/**
	 * linenumber (first line is 0) of the line where the header begins.
	 * firstHeaderRow must be <= firstDataRow, so it will be set to the next
	 * line if that happens.
	 */
	public void setFirstHeaderRow(int value) {
		assert (value >= 0);
		firstHeaderRow = value;
		if (firstHeaderRow > firstDataRow) {
			firstDataRow = firstHeaderRow + 1;
		}
	}

	public boolean isHeaderRow(int row) {
		return row >= firstHeaderRow && row < firstDataRow;
	}

	public boolean isDataRow(int row) {
		return row >= firstDataRow;
	}

	/**
	 * linenumber (first line is 0) of the line containing the column headers
	 */
	int headerRow = 0;

	private int idColumnReg = 0;

	/**
	 * Column number (first column is 0) of the column containing the gene
	 * identifier of the regulators
	 */
	public int getIdColumnReg() {
		return idColumnReg;
	}

	public void setIdColumnReg(int value) {
		idColumnReg = value;
	}

	private int idColumnTar = 1;

	/**
	 * Column number (first column is 0) of the column containing the gene
	 * identifier of the targets
	 */
	public int getIdColumnTar() {
		return idColumnTar;
	}

	public void setIdColumnTar(int value) {
		idColumnTar = value;
	}

	private int syscodeColumnReg = 0;

	/**
	 * Column number (first column is 0) of the column containing the system
	 * code of the regulators
	 */
	public int getSyscodeColumnReg() {
		return syscodeColumnReg;
	}

	public void setSysodeColumnReg(int value) {
		syscodeColumnReg = value;
	}

	private int syscodeColumnTar = 0;

	/**
	 * Column number (first column is 0) of the column containing the system
	 * code of the targets
	 */
	public int getSyscodeColumnTar() {
		return syscodeColumnTar;
	}

	public void setSysodeColumnTar(int value) {
		syscodeColumnTar = value;
	}

	/** Various possible column types */
	public enum ColumnType {
		COL_SYSCODE_REG, COL_ID_REG, COL_STRING, COL_NUMBER, COL_ID_TAR, COL_SYSCODE_TAR, COL_PMID
	};

	public ColumnType getColumnType(int col) {
		if (col == idColumnReg)
			return ColumnType.COL_ID_REG;
		if (col == idColumnTar)
			return ColumnType.COL_ID_TAR;
		if (isPMIDColumnEnabled() && col == PMIDColumn)
			return ColumnType.COL_PMID;
		if (isSyscodeFixedReg ? false : col == syscodeColumnReg)
			return ColumnType.COL_SYSCODE_REG;
		if (isSyscodeFixedTar ? false : col == syscodeColumnTar)
			return ColumnType.COL_SYSCODE_TAR;
		return isStringCol(col) ? ColumnType.COL_STRING : ColumnType.COL_NUMBER;
	}

	/**
	 * Boolean which can be set to false if there is no column for the system
	 * code of the regulators available in the dataset.
	 */
	private boolean isSyscodeFixedReg = false;
	/**
	 * Boolean which can be set to false if there is no column for the system
	 * code of the targets available in the dataset.
	 */
	private boolean isSyscodeFixedTar = false;

	/**
	 * Data source of the regulators that has been set by the user in
	 * LoadFileWizard (if no system code column is available).
	 */
	DataSource dsReg = null;
	/**
	 * Data source of the targets that has been set by the user in
	 * LoadFileWizard (if no system code column is available).
	 */
	DataSource dsTar = null;

	/**
	 * Delimiter used to separate columns in the text file containing expression
	 * data
	 */
	private String delimiter = "\t";

	/**
	 * Column numbers (first column is 0) of the columns of which the data
	 * should not be treated as numeric
	 */
	private Set<Integer> stringCols = new HashSet<Integer>();

	/**
	 * Set if the given column is of type String or not
	 */
	public void setStringColumn(int col, boolean value) {
		if (value)
			stringCols.add(col);
		else
			stringCols.remove(col);
	}

	/**
	 * Checks if the column for the given column index is marked as 'string
	 * column' and should not be treated as numeric
	 * 
	 * @param colIndex
	 *            the index of the column to check (start with 0)
	 * @return true if the column is marked as 'string column', false if not
	 */
	public boolean isStringCol(int colIndex) {
		return stringCols.contains(colIndex);
	}

	/**
	 * Creates an excel "Column name" for a given 0-based column index. 0 -> A,
	 * 25-> Z 26 -> AA 26 + 26^2 -> AAA etc.
	 */
	static String colIndexToExcel(int i) {
		assert (i >= 0);
		String result = "";
		while (i >= 0) {
			result = (char) ('A' + i % 26) + result;
			i /= 26;
			i--;
		}
		return result;
	}

	/**
	 * Reads the column names from the text file containing the interaction data
	 * at the header row specified by the user. Multiple header rows can also be
	 * read. When no header row is present, a header row is created manually.
	 *
	 * The column names are guaranteed to be unique, non-empty, and there will
	 * be at least as many columns as sampleMaxNumCols.
	 *
	 * @return the column names
	 */
	public String[] getColNames() {
		String[] result = null;
		result = new String[sampleMaxNumCols];

		// initialize columns to empty strings
		for (int i = 0; i < sampleMaxNumCols; i++)
			result[i] = "";

		// concatenate header rows
		if (!getNoHeader()) {
			int i = 0;
			// Read headerlines till the first data row
			boolean first = true;
			while (i < firstDataRow) {
				for (int j = 0; j < cells[i].length; j++) {
					// All header rows are added
					if (i >= headerRow) {
						if (!first)
							result[j] += " ";
						result[j] = result[j] += cells[i][j].trim();
					}
				}
				first = false;
				i++;
			}
		}

		// check that column names are unique
		Set<String> unique = new HashSet<String>();
		// set remaining emtpy column names to default string
		for (int j = 0; j < result.length; ++j) {
			String col = result[j];
			if (col.equals("") || unique.contains(col)) {
				// generate default column name
				result[j] = "Column " + colIndexToExcel(j);
			}
			unique.add(result[j]);
		}
		return result;
	}

	/**
	 * indicates whether a header is present or not
	 * 
	 * @return value of noHeader (true or false)
	 */
	public boolean getNoHeader() {
		return firstDataRow - firstHeaderRow <= 0;
	}

	/**
	 * Returns the boolean value set by the user which indicates whether the
	 * system code for regulators is fixed, or specified in another column
	 */
	public boolean isSyscodeFixedReg() {
		return isSyscodeFixedReg;
	}

	/**
	 * Sets the boolean value to determine if the system code for regulators is
	 * fixed, or specified in separate column
	 */
	public void setSyscodeFixedReg(boolean target) {
		isSyscodeFixedReg = target;
	}

	/**
	 * Returns the boolean value set by the user which indicates whether the
	 * system code for targets is fixed, or specified in another column
	 */
	public boolean isSyscodeFixedTar() {
		return isSyscodeFixedTar;
	}

	/**
	 * Sets the boolean value to determine if the system code for targets is
	 * fixed, or specified in separate column
	 */
	public void setSyscodeFixedTar(boolean target) {
		isSyscodeFixedTar = target;
	}

	/**
	 * Sets the data source to use for the regulators. Only meaningful if
	 * getSyscodeColumnReg returns false.
	 */
	public void setDataSourceReg(DataSource value) {
		dsReg = value;
	}

	/**
	 * Gets the data source to use for the regulators. Only meaningful if
	 * getSyscodeColumn returns false.
	 */
	public DataSource getDataSourceReg() {
		return dsReg;
	}

	/**
	 * Sets the data source to use for the targets. Only meaningful if
	 * getSyscodeColumnReg returns false.
	 */
	public void setDataSourceTar(DataSource value) {
		dsTar = value;
	}

	/**
	 * Gets the data source to use for the targets. Only meaningful if
	 * getSyscodeColumn returns false.
	 */
	public DataSource getDataSourceTar() {
		return dsTar;
	}

	/**
	 * Returns the string that is used as the delimiter for reading the input
	 * data. This string is used to separate columns in the input data. The
	 * returned string can be any length, but during normal use it is typically
	 * 1 or 2 characters long.
	 */
	public String getDelimiter() {
		return delimiter;
	}

	/**
	 * Set the delimiter string. This string is used to separate columns in the
	 * input data. The delimiter string can be set to any length, but during
	 * normal use it is typically 1 or 2 characters long
	 */
	public void setDelimiter(String target) {
		delimiter = target;
		interpretSample();
	}

	private static final int NUM_SAMPLE_LINES = 50;
	private List<String> lines = null;
	private String[][] cells = null;

	private int sampleMaxNumCols = 0;

	public int getSampleMaxNumCols() {
		return sampleMaxNumCols;
	}

	public int getSampleNumRows() {
		return lines == null ? 0 : lines.size();
	}

	public String getSampleData(int row, int col) {
		if (cells != null && cells[row] != null && cells[row].length > col) {
			return cells[row][col];
		} else {
			return "";
		}
	}

	/** derive datasource from sample data */
	public void guessSettings() {
		int match = 0;
		for (int i = 0; i < getSampleMaxNumCols(); i++) {
			for (DataSource ds : DataSource.getDataSources()) {
				if (ds.getFullName() != null && ds.getFullName().equalsIgnoreCase(getColNames()[i])) {
					match++;
					if (match == 1) {
						setDataSourceReg(ds);
						setIdColumnReg(i);
						setSyscodeFixedReg(true);
					} else if (match == 2) {
						setDataSourceTar(ds);
						setIdColumnTar(i);
						setSyscodeFixedTar(true);
					}
				}
			}
		}
		if (match < 2) {
			isSyscodeFixedReg = !guessHasSyscodeColumn;
			if (guessDataSource != null)
				setDataSourceReg(guessDataSource);
			if (guessHasSyscodeColumn && guessSyscodeColumn >= 0) {
				setSysodeColumnReg(guessSyscodeColumn);
			}
			if (guessIdColumn >= 0)
				setIdColumnReg(guessIdColumn);
			Logger.log.info("Guessing sysCode: " + guessHasSyscodeColumn + " " + guessSyscodeColumn + " id: "
					+ guessIdColumn + " " + guessDataSource);
		}

		if (guessHasPMIDColumn && guessPMIDColumn >= 0) {
			setPMIDColumnEnabled(guessHasPMIDColumn);
			setPMIDColumn(guessPMIDColumn);
		}
	}

	private boolean guessHasSyscodeColumn = true;
	private int guessSyscodeColumn = -1;
	private int guessIdColumn = -1;
	private DataSource guessDataSource = null;

	private boolean guessHasPMIDColumn = false;
	private int guessPMIDColumn = -1;

	/**
	 * Helper class to keep track of how often patterns occur, and in which
	 * column
	 */
	private static class PatternCounter {
		private final Pattern p;
		private Map<Integer, Integer> counts = new HashMap<Integer, Integer>();

		PatternCounter(Pattern p) {
			this.p = p;
		}

		void countCell(String cell, int column) {
			Matcher m = p.matcher(cell);

			// check if it matches
			if (m.matches()) {
				// increase total and per-column counts
				int prev = counts.containsKey(column) ? counts.get(column) : 0;
				counts.put(column, ++prev);
			}
		}

		int getColumnCount(int col) {
			return (counts.containsKey(col) ? counts.get(col) : 0);
		}
	}

	/**
	 * fraction of cells that have to match a pattern for it to be a good guess
	 */
	private static final double GOOD_GUESS_FRACTION = 0.9;

	/*
	 * read a sample from the selected text file
	 */
	private void readSample() throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(txtFile));
		lines = new ArrayList<String>();
		String line;
		while ((line = in.readLine()) != null && lines.size() < NUM_SAMPLE_LINES) {
			lines.add(line);
		}
		in.close();
	}

	/* guess some parameters */
	private void interpretSample() {
		// "Guess" the system code based on the first 50 lines.

		// Make regular expressions patterns for the gene ID's.
		Map<DataSource, Pattern> patterns = DataSourcePatterns.getPatterns();

		// Make regular expressions pattern for the system code.
		final PatternCounter syscodeCounter = new PatternCounter(Pattern.compile("[A-Z][a-z]?"));

		// Make count variables.
		Map<DataSource, PatternCounter> counters = new HashMap<DataSource, PatternCounter>();

		for (DataSource ds : patterns.keySet()) {
			counters.put(ds, new PatternCounter(patterns.get(ds)));
		}

		sampleMaxNumCols = 0;
		int row = 0;
		cells = new String[NUM_SAMPLE_LINES][];
		for (String line : lines) {
			cells[row] = line.split(delimiter);
			int numCols = cells[row].length;

			if (numCols > sampleMaxNumCols) {
				sampleMaxNumCols = numCols;
			}

			for (int col = 0; col < cells[row].length; ++col) {
				// Count all the times that an element matches a gene
				// identifier.
				syscodeCounter.countCell(cells[row][col], col);

				for (DataSource ds : patterns.keySet()) {
					counters.get(ds).countCell(cells[row][col], col);
				}
			}

			row++;
		}

		/*
		 * Calculate percentage of rows where a system code is found and compare
		 * with a given percentage
		 */
		{
			double max = 0;
			int maxCol = -1;

			for (int col = 0; col < sampleMaxNumCols; ++col) {
				double syscodepercentage = (double) syscodeCounter.getColumnCount(col) / (double) row;

				if (syscodepercentage > max) {
					max = syscodepercentage;
					maxCol = col;
				}
			}

			/*
			 * Set the selection to the codeRadio button if a system code is
			 * found in more than rows than the given percentage, otherwise set
			 * the selection to the syscodeRadio button
			 */
			if (max >= GOOD_GUESS_FRACTION) {
				guessHasSyscodeColumn = true;
				guessSyscodeColumn = maxCol;
			} else {
				guessHasSyscodeColumn = false;
				guessSyscodeColumn = -1;
			}
		}

		// Look for maximum.
		double max = 0;
		double second = 0;
		DataSource maxds = null;
		int maxCol = -1;

		for (int col = 0; col < sampleMaxNumCols; ++col) {
			for (DataSource ds : patterns.keySet()) {
				// Determine the maximum of the percentages (most hits).
				// Sometimes, normal data can match a gene identifier, in which
				// case percentages[i]>1.
				// Ignores these gene identifiers.
				double percentage = (double) counters.get(ds).getColumnCount(col) / (double) row;
				if (percentage > max && percentage <= 1) {
					// remember the second highest percentage too
					second = max;
					max = percentage;
					maxds = ds;
					maxCol = col;
				}
			}
		}

		// Select the right entry in the drop down menu and change the system
		// code in importInformation
		guessDataSource = maxds;

		// only set guessIdColumn if the guess is a clear outlier,
		// if it's hardly above noise, just set it to 0.
		if (max > 2 * second)
			guessIdColumn = maxCol;
		else
			guessIdColumn = 0;

		for (int i = 0; i < getColNames().length; i++) {
			String str = getColNames()[i];
			if (str.equalsIgnoreCase("PMID") || str.equalsIgnoreCase("PubMed") || str.equalsIgnoreCase("PubMed ID")) {
				guessHasPMIDColumn = true;
				guessPMIDColumn = i;
			}
		}
	}

	int dataRowsImported = 0;
	int rowsMapped = 0;

	public int getDataRowsImported() {
		return dataRowsImported;
	}

	public int getRowsMapped() {
		return rowsMapped;
	}

	private boolean PMIDColumnEnabled;
	private int PMIDColumn = 0;

	public void setPMIDColumnEnabled(boolean enabled) {
		PMIDColumnEnabled = enabled;
	}

	public void setPMIDColumn(int column) {
		PMIDColumn = column;
	}

	public boolean isPMIDColumnEnabled() {
		return PMIDColumnEnabled;
	}

	public int getPMIDColumn() {
		return PMIDColumn;
	}
}
