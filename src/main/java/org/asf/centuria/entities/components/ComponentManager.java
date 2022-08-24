package org.asf.centuria.entities.components;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.asf.centuria.Centuria;
import org.reflections.Reflections;

/**
 * This class is responsible for holding a type register of all components.
 * It retrieves the component's class type from its component json name. 
 * @author Owenvii
 *
 */
public final class ComponentManager {
	
	private static final String COMPONENT_PACKAGE_NAME = "org.asf.centuria.entities.components";
	
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
	public static void registerAllComponents() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		var classes = new Reflections(COMPONENT_PACKAGE_NAME).getTypesAnnotatedWith(Component.class);
		
		for(var classObject : classes)
		{
			@SuppressWarnings("unchecked")
			var componentClass = (Class<InventoryItemComponent>)classObject;
			
			var componentName = componentClass.getDeclaredConstructor().newInstance().getComponentName();
			
			if (Centuria.debugMode) {
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
