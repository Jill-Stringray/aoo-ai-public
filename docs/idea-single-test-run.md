# IDEA 中只运行单个 JUnit 测试的方法

如果在 IntelliJ IDEA 中点击某个测试方法旁边的运行按钮，却发现同一个模块里的其它测试也一起被启动，通常是因为当前 Run Configuration 被配置成了运行整个包、整个模块，或者使用 Maven `test` goal 时没有带上 `-Dtest=类名#方法名`。

## 推荐设置

1. 打开 **Settings/Preferences | Build, Execution, Deployment | Build Tools | Maven | Runner**。
2. 如果不需要强制走 Maven，建议把 **Run tests using** 设置为 **IntelliJ IDEA**，这样从测试方法 gutter 图标启动时会精确运行该方法。
3. 如果项目要求委托给 Maven 执行，请确认 IDEA 生成的命令包含类似参数：

   ```bash
   mvn -Dtest=UploadTests#testReadAndSplit test
   ```

   只有执行 `mvn test` 时，Maven Surefire 才会扫描并运行所有符合命名规则的测试类。

## 本项目的 Maven 测试命名规则

`pom.xml` 已显式配置 Maven Surefire 只扫描下面两类 JUnit 测试文件：

- `**/*Test.java`
- `**/*Tests.java`

因此，命令行运行单个测试时请使用：

```bash
mvn -Dtest=类名#方法名 test
```

例如：

```bash
mvn -Dtest=UploadTests#testReadAndSplit test
```

不要只运行 `mvn test`，否则 Maven 会按上述规则发现并执行全部测试类。
