package com.example.java2json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;  // 添加这行导入
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;  // 添加这行导入
import java.util.Map;

// 在文件开头的导入部分添加
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Set;

public class Java2JsonAction extends AnAction {
    private Project project;  // Add this field

    @Override
    public void actionPerformed(AnActionEvent e) {
        this.project = e.getProject();  // Initialize the project field
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || psiFile == null) {
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        PsiElement element = psiFile.findElementAt(selectionModel.getSelectionStart());
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);

        if (psiClass == null) {
            return;
        }

        try {
            // 将 HashMap 替换为 LinkedHashMap
            Map<String, Object> jsonMap = new LinkedHashMap<>();
            for (PsiField field : psiClass.getAllFields()) {
                // 跳过静态字段和常量
                if (field.hasModifierProperty(PsiModifier.STATIC) || 
                    field.hasModifierProperty(PsiModifier.FINAL)) {
                    continue;
                }
                
                String fieldName = field.getName();
                PsiType fieldType = field.getType();
                
                // 获取字段的文档注释
                PsiDocComment docComment = field.getDocComment();
                String commentText = docComment != null ?
                        docComment.getText().replaceAll("/\\*\\*|\\*/|\\*", "").trim() : null;

                // 处理嵌套类型
                PsiClass fieldPsiClass = null;
                if (fieldType instanceof PsiClassType) {
                    fieldPsiClass = ((PsiClassType) fieldType).resolve();
                }

                // 首先处理集合类型 - 无论是否有注释都生成集合结构
                String fieldTypeName = fieldType.getCanonicalText();
                if (fieldTypeName.startsWith("java.util.List") || 
                    fieldTypeName.startsWith("java.util.Set") ||
                    fieldTypeName.startsWith("java.util.Collection")) {
                    
                    // 处理泛型集合
                    if (fieldType instanceof PsiClassType) {
                        PsiClassType classType = (PsiClassType) fieldType;
                        PsiType[] parameters = classType.getParameters();
                        
                        if (parameters.length > 0) {
                            PsiType paramType = parameters[0];
                            if (paramType instanceof PsiClassType) {
                                PsiClass paramClass = ((PsiClassType) paramType).resolve();
                                if (paramClass != null) {
                                    ArrayList<Object> list = new ArrayList<>();
                                    
                                    // 检查是否为Java标准类型
                                    if (isJavaStandardType(paramType)) {
                                        // 如果是Java标准类型，且有注释，使用注释
                                        if (commentText != null && !commentText.isEmpty()) {
//                                            // 创建一个只包含注释内容的对象，不包含属性名
//                                            HashMap<String, Object> commentObject = new HashMap<>();
//                                            commentObject.put(commentText, "");
                                            list.add("${"+commentText+"}");
                                        } else {
                                            // 没有注释则使用空对象
                                            list.add(new HashMap<>());
                                        }
                                    } else {
                                        // 如果不是Java标准类型，递归处理自定义类
                                        Map<String, Object> elementMap = handlePsiClass(paramClass, new HashSet<>(), 0);
                                        list.add(elementMap);
                                    }
                                    
                                    jsonMap.put(fieldName, list);
                                    continue;
                                }
                            }
                        }
                    }
                    
                    // 没有泛型参数，使用空数组
                    jsonMap.put(fieldName, new ArrayList<>());
                    continue;
                } 
                // 然后处理Map类型
                else if (fieldTypeName.startsWith("java.util.Map")) {
                    jsonMap.put(fieldName, new HashMap<>());
                    continue;
                } 
                // 然后处理自定义类型
                else if (fieldPsiClass != null && !isJavaStandardType(fieldType)) {
                    Map<String, Object> nestedMap = handlePsiClass(fieldPsiClass, new HashSet<>(), 0);
                    jsonMap.put(fieldName, nestedMap);
                    continue;
                }
                
                // 最后处理Java标准类型和其他类型 - 使用注释或空字符串
                if (commentText != null && !commentText.isEmpty()) {
                    jsonMap.put(fieldName, "${" + commentText + "}");
                } else {
                    jsonMap.put(fieldName, "");
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            final String jsonString = writer.writeValueAsString(jsonMap);

            // 替换原来的 Messages.showInfoMessage
            showJsonDialog(project, jsonString);

        } catch (Exception ex) {
            ex.printStackTrace();
            Messages.showErrorDialog(ex.getMessage(), "Error");
        }
    }

    // 添加新方法
    private void showJsonDialog(Project project, String jsonString) {
        // 创建面板并设置布局
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建文本区域 - 恢复原始大小
        JTextArea textArea = new JTextArea(jsonString);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setRows(20);  // 还原为原始的20行
        textArea.setColumns(50);  // 还原为原始的50列

        // 添加滚动面板
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        panel.add(scrollPane, BorderLayout.CENTER);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // 创建对话框实例（提前创建，以便按钮中可以引用）
        DialogWrapper dialog = new DialogWrapper(project) {
            {
                init();
                setTitle("Generated JSON With Comment");
            }

            @Override
            protected @Nullable JComponent createCenterPanel() {
                return panel;
            }
        };

        // 创建复制按钮
        JButton copyButton = new JButton("复制到剪贴板");
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(jsonString);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            
            // 复制后自动关闭窗口
            dialog.close(DialogWrapper.OK_EXIT_CODE);
        });
        buttonPanel.add(copyButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 显示对话框
        dialog.show();
    }

    // 判断是否为Java标准类型
    private boolean isJavaStandardType(PsiType type) {
        if (type instanceof PsiPrimitiveType) {
            return true;
        }
        
        String typeName = type.getCanonicalText();
        return typeName.startsWith("java.");
    }

    // 简化的默认值获取方法
    private Object getSimpleDefaultValue(String type) {
        return "";  // 始终返回空字符串
    }

    private Map<String, Object> handlePsiClass(PsiClass psiClass, Set<String> processedClasses, int depth) {
        // 增加深度限制，避免无限递归
        if (depth > 5) { // 从3增加到5，允许更深的嵌套
            return new HashMap<>();
        }

        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName != null && processedClasses.contains(qualifiedName)) {
            return new HashMap<>(); // 防止循环引用
        }
        if (qualifiedName != null) {
            processedClasses.add(qualifiedName);
        }
        
        // 如果是Java标准类，直接返回空map
        if (qualifiedName != null && qualifiedName.startsWith("java.")) {
            return new HashMap<>();
        }

        // 将 HashMap 替换为 LinkedHashMap
        Map<String, Object> objectMap = new LinkedHashMap<>();
        for (PsiField field : psiClass.getAllFields()) {
            // 跳过静态字段和常量
            if (field.hasModifierProperty(PsiModifier.STATIC) || 
                field.hasModifierProperty(PsiModifier.FINAL)) {
                continue;
            }
            
            String fieldName = field.getName();
            PsiType fieldType = field.getType();
            
            // 获取字段的文档注释
            PsiDocComment docComment = field.getDocComment();
            String commentText = docComment != null ?
                    docComment.getText().replaceAll("/\\*\\*|\\*/|\\*", "").trim() : null;

            // 首先处理集合类型
            String fieldTypeName = fieldType.getCanonicalText();
            if (fieldTypeName.startsWith("java.util.List") ||
               fieldTypeName.startsWith("java.util.Set") ||
               fieldTypeName.startsWith("java.util.Collection")) {
                
                // 处理泛型集合
                if (fieldType instanceof PsiClassType) {
                    PsiClassType classType = (PsiClassType) fieldType;
                    PsiType[] parameters = classType.getParameters();
                    
                    if (parameters.length > 0) {
                        PsiType paramType = parameters[0];
                        if (paramType instanceof PsiClassType) {
                            PsiClass paramClass = ((PsiClassType) paramType).resolve();
                            if (paramClass != null) {
                                ArrayList<Object> list = new ArrayList<>();
                                
                                // 检查是否为Java标准类型
                                if (isJavaStandardType(paramType)) {
                                    // 如果是Java标准类型，且有注释，使用注释
                                    if (commentText != null && !commentText.isEmpty()) {
                                        // 创建一个只包含注释内容的对象，不包含属性名
//                                        HashMap<String, Object> commentObject = new HashMap<>();
//                                        commentObject.put(commentText, "");
                                        list.add("${" + commentText + "}");
                                    } else {
                                        // 没有注释则使用空对象
                                        list.add(new HashMap<>());
                                    }
                                } else {
                                    // 如果不是Java标准类型，递归处理自定义类
                                    Map<String, Object> elementMap = handlePsiClass(paramClass, new HashSet<>(processedClasses), depth + 1);
                                    list.add(elementMap);
                                }
                                
                                objectMap.put(fieldName, list);
                                continue;
                            }
                        }
                    }
                }
                objectMap.put(fieldName, new ArrayList<>());
                continue;
            } else if (fieldTypeName.startsWith("java.util.Map")) {
                objectMap.put(fieldName, new HashMap<>());
                continue;
            } else if (fieldType instanceof PsiClassType) {
                PsiClass fieldPsiClass = ((PsiClassType) fieldType).resolve();
                if (fieldPsiClass != null) {
                    if (fieldPsiClass.isEnum() || isJavaStandardType(fieldType)) {
                        // 枚举和Java标准类使用注释或空字符串
                        objectMap.put(fieldName, commentText != null ? "${" + commentText + "}" : "");
                    } else {
                        // 非Java标准类递归处理
                        Map<String, Object> nestedMap = handlePsiClass(fieldPsiClass, new HashSet<>(processedClasses), depth + 1);
                        objectMap.put(fieldName, nestedMap);
                    }
                    continue;
                }
            }
            
            // 默认处理 - 使用注释或空字符串
            objectMap.put(fieldName, commentText != null ? "${" + commentText + "}" : "");
        }
        return objectMap;
    }

    private Object handleCustomObject(String className, Set<String> processedClasses, int depth) {
        if (depth > 3) { // 限制递归深度
            return new HashMap<>();
        }

        if (processedClasses.contains(className)) {
            return new HashMap<>();
        }
        processedClasses.add(className);

        try {
            // 将 HashMap 替换为 LinkedHashMap
            Map<String, Object> objectMap = new LinkedHashMap<>();
            Class<?> clazz = Class.forName(className);

            if (clazz.isEnum()) {
                // 枚举也使用空字符串
                return "";
            }

            // 快速处理基本类型的包装类
            if (clazz.getName().startsWith("java.lang.") || 
                clazz.getName().startsWith("java.util.") || 
                clazz.getName().startsWith("java.time.")) {
                return "";  // 统一使用空字符串
            }

            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String fieldType = field.getType().getName();
                String fieldName = field.getName();

                // 获取字段的文档注释
                String commentText = null;
                try {
                    java.lang.reflect.Method getDeclaredAnnotations = field.getClass().getMethod("getDeclaredAnnotations");
                    Object[] annotations = (Object[]) getDeclaredAnnotations.invoke(field);
                    for (Object annotation : annotations) {
                        if (annotation.getClass().getName().contains("Documented")) {
                            commentText = annotation.toString();
                            break;
                        }
                    }
                } catch (Exception ignored) {}

                // 处理循环引用
                if (fieldType.equals(className)) {
                    objectMap.put(fieldName, new HashMap<>());
                    continue;
                }

                // 处理集合类型
                if (fieldType.startsWith("java.util.List") || 
                    fieldType.startsWith("java.util.Set") ||
                    fieldType.startsWith("java.util.Collection")) {
                    
                    // 尝试获取泛型类型
                    java.lang.reflect.Type genericType = field.getGenericType();
                    if (genericType instanceof java.lang.reflect.ParameterizedType) {
                        java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericType;
                        java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
                        
                        if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                            Class<?> elementClass = (Class<?>) typeArgs[0];
                            if (!elementClass.isPrimitive() && 
                                !elementClass.getName().startsWith("java.lang.") &&
                                !elementClass.getName().startsWith("java.util.")) {
                                
                                ArrayList<Object> list = new ArrayList<>();
                                list.add(handleCustomObject(elementClass.getName(), new HashSet<>(processedClasses), depth + 1));
                                objectMap.put(fieldName, list);
                                continue;
                            }
                        }
                    }
                    objectMap.put(fieldName, new ArrayList<>());
                    continue;
                }

                Class<?> fieldClass = field.getType();
                if (fieldClass.isPrimitive() ||
                    fieldClass.getName().startsWith("java.lang.") ||
                    fieldClass.getName().startsWith("java.util.") ||
                    fieldClass.getName().startsWith("java.time.")) {
                    
                    // 如果有注释，优先使用注释
                    if (commentText != null && !commentText.isEmpty()) {
                        objectMap.put(fieldName, "${" + commentText + "}");
                    } else {
                        objectMap.put(fieldName, "");  // 统一使用空字符串
                    }
                } else {
                    try {
                        // 优先使用PSI系统处理
                        PsiClass psiClass = JavaPsiFacade.getInstance(project)
                                .findClass(fieldType, GlobalSearchScope.allScope(project));
                        if (psiClass != null) {
                            objectMap.put(fieldName, handlePsiClass(psiClass, new HashSet<>(processedClasses), depth + 1));
                        } else {
                            // 如果PSI系统找不到，使用反射处理
                            Object nestedValue = handleCustomObject(fieldType, new HashSet<>(processedClasses), depth + 1);
                            objectMap.put(fieldName, nestedValue);
                        }
                    } catch (Exception e) {
                        // 如果处理失败，尝试使用简单类名
                        String simpleTypeName = fieldType.substring(fieldType.lastIndexOf('.') + 1);
                        objectMap.put(fieldName, getSimpleDefaultValue(simpleTypeName));
                    }
                }
            }
            return objectMap;
        } catch (Exception e) {
            return new LinkedHashMap<>();  // 这里也替换为 LinkedHashMap
        }
    }
}