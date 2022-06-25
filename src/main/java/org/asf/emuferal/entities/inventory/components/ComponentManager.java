package org.asf.emuferal.entities.inventory.components;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.reflections.Reflections;

/**
 * This class is responsible for holding a type register of all components.
 * It retrieves the component's class type from its component json name. 
 * @author owen9
 *
 */
public final class ComponentManager {
	
	private static String ComponentPackageName = "org.asf.emuferal.entities.inventory.components";
	
	private static Map<String, Class<InventoryItemComponent>> componentRegister = new HashMap<String, Class<InventoryItemComponent>>();
	
	/**
	 * This method registers all components (Items marked with the Component Annotation) into the register.
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static void RegisterAllComponents() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		var classes = new Reflections(ComponentPackageName).getTypesAnnotatedWith(Component.class);
		
		for(var classObject : classes)
		{
			@SuppressWarnings("unchecked")
			var componentClass = (Class<InventoryItemComponent>)classObject;
			
			var componentName = componentClass.getDeclaredConstructor().newInstance().getComponentName();
			
			if (System.getProperty("debugMode") != null) {
				System.out.println("Loading component into component register: " + componentName);
			}
			
			componentRegister.put(componentName, componentClass);	
		}
	}
	
	public static Class<InventoryItemComponent> getComponentTypeFromComponentName(String componentName)
	{
		return componentRegister.get(componentName);
	}
}
