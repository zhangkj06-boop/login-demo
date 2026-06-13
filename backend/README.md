已读取完毕，下面是该项目的完整概览：

  ---
  项目整体结构 login-demo

  这是一个三端合一的演示项目，包含：

  ┌────────────┬───────────────────────────────────┬──────────────────────┐
  │    目录    │              技术栈               │         作用         │
  ├────────────┼───────────────────────────────────┼──────────────────────┤
  │ backend/   │ Spring Boot 3.2.5 + Java 17 + JPA │ RESTful API 后端服务 │
  ├────────────┼───────────────────────────────────┼──────────────────────┤
  │ api-tests/ │ TestNG + RestAssured + Allure     │ 接口自动化测试       │
  ├────────────┼───────────────────────────────────┼──────────────────────┤
  │ weapp/     │ uni-app 3.0 (Vue 3 + Vite)        │ 微信小程序前端       │
  └────────────┴───────────────────────────────────┴──────────────────────┘

  ---
  1. 后端 (backend/)

  技术依赖

  - Spring Boot Web (排除 logback，使用 Log4j2)
  - Spring Data JPA + MySQL8
  - Spring Security Crypto (BCrypt 密码加密)
  - 参数校验 (spring-boot-starter-validation)

  运行配置 (application.properties)

  - 端口：8443 (HTTPS)
  - 数据库：jdbc:mysql://localhost:3306/login_demo
  - SSL 证书：keystore.p12
  - 跨域：@CrossOrigin(origins = "*")

  业务模块与 API

  ┌──────────┬──────────────────────────┬────────────────────────────────────────────────────────────┐
  │   模块   │        Controller        │                          核心功能                          │
  ├──────────┼──────────────────────────┼────────────────────────────────────────────────────────────┤
  │ 认证     │ AuthController           │ 登录(学生/教师/管理员)、注册、修改密码、获取当前用户       │
  ├──────────┼──────────────────────────┼────────────────────────────────────────────────────────────┤
  │ 用户管理 │ UserController           │ 用户 CRUD、更新头像                                        │
  ├──────────┼──────────────────────────┼────────────────────────────────────────────────────────────┤
  │ 学生管理 │ StudentController        │ 学生 CRUD，含学号唯一性、年龄 0-150 校验                   │
  ├──────────┼──────────────────────────┼────────────────────────────────────────────────────────────┤
  │ 教师管理 │ TeacherController        │ 教师 CRUD，含工号唯一性、授课班级(ClassEnum)               │
  ├──────────┼──────────────────────────┼────────────────────────────────────────────────────────────┤
  │ 商品管理 │ ProductController        │ 商品 CRUD、入库(/stock/in)、出库(/stock/out)、库存记录查询 │
  ├──────────┼──────────────────────────┼────────────────────────────────────────────────────────────┤
  │ 商品类型 │ ProductTypeController    │ 商品分类 CRUD                                              │
  ├──────────┼──────────────────────────┼────────────────────────────────────────────────────────────┤
  │ 投诉反馈 │ ComplaintController      │ 投诉提交、回复、标记解决状态                               │
  ├──────────┼──────────────────────────┼────────────────────────────────────────────────────────────┤
  │ 角色权限 │ RolePermissionController │ 按角色查询权限、更新权限、初始化默认权限数据               │
  ├──────────┼──────────────────────────┼────────────────────────────────────────────────────────────┤
  │ 文件上传 │ FileController           │ 头像上传，保存到 uploads/avatars/                          │
  └──────────┴──────────────────────────┴────────────────────────────────────────────────────────────┘

  实体类 (entity/)

  User, Student, Teacher, Product, ProductType, StockRecord, Complaint, RolePermission, ClassEnum(枚举)

  安全设计

  - 密码使用 BCryptPasswordEncoder 加密存储
  - User 实体的 password 字段标记 @JsonProperty(Access.WRITE_ONLY)，防止序列化返回

  ---
  2. API 自动化测试 (api-tests/)

  技术栈

  - TestNG 7.10.2
  - RestAssured 5.5.0
  - Allure 2.29.0 (含 allure-rest-assured 过滤器)
  - AssertJ 3.26.3
  - Lombok + Jackson

  测试架构

  base/
    └── BaseTest.java          # 配置 baseURI(8080)、请求/响应规范、Token 管理
  api/
    ├── AuthApi.java           # API 封装层：login / register / changePassword / getUserInfo
    ├── UserApi.java
    ├── ProductApi.java
    ├── StudentApi.java
    └── ...                    # 每个模块对应一个 Api 类
  tests/
    ├── AuthTests.java         # 测试用例层，使用 @Epic / @Feature / @Story / @Severity
    ├── UserTests.java
    └── ...
  utils/
    └── DataProviderUtil.java  # TestNG 数据驱动
  resources/suite/
    └── testng.xml             # 测试套件，AuthAPITests 优先，其余 depends-on

  测试特点

  - 采用 Page Object 思想分层：Api 层封装 HTTP 调用，Tests 层做断言
  - 全量使用 Allure 注解生成测试报告：@Epic, @Feature, @Story, @Step, @Description
  - 登录成功后通过 setAuthToken(token) 将 Token 注入后续请求的 Authorization Header
  - 支持数据驱动测试 (DataProviderUtil)

  ---
  3. 微信小程序前端 (weapp/)

  - 框架：uni-app 3.0 alpha-50102 + Vue 3.4 + Vite 5.2
  - 目标平台：微信小程序 (mp-weixin)，同时支持 H5、APP、支付宝等多端
  - 小程序 AppID：wxc4da7b77cd7e544f
  - 源码位于 src/，构建产物在 dist/

  ---
  关键文件路径速查

  ┌──────────────┬─────────────────────────────────────────────────────────────┐
  │     文件     │                            路径                              │
  ├──────────────┼─────────────────────────────────────────────────────────────┤
  │ 后端主类     │ backend/src/main/java/com/example/demo/DemoApplication.java  │
  ├──────────────┼─────────────────────────────────────────────────────────────┤
  │ 后端配置     │ backend/src/main/resources/application.properties             │
  ├──────────────┼─────────────────────────────────────────────────────────────┤
  │ 后端 POM     │ backend/pom.xml                                              │
  ├──────────────┼─────────────────────────────────────────────────────────────┤
  │ 测试 POM     │ api-tests/pom.xml                                            │
  ├──────────────┼─────────────────────────────────────────────────────────────┤
  │ 测试套件     │ api-tests/src/test/resources/suite/testng.xml               │
  ├──────────────┼─────────────────────────────────────────────────────────────┤
  │ 测试基类     │ api-tests/src/test/java/com/example/demo/base/BaseTest.java │
  ├──────────────┼─────────────────────────────────────────────────────────────┤
  │ 前端 package │ weapp/package.json                                          │
  └──────────────┴─────────────────────────────────────────────────────────────┘

  如需进一步了解某个模块的具体实现（例如某个 Controller 的逻辑、测试用例细节、或前端页面），可以直接告诉我。