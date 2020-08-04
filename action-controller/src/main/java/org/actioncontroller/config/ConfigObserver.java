package org.actioncontroller.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Used to monitor a set of configuration files and notify {@link ConfigListener}s
 * on initialization and change. Given a directory and an application name,
 * appropriate resources and files are monitored with a {@link ConfigDirectoryLoader}
 */
public class ConfigObserver {
    private static final Logger logger = LoggerFactory.getLogger(ConfigObserver.class);
    private Map<String, String> currentConfiguration;
    private final List<ConfigListener> listeners = new ArrayList<>();

    private final ConfigLoader configLoader;

    public ConfigObserver(String applicationName) {
        this(new File("."), applicationName, new ArrayList<>());
    }

    public ConfigObserver(File configDirectory, String applicationName) {
        this(configDirectory, applicationName, new ArrayList<>());
    }

    public ConfigObserver(File configDirectory, String applicationName, List<String> profiles) {
        this(new ConfigDirectoryLoader(configDirectory, applicationName, profiles));
    }

    public ConfigObserver(ConfigLoader configLoader) {
        this.configLoader = configLoader;
        currentConfiguration = this.configLoader.loadConfiguration();
        this.configLoader.watch(this::updateConfiguration);
    }

    /**
     * The generic observer method. Call {@link ConfigListener#onConfigChanged} on
     * the listener when this method is first called and again each time config changes.
     */
    public ConfigObserver onConfigChange(ConfigListener listener) {
        this.listeners.add(listener);
        notifyListener(listener, null, new ConfigMap(currentConfiguration));
        return this;
    }

    public <T> ConfigObserver onSingleConfigValue(String key, Function<String, T> transformer, T defaultValue, ConfigValueListener<T> listener) {
        return onConfigChange(new ConfigListener() {
            @Override
            public void onConfigChanged(Set<String> changedKeys, ConfigMap newConfiguration) throws Exception {
                if (changeIncludes(changedKeys, key)) {
                    String value = newConfiguration.getOrDefault(key, null);
                    T configValue;
                    if (value == null) {
                        configValue = defaultValue;
                    } else {
                        try {
                            configValue = transformer.apply(value);
                        } catch (Exception e) {
                            throw new ConfigException("Failed to convert " + key + "=" + value, e);
                        }
                    }
                    logger.info("onConfigChanged key={} value={}", key, configValue);
                    listener.apply(configValue);
                }
            }
        });
    }

    public ConfigObserver onStringValue(String key, String defaultValue, ConfigValueListener<String> listener) {
        return onSingleConfigValue(key, Function.identity(), defaultValue, listener);
    }

    public ConfigObserver onIntValue(String key, int defaultValue, ConfigValueListener<Integer> listener) {
        return onSingleConfigValue(key, Integer::parseInt, defaultValue, listener);
    }

    public ConfigObserver onLongValue(String key, long defaultValue, ConfigValueListener<Long> listener) {
        return onSingleConfigValue(key, Long::parseLong, defaultValue, listener);
    }

    public ConfigObserver onInetSocketAddress(String key, int defaultPort, ConfigValueListener<InetSocketAddress> listener) {
        return onInetSocketAddress(key, new InetSocketAddress(defaultPort), listener);
    }

    public ConfigObserver onInetSocketAddress(String key, InetSocketAddress defaultAddress, ConfigValueListener<InetSocketAddress> listener) {
        return onSingleConfigValue(key, ConfigListener::asInetSocketAddress, defaultAddress, listener);
    }

    public ConfigObserver onDurationValue(String key, Duration defaultValue, ConfigValueListener<Duration> listener) {
        return onSingleConfigValue(key, Duration::parse, defaultValue, listener);
    }

    public ConfigObserver onStringListValue(String key, String defaultValue, ConfigValueListener<List<String>> listener) {
        return onSingleConfigValue(key, ConfigObserver::parseStringList, defaultValue != null ?  parseStringList(defaultValue) : null, listener);
    }

    public <T> ConfigObserver onPrefixedValue(String prefix, ConfigValueListener<Map<String, String>> listener) {
        return onPrefixedOptionalValue(
                prefix,
                opt -> listener.apply(opt.orElseThrow(() -> new ConfigException("Missing required property group " + prefix)))
        );
    }

    public <T> ConfigObserver onPrefixedValue(String prefix, ConfigListener.Transformer<T> transformer, ConfigValueListener<T> listener) {
        return onPrefixedOptionalValue(
                prefix,
                transformer,
                opt -> listener.apply(opt.orElseThrow(() -> new ConfigException("Missing required property group " + prefix)))
        );
    }

    public <T> ConfigObserver onPrefixedOptionalValue(String prefix, ConfigListener.Transformer<T> transformer, ConfigValueListener<Optional<T>> listener) {
        return onConfigChange(new ConfigListener() {
            @Override
            public void onConfigChanged(Set<String> changedKeys, ConfigMap newConfiguration) {
                if (changeIncludes(changedKeys, prefix)) {
                    Optional<Map<String, String>> configuration = newConfiguration.subMap(prefix).map(Function.identity());
                    Optional<T> configValue = Optional.empty();
                    if (configuration.isPresent()) {
                        try {
                            configValue = Optional.of(transformer.apply(configuration.get()));
                        } catch (Exception e) {
                            throw new ConfigException("Failed to convert " + configuration, e);
                        }
                    }
                    logger.info("onConfigChanged config={} value={}", configuration, configValue);
                    try {
                        listener.apply(configValue);
                    } catch (Exception e1) {
                        throw new ConfigException("While applying " + configuration, e1);
                    }
                }
            }
        });
    }

    public ConfigObserver onPrefixedOptionalValue(String prefix, ConfigValueListener<Optional<Map<String, String>>> listener) {
        return onConfigChange(new ConfigListener() {
            @Override
            public void onConfigChanged(Set<String> changedKeys, ConfigMap newConfiguration) {
                if (changeIncludes(changedKeys, prefix)) {
                    Optional<Map<String, String>> configuration = newConfiguration.subMap(prefix).map(Function.identity());
                    try {
                        listener.apply(configuration);
                        logger.info("onConfigChange key={} value={}", prefix, configuration);
                    } catch (Exception e) {
                        throw new ConfigException("While applying " + configuration, e);
                    }
                }
            }
        });
    }

    private static List<String> parseStringList(String value) {
        return Stream.of(value.split(",")).map(String::trim).collect(Collectors.toList());
    }

    public void updateConfiguration(Map<String, String> newConfiguration) {
        logger.trace("New configuration {}", newConfiguration);
        Set<String> changedKeys = findChangedKeys(newConfiguration, currentConfiguration);
        this.currentConfiguration = newConfiguration;
        handleConfigurationChanged(changedKeys, new ConfigMap(newConfiguration));
    }

    private Set<String> findChangedKeys(Map<String, String> newConfiguration, Map<String, String> currentConfiguration) {
        Set<String> changedKeys = new HashSet<>();
        changedKeys.addAll(newConfiguration.keySet());
        changedKeys.addAll(currentConfiguration.keySet());

        changedKeys.removeIf(key -> Objects.equals(newConfiguration.get(key), this.currentConfiguration.get(key)));
        return changedKeys;
    }

    protected void handleConfigurationChanged(Set<String> changedKeys, ConfigMap newConfiguration) {
        for (ConfigListener listener : listeners) {
            notifyListener(listener, changedKeys, newConfiguration);
        }
    }

    private void notifyListener(ConfigListener listener, Set<String> changedKeys, ConfigMap newConfiguration) {
        try {
            logger.trace("Notifying listener {}", listener);
            listener.onConfigChanged(changedKeys, newConfiguration);
        } catch (Exception e) {
            logger.error("Failed to notify listener while reloading {}", configLoader.describe(), e);
        }
    }
}
