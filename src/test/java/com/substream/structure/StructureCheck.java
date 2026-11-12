package com.substream.structure;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROVIDED — DO NOT MODIFY.
 *
 * Used by the structure tests of this folder: collects every difference between one class and
 * README section 4, and reports them together.
 */
final class StructureCheck {

    private final Class<?> type;
    private final List<String> problems = new ArrayList<>();

    StructureCheck(Class<?> type) {
        this.type = type;
    }

    void constructor(Class<?>... parameters) {
        try {
            if (!Modifier.isPublic(type.getDeclaredConstructor(parameters).getModifiers())) {
                problems.add("Constructor " + signature(type.getSimpleName(), parameters) + " must be public");
            }
        } catch (NoSuchMethodException e) {
            problems.add("Missing public constructor " + signature(type.getSimpleName(), parameters));
        }
    }

    void getter(Class<?> returnType, String name) {
        instanceMethod(returnType, name);
    }

    void instanceMethod(Class<?> returnType, String name, Class<?>... parameters) {
        method(false, returnType, name, parameters);
    }

    void staticMethod(Class<?> returnType, String name, Class<?>... parameters) {
        method(true, returnType, name, parameters);
    }

    private void method(boolean isStatic, Class<?> returnType, String name, Class<?>... parameters) {
        String expected = (isStatic ? "static " : "") + signature(name, parameters)
                + " : " + returnType.getSimpleName();
        Method method;
        try {
            method = type.getDeclaredMethod(name, parameters);
        } catch (NoSuchMethodException e) {
            problems.add("Missing public method " + type.getSimpleName() + "." + expected);
            return;
        }
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers)
                || Modifier.isStatic(modifiers) != isStatic
                || !method.getReturnType().equals(returnType)) {
            problems.add("Wrong declaration, expected public " + type.getSimpleName() + "." + expected);
        }
    }

    void noPublicMethod(String name) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name) && Modifier.isPublic(method.getModifiers())) {
                problems.add(type.getSimpleName() + "." + name + "() must not be public (see the specification)");
            }
        }
    }

    /** Every listed field exists; every instance field is private; there are no setters. */
    void fields(String... names) {
        for (String name : names) {
            try {
                Field field = type.getDeclaredField(name);
                if (Modifier.isStatic(field.getModifiers())) {
                    problems.add("Field " + type.getSimpleName() + "." + name + " must not be static");
                }
            } catch (NoSuchFieldException e) {
                problems.add("Missing field " + type.getSimpleName() + "." + name);
            }
        }
        for (Field field : type.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!field.isSynthetic() && !field.isEnumConstant() && !Modifier.isStatic(modifiers)
                    && !Modifier.isPrivate(modifiers)) {
                problems.add("Field " + type.getSimpleName() + "." + field.getName() + " must be private");
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().startsWith("set") && Modifier.isPublic(method.getModifiers())) {
                problems.add("Setters are not allowed: " + type.getSimpleName() + "." + method.getName());
            }
        }
    }

    void noField(String name) {
        try {
            type.getDeclaredField(name);
            problems.add(type.getSimpleName() + " must not store '" + name + "' in a field (calculate it)");
        } catch (NoSuchFieldException expected) {
            // correct
        }
    }

    void constants(String... names) {
        for (String name : names) {
            try {
                if (!type.getDeclaredField(name).isEnumConstant()) {
                    problems.add(name + " must be an enum constant of " + type.getSimpleName());
                }
            } catch (NoSuchFieldException e) {
                problems.add("Missing enum constant " + type.getSimpleName() + "." + name);
            }
        }
    }

    void publicConstant(String name, Class<?> fieldType) {
        try {
            Field field = type.getDeclaredField(name);
            int modifiers = field.getModifiers();
            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)
                    || !field.getType().equals(fieldType)) {
                problems.add(type.getSimpleName() + "." + name + " must be public static final "
                        + fieldType.getSimpleName());
            }
        } catch (NoSuchFieldException e) {
            problems.add("Missing constant " + type.getSimpleName() + "." + name);
        }
    }

    void verify() {
        assertTrue(problems.isEmpty(), () -> type.getSimpleName() + " does not match README section 4:\n  - "
                + String.join("\n  - ", problems));
    }

    private static String signature(String name, Class<?>... parameters) {
        return name + "(" + String.join(", ",
                Arrays.stream(parameters).map(Class::getSimpleName).toList()) + ")";
    }
}
