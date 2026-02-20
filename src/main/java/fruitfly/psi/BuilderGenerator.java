package fruitfly.psi;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiRecordComponent;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.StringJoiner;

import static com.intellij.openapi.util.text.StringUtil.decapitalize;
import static fruitfly.ide.ClassMemberChooser.mapRecordComponentNames;
import static java.util.Arrays.stream;

public class BuilderGenerator {

    // TODO:
    //  1. support regular classes
    //  2. do not delete existing builder, but only append with new setter methods
    //  3. tests
    public static void generateBuilderPattern(
        PsiClass recordClass,
        List<String> selectFieldNames
    ) {
        final var selectedFields = mapNamesToFields(recordClass, selectFieldNames);

        removeBuilderClasses(recordClass);

        // denotes the `}` token that declares the end of the class
        final var endOfClass = recordClass.getLastChild();

        // create builder pattern structures and add them to the record
        final var builderClass = recordClass.addBefore(
            createBuilderClass(recordClass, selectedFields),
            endOfClass);

        formatRecordCode(recordClass, builderClass);
    }

    @NotNull
    public static PsiClass createBuilderClass(
        PsiClass recordClass,
        PsiVariable[] components
    ) {
        final var elementFactory =
            JavaPsiFacade.getElementFactory(recordClass.getProject());

        final var text = new StringBuilder(
            "public static final class Builder {");

        // define fields
        for (final var component : components) {
            text.append(createFieldDeclaration(component));
        }

        text.append("\n");
        text.append(createBuilderMethod(recordClass));

        // define setters
        for (final var component : components) {
            final var fieldName = component.getName();
            final var fieldType = component.getType().getCanonicalText();

            text.append("public Builder ")
                .append(fieldName)
                .append("(")
                .append(fieldType)
                .append(" ")
                .append(fieldName)
                .append(") {");
            text.append("this.")
                .append(fieldName)
                .append(" = ")
                .append(fieldName)
                .append(";");
            text.append("return this;");
            text.append("}");
        }

        // Append build method to Builder class
        text.append("public ")
            .append(recordClass.getName())
            .append(" build() {");
        text.append("return new ")
            .append(recordClass.getName())
            .append("(");
        final var parameters = new StringJoiner(", ");
        for (final var component : components) {
            parameters.add("this." + component.getName());
        }
        text.append(parameters).append(");");
        text.append("}}");

        final var dummyClass = elementFactory.createClassFromText(
            text.toString(), recordClass);

       /* It seems the createClassFromText() method generates a _Dummy_ parent
         class for the inner class, we don't care about that - so dig out the
         Builder class and return it */
        return dummyClass.getInnerClasses()[0];
    }

    public static String createFieldDeclaration(
        PsiVariable component
    ) {
        final var fieldName = component.getName();
        final var type = component.getType();
        final var fieldTypeString = type.getCanonicalText();

        // Проверяем, является ли тип классом (а не примитивом или массивом)
        var isOptional = false;
        if (type instanceof PsiClassType classType) {
            // Берем сырой тип (стираем дженерики) без обращения к индексам и resolve()
            final var rawClassName = classType.rawType().getCanonicalText();

            // Проверяем и полное имя (для production), и короткое (для тестов без JDK)
            if ("java.util.Optional".equals(rawClassName) || "Optional".equals(rawClassName)) {
                isOptional = true;
            }
        }

        final var postfix = isOptional ? " = java.util.Optional.empty()" : "";

        return "private " + fieldTypeString + " " + fieldName + postfix + ";";
    }

    public static String createBuilderMethod(
        PsiClass recordClass
    ) {
        final var elementFactory =
            JavaPsiFacade.getElementFactory(recordClass.getProject());

        // Получаем имя record'а и делаем первую букву строчной
        final var recordName = recordClass.getName();
        final var methodName = recordName != null
                               ? decapitalize(recordName)
                               : "builder"; // фолбэк на случай безымянного класса (хотя для records это экзотика)

        // Генерируем статический метод создания билдера с новым именем
        return "public static Builder " + methodName + "() {" +
            "return new Builder();" +
            "}";
    }

    /**
     * Removes the following:
     * - `builder()` instance method
     * - `but()` instance method
     * - `Builder` nested class
     */
    public static void removeBuilderClasses(PsiClass recordClass) {
        // check if Builder class already exists and delete it
        final var innerClasses = recordClass.getInnerClasses();
        for (final var innerClass : innerClasses) {
            if ("Builder".equals(innerClass.getName())) {
                innerClass.delete();
                break; // Assuming only one Builder class exists
            }
        }

        // Check if the but() method already exists and delete it
        final var methods = recordClass.getMethods();
        for (final var method : methods) {
            // Check for method name and parameter count to identify the but() method
            if ("but".equals(method.getName()) && method.getParameterList()
                .getParametersCount() == 0) {
                method.delete();
                break; // Assuming only one but() method exists
            }
        }

        // Check if the builder() method already exists and delete it
        for (final var method : methods) {
            if ("builder".equals(method.getName()) && method.getParameterList()
                .getParametersCount() == 0) {
                method.delete();
                break; // Assuming only one builder() method exists
            }
        }
    }

    /**
     * Maps the given fieldNames to an array of PSI objects (Fields or Record Components)
     */
    @NotNull
    public static PsiVariable[] mapNamesToFields(
        PsiClass targetClass,
        List<String> selectFieldNames
    ) {
        // Получаем либо компоненты рекорда, либо поля обычного класса
        final var variables = targetClass.isRecord()
                              ? targetClass.getRecordComponents()
                              : targetClass.getFields();

        return stream(variables)
            .filter(i -> selectFieldNames.contains(i.getName()))
            .toArray(PsiVariable[]::new);
    }

    /**
     * Reformat code to adhere to project's code style settings
     */
    public static void formatRecordCode(
        PsiClass recordClass,
        PsiElement builderClass
    ) {
        final var project = recordClass.getProject();
        final var file = recordClass.getContainingFile();

        // 1. Внедряем статический импорт до форматирования
        if (file instanceof PsiJavaFile javaFile) {
            addOptionalEmptyStaticImport(javaFile, project);
        }

        final var styleManager = JavaCodeStyleManager.getInstance(recordClass.getProject());
        styleManager.shortenClassReferences(builderClass);
        styleManager.optimizeImports(recordClass.getContainingFile());
    }

    private static void addOptionalEmptyStaticImport(PsiJavaFile file, com.intellij.openapi.project.Project project) {
        final var importList = file.getImportList();
        if (importList == null) return;

        // Проверяем, нет ли уже такого импорта, чтобы не дублировать
        final var alreadyImported = stream(importList.getImportStaticStatements())
            .anyMatch(stmt -> "empty".equals(stmt.getReferenceName()) &&
                stmt.resolveTargetClass() != null &&
                "java.util.Optional".equals(stmt.resolveTargetClass().getQualifiedName()));

        if (!alreadyImported) {
            final var facade = JavaPsiFacade.getInstance(project);
            // Ищем класс Optional в скоупе проекта
            final var optionalClass = facade.findClass("java.util.Optional", GlobalSearchScope.allScope(project));

            if (optionalClass != null) {
                final var factory = JavaPsiFacade.getElementFactory(project);
                // Создаем import static java.util.Optional.empty;
                final var importStatement = factory.createImportStaticStatement(optionalClass, "empty");
                importList.add(importStatement);
            }
        }
    }

    /**
     * Convenience method for the unit tests.
     */
    public static void generateBuilderPattern(
        PsiClass recordClass
    ) {
        generateBuilderPattern(
            recordClass,
            mapRecordComponentNames(recordClass)
        );
    }

}
