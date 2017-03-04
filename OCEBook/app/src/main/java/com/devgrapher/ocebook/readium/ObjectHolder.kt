package com.devgrapher.ocebook.readium

import org.readium.sdk.android.Container

import java.util.HashMap

/**
 * Created by Brent on 2/16/17.
 */

class ObjectHolder {
    private var containers = HashMap<Long, Container>()
    private var contexts = HashMap<Long, ReadiumContext>()

    fun getContainer(id: Long): Container? {
        return containers.get(id)
    }

    fun removeContainer(id: Long): Container? {
        return containers.remove(id)
    }

    fun putContainer(key: Long, value: Container): Container? {
        return containers.put(key, value)
    }

    fun getContext(id: Long): ReadiumContext? {
        return contexts.get(id)
    }

    fun removeContext(id: Long): ReadiumContext? {
        return contexts.remove(id)
    }

    fun putContext(key: Long, value: ReadiumContext): ReadiumContext? {
        return contexts.put(key, value)
    }

    companion object {
        val instance = ObjectHolder()
    }
}
