package com.youthlin.example.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import lombok.Getter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author youthlin.chen
 * @date 2020-04-15 17:41
 */
public class PluginManager<T> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Getter
    private final Set<PluginInstance<T>> instances = Sets.newHashSet();

    public PluginInstance<T> install(File file) {
        try {
            PluginMeta meta = getMeta(file);
            PluginClassLoader classLoader = new PluginClassLoader(fileToUrls(file));
            @SuppressWarnings("unchecked")
            Class<IPlugin<T>> pluginClass = (Class<IPlugin<T>>) classLoader.loadClass(meta.getEntrance());
            IPlugin<T> plugin = pluginClass.getDeclaredConstructor().newInstance();
            PluginInstance<T> instance = new PluginInstance<>(meta, classLoader, plugin);
            instances.add(instance);
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException("Can not install plugin from file: " + file, e);
        }
    }

    private PluginMeta getMeta(File file) throws IOException {
        InputStream is;
        if (file.isDirectory()) {
            is = new FileInputStream(new File(file, "plugin.json"));
        } else {
            JarFile jarFile = new JarFile(file);
            ZipEntry metaEntry = jarFile.getEntry("plugin.json");
            is = jarFile.getInputStream(metaEntry);
        }
        return MAPPER.readValue(is, PluginMeta.View.class).toMeta();
    }

    private URL[] fileToUrls(File file) throws MalformedURLException {
        return new URL[]{file.toURI().toURL()};
    }

    public void active(PluginInstance<T> instance, T context) {
        instance.getPlugin().onActive(context);
        instance.setState(State.ACTIVE);
    }

    public void disable(PluginInstance<T> instance, T context) {
        instance.getPlugin().onDisabled(context);
        instance.setState(State.DISABLED);
    }

    public void uninstall(PluginInstance<T> instance) {
        Preconditions.checkArgument(Objects.equals(instance.getState(), State.DISABLED),
                "Can not uninstall cause this plugin is not disabled");
        instance.setState(State.UNINSTALLED);
        instances.remove(instance);
    }

}
