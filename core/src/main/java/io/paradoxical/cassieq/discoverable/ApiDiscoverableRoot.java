package io.paradoxical.cassieq.discoverable;

public interface ApiDiscoverableRoot {
    static String packageName() {
        return ApiDiscoverableRoot.class.getPackage().getName();
    }
}
