package org.mineacademy.fo.model;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.WordUtils;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.NonNull;
import lombok.Setter;

/**
 * A special class that can store loaded {@link YamlConfig} files
 *
 * @param <T>
 */
public final class ConfigItems<T extends YamlConfig> {

	/**
	 * A list of all loaded items
	 */
	private volatile List<T> loadedItems = new ArrayList<>();

	/**
	 * The item type this class stores, such as "variable, "format", or "arena class"
	 */
	private final String type;

	/**
	 * The folder name where the items are stored, this must be the same
	 * on both your JAR file and in your plugin folder, for example
	 * "classes/" in your JAR file and "classes/" in your plugin folder
	 */
	private final String folder;

	/**
	 * The class we are loading in the list
	 * <p>
	 * *MUST* have a private constructor without any arguments
	 */
	private final Class<T> prototypeClass;

	/**
	 * Does these items have a default prototype in the prototype/ folder in your JAR?
	 */
	private final boolean hasDefaultPrototype;

	/**
	 * Shall we log each item loaded? True by default
	 */
	@Setter
	private boolean verbose = true;

	/**
	 * Create a new config items instance with prototype class,
	 * ensure you make one in prototype/type.yml in your JAR
	 *
	 * @param type
	 * @param folder
	 * @param prototypeClass
	 */
	public ConfigItems(String type, String folder, Class<T> prototypeClass) {
		this(type, folder, prototypeClass, true);
	}

	/**
	 * Create a new config items instance
	 *
	 * @param type
	 * @param folder
	 * @param prototypeClass
	 * @param hasDefaultPrototype
	 */
	public ConfigItems(String type, String folder, Class<T> prototypeClass, boolean hasDefaultPrototype) {
		this.type = type;
		this.folder = folder;
		this.prototypeClass = prototypeClass;
		this.hasDefaultPrototype = hasDefaultPrototype;
	}

	/**
	 * Load all item classes by creating a new instance of them and copying their folder from JAR to disk
	 */
	public void loadItems() {

		// Clear old items
		loadedItems.clear();

		// Try copy items from our JAR
		if (!FileUtil.getFile(folder).exists())
			FileUtil.extractFolderFromJar(folder + "/", folder);

		// Load items on our disk
		final File[] files = FileUtil.getFiles(folder, "yml");

		for (final File file : files) {
			final String name = FileUtil.getFileName(file);

			loadOrCreateItem(name);
		}
	}

	/**
	 * Create the class (make new instance of) by the given name,
	 * the class must have a private constructor taking in the String (name) or nothing
	 *
	 * @param name
	 * @return
	 */
	public T loadOrCreateItem(final String name) {
		Valid.checkBoolean(!isItemLoaded(name), WordUtils.capitalize(type) + name + " is already loaded: " + getItemNames());

		try {
			Constructor<T> constructor;
			boolean nameConstructor = true;

			try {
				constructor = prototypeClass.getDeclaredConstructor(String.class);

			} catch (final Exception e) {
				constructor = prototypeClass.getDeclaredConstructor();
				nameConstructor = false;
			}

			Valid.checkBoolean(Modifier.isPrivate(constructor.getModifiers()), "Your class " + prototypeClass + " must have private constructor taking a String or nothing!");
			constructor.setAccessible(true);

			// Create a new instance of our item
			final T item = nameConstructor ? constructor.newInstance(name) : constructor.newInstance();

			// Automatically load configuration and paste prototype
			item.loadConfiguration(hasDefaultPrototype ? "prototype/" + type + ".yml" : YamlConfig.NO_DEFAULT, folder + "/" + name + ".yml");

			// Register
			loadedItems.add(item);

			if (verbose)
				Common.log("[+] Loaded " + type + " " + item.getName());

			return item;

		} catch (final Throwable t) {
			Common.throwError(t, "Failed to load " + type + " " + name);
		}

		return null;
	}

	/**
	 * Remove the given item by instance
	 *
	 * @param item
	 */
	public void removeItem(@NonNull final T item) {
		Valid.checkBoolean(isItemLoaded(item.getName()), WordUtils.capitalize(type) + " " + item.getName() + " not loaded. Available: " + getItemNames());

		item.delete();
		loadedItems.remove(item);
	}

	/**
	 * Check if the given item by name is loaded
	 *
	 * @param name
	 * @return
	 */
	public boolean isItemLoaded(final String name) {
		return findItem(name) != null;
	}

	/**
	 * Return the item instance by name, or null if not loaded
	 *
	 * @param name
	 * @return
	 */
	public T findItem(@NonNull final String name) {
		for (final T item : loadedItems)
			if (item.getName().equalsIgnoreCase(name))
				return item;

		return null;
	}

	/**
	 * Return all loaded items
	 *
	 * @return
	 */
	public List<T> getItems() {
		return Collections.unmodifiableList(loadedItems);
	}

	/**
	 * Return all loaded item names
	 *
	 * @return
	 */
	public List<String> getItemNames() {
		return Common.convert(loadedItems, T::getName);
	}
}
