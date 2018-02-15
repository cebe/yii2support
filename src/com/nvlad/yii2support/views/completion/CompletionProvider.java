package com.nvlad.yii2support.views.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.nvlad.yii2support.common.YiiApplicationUtils;
import com.nvlad.yii2support.views.ViewUtil;
import com.nvlad.yii2support.views.ViewsUtil;
import com.nvlad.yii2support.views.index.ViewFileIndex;
import com.nvlad.yii2support.views.index.ViewInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by NVlad on 27.12.2016.
 */
class CompletionProvider extends com.intellij.codeInsight.completion.CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters,
                                  ProcessingContext processingContext,
                                  @NotNull CompletionResultSet completionResultSet) {

        final PsiElement psiElement = completionParameters.getPosition();
        final MethodReference method = PsiTreeUtil.getParentOfType(psiElement, MethodReference.class);

        if (method == null || method.getParameters().length == 0) {
            return;
        }

        if (!ViewsUtil.isValidRenderMethod(method)) {
            return;
        }

        PsiElement parameter = psiElement;
        while (parameter != null && !(parameter.getParent() instanceof ParameterList)) {
            parameter = parameter.getParent();
        }

        if (parameter == null || !parameter.equals(method.getParameters()[0])) {
            return;
        }

        final String prefix = ViewUtil.getViewPrefix(parameter);
        if (prefix == null) {
            return;
        }

        final Project project = psiElement.getProject();
        final GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        final FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();

        int prefixLength = prefix.length();
        if (prefix.contains("/") && !prefix.endsWith("/")) {
            prefixLength = prefix.lastIndexOf('/') + 1;
        }

        final String prefixFilter = prefix.substring(0, prefixLength);
        final Set<String> keys = new HashSet<>();
        fileBasedIndex.processAllKeys(ViewFileIndex.identity, key -> {
            if (key.startsWith(prefixFilter)) {
                keys.add(key);
            }
            return true;
        }, scope, null);

        if (!completionParameters.isAutoPopup()) {
            completionResultSet = completionResultSet.withPrefixMatcher(prefix.substring(prefixLength));
        }

        final PsiManager psiManager = PsiManager.getInstance(project);
        final String application = YiiApplicationUtils.getApplicationName(psiElement.getContainingFile());
        for (String key : keys) {
            Collection<ViewInfo> views = fileBasedIndex.getValues(ViewFileIndex.identity, key, scope);
            for (ViewInfo view : views) {
                if (!application.equals(view.application)) {
                    continue;
                }

                PsiFile psiFile = psiManager.findFile(view.getVirtualFile());
                if (psiFile != null) {
                    String insertText = key.substring(prefixLength);
                    completionResultSet.addElement(new ViewLookupElement(psiFile, insertText));
                    break;
                } else {
                    System.out.println(view.fileUrl + " => not exists");
                }
            }
        }

//        Collection<String> keys = StubIndex.getInstance().getAllKeys(YiiViewIndex.KEY, completionParameters.getPosition().getProject());

//


//        String path = getValue(method.getParameters()[0]);
//        PsiDirectory directory;
//        if (path.startsWith("/")) {
//            path = path.substring(1);
//            directory = ViewsUtil.getRootDirectory(psiElement);
//        } else {
//            directory = ViewsUtil.getContextDirectory(psiElement);
//        }
//        if (path.contains("/")) {
//            path = path.substring(0, path.lastIndexOf('/') + 1);
//        }
//
//        while (path.contains("/") && directory != null) {
//            String subdirectory = path.substring(0, path.indexOf('/'));
//            path = path.substring(path.indexOf('/') + 1);
//            directory = subdirectory.equals("..") ? directory.getParent() : directory.findSubdirectory(subdirectory);
//        }
//        if (directory != null) {
//            if (completionResultSet.getPrefixMatcher().getPrefix().contains("/")) {
//                String prefix = completionResultSet.getPrefixMatcher().getPrefix();
//                prefix = prefix.substring(prefix.lastIndexOf("/") + 1);
//                completionResultSet = completionResultSet.withPrefixMatcher(prefix);
//            }
//
//            for (PsiDirectory psiDirectory : directory.getSubdirectories()) {
//                completionResultSet.addElement(new DirectoryLookupElement(psiDirectory));
//            }
//
//            for (PsiFile psiFile : directory.getFiles()) {
//                completionResultSet.addElement(new ViewLookupElement(psiFile));
//            }
//        }
    }
//
//    @NotNull
//    private String getValue(PsiElement expression) {
//        if (expression instanceof StringLiteralExpression) {
//            String value = ((StringLiteralExpression) expression).getContents();
//            return value.substring(0, value.indexOf("IntellijIdeaRulezzz "));
//        }
//
//        return "";
//    }
}
