package org.molkex.spring.minimalrest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.io.IOUtils;
import org.molkex.spring.minimalrest.tools.Json;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MinimalStorage<V extends MinimalStorage.Entity> {

    static String ROOT_PATH = "./tmp/stores/";

    public static class Entity implements Serializable {
        @JsonIgnore MinimalStorage store;
        @JsonIgnore String key;

        protected void commit() {
            if (store != null) {
                store.save(key, this);
            }
        }

        protected void init() {}
    }

    protected Map<String,V> data = new HashMap<>();

    protected File dir;

    protected Class<V> clazz;

    public MinimalStorage(String name, Class<V> clazz) {
        this.clazz = clazz;
        dir = new File(ROOT_PATH+name.replaceAll("/", "-"));
        dir.mkdirs();
    }

    public Collection<V> values() {
        return data.values();
    }

    public void put(String key, V value) {
        if (!data.containsKey(key)) {
            value.init();
        }
        data.put(key, save(key, value));
    }

    public V get(String key) {
        if (!data.containsKey(key)) {
            V entity = read(key);
            if (entity != null) {
                data.put(key, entity);
            }
        }
        return data.get(key);
    }

    public V computeIfAbsent(String key, Function<String, V> create) {
        if (!data.containsKey(key)) {
            get(key); // tries to read from storage
        }
        return data.computeIfAbsent(key, (k)-> {
            V value = create.apply(key);
            value.init();
            return save(key, value);
        });
    }

    public void delete(String key) {
        File f = new File(dir, key.hashCode() + ".ministore");
        if (f.exists() && f.delete()) {
            data.remove(key);
        }
    }

    V read(String key) {
        try (InputStream in = readStream(key)) {
            V entity = deserialize(IOUtils.toByteArray(in));
            entity.key = key;
            entity.store = this;
            entity.init();
            return entity;
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return null;
    }

    V save(String key, V value) {
        value.key = key;
        value.store = this;
        try (OutputStream o = writeStream(key)) {
            o.write(serialize(value));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    protected InputStream readStream(String key) throws IOException {
        return new FileInputStream(new File(dir, key.hashCode() + ".ministore"));
    }

    protected OutputStream writeStream(String key) throws IOException {
        return new FileOutputStream(new File(dir, key.hashCode() + ".ministore"));
    }

    protected byte[] serialize(V value) {
        return Json.write(value).getBytes();
    }

    protected V deserialize(byte[] bytes) {
        return Json.read(new String(bytes), clazz);
    }
}
