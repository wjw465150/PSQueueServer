package wjw.psqueue.server.jmx;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class JMXTools {
	private static org.slf4j.Logger _log = org.slf4j.LoggerFactory.getLogger(JMXTools.class);

	public static final Map<String, Class<?>> mapJMXPrimitive = new HashMap<String, Class<?>>();
	static {
		mapJMXPrimitive.put("byte", byte.class);
		mapJMXPrimitive.put("short", short.class);
		mapJMXPrimitive.put("int", int.class);
		mapJMXPrimitive.put("long", long.class);
		mapJMXPrimitive.put("float", float.class);
		mapJMXPrimitive.put("double", double.class);
		mapJMXPrimitive.put("char", char.class);
		mapJMXPrimitive.put("boolean", boolean.class);
	}

	private JMXTools() {
	}

	/**
	 * Java 1.5 and above supports the ability to register the WrapperManager
	 * MBean internally.
	 */
	@SuppressWarnings("rawtypes")
	static public void registerMBean(Object mbean, String name) {
		Class classManagementFactory;
		Class classMBeanServer;
		Class classObjectName;
		try {
			classManagementFactory = Class.forName("java.lang.management.ManagementFactory");
			classMBeanServer = Class.forName("javax.management.MBeanServer");
			classObjectName = Class.forName("javax.management.ObjectName");
		} catch (ClassNotFoundException e) {
			_log.error("Registering MBeans not supported by current JVM:" + name);
			return;
		}

		try {
			// This code uses reflection so it combiles on older JVMs.
			// The original code is as follows:
			// javax.management.MBeanServer mbs =
			//     java.lang.management.ManagementFactory.getPlatformMBeanServer();
			// javax.management.ObjectName oName = new javax.management.ObjectName( name );
			// mbs.registerMBean( mbean, oName );

			// The version of the above code using reflection follows.
			Method methodGetPlatformMBeanServer = classManagementFactory.getMethod("getPlatformMBeanServer", (Class[]) null);
			Constructor constructorObjectName = classObjectName.getConstructor(new Class[] { String.class });
			Method methodRegisterMBean = classMBeanServer.getMethod("registerMBean", new Class[] { Object.class, classObjectName });
			Object mbs = methodGetPlatformMBeanServer.invoke(null, (Object[]) null);
			Object oName = constructorObjectName.newInstance(new Object[] { name });
			methodRegisterMBean.invoke(mbs, new Object[] { mbean, oName });

			_log.info("Registered MBean with Platform MBean Server:" + name);
		} catch (Throwable t) {
			if (t instanceof ClassNotFoundException) {
				_log.error("Using MBean requires at least a JVM version 1.5.");
			}
			_log.error("Unable to register the " + name + " MBean.");
			t.printStackTrace();
		}
	}

}
