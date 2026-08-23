# Generated builder style requirements

Fruitfly-generated builder code must follow these rules.

## `Set` fields

Every selected field whose raw type is `java.util.Set` must have an immutable
empty default in the generated builder:

```java
private Set<String> tags = Set.of();
```

Every generated setter for a `Set` field must make an immutable defensive copy:

```java
public Builder tags(Set<String> tags) {
    this.tags = Set.copyOf(tags);
    return this;
}
```

## Builder position

The generated `Builder` class must be inserted after the editor caret position.
Because a nested class cannot be inserted inside another class member, a caret
inside a field, method, initializer, or nested class means insertion after that
entire member. If the caret is between members, the builder is inserted before
the next member. If no member follows the caret, the builder is inserted at the
end of the target class.
