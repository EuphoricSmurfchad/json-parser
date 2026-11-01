package my.json;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.*;

public class JsonSerializer {

    private JsonSerializer() {

    }

    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        Set<Object> path = Collections.newSetFromMap(new IdentityHashMap<>());

        writeValue(sb, value, path);

        return sb.toString();
    }

    private static void writeValue(StringBuilder out, Object value, Set<Object> path) {

        if (value == null) {
            out.append("null");
            return;
        }

        if (value instanceof String) {
            String s = (String) value;
            out.append('"');
            out.append(escape(s));
            out.append('"');
            return;
        }
        if (value instanceof Boolean) {
            out.append(value);
            return;
        }
        if (value instanceof Integer) {
            out.append(value);
            return;
        }
        if (value instanceof Long) {
            out.append(value);
            return;
        }
        if (value instanceof Double) {
            out.append(value);
            return;
        }
        if (value instanceof Float) {
            out.append(value);
            return;
        }
        if (value instanceof Short) {
            out.append(value);
            return;
        }
        if (value instanceof Byte) {
            out.append(value);
            return;
        }
        if (value.getClass().getName().startsWith("java.") || value.getClass().getName().startsWith("javax.")) {
            throw new IllegalArgumentException("Unsupported type: " + value.getClass());
        }
        if (path.contains(value))
            throw new IllegalStateException("циклическая ссылка найдена");
        path.add(value);
        writeObject(out, value, path);
    }

    private static String escape(String s) {

        StringBuilder sb = new StringBuilder(s.length() + 16);

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20)
                        sb.append(String.format("\\u%04x", (int) c));
                    else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static void writeObject(StringBuilder out, Object value, Set<Object> path) {
        out.append('{');
        List<Field> fields = getFields(value);
        int i = 0;
        for (Field field : fields) {
            field.setAccessible(true);
            String name = field.getName();
            Object fieldValue = null;
            try {
                fieldValue = field.get(value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (field.isAnnotationPresent(JsonField.class)) {
                name = field.getAnnotation(JsonField.class).name();

            }
            out.append('"').append(name).append('"');
            out.append(':');

            writeValue(out, fieldValue, path);

            if (++i < fields.size())
                out.append(',');
        }
        out.append('}');

    }

    private static List<Field> getFields(Object value) {
        Class<?> clazz = value.getClass();
        List<Field> fieldsExpected = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                int modifiers = f.getModifiers();
                boolean isStatic = Modifier.isStatic(modifiers);
                boolean isTransient = Modifier.isTransient(modifiers);
                boolean isSynthetic = f.isSynthetic();
                boolean isAnnotationPresent = f.isAnnotationPresent(JsonIgnore.class);
                if (!(isStatic || isAnnotationPresent || isTransient || isSynthetic))
                    fieldsExpected.add(f);
            }
            clazz = clazz.getSuperclass();

        }
        return fieldsExpected;

    }

}
