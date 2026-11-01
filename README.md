# json-parser

Небольшой JSON-сериализатор для Java: превращает объекты в JSON-строку с помощью рефлексии.
Без внешних зависимостей — только стандартная библиотека JDK.

Проект учебный, но написан как обычная библиотека: отдельный Maven-модуль, тесты на JUnit 5,
аннотации для управления сериализацией и защита от циклических ссылок.

## Возможности

- Сериализация `null`, строк, `boolean` и всех числовых примитивов
  (`byte`, `short`, `int`, `long`, `float`, `double` и их обёрток)
- Сериализация произвольных объектов через рефлексию, включая **приватные** поля
- Вложенные объекты любой глубины
- Обход цепочки суперклассов: поля наследуются
- Аннотация `@JsonField(name = "...")` — переименовать поле в JSON
- Аннотация `@JsonIgnore` — исключить поле из вывода
- Автоматический пропуск `static`, `transient` и синтетических полей
- Экранирование кавычек, обратных слэшей и управляющих символов
- Обнаружение циклических ссылок (`IllegalStateException`)
- Корректная обработка разделяемых ссылок: один объект в двух полях — это не цикл

## Ограничения

Проект намеренно маленький, поэтому многое пока не умеет:

- **Нет десериализации** — только Java → JSON, обратного преобразования нет
- **Нет коллекций и массивов**: `List`, `Map`, `Set`, массивы не поддерживаются
- **Нет `enum`, `Character`, `BigDecimal`, дат** и прочих типов, кроме перечисленных выше
- **Нет форматирования**: вывод всегда компактный, без отступов и переводов строк
- **Нет настройки**: null-поля всегда попадают в вывод, порядок полей задаётся рефлексией
- `NaN` и `Infinity` для `double`/`float` выводятся как есть, а это невалидный JSON
- Очень глубокая вложенность может привести к `StackOverflowError`
- Любой неподдерживаемый тип приводит к `IllegalArgumentException`, а не к пропуску поля

## Требования

- JDK 17 или новее (проект компилируется с `--release 17`)
- Maven 3.6+

## Сборка и тесты

```bash
# запустить тесты
mvn test

# собрать jar в target/
mvn package
```

## Использование

### Примитивы и строки

```java
JsonSerializer.toJson(null);        // null
JsonSerializer.toJson("привет");    // "привет"
JsonSerializer.toJson(42);          // 42
JsonSerializer.toJson(true);        // true
JsonSerializer.toJson(3.14);        // 3.14
```

Строки экранируются автоматически:

```java
JsonSerializer.toJson("он сказал \"да\"");
// "он сказал \"да\""
```

### Объекты

```java
public class Address {
    private String city;
}

public class User {
    private String name;
    private int age;
    private Address address;
}

User user = new User();
user.name = "Иван";
user.age = 30;
user.address = new Address();
user.address.city = "Москва";

String json = JsonSerializer.toJson(user);
// {"name":"Иван","age":30,"address":{"city":"Москва"}}
```

Поля могут быть `private` — сериализатор открывает их через `setAccessible(true)`.
Поля без значения попадут в JSON как `null`.

### Аннотации

```java
public class Account {
    @JsonField(name = "full_name")
    private String name;

    private int age;

    @JsonIgnore
    private String password;
}
```

```java
JsonSerializer.toJson(account);
// {"full_name":"Иван","age":30}
```

`@JsonField` меняет только имя в JSON, поле в Java остаётся прежним.
`@JsonIgnore` полностью исключает поле, даже если оно не `null`.

### Циклические и разделяемые ссылки

Цикл (`a.next = b`, `b.next = a`) — ошибка, сериализация прерывается:

```java
JsonSerializer.toJson(a);
// IllegalStateException: циклическая ссылка найдена
```

А вот один и тот же объект в двух разных полях — это нормальный граф, а не цикл,
и он сериализуется дважды:

```java
SharedLeaf leaf = new SharedLeaf();
leaf.value = 7;

SharedHolder holder = new SharedHolder();
holder.first = leaf;
holder.second = leaf;

JsonSerializer.toJson(holder);
// {"first":{"value":7},"second":{"value":7}}
```

## Как это работает

1. `toJson` создаёт `StringBuilder` и множество уже посещённых объектов `path`.
2. `writeValue` определяет тип значения и пишет его в буфер.
3. Примитивы и строки пишутся сразу, объекты уходят в `writeObject`.
4. `writeObject` собирает поля через рефлексию (с учётом аннотаций и модификаторов)
   и рекурсивно вызывает `writeValue` для каждого значения.
5. Объект добавляется в `path` только на время обхода своего поддерева и удаляется
   после — поэтому `path` отражает текущий стек рекурсии, а не всю историю.

Класс не хранит состояния: всё необходимое создаётся внутри вызова,
поэтому методы можно безопасно вызывать из нескольких потоков.

## Структура проекта

```
json-parser/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/java/my/json/
    │   ├── JsonSerializer.java   # сериализатор
    │   ├── JsonField.java        # аннотация переименования поля
    │   └── JsonIgnore.java       # аннотация исключения поля
    └── test/java/my/json/
        └── JsonSerializerTest.java
```

## Лицензия

Пока не выбрана. Если планируешь публиковать проект, добавь файл `LICENSE`
(например, MIT) — без него по умолчанию действует полный копирайт.
