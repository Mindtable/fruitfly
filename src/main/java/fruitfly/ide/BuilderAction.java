package fruitfly.ide;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;
import static com.intellij.psi.util.PsiTreeUtil.getParentOfType;
import static fruitfly.ide.ClassMemberChooser.chooseFieldNames;
import static fruitfly.psi.BuilderGenerator.generateBuilderPattern;

/**
 * Defines the `Fruitfly Builder` item in the generate menu.
 */
public class BuilderAction extends AnAction {

    private static final Logger log = Logger.getInstance(BuilderAction.class);

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * defines the visibility (item is only visible when you
     * have a valid class or record selected)
     */
    @Override
    public void update(@NotNull AnActionEvent event) {
        // log.warn("update()");
        final var project = event.getProject();
        final var editor = event.getData(CommonDataKeys.EDITOR);
        if (project == null || editor == null) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }

        if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
            /* this branch was added when I was trying something else,
               logging it to see when it actually happens, if ever? */
            log.info("readAccessAllowed=false");
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }

        // Используем новый метод, который ищет подходящий класс
        event.getPresentation().setEnabledAndVisible(getValidTargetClass(event) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final var project = event.getProject();
        final var editor = event.getData(CommonDataKeys.EDITOR);
        final var psiFile = event.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || psiFile == null) {
            log.warn("actionPerformed() no project, editor or file");
            return;
        }

        // Ищем элемент под курсором и находим ближайший к нему класс (PsiClass)
        final var element = psiFile.findElementAt(editor.getCaretModel().getOffset());
        final var targetClass = getParentOfType(element, PsiClass.class);

        if (targetClass == null || targetClass.isEnum() || targetClass.isInterface() || targetClass.isAnnotationType()) {
            log.warn("actionPerformed() valid class not found or it's an enum/interface");
            return;
        }

        final var fields = chooseFieldNames(targetClass);

        runWriteCommandAction(project, () -> {
            generateBuilderPattern(targetClass, fields);
        });
    }

    /**
     * Finds a valid PsiClass (record or standard class) under the caret.
     * Rejects interfaces, enums, and annotations.
     * use of PSI_FILE in update() method requires updateThread = BGT
     */
    private static PsiClass getValidTargetClass(@NotNull AnActionEvent event) {
        final var project = event.getProject();
        final var editor = event.getData(CommonDataKeys.EDITOR);
        if (project == null || editor == null) {
            return null;
        }

        final var file = event.getData(CommonDataKeys.PSI_FILE);
        if (file == null) {
            return null;
        }

        final var elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        if (elementAtCaret == null) {
            return null;
        }

        final var psiClass = getParentOfType(elementAtCaret, PsiClass.class, false);
        if (psiClass == null) {
            return null;
        }

        // Отсекаем интерфейсы, enum и аннотации. 
        // Если это обычный class или record - возвращаем его.
        if (psiClass.isInterface() || psiClass.isEnum() || psiClass.isAnnotationType()) {
            return null;
        }

        return psiClass;
    }
}
