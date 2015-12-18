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

package org.pathvisio.regint.impl.preferences;

import java.io.File;
import java.util.LinkedHashSet;

/**
 * The class that manages preferences for the RI-plugin.
 * 
 * @author Stefan van Helden
 * @author mku
 */
public class RegIntPreferences {
	private static RegIntPreferences preferences;
	private int sort;
	public static final int ALPHABETICALLY = 0;
	public static final int BY_NUMBER_OF_OCCURRENCES = 1;
	private int[] selectedIntFileIndices;
	private LinkedHashSet<File> selectedIntFiles;

	private RegIntPreferences() {
		sort = ALPHABETICALLY;
	}

	public int getSort() {
		return sort;
	}

	public void setSort(int anInt) {
		if (anInt == ALPHABETICALLY) {
			sort = ALPHABETICALLY;
		} else if (anInt == BY_NUMBER_OF_OCCURRENCES) {
			sort = BY_NUMBER_OF_OCCURRENCES;
		}
	}

	public int[] getSelectedIntFileIndices() {
		return selectedIntFileIndices;
	}

	public void setSelectedIntFileIndices(int[] ints) {
		selectedIntFileIndices = ints;
	}

	public LinkedHashSet<File> getSelectedIntFiles() {
		return selectedIntFiles;
	}

	public void setSelectedIntFiles(LinkedHashSet<File> intFiles) {
		selectedIntFiles = intFiles;
	}

	public static RegIntPreferences getPreferences() {
		if (preferences == null) {
			preferences = new RegIntPreferences();
		}
		return preferences;
	}
}