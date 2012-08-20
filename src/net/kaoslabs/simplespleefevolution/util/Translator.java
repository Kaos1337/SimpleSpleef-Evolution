/**
 * This file is part of the SimpleSpleef bukkit plugin.
 * Copyright (C) 2011 Maximilian Kalus
 * See http://dev.bukkit.org/server-mods/simple-spleef/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/
package net.kaoslabs.simplespleefevolution.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.file.YamlConfiguration;

import net.kaoslabs.simplespleefevolution.SimpleSpleef;

/**
 * @author mkalus
 * Tranlation Helper
 */
public class Translator {
	/**
	 * language
	 */
	private final String language;

	/**
	 * language configuration
	 */
	private final YamlConfiguration lang;

	/**
	 * Constructor
	 * @param plugin reference to plugin
	 */
	public Translator(SimpleSpleef plugin, String language) {
		this.language = language;
		// load yaml file - test language existance first
		File languageFile = new File(plugin.getDataFolder(), "lang_" + language + ".yml");
		if (!languageFile.exists()) {
			SimpleSpleef.log.warning("[SimpleSpleef] Language file lang_" + language + ".yml does not exist - falling back to English default file.");
			languageFile = new File(plugin.getDataFolder(), "lang_en.yml");
		}
		lang = YamlConfiguration.loadConfiguration(languageFile);
	}
	
	/**
	 * @return current language
	 */
	public String getLanguage() {
		return this.language;
	}
	
	/**
	 * @param key of translation file
	 * @param replacers an even number of key/value pairs to replace key entries
	 * @return translated string
	 */
	public String ll(String key, String... replacers) {
		String translated = lang.getString(key); // get key from config
		// error?
		if (translated == null) return "ERROR: Key " + key + " does not exist in language file lang_" + getLanguage() + ".yml).";
		// check replacements
		if (replacers.length % 2 != 0) return "ERROR: Usage of ll with " + key + " has to contain an even number of needles/replacers (" + translated + ").";
		// cycle through replacements
		for (int i = 0; i < replacers.length; i+=2) {
			translated = translated.replace(replacers[i], replacers[i+1]);
		}
		return translated;
	}

	/**
	 * Get translation section list
	 * @param section
	 * @return
	 */
	public Map<String, String> lls(String section) {
		// sanity check
		if (!lang.isConfigurationSection(section)) return null;
		// get keys
		Set<String> keys = lang.getConfigurationSection(section).getKeys(false);
		Map<String, String> map = new HashMap<String, String>();
		for (String key : keys) {
			map.put(key, lang.getString(section + "." + key));
		}

		return map;
	}
}
