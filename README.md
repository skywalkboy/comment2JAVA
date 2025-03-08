# Comment2JSON

一个智能工具，可将Java类转换为JSON格式数据，专注于使用JavaDoc注释作为字段值。

## 核心功能

- **JavaDoc注释提取**：自动识别并提取JavaDoc注释作为JSON值
- **智能嵌套结构处理**：自动处理自定义类的嵌套结构，生成完整的JSON结构
- **集合类型支持**：正确处理List、Set等集合类型，保留元素嵌套结构
- **一键复制**：生成的JSON可以一键复制到剪贴板并自动关闭窗口

## 使用方法

1. 在Java文件中，将光标放在类名上
2. 使用以下任一方法触发转换：
   - 右键单击并选择"Comment to JSON"
   - 使用快捷键Ctrl + Alt + J
   - 按Alt + Insert并选择"Comment2JSON"（生成菜单）
3. 插件将在弹出窗口中显示生成的JSON数据
4. 点击"复制到剪贴板"按钮快速复制JSON内容并关闭窗口

## 示例

Java类：
```java
public class UserInfo {
    /**
     * 用户唯一标识符
     */
    private Long id;
    
    /**
     * 用户名称
     */
    private String name;
    
    /**
     * 用户角色列表
     */
    private List<Role> roles;
}

public class Role {
    /**
     * 角色代码
     */
    private String code;
}
```

生成的JSON：
```json
{
  "id": "${用户唯一标识符}",
  "name": "${用户名称}",
  "roles": [
    {
      "code": "${角色代码}"
    }
  ]
}
```

## 支持的数据类型

- Java标准类型（java.*包中的类）：使用JavaDoc注释或空字符串
- 自定义类型：生成完整的嵌套JSON结构，内部字段以类似方式处理
- 集合类型（List、Set等）：生成带有元素的数组结构
- Map类型：生成空对象
- 枚举类型：使用JavaDoc注释或空字符串

## 许可证

本项目采用Apache License 2.0许可证。 