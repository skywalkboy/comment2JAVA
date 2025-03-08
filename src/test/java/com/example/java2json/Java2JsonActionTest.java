package com.example.java2json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class Java2JsonActionTest {

    private Java2JsonAction action;

    @Mock
    private Project project;

    @Mock
    private PsiJavaFile psiFile;

    @Mock
    private PsiClass psiClass;

    @Mock
    private PsiField psiField;

    @Mock
    private PsiType psiType;

    @Mock
    private JavaPsiFacade javaPsiFacade;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        action = new Java2JsonAction();
        
        // 通过反射设置 project 字段
        try {
            java.lang.reflect.Field projectField = Java2JsonAction.class.getDeclaredField("project");
            projectField.setAccessible(true);
            projectField.set(action, project);
        } catch (Exception e) {
            fail("无法设置 project 字段: " + e.getMessage());
        }
    }

    @Test
    public void testHandlePsiClass_基本类型() throws Exception {
        // 准备测试数据
        when(psiClass.getQualifiedName()).thenReturn("com.example.TestClass");
        
        // 模拟一个基本类型字段
        PsiField stringField = mock(PsiField.class);
        PsiType stringType = mock(PsiType.class);
        when(stringField.getName()).thenReturn("name");
        when(stringField.getType()).thenReturn(stringType);
        when(stringType.getCanonicalText()).thenReturn("java.lang.String");
        
        // 模拟一个整数类型字段
        PsiField intField = mock(PsiField.class);
        PsiType intType = mock(PsiPrimitiveType.class);
        when(intField.getName()).thenReturn("age");
        when(intField.getType()).thenReturn(intType);
        when(intType.getCanonicalText()).thenReturn("int");
        
        when(psiClass.getAllFields()).thenReturn(new PsiField[]{stringField, intField});
        
        // 调用测试方法
        Map<String, Object> result = callHandlePsiClass(psiClass, new HashSet<>(), 0);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("", result.get("name"));
        assertEquals(0, result.get("age"));
    }

    @Test
    public void testHandlePsiClass_嵌套对象() throws Exception {
        // 准备测试数据
        when(psiClass.getQualifiedName()).thenReturn("com.example.Person");
        
        // 模拟一个基本类型字段
        PsiField nameField = mock(PsiField.class);
        PsiType stringType = mock(PsiType.class);
        when(nameField.getName()).thenReturn("name");
        when(nameField.getType()).thenReturn(stringType);
        when(stringType.getCanonicalText()).thenReturn("java.lang.String");
        
        // 模拟一个嵌套对象字段
        PsiField addressField = mock(PsiField.class);
        PsiClassType addressType = mock(PsiClassType.class);
        PsiClass addressClass = mock(PsiClass.class);
        
        when(addressField.getName()).thenReturn("address");
        when(addressField.getType()).thenReturn(addressType);
        when(addressType.getCanonicalText()).thenReturn("com.example.Address");
        when(addressType.resolve()).thenReturn(addressClass);
        
        // 设置嵌套类的字段
        PsiField streetField = mock(PsiField.class);
        PsiType streetType = mock(PsiType.class);
        when(streetField.getName()).thenReturn("street");
        when(streetField.getType()).thenReturn(streetType);
        when(streetType.getCanonicalText()).thenReturn("java.lang.String");
        
        when(addressClass.getQualifiedName()).thenReturn("com.example.Address");
        when(addressClass.getAllFields()).thenReturn(new PsiField[]{streetField});
        
        when(psiClass.getAllFields()).thenReturn(new PsiField[]{nameField, addressField});
        
        // 调用测试方法
        Map<String, Object> result = callHandlePsiClass(psiClass, new HashSet<>(), 0);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("", result.get("name"));
        
        Object addressObj = result.get("address");
        assertTrue(addressObj instanceof Map);
        Map<String, Object> addressMap = (Map<String, Object>) addressObj;
        assertEquals(1, addressMap.size());
        assertEquals("", addressMap.get("street"));
    }

    @Test
    public void testHandlePsiClass_集合类型() throws Exception {
        // 准备测试数据
        when(psiClass.getQualifiedName()).thenReturn("com.example.Department");
        
        // 模拟一个基本类型字段
        PsiField nameField = mock(PsiField.class);
        PsiType stringType = mock(PsiType.class);
        when(nameField.getName()).thenReturn("name");
        when(nameField.getType()).thenReturn(stringType);
        when(stringType.getCanonicalText()).thenReturn("java.lang.String");
        
        // 模拟一个集合类型字段
        PsiField employeesField = mock(PsiField.class);
        PsiClassType listType = mock(PsiClassType.class);
        PsiType[] typeParameters = new PsiType[1];
        PsiClassType employeeType = mock(PsiClassType.class);
        PsiClass employeeClass = mock(PsiClass.class);
        
        typeParameters[0] = employeeType;
        
        when(employeesField.getName()).thenReturn("employees");
        when(employeesField.getType()).thenReturn(listType);
        when(listType.getCanonicalText()).thenReturn("java.util.List<com.example.Employee>");
        when(listType.getParameters()).thenReturn(typeParameters);
        when(employeeType.resolve()).thenReturn(employeeClass);
        
        // 设置集合元素类的字段
        PsiField empNameField = mock(PsiField.class);
        PsiType empNameType = mock(PsiType.class);
        when(empNameField.getName()).thenReturn("empName");
        when(empNameField.getType()).thenReturn(empNameType);
        when(empNameType.getCanonicalText()).thenReturn("java.lang.String");
        
        when(employeeClass.getQualifiedName()).thenReturn("com.example.Employee");
        when(employeeClass.getAllFields()).thenReturn(new PsiField[]{empNameField});
        
        when(psiClass.getAllFields()).thenReturn(new PsiField[]{nameField, employeesField});
        
        // 调用测试方法
        Map<String, Object> result = callHandlePsiClass(psiClass, new HashSet<>(), 0);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("", result.get("name"));
        
        Object employeesObj = result.get("employees");
        assertTrue(employeesObj instanceof List);
        List<Object> employeesList = (List<Object>) employeesObj;
        assertEquals(1, employeesList.size());
        
        Object employeeObj = employeesList.get(0);
        assertTrue(employeeObj instanceof Map);
        Map<String, Object> employeeMap = (Map<String, Object>) employeeObj;
        assertEquals(1, employeeMap.size());
        assertEquals("", employeeMap.get("empName"));
    }

    @Test
    public void testHandlePsiClass_枚举类型() throws Exception {
        // 准备测试数据
        when(psiClass.getQualifiedName()).thenReturn("com.example.Person");
        
        // 模拟一个枚举类型字段
        PsiField genderField = mock(PsiField.class);
        PsiClassType enumType = mock(PsiClassType.class);
        PsiClass enumClass = mock(PsiClass.class);
        
        when(genderField.getName()).thenReturn("gender");
        when(genderField.getType()).thenReturn(enumType);
        when(enumType.getCanonicalText()).thenReturn("com.example.Gender");
        when(enumType.resolve()).thenReturn(enumClass);
        
        // 设置枚举类
        when(enumClass.isEnum()).thenReturn(true);
        PsiEnumConstant maleConstant = mock(PsiEnumConstant.class);
        when(maleConstant.getName()).thenReturn("MALE");
        
        when(enumClass.getFields()).thenReturn(new PsiField[]{maleConstant});
        
        when(psiClass.getAllFields()).thenReturn(new PsiField[]{genderField});
        
        // 调用测试方法
        Map<String, Object> result = callHandlePsiClass(psiClass, new HashSet<>(), 0);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("MALE", result.get("gender"));
    }

    @Test
    public void testHandlePsiClass_循环引用() throws Exception {
        // 准备测试数据
        when(psiClass.getQualifiedName()).thenReturn("com.example.Employee");
        
        // 模拟一个基本类型字段
        PsiField nameField = mock(PsiField.class);
        PsiType stringType = mock(PsiType.class);
        when(nameField.getName()).thenReturn("name");
        when(nameField.getType()).thenReturn(stringType);
        when(stringType.getCanonicalText()).thenReturn("java.lang.String");
        
        // 模拟一个循环引用字段
        PsiField managerField = mock(PsiField.class);
        PsiClassType managerType = mock(PsiClassType.class);
        
        when(managerField.getName()).thenReturn("manager");
        when(managerField.getType()).thenReturn(managerType);
        when(managerType.getCanonicalText()).thenReturn("com.example.Employee");
        when(managerType.resolve()).thenReturn(psiClass);  // 循环引用到自身
        
        when(psiClass.getAllFields()).thenReturn(new PsiField[]{nameField, managerField});
        
        // 调用测试方法
        Map<String, Object> result = callHandlePsiClass(psiClass, new HashSet<>(), 0);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("", result.get("name"));
        
        Object managerObj = result.get("manager");
        assertTrue(managerObj instanceof Map);
        Map<String, Object> managerMap = (Map<String, Object>) managerObj;
        assertEquals(0, managerMap.size());  // 应该是空的，因为检测到循环引用
    }

    // 通过反射调用私有方法 handlePsiClass
    @SuppressWarnings("unchecked")
    private Map<String, Object> callHandlePsiClass(PsiClass psiClass, Set<String> processedClasses, int depth) throws Exception {
        java.lang.reflect.Method method = Java2JsonAction.class.getDeclaredMethod("handlePsiClass", PsiClass.class, Set.class, int.class);
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(action, psiClass, processedClasses, depth);
    }

    // 测试辅助类
    static class TestClass {
        private String name;
        private int age;
    }

    static class Person {
        private String name;
        private Address address;
    }

    static class Address {
        private String street;
    }

    static class Department {
        private String name;
        private List<Employee> employees;
    }

    static class Employee {
        private String empName;
        private Employee manager;
    }

    enum Gender {
        MALE, FEMALE
    }
} 