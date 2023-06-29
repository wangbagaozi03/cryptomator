package org.cryptomator.common;

import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class LazyProcessedProperties extends PropertiesDecorator {

	private static final Pattern TEMPLATE = Pattern.compile("@\\{(\\w+)}");

	private final Map<String, String> env;

	public LazyProcessedProperties(Properties props, Map<String, String> systemEnvironment) {
		super(props);
		this.env = systemEnvironment;
	}

	@Override
	public String getProperty(String key) {
		var value = delegate.getProperty(key);
		if (key.startsWith("cryptomator.") && value != null) {
			return process(value);
		} else {
			return value;
		}
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		var value = delegate.getProperty(key, defaultValue);
		if (key.startsWith("cryptomator.") && value != null) {
			return process(value);
		} else {
			return value;
		}
	}

	//visible for testing
	String process(String value) {
		return TEMPLATE.matcher(value).replaceAll(match -> //
				switch (match.group(1)) {
					case "appdir" -> resolveFrom("APPDIR", Source.ENV);
					case "appdata" -> resolveFrom("APPDATA", Source.ENV);
					case "localappdata" -> resolveFrom("LOCALAPPDATA", Source.ENV);
					case "userhome" -> resolveFrom("user.home", Source.PROPS);
					default -> {
						LoggerFactory.getLogger(LazyProcessedProperties.class).warn("Unknown variable {} in property value {}.", match.group(), value);
						yield match.group();
					}
				});
	}

	private String resolveFrom(String key, Source src) {
		var val = switch (src) {
			case ENV -> env.get(key);
			case PROPS -> delegate.getProperty(key);
		};
		if (val == null) {
			LoggerFactory.getLogger(LazyProcessedProperties.class).warn("Variable {} used for substitution not found in {}. Replaced with empty string.", key, src);
			return "";
		} else {
			return val.replace("\\", "\\\\");
		}
	}

	private enum Source {
		ENV,
		PROPS;
	}

}
