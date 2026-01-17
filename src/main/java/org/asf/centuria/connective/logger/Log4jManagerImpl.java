package org.asf.centuria.connective.logger;

import java.util.HashMap;

import org.asf.connective.logger.ConnectiveLogger;
import org.asf.connective.logger.ConnectiveLoggerManager;
import org.apache.logging.log4j.LogManager;

public class Log4jManagerImpl extends ConnectiveLoggerManager {

	private HashMap<String, ConnectiveLogger> loggers = new HashMap<String, ConnectiveLogger>();

	public void assignAsMain() {
		implementation = this;
	}

	@Override
	public ConnectiveLogger getLogger(String name) {
		synchronized (loggers) {
			if (loggers.containsKey(name))
				return loggers.get(name);
			ConnectiveLogger logger = new Log4jLoggerImpl(this, LogManager.getLogger("connective-http"));
			loggers.put(name, logger);
			return logger;
		}
	}

}
