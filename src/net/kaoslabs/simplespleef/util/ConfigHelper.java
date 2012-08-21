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
package net.kaoslabs.simplespleef.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Set;
import java.util.zip.CRC32;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.kaoslabs.simplespleef.SimpleSpleef;

/**
 * Config helper/utility class
 * @author mkalus
 */
public class ConfigHelper {
	/**
	 * available languages
	 */
	public final static String[] languagesAvailable = {"en", "de", "pl"};
	
	/**
	 * Create a new arena configuration section.
	 * This method does not check if the arena exists already (done in simple speef admin e.g.)!
	 * Might want to change this in the future...
	 * @param id lowercase key of arena
	 * @param name full name
	 * @return
	 */
	public boolean createNewArena(String id, String name) {
		// get sample config from resource
		try {
			// input default file
			InputStream is = SimpleSpleef.getPlugin().getResource("config.yml");
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(is);
			
			// create new configuration section in main config
			ConfigurationSection newSection = SimpleSpleef.getPlugin().getConfig().createSection("arenas." + id);

			// get default arena
			ConfigurationSection section = defConfig.getConfigurationSection("arenas.default");

			// copy default to target section
			copySection(newSection, section);
			newSection.set("name", name); //set new name

			// save configuration now
			SimpleSpleef.getPlugin().saveConfig();
		} catch (Exception e) {
			e.printStackTrace();
			SimpleSpleef.log.severe("[SimpleSpleef] Could not load sample arena due to exception: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * copy a whole section from a source to a target
	 * @param target
	 * @param source
	 */
	public void copySection(ConfigurationSection target, ConfigurationSection source) {
		if (target == null || source == null) return; //no NPEs!
		for (String section : source.getKeys(true)) { //copy sections
			if (source.isConfigurationSection(section)) {
				target.createSection(section);
			} else target.set(section, source.get(section));
		}
	}

	/**
	 * Check for the existence of sample_config.yml in the plugin folder and copy it if it is newer/changed
	 * @return boolean true, if everything went ok
	 */
	public boolean updateSampleConfig() {
		// check, if "sample" config file has to be updated
		try {
			// output sample file
			File file = new File(SimpleSpleef.getPlugin().getDataFolder(),"sample_config.yml");
			// input default file
			InputStream is = SimpleSpleef.getPlugin().getResource("config.yml");
			
			boolean copyDefault;
			if (file.exists()) { // file exists - check crc32 sums of files
				if (crc32Sum(new FileInputStream(file)) == crc32Sum(is)) copyDefault =  false;
				else {
					copyDefault =  true;
					is = SimpleSpleef.getPlugin().getResource("config.yml"); // reopen stream
				}
			} else { // file does not exist and can be copied right away
				copyDefault = true;
			}

			// shall we copy the resource?
			if (copyDefault) {
				// open output stream
				OutputStream out = new FileOutputStream(file);
				int read = 0;
				byte[] bytes = new byte[1024];

				// write input to output stream (aka copy file)
				while ((read = is.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}

				// close everything
				is.close();
				out.flush();
				out.close();
				SimpleSpleef.log.info("[SimpleSpleef] Updated sample_config.yml to new version.");
				return true;
			}
		} catch(Exception e) {
			SimpleSpleef.log.warning("[SimpleSpleef] Warning: Could not write sample_config.yml - reason: " + e.getMessage());
		}
		return false;
	}

	/**
	 * Calculate crc of input stream - stream is read, but not closed!
	 * @param is
	 * @return crc32 sum of stream
	 * @throws Exception
	 */
	protected long crc32Sum(InputStream is) throws Exception {
		CRC32 crc = new CRC32();
		byte[] ba = new byte[(int) is.available()];
		is.read(ba);
		crc.update(ba);
		is.close();
		
		return crc.getValue();
	}
	
	/**
	 * update language files to comply to defaults
	 */
	public void updateLanguageFiles() {
		// load language files one by one
		for (String language : ConfigHelper.languagesAvailable) {
			// load saved file from data folder
			File languageFile = new File(SimpleSpleef.getPlugin().getDataFolder(), "lang_" + language + ".yml");
			FileConfiguration languageConfig;
			try {
				languageConfig = YamlConfiguration.loadConfiguration(languageFile);
			} catch (Exception e) {
				SimpleSpleef.log.severe("[SimpleSpleef] Could not load language file " + languageFile + "! Please convert it to UTF-8.");
				continue; // ignore updating language files
			}
			
			// get default config from resource
			InputStream languageConfigStream = SimpleSpleef.getPlugin().getResource("lang_" + language + ".yml");
		    if (languageConfigStream != null) {
		    	// read into string - this will circumvent breaks in non-UTF-8-environments
		    	YamlConfiguration defConfig = loadUTF8Stream(languageConfigStream, "lang_" + language + ".yml");
		    	// the above is the same as
		        //YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(languageConfigStream);
		    	// only save...
		    	if (defConfig == null) continue; // errors have been logged already
		        // update config
		        languageConfig.setDefaults(defConfig);
		        languageConfig.options().copyDefaults(true); // copy defaults, too
		        // force update of admin.help
		        languageConfig.set("admin.help", defConfig.getString("admin.help"));
		        try {
		        	// save updated config
		        	languageConfig.save(languageFile);
		        } catch (Exception e) {
		        	SimpleSpleef.log.warning("[SimpleSpleef] Warning: Could not write lang_" + language + ".yml - reason: " + e.getMessage());
		        }
		    }
		}
	}
	
	/**
	 * Converts stream to YAML configuration - UTF-8 enconded. Will prevent code breaks - fixes very stupid default behaviour of Bukkit
	 * @param stream
	 * @param file
	 * @return
	 */
	protected YamlConfiguration loadUTF8Stream(InputStream stream, String file) {
    	// read into string - this will circumvent breaks in non-UTF-8-environments
    	String config;
    	try {
	    	BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8")); // force UTF-8 => why does Bukkit not do this itself???
	    	StringBuilder sb = new StringBuilder();
	    	String readLine;
	    	while ((readLine = br.readLine()) != null) {
	    		sb.append(readLine).append("\n");
	    	}
	    	config =  sb.toString();
    	} catch (Exception e) {
    		SimpleSpleef.log.severe("[SimpleSpleef] Could not load resource file " + file + "! Reason: " + e.getMessage());
    		return null;
		}

    	YamlConfiguration defConfig = new YamlConfiguration();
    	try {
			defConfig.loadFromString(config);
			return defConfig;
		} catch (InvalidConfigurationException e) {
			SimpleSpleef.log.severe("[SimpleSpleef] Could not convert resource file " + file + " to valid configuration! Reason: " + e.getMessage());
			return null;
		}
	}

	/**
	 * update defaults of existig arenas
	 */
	public void updateDefaults() {
		boolean changed = false; // defaults changed?
		// get default keys
		Set<String> defaultConfigKeys = SimpleSpleef.getPlugin().getConfig().getDefaultSection().getConfigurationSection("arenas.default").getKeys(true);
		
		ConfigurationSection arenas = SimpleSpleef.getPlugin().getConfig().getConfigurationSection("arenas");
		for (String arena : arenas.getKeys(false)) {
			if (!arena.equalsIgnoreCase("default")) {
				// note uppercase arenas
				for (char c : arena.toCharArray()) {
					if (Character.isUpperCase(c)) {
						SimpleSpleef.log.warning("[SimpleSpleef] Arena " + arena + " contains uppercase letters - please use all lowercase letters for the arena id.");
						break;
					}
				}
				//System.out.println("Arena " + arena + " found!");
				ConfigurationSection currentConfig = SimpleSpleef.getPlugin().getConfig().getConfigurationSection("arenas." + arena);
				// sanity check - it seems that some users somehow warp their configuration in strange ways...
				if (currentConfig == null) {
					SimpleSpleef.log.warning("[SimpleSpleef] Arena " + arena + " configuration seems to be broken somehow - no configuration key arenas." + arena + " could be retreived from the config.");
					continue;
				}
				for (String configKey : defaultConfigKeys) {
					// ignore sections to avoid exceptions
					if (SimpleSpleef.getPlugin().getConfig().isConfigurationSection("arenas.default." + configKey)) continue;
					if (!currentConfig.contains(configKey)) {
						//System.out.println(configKey + " is missing! Replacing it with default.");
						changed = true; // yes, config was changed
						// copy default
						currentConfig.set(configKey, SimpleSpleef.getPlugin().getConfig().getDefaultSection().get("arenas.default." + configKey));
					}
				}
			}
		}
		
		if (changed) {
			SimpleSpleef.log.info("[SimpleSpleef] Updated defaults in some arenas due to config changes.");
			SimpleSpleef.getPlugin().saveConfig(); // save config to disk
		}
	}
}
