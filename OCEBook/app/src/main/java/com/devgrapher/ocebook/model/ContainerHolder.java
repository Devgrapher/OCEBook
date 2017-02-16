package com.devgrapher.ocebook.model;

import org.readium.sdk.android.Container;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brent on 2/16/17.
 */

public class ContainerHolder {
    private static final ContainerHolder INSTANCE = new ContainerHolder();
    private final Map<Long, Container> containers = new HashMap<Long, Container>();

    public static ContainerHolder getInstance() {
        return INSTANCE;
    }

    public Container get(Object id) {
        return containers.get(id);
    }

    public Container remove(Object id) {
        return containers.remove(id);
    }

    public Container put(Long key, Container value) {
        return containers.put(key, value);
    }
}
