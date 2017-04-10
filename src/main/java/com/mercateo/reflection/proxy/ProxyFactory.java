package com.mercateo.reflection.proxy;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.mercateo.reflection.CallInterceptor;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

public class ProxyFactory {

    public static final Objenesis OBJENESIS = new ObjenesisStd();

    public static final ElementMatcher.Junction<MethodDescription> NOT_DECLARED_BY_OBJECT = not(isDeclaredBy(Object.class));

    public static <T> T createProxy(Class<T> clazz) {
        return createProxy(clazz, new CallInterceptor<>(clazz), CallInterceptor.InvocationRecorder.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> clazz, InvocationHandler invocationHandler, Class<?>... interfaces) {
        checkClassForFinalPublicMethods(clazz);
        try {
            final Class<? extends T> loaded = new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .subclass(clazz)
                .implement(interfaces)
                .method(NOT_DECLARED_BY_OBJECT)
                .intercept(InvocationHandlerAdapter.of(invocationHandler))
                .make()
                .load(clazz.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();

            ObjectInstantiator thingyInstantiator = OBJENESIS.getInstantiatorOf(loaded);
            return (T) thingyInstantiator.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error creating proxy for class " + clazz.getSimpleName(), e);
        }
    }

    private static void checkClassForFinalPublicMethods(Class<?> ct) {
        int classModifiers = ct.getModifiers();
        if (Modifier.isFinal(classModifiers)) {
            throw new IllegalStateException("The proxied class is not allowed to be final!");

        }
        Method[] methods = ct.getMethods();
        for (Method method : methods) {
            int modifiers = method.getModifiers();
            if (Modifier.isFinal(modifiers) && !method.getDeclaringClass().equals(Object.class)) {
                throw new IllegalStateException("The proxied class does not have to have any final public method!");
            }
        }
    }

}
