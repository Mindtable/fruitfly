package fruitfly.psi;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import fruitfly.test.FruitflyTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleClassTest extends FruitflyTestCase {

    private static final Logger log = Logger.getInstance(SimpleClassTest.class);

    public void testSimple() {
        var inputJava = getTestPsiJavaFile("fruitfly/psi/simple_class/Input.java");
        var outputText = getTestPsiTextFile("fruitfly/psi/simple_class/Output.txt");

        var recordClass = inputJava.getClasses()[0];
        WriteCommandAction.runWriteCommandAction(inputJava.getProject(), () -> {
            BuilderGenerator.generateBuilderPattern(recordClass);
        });

        log.info("generated: " + inputJava.getText());
        assertThat(inputJava.getText()).isEqualTo(outputText.getText());
    }

}
