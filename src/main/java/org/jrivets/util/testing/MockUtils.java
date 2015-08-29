package org.jrivets.util.testing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Naive utilities which would be helpful for unit-testing to inject or invoke
 * some private class members/methods.
 * 
 * @author Dmitry Spasibenko
 */
public final class MockUtils {

    private MockUtils() {
        throw new AssertionError("The MockUtils class cannot be instantiated.");
    }

    public static boolean doesExist(Class<?> clazz, String fieldName) {
        try {
            clazz.getDeclaredField(fieldName);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    public static <E> Object getFieldValue(E entity, String fieldName) {
        try {
            Class<?> clazz = entity.getClass();
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(entity);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot obtain field " + fieldName + " value", ex);
        }
    }

    public static <E, V> boolean setFieldValue(E entity, String fieldName, V value) {
        return setFieldValue(entity.getClass(), entity, fieldName, value);
    }

    public static <V> boolean setFieldValue(Class<?> clazz, Object entity, String fieldName, V value) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(entity, value);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException("Cannot set field " + fieldName + " value to " + value, ex);
        }
    }

    public static <V> boolean setStaticFieldValue(Class<?> clazz, String fieldName, V value) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(null, value);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot set static field " + fieldName, ex);
        }
        return true;
    }

    /**
     * Produce a shallow copy of an instance. USE WITH EXTRA CARE!!! It can
     * bring a lot of surprises, with trivial and non-trivial objects! You have
     * to clearly understand what is going on if you use the method.
     * 
     * @param e
     *            an instance to be copied
     * @return copy of the instance
     */
    @SuppressWarnings("unchecked")
    public static <E> E makeCopy(E e) {
        if (e == null) {
            return null;
        }
        Class<E> clazz = (Class<E>) e.getClass();
        Constructor<E> c = getDefaultConstructor(clazz);
        if (c == null) {
            c = getCopyConstructor(clazz);
            if (c != null) {
                try {
                    c.setAccessible(true);
                    return (E) c.newInstance(e);
                } catch (Exception ex) {
                    throw new RuntimeException("Could not invoke constructor copy.");
                }
            }
            throw new RuntimeException("No default or copy constructor for class " + clazz);
        }
        try {
            c.setAccessible(true);
            E e1 = (E) c.newInstance();
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                setFieldValue(e1, f.getName(), getFieldValue(e, f.getName()));
            }
            return e1;
        } catch (Exception ex) {
            throw new RuntimeException("Cannot clone " + e, ex);
        }
    }

    public static <E> E createInstance(Class<E> clazz) {
        Constructor<E> c = getDefaultConstructor(clazz);
        if (c == null) {
            throw new IllegalArgumentException("No default constructor for " + clazz);
        }
        try {
            c.setAccessible(true);
            return (E) c.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException("Cannot create an instance of " + clazz, ex);
        }
    }

    public static String getAnnotatedField(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                if (f.getAnnotation(annotationClass) != null) {
                    return f.getName();
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <E> Constructor<E> getDefaultConstructor(Class<E> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> c : constructors) {
            if (c.getGenericParameterTypes().length == 0) {
                return (Constructor<E>) c;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <E> Constructor<E> getCopyConstructor(Class<E> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> c : constructors) {
            Type[] types = c.getGenericParameterTypes();
            if (types.length == 1 && types[0].equals(clazz)) {
                return (Constructor<E>) c;
            }
        }
        return null;
    }

    public static <E> String objectToString(E e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass()).append(": {");
        printFields(e, sb, true);
        sb.append("}");
        return sb.toString();
    }
    
    public static <E> void printFields(E e, StringBuilder sb, boolean includeSuperClasses) {
        printFields(e.getClass(), e, sb, includeSuperClasses);
    }
    
    public static <E> void printFields(Class<?> clazz, E e, StringBuilder sb, boolean includeSuperClasses) {
        while (clazz != null) {
            printFields(clazz, e, sb);
            clazz = includeSuperClasses ? clazz.getSuperclass() : null;
        }
    }
    
    public static <E> void printFields(Class<?> clazz, E e, StringBuilder sb) {
        boolean comma = false;
        for (Field field: clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String name = field.getName();
            if (comma) {
                sb.append(", ");
            }
            sb.append(name).append("=");
            try {
                Object value = field.get(e);
                sb.append(value);
            } catch (Exception ex) {
                sb.append("<<<N/A>>>");
            }
            comma = true;
        }
    }

    public static <E> Object invoke(E e, String methodName) {
        return invoke(e, methodName, new Object[0]);
    }
    
    @SuppressWarnings("unchecked")
    public static <E> Object invoke(E e, String methodName, Object... params) {
        Class<E> clazz = (Class<E>) e.getClass();
        try {
            Method method = clazz.getDeclaredMethod(methodName, paramsClasses(params));
            method.setAccessible(true);
            return method.invoke(e, params);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot execute " + methodName, ex);
        }
    }

    private static Class<?>[] paramsClasses(Object... params) {
        if (params == null || params.length == 0) {
            return new Class<?>[0];
        }
        Class<?>[] classez = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            Object obj = params[i];
            if (obj == null) {
                classez[i] = null;
            } else {
                classez[i] = obj.getClass();
            }
        }
        return classez;
    }
}
