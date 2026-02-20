package fruitfly.psi;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import fruitfly.test.FruitflyTestCase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Just a simple record with some comments.
 */
public class CommentedRecordTest extends FruitflyTestCase {

    private static final Logger log = Logger.getInstance(CommentedRecordTest.class);

    public void testCommentsShouldPersist() {
        var inputJava = getTestPsiJavaFile("fruitfly/psi/commented_record/CommentedRecordTestInput.java");
        var outputText = getTestPsiTextFile("fruitfly/psi/commented_record/CommentedRecordTestOutput.txt");

        var recordClass = inputJava.getClasses()[0];
        WriteCommandAction.runWriteCommandAction(inputJava.getProject(), () -> {
            BuilderGenerator.generateBuilderPattern(recordClass);
        });

        log.info("generated: " + inputJava.getText());
        assertThat(inputJava.getText()).isEqualTo(outputText.getText());

    }

}

