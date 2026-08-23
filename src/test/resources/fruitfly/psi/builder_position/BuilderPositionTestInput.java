package fruitfly.psi.builder_position;

public record BuilderPositionTestInput(String value) {
    public void beforePointer() {
        System.out.println(<caret>value);
    }

    public void afterPointer() {
        System.out.println(value);
    }
}
