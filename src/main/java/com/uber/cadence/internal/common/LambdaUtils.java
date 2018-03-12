package com.uber.cadence.internal.common;

import com.uber.cadence.workflow.Functions;
import sun.reflect.MethodAccessor;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class LambdaUtils {

    /**
     * Get target of the method reference that was converted to a lambda.
     *
     * @param l lambda expression that could be a method reference.
     * @return either target of the method reference or null if it is not reference in form object::method.
     */
    public static Object getTarget(SerializedLambda l) {
        if (l == null) {
            return null;
        }
        // If lambda is a method call on an object then the first captured argument is the object.
        if (l.getCapturedArgCount() > 0) {
            return l.getCapturedArg(0);
        }
        return null;
    }

    /**
     * Unfortunate sorcery to reflect on lambda. Works only if function that lambda implements is serializable.
     * This is why {@link Functions} is needed as all its functions are serializable.
     * @param lambda lambda that potentially implements {@link java.io.Serializable}.
     * @return lambda in {@link SerializedLambda} form or null if its function doesn't implement Serializable.
     */
    public static SerializedLambda toSerializedLambda(Object lambda) {
        for (Class<?> cl = lambda.getClass(); cl != null; cl = cl.getSuperclass()) {
            try {
                Method m = cl.getDeclaredMethod("writeReplace");
                m.setAccessible(true);
                Object replacement = m.invoke(lambda);
                if (!(replacement instanceof SerializedLambda))
                    break;// custom interface implementation
                return (SerializedLambda) replacement;
            } catch (NoSuchMethodException e) {
            } catch (IllegalAccessException | InvocationTargetException e) {
                break;
            }
        }
        return null;
    }

//    private static Method getMethodFromLambda(Object lambda) throws Exception {
//        Constructor<?> c = Method.class.getDeclaredConstructors()[0];
//        c.setAccessible(true);
//        Method m = (Method) c.newInstance(null, null, null, null, null, 0, 0, null, null, null, null);
//        m.setAccessible(true); //sets override field to true
//
//        //m.methodAccessor = new LambdaAccessor(...)
//        Field ma = Method.class.getDeclaredField("methodAccessor");
//        ma.setAccessible(true);
//        ma.set(m, new LambdaAccessor(array -> lambda.apply((String) array[0])));
//
//        return m;
//    }
//
//    static class LambdaAccessor implements MethodAccessor {
//        private final Object lambda;
//        public LambdaAccessor(Function<Object[], Object> lambda) {
//            this.lambda = lambda;
//        }
//
//        @Override public Object invoke(Object o, Object[] os) {
//            return lambda.apply(os);
//        }
//    }

    /**
     * Prohibits instantiation.
     */
    private LambdaUtils() {
    }
}
