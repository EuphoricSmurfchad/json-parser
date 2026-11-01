# json-parser
A small, dependency-free JSON serializer for Java 17+.
## Features
- Serializes `null`, strings, booleans and numeric primitives
- Serializes arbitrary objects via reflection, including nested ones
- `@JsonField(name = "...")` renames a field in the output
- `@JsonIgnore` excludes a field from the output
- Skips static, transient and synthetic fields automatically
- Detects circular references and throws `IllegalStateException`
- Escapes quotes, backslashes and control characters
## Build and test
mvn test
## Usage
    String json = JsonSerializer.toJson(user);
