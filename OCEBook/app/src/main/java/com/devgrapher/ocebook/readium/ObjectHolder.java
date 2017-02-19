package com.devgrapher.ocebook.readium;

import org.readium.sdk.android.Container;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brent on 2/16/17.
 */

public class ObjectHolder {
    private static final ObjectHolder INSTANCE = new ObjectHolder();
    private final Map<Long, Container> containers = new HashMap<Long, Container>();
    private final Map<Long, ReadiumContext> contexts = new HashMap<Long, ReadiumContext>();

    public static ObjectHolder getInstance() {
        return INSTANCE;
    }

    public Container getContainer(Object id) {
        return containers.get(id);
    }

    public Container removeContainer(Object id) {
        return containers.remove(id);
    }

    public Container putContainer(Long key, Container value) {
        return containers.put(key, value);
    }

    public ReadiumContext getContext(Object id) {
        return contexts.get(id);
    }

    public ReadiumContext removeContext(Object id) {
        return contexts.remove(id);
    }

    public ReadiumContext putContext(Long key, ReadiumContext value) {
        return contexts.put(key, value);
    }
}
