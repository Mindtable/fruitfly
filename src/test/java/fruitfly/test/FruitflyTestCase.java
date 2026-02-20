package fruitfly.test;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiPlainTextFile;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class FruitflyTestCase extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        // Устанавливаем Mock JDK 17 (или JAVA_21, если используете самую новую платформу)
        // Это даст нам и уровень языка с поддержкой record,
        // и базовые классы вроде java.util.Optional в classpath
        return LightJavaCodeInsightFixtureTestCase.JAVA_17;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        /* I prefer my resource files to be right next to the test instead of in
         a separate hierarchy.  Makes refactoring tests and packages easier and
         looking up input/output data easier. */
        myFixture.setTestDataPath("src/test/resources");

        // Создаем "заглушку" (stub) для Optional прямо в памяти перед запуском тестов.
        // IDEA проиндексирует её, и resolve() начнет работать идеально.
        myFixture.addClass("""
            package java.util;
            public final class Optional<T> {
                public static <T> Optional<T> empty() { return null; }
            }
            """);
    }

    public PsiJavaFile getTestPsiJavaFile(String filename) {
        var file = myFixture.configureByFile(filename);

        assertThat(file).isNotNull();
        assertThat(file).isInstanceOf(PsiJavaFile.class);

        return (PsiJavaFile) file;
    }

    public PsiPlainTextFile getTestPsiTextFile(String filename) {
        var file = myFixture.configureByFile(filename);

        assertThat(file).isNotNull();
        assertThat(file).isInstanceOf(PsiPlainTextFile.class);

        return (PsiPlainTextFile) file;
    }

    public PsiFile getTestPsiFile(String filename) {
        return myFixture.configureByFile(filename);
    }

}
