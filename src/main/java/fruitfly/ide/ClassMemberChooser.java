package fruitfly.ide;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.ide.util.MemberChooser;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiRecordComponent;
import com.intellij.psi.PsiVariable;
import fruitfly.psi.BuilderGenerator;

import java.util.List;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public class ClassMemberChooser {

    /**
     * Displays the confirmation dialog where users can choose what fields to
     * generate.
     */
    public static List<String> chooseFieldNames(PsiClass recordClass) {
        final var members = mapAllFieldMembers(recordClass);

        final var chooser = new MemberChooser<>(
            members.toArray(PsiFieldMember[]::new),
            false, // allowEmptySelection
            true,  // allowMultiSelection
            recordClass.getProject(),
            false // isInsertOverrideVisible
        );
        chooser.setCopyJavadocVisible(false);
        chooser.selectElements(
            members.stream().
                filter(ClassMemberChooser::isDefaultSelection).
                toArray(PsiFieldMember[]::new)
        );
        chooser.setTitle("Select Fields to Be Available in Builder");

        chooser.show();
        if (!chooser.isOK()) {
            return emptyList();
        }

        // return the chosen fields as a list of field names
        final var selectedMembers =
            requireNonNull(chooser.getSelectedElements());
        return selectedMembers.stream().
            map(i -> i.getElement().getName()).
            toList();
    }

    public static List<String> mapRecordComponentNames(
        PsiClass recordClass
    ) {
        final var variables = recordClass.isRecord()
                              ? recordClass.getRecordComponents()
                              : recordClass.getFields();

        return stream(variables)
            .map(PsiVariable::getName)
            .toList();
    }

    /**
     * Return all fields of the given class as "members".
     * Chooser has its own "Member" abstraction wrapped around the PSI types.
     */
    public static List<PsiFieldMember> mapAllFieldMembers(
        PsiClass psiClass
    ) {
        return stream(psiClass.getAllFields()).
            map(PsiFieldMember::new).
            toList();
    }

    /**
     * Defines fields should be selected by default.
     */
    public static boolean isDefaultSelection(PsiFieldMember field) {
        return !field.getElement().getName().equals("serialVersionUID");
    }

}
