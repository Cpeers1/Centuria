package org.asf.centuria.connective.logger;

import org.asf.connective.logger.ConnectiveLogMessage;
import org.asf.connective.logger.ConnectiveLogger;
import org.asf.connective.logger.ConnectiveLoggerManager;
import org.apache.logging.log4j.Logger;

public class Log4jLoggerImpl implements ConnectiveLogger {

	private ConnectiveLoggerManager manager;
	private Logger logger;

	public Log4jLoggerImpl(ConnectiveLoggerManager manager, Logger delegateLogger) {
		this.manager = manager;
		this.logger = delegateLogger;
	}

	private String formatMessage(ConnectiveLogMessage message) {
		String msg = message.getMessage();
		if (message.hasClient())
			msg += " [" + message.resolvePrettyAddressString() + "]";
		return msg;
	}

	@Override
	public void error(ConnectiveLogMessage message) {
		if (message.hasException())
			logger.error(formatMessage(message), message.getException());
		else
			logger.error(formatMessage(message));
	}

	@Override
	public void warn(ConnectiveLogMessage message) {
		if (message.hasException())
			logger.warn(formatMessage(message), message.getException());
		else
			logger.warn(formatMessage(message));
	}

	@Override
	public void info(ConnectiveLogMessage message) {
		if (message.hasException())
			logger.info(formatMessage(message), message.getException());
		else
			logger.info(formatMessage(message));
	}

	@Override
	public void debug(ConnectiveLogMessage message) {
		if (message.hasException())
			logger.debug(formatMessage(message), message.getException());
		else
			logger.debug(formatMessage(message));
	}

	@Override
	public ConnectiveLoggerManager getManager() {
		return manager;
	}

}
