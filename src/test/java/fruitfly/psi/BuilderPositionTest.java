package fruitfly.psi;

import com.intellij.openapi.command.WriteCommandAction;
import fruitfly.test.FruitflyTestCase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BuilderPositionTest extends FruitflyTestCase {

    public void testBuilderIsInsertedAfterCaretPosition() {
        var inputJava = getTestPsiJavaFile(
            "fruitfly/psi/builder_position/BuilderPositionTestInput.java");
        var caretOffset = myFixture.getCaretOffset();
        var outputText = getTestPsiTextFile(
            "fruitfly/psi/builder_position/BuilderPositionTestOutput.txt");

        var recordClass = inputJava.getClasses()[0];
        WriteCommandAction.runWriteCommandAction(inputJava.getProject(), () -> {
            BuilderGenerator.generateBuilderPattern(
                recordClass, List.of("value"), caretOffset);
        });

        assertThat(inputJava.getText()).isEqualTo(outputText.getText());
    }
}
