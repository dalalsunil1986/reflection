package com.mercateo.reflection;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import com.mercateo.reflection.proxy.ProxyFactory;

public class Call<T> {

    private final Class<T> clazz;

    private final Method method;

    private final Object[] args;

    public Call(Class<T> clazz, Method method, Object[] args) {
        this.clazz = clazz;
        this.method = method;
        this.args = args;
    }

    public static <T> Call<T> of(Class<T> clazz, Consumer<T> invocation) {
        final T proxy = ProxyFactory.createProxy(clazz);
        invocation.accept(proxy);

        // noinspection unchecked
        return ((InvocationRecorder<T>) proxy).getInvocationRecordingResult();
    }

    public static <T> Method methodOf(Class<T> clazz, Consumer<T> invocation) {
        return of(clazz, invocation).method();
    }

    public Class<T> declaringClass() {
        return clazz;
    }

    public Method method() {
        return method;
    }

    public Object[] args() {
        return args;
    }
}
