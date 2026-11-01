package my.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import my.json.JsonSerializer;

public class JsonSerializerTest {

    // Тесты на сериализацию строк
    @Test
    public void serializesNull() {
        String expected = "null";
        String result = JsonSerializer.toJson(null);

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void wrapsStringInQuotes() {
        String expected = "\"hi\"";
        String result = JsonSerializer.toJson("hi");

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void serializesIntegerWithoutQuotes() {
        String expected = "42";
        String result = JsonSerializer.toJson(42);

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void serializesLongWithoutQuotes() {
        String expected = "9223372036854775807";
        String result = JsonSerializer.toJson(9223372036854775807L);

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void serializesBooleanWithoutQuotes() {
        String expected = "true";
        String result = JsonSerializer.toJson(true);

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void serializesDoubleWithoutQuotes() {
        String expected = "3.14";
        String result = JsonSerializer.toJson(3.14);

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void serializesFloatWithoutQuotes() {
        String expected = "2.5";
        String result = JsonSerializer.toJson(2.5f);

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void serializesShortWithoutQuotes() {
        String expected = "100";
        String result = JsonSerializer.toJson((short) 100);

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void serializesByteWithoutQuotes() {
        String expected = "127";
        String result = JsonSerializer.toJson((byte) 127);

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void serializesEmptyStringAsEmptyQuotedString() {
        String expected = "\"\"";
        String result = JsonSerializer.toJson("");

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void throwsOnUnsupportedType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            JsonSerializer.toJson(new StringBuilder());
        });
    }

    @Test
    public void escapesQuotesInsideString() {
        String expected = "\"fd\\\"fd\"";
        String result = JsonSerializer.toJson("fd\"fd");
        Assertions.assertEquals(expected, result);
    }

    static class Node {
        Node next;
        String value;
    }

    static class User {
        public User next;
        public int age;
    }

    @Test
    public void throwsOnCircularReference() {
        User a = new User();
        User b = new User();
        a.next = b;
        b.next = a;
        a.age = 10;
        b.age = 20;
        Assertions.assertThrows(IllegalStateException.class, () -> {
            JsonSerializer.toJson(a);
        });
    }

    // Тесты на сериализацию объектов

    static class SimplePerson {
        public String name;
        public int age;
    }

    @Test
    public void serializesSimpleObject() {
        SimplePerson p = new SimplePerson();
        p.name = "Alice";
        p.age = 30;
        String result = JsonSerializer.toJson(p);
        Assertions.assertTrue(result.contains("\"name\":\"Alice\""));
        Assertions.assertTrue(result.contains("\"age\":30"));
        Assertions.assertTrue(result.startsWith("{"));
        Assertions.assertTrue(result.endsWith("}"));
    }

    static class NestedAddress {
        public String city;
    }

    static class Employee {
        public String name;
        public NestedAddress address;
    }

    @Test
    public void serializesNestedObjects() {
        Employee emp = new Employee();
        emp.name = "Bob";
        emp.address = new NestedAddress();
        emp.address.city = "Moscow";
        String result = JsonSerializer.toJson(emp);
        Assertions.assertTrue(result.contains("\"name\":\"Bob\""));
        Assertions.assertTrue(result.contains("\"city\":\"Moscow\""));
    }

    @Test
    public void serializesObjectWithNullField() {
        SimplePerson p = new SimplePerson();
        p.name = "Charlie";
        p.age = 0;
        String result = JsonSerializer.toJson(p);
        Assertions.assertTrue(result.contains("\"name\":\"Charlie\""));
        Assertions.assertTrue(result.contains("\"age\":0"));
    }

    static class PersonWithNull {
        public String name;
        public String nickname;
    }

    @Test
    public void serializesObjectWithNullValue() {
        PersonWithNull p = new PersonWithNull();
        p.name = "Grace";
        p.nickname = null;
        String result = JsonSerializer.toJson(p);
        Assertions.assertTrue(result.contains("\"name\":\"Grace\""));
        Assertions.assertTrue(result.contains("\"nickname\":null"));
    }

    // Тесты на аннотации

    static class PersonWithAnnotations {
        @my.json.JsonField(name = "full_name")
        public String name;

        @my.json.JsonIgnore
        public int secretAge;

        public String city;
    }

    @Test
    public void renamesFieldWithJsonFieldAnnotation() {
        PersonWithAnnotations p = new PersonWithAnnotations();
        p.name = "Diana";
        p.secretAge = 25;
        p.city = "Kiev";
        String result = JsonSerializer.toJson(p);
        Assertions.assertTrue(result.contains("\"full_name\":\"Diana\""));
        Assertions.assertFalse(result.contains("\"name\""));
    }

    @Test
    public void skipsFieldWithJsonIgnoreAnnotation() {
        PersonWithAnnotations p = new PersonWithAnnotations();
        p.name = "Eve";
        p.secretAge = 99;
        p.city = "Minsk";
        String result = JsonSerializer.toJson(p);
        Assertions.assertFalse(result.contains("\"secretAge\""));
        Assertions.assertFalse(result.contains("99"));
    }

    @Test
    public void handlesMixedAnnotations() {
        PersonWithAnnotations p = new PersonWithAnnotations();
        p.name = "Frank";
        p.secretAge = 40;
        p.city = "Spb";
        String result = JsonSerializer.toJson(p);
        Assertions.assertTrue(result.contains("\"full_name\":\"Frank\""));
        Assertions.assertTrue(result.contains("\"city\":\"Spb\""));
        Assertions.assertFalse(result.contains("\"secretAge\""));
    }

    static class SharedLeaf {
        public int value;
    }

    static class SharedHolder {
        public Object first;
        public Object second;
    }

    @Test
    public void serializesSharedReferenceWithoutFalseCycle() {
        SharedLeaf leaf = new SharedLeaf();
        leaf.value = 7;

        SharedHolder holder = new SharedHolder();
        holder.first = leaf;
        holder.second = leaf;

        String result = JsonSerializer.toJson(holder);

        Assertions.assertTrue(result.contains("\"first\":{\"value\":7}"));
        Assertions.assertTrue(result.contains("\"second\":{\"value\":7}"));
    }
}
