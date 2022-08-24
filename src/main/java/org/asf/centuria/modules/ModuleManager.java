package org.asf.centuria.modules;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipInputStream;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.config.serializing.internal.Splitter;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.bytecode.sources.LoaderClassSourceProvider;
import org.asf.centuria.Centuria;
import org.asf.centuria.modules.eventbus.EventBus;
import org.objectweb.asm.tree.ClassNode;

@CYAN_COMPONENT
public class ModuleManager extends CyanComponent {

	//
	//
	// ----------------------------------------------------------------------------
	//
	// Based on the ConnectiveHTTP module loading system as i needed something fast
	//
	// ----------------------------------------------------------------------------
	//
	// Source for it available at:
	// https://github.com/Stefan0436/ConnectiveStandalone
	//
	// Hope to replace this some day in the future
	//
	//

	//
	// Primary fields
	//

	// Main manager
	protected static ModuleManager instance = new ModuleManager();

	/**
	 * Retrieves the active module manager
	 * 
	 * @return ModuleManager instance
	 */
	public static ModuleManager getInstance() {
		return instance;
	}

	//
	// Module loading core systems
	//

	// Module loading
	private URLClassLoader moduleLoader = null;
	private FluidClassPool modulePool = FluidClassPool.createEmpty();

	// Init
	private boolean simpleInit = false;
	private boolean init = false;

	// Module management
	private HashMap<String, ICenturiaModule> modules = new HashMap<String, ICenturiaModule>();

	/**
	 * Simple init, only assigns main implementation
	 */
	public void simpleInit() {
		if (simpleInit)
			return;

		simpleInit = true;
		assignImplementation();
	}

	/**
	 * Initializes all components
	 * 
	 * @throws IllegalStateException If loading fails
	 * @throws IOException           If loading modules fails.
	 */
	public void initializeComponents() throws IllegalStateException, IOException {
		if (init)
			throw new IllegalStateException("Components have already been initialized.");

		// Log init message
		System.out.println("Preparing to load modules...");

		// Run simple initialization
		simpleInit();

		// Prepare the modules folder
		File modules = new File("modules");
		if (!modules.exists())
			modules.mkdirs();

		// Prepare source collection
		ArrayList<URL> sources = new ArrayList<URL>();

		// Add default sources to the class pool
		modulePool.addSource(new LoaderClassSourceProvider(ClassLoader.getSystemClassLoader()));
		modulePool.addSource(new LoaderClassSourceProvider(Thread.currentThread().getContextClassLoader()));

		// Add the complete classpath to the pool
		System.out.println("Importing java classpath...");
		for (String path : Splitter.split(System.getProperty("java.class.path"), File.pathSeparator)) {
			if (path.equals("."))
				continue;

			// Load as file
			File f = new File(path);

			// Convert to URL and add
			sources.add(f.toURI().toURL());

			try {
				// Import to the module class pool
				modulePool.addSource(f.toURI().toURL());
			} catch (MalformedURLException e) {
				// Log failure
				System.err.println("Failed to load class path entry " + path + ": " + e.getClass().getSimpleName()
						+ (e.getMessage() != null ? ": " + e.getMessage() : ""));
				e.printStackTrace();
			}
		}

		// Prepare the module class loader
		moduleLoader = new URLClassLoader(sources.toArray(t -> new URL[t]), getClass().getClassLoader());

		// Add debug modules if debugMode is enabled
		if (Centuria.debugMode) {
			System.out.println("Importing debug classpath...");
			if (System.getProperty("addCpModules") != null) {
				for (String mod : System.getProperty("addCpModules").split(":")) {
					try {
						Class<?> cls = moduleLoader.loadClass(mod);
						modulePool.addSource(cls.getProtectionDomain().getCodeSource().getLocation());
						modulePool.getClassNode(cls.getTypeName());
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}

		// Load module jars
		System.out.println("Loading module files...");
		for (File jar : modules.listFiles(t -> !t.isDirectory())) {
			System.out.println("Loading module: " + jar.getName());
			ZipInputStream strm = new ZipInputStream(new FileInputStream(jar));
			modulePool.importArchive(strm);
			strm.close();
			sources.add(jar.toURI().toURL());
		}

		// Prepare the module class loader
		moduleLoader = new URLClassLoader(sources.toArray(t -> new URL[t]), getClass().getClassLoader());

		// Load module classes
		ArrayList<Class<? extends ICenturiaModule>> moduleClasses = new ArrayList<Class<? extends ICenturiaModule>>();
		System.out.println("Loading module classes...");
		for (ClassNode cls : modulePool.getLoadedClasses()) {
			if (isModuleClass(cls)) {
				try {
					// Load the module class
					System.out.println("Loading module class: " + cls.name.replace("/", "."));
					@SuppressWarnings("unchecked")
					Class<? extends ICenturiaModule> modCls = (Class<? extends ICenturiaModule>) moduleLoader
							.loadClass(cls.name.replace("/", "."));
					moduleClasses.add(modCls);
				} catch (ClassNotFoundException e) {
					System.err.println("Module class load failure: " + cls.name.replace("/", "."));
				}
			}
		}

		// Load modules
		System.out.println("Loading Centuria modules...");
		for (Class<? extends ICenturiaModule> mod : moduleClasses) {
			try {
				ICenturiaModule module = mod.getConstructor().newInstance();
				module.preInit();
				System.out.println("Loading module: " + module.id());
				this.modules.put(module.id(), module);
				EventBus.getInstance().addEventReceiver(module);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				System.err.println("Module loading failure: " + mod.getTypeName() + ": " + e.getClass().getSimpleName()
						+ (e.getMessage() != null ? ": " + e.getMessage() : ""));
			}
		}

		// Initialize modules
		System.out.println("Initializing Centuria modules...");
		for (ICenturiaModule module : this.modules.values()) {
			System.out.println("Initializing module: " + module.id());
			module.init();
		}
	}

	// Method to determine if a class node is a module class
	private boolean isModuleClass(ClassNode cls) {
		// Check interfaces
		if (cls.interfaces != null) {
			for (String inter : cls.interfaces) {
				try {
					// Check interface
					if (checkInterface(modulePool.getClassNode(inter))) {
						// Its a module
						return true;
					}
				} catch (ClassNotFoundException e) {
				}
			}
		}

		// Check supertype
		if (cls.superName != null && !cls.superName.equals(Object.class.getTypeName().replace(".", "/"))) {
			try {
				return isModuleClass(modulePool.getClassNode(cls.superName));
			} catch (ClassNotFoundException e) {
			}
		}

		// Not a module
		return false;
	}

	private boolean checkInterface(ClassNode node) {
		if (node.name.equals(ICenturiaModule.class.getTypeName().replace(".", "/")))
			return true;

		// Check supertype
		if (node.superName != null && !node.superName.equals(Object.class.getTypeName().replace(".", "/"))) {
			try {
				return checkInterface(modulePool.getClassNode(node.superName));
			} catch (ClassNotFoundException e) {
			}
		}

		// Not a subtype of ICenturiaModule
		return false;
	}

	//
	// Module management
	//

	/**
	 * Retrieves all module instances
	 * 
	 * @return Array of ICenturiaModule instances
	 */
	public ICenturiaModule[] getAllModules() {
		return modules.values().toArray(t -> new ICenturiaModule[t]);
	}

	/**
	 * Retrieves a module by ID
	 * 
	 * @param id Module ID
	 * @return ICenturiaModule instance or null
	 */
	public ICenturiaModule getModule(String id) {
		if (modules.containsKey(id))
			return modules.get(id);
		return null;
	}
}
