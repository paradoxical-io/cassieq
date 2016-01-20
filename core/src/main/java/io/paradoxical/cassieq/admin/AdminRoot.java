package io.paradoxical.cassieq.admin;

public interface AdminRoot {
    static String packageName() {
        return AdminRoot.class.getPackage().getName();
    }
}
