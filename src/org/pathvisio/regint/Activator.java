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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.pathvisio.desktop.plugin.Plugin;

/**
 * Activator class registers the plugin with the OSGi registry, so that the
 * PluginManager in the PathVisio core can start the plugin.
 * 
 * @author Stefan van Helden
 * @author mku
 *
 */
public class Activator implements BundleActivator {

	private RegIntPlugin plugin;

	@Override
	public void start(BundleContext context) throws Exception {

		plugin = new RegIntPlugin();
		context.registerService(Plugin.class.getName(), plugin, null);

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin.done();
	}
}
