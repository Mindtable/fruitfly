package fruitfly.psi;

import com.intellij.openapi.command.WriteCommandAction;
import fruitfly.test.FruitflyTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class SetFieldTest extends FruitflyTestCase {

    public void testSetFieldsUseImmutableDefaultsAndCopies() {
        var inputJava = getTestPsiJavaFile(
            "fruitfly/psi/set_field/SetFieldTestInput.java");
        var outputText = getTestPsiTextFile(
            "fruitfly/psi/set_field/SetFieldTestOutput.txt");

        var recordClass = inputJava.getClasses()[0];
        WriteCommandAction.runWriteCommandAction(inputJava.getProject(), () -> {
            BuilderGenerator.generateBuilderPattern(recordClass);
        });

        assertThat(inputJava.getText()).isEqualTo(outputText.getText());
    }
}
