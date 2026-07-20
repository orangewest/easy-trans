# easy-trans

一款通用的数据翻译框架

<!-- PROJECT SHIELDS -->

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]

<!-- PROJECT LOGO -->

<br />

<p align="center">
  <a href="https://github.com/orangewest/easy-trans">
    <img src="logo.png" alt="Logo" width="300" height="70">
  </a>

<h3 align="center">easy-trans</h3>
  <p align="center">
    一款通用的数据翻译框架
    <br />
    <a href="https://github.com/orangewest/easy-trans"><strong>探索本项目的文档 »</strong></a>
    <br />
    <br />
    <a href="https://github.com/orangewest/easy-trans/blob/main/easy-trans-core/src/test/java/io/github/orangewest/trans/service/TransServiceTest.java">查看Demo</a>
    ·
    <a href="https://github.com/orangewest/easy-trans/issues">报告Bug</a>
    ·
    <a href="https://github.com/orangewest/easy-trans/issues">提出新特性</a>
  </p>

</p>

> easy-trans 是一个**轻量、零依赖**的通用数据翻译框架：通过注解声明「翻译哪个字段、从哪个数据源取数」，框架负责并行取数并回填。核心层 `easy-trans-core` 不依赖任何第三方库，缓存、数据源等均由使用者自定义的 `TransRepository` 实现——框架只做编排。

### 特性

- **注解驱动**：在字段上声明 `@Trans` 即可完成翻译，零侵入业务代码
- **多源取数**：数据库、字典、HTTP、缓存……任意数据源，只需实现 `TransRepository` 接口
- **并行翻译**：不同仓库的字段并行取数，提升批量翻译性能
- **嵌套翻译**：支持省→市→区等多层级联翻译
- **包装对象翻译**：`Result`、`Page` 等包装类型通过 `TransObjResolver` 自动拆包
- **异步 / 响应式支持**：Spring 集成下 `TransUtil.transResult` 可处理 `CompletableFuture`、`Mono`、`Flux` 等返回值
- **异常安全**：未初始化、仓库未注册、引用悬空等场景抛出 `TransException`，不再静默失败
- **可观测性**：内置监控指标接口（翻译耗时 / 仓库耗时），Spring 下自动桥接 Micrometer
- **有界缓存**：类元数据缓存使用 LRU（上限 1024），避免内存无限增长
- **零运行时依赖**：`easy-trans-core` 仅依赖 JDK，可嵌入任意项目

### 目录

- [环境要求](#环境要求)
- [一、架构设计](#一架构设计)
- [二、优点](#二优点)
- [三、基本使用](#三基本使用)
- [四、高级功能](#四高级功能)
  - [1、自定义注解](#1自定义注解)
  - [2、嵌套翻译](#2嵌套翻译)
  - [3、包装类翻译](#3包装类翻译)
  - [4、对象直接填充](#4对象直接填充)
  - [5、异常处理](#5异常处理)
- [五、与 Spring Boot 集成](#五与-spring-boot-集成)
  - [Micrometer 监控](#可选micrometer-监控)

## 环境要求

easy-trans 分为两条版本线，按需选择：

| 版本线 | JDK | Spring Boot | 适用场景 |
| --- | --- | --- | --- |
| **2.x**（当前主线，2.0.0+） | **JDK 25**（运行需 JRE 25+，构建需 JDK 25） | **Spring Boot 4.x**（已验证 4.1.0） | 新项目 / 可升级 JDK 与 Spring 的用户 |
| **1.x**（旧线维护） | **JDK 8+** | **Spring Boot 2.7.x** | 仍停留在 JDK 8 或 Spring Boot 2.7 的用户 |

- 当前主线 **2.x** 要求 **JDK 25 + Spring Boot 4**；若你的项目还停留在 **JDK 8**（或 Spring Boot 2.7），请使用 **1.x** 版本（`easy-trans-core` / `easy-trans-spring-start` 的 `1.x.y`）。
- `easy-trans-core` 始终保持零第三方依赖，可脱离 Spring 独立使用（仅需 JDK）。
- **GraalVM Native Image**：需 Spring Boot 4 的 AOT + RuntimeHints（已在 `easy-trans-spring-start` 内置 `EasyTransRuntimeHints`），构建时使用 **JDK 25 + GraalVM for JDK 25**。

## 一、架构设计

框架的核心思路只有一句话：**在字段上声明「翻译哪个字段、从哪个数据源取数」，框架负责并行取数并回填**。整个翻译过程分为四步：

```
对象 obj
   │  trans(obj)
   ▼
[1] 拆包 resolveObj      ← TransObjResolver 把 Result / Page 等包装对象拆到内部业务对象
   ▼
[2] 解析元数据           ← 构建 TransClassMeta，按字段引用关系生成「key → 子字段」翻译树
   ▼
[3] 分组并行取数 doTrans ← 按 @TransRepo 分组，各组用 CompletableFuture 并行调用
   │                       对应 TransRepository.getTransValueMap()
   ▼
[4] 回填字段             ← 按 @Trans 把取到的值写回目标字段（支持对象 / 集合 / 嵌套）
```

四个核心角色相互配合，各司其职：

| 角色 | 说明 |
| --- | --- |
| `@Trans` | 标注在**目标字段**上，声明「从哪取数、取出来提取哪个字段」 |
| `@TransRepo` | 标注在**源字段**上，把源字段绑定到某个 `TransRepository`（可重复、可在自定义注解上作为元注解） |
| `TransRepository` | 唯一需要你自己实现的接口：`getTransValueMap()` 负责从任意数据源批量取数 |
| `TransObjResolver` | 负责把 `Result`、`Page` 等包装对象拆包，让翻译触达内部业务对象 |

下面看看这几个核心组件的源码：</br>翻译核心注解

```java

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Trans {

    /**
     * @return 需要获取数据的仓库（或字段）
     */
    String trans();

    /**
     * @return 从仓库中提取的字段
     */
    String key() default "";

    /**
     * @return 翻译数据获取仓库；未指定时通过 trans() 名称匹配 @TransRepo
     */
    Class<? extends TransRepository<?, ?>> using() default None.class;

    /**
     * 哨兵类型，用于在未显式指定 using() 时占位
     */
    interface None extends TransRepository<Object, Object> {
    }

}

```

</br>翻译仓库

```java
public interface TransRepository<T, R> {

    /**
     * 获取翻译结果（适用于数据库等翻译）
     *
     * @param transValues 需要翻译的值
     * @param transAnno   翻译对象上的注解
     * @return 查询结果值 val-翻译值
     */
    Map<T, R> getTransValueMap(List<T> transValues, Annotation transAnno);

}
```

</br>翻译仓库注解

```java

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Repeatable(TransRepos.class)
public @interface TransRepo {

    /**
     * @return 仓库名称（默认使用字段名）
     */
    String name() default "";

    /**
     * @return 翻译数据获取仓库
     */
    Class<? extends TransRepository<?, ?>> using();
}
```

## 二、优点

1. **核心源码简单**：`easy-trans-core` 仅数百行，无任何第三方依赖，可嵌入任意项目；
2. **高度可扩展**：拓展数据源只需实现 `TransRepository` 一个接口；
3. **功能完备**：支持数据库翻译、字典翻译、集合翻译、嵌套翻译、包装对象翻译等；
4. **并行翻译**：不同仓库的字段并行取数，批量翻译性能高；
5. **异常安全**：初始化、仓库注册、引用等异常场景统一抛出 `TransException`，不再静默吞掉；
6. **可观测**：内置指标接口，Spring 环境下自动桥接 Micrometer，便于监控翻译耗时；
7. **有界缓存**：类元数据缓存采用 LRU（默认上限 1024），内存可控。

## 三、基本使用

maven引入

```xml

<dependency>
    <groupId>io.github.orangewest</groupId>
    <artifactId>easy-trans-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

比如现在有一个老师的实体对象

```java

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherDto {

    private Long id;

    private String name;

    // 关联教哪个学科
    private Long subjectId;

}

```

课程科目实体对象

```java

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubjectDto {

    private Long id;

    private String name;

}

```

学生实体对象

```java
@Data
public class UserDto {

    private Long id;

    private String name;

    @DictTransRepo(group = "sexDict")
    private String sex;

    @Trans(trans = "sex")
    private String sexName;

    @DictTransRepo(group = "jobDict")
    private String job;

    @Trans(trans = "job")
    private String jobName;

    @TransRepo(using = TeacherTransRepository.class)
    private Long teacherId;

    @Trans(trans = "teacherId", key = "name")
    private String teacherName;

    @TransRepo(using = SubjectTransRepository.class)
    @Trans(trans = "teacherId", key = "subjectId")
    private Long subjectId;

    @Trans(trans = "subjectId", key = "name")
    private String subjectName;

    private String deptCode;

    @Trans(trans = "deptCode", key = "name", using = DeptTransRepository.class)
    private String deptName;

    public UserDto(Long id, String name, Long teacherId, String sex, String job, String deptCode) {
        this.id = id;
        this.name = name;
        this.teacherId = teacherId;
        this.sex = sex;
        this.job = job;
        this.deptCode = deptCode;
    }
}
```

**1、定义翻译仓库**</br>
我们在teacherId上增加@TransRepo注解，using 说明的是使用哪个数据仓库获取数据</br>
**2、定义提取字段**</br>
然后我们在teacherName上增加@Trans的注解；</br>
其中trans 指的是从哪个仓库获取数据，key说明需要从仓库中提取的哪个字段；</br>
其他字段同理；</br>
也可以直接在@Trans注解上使用using指定翻译仓库，这个时候会使用这个仓库进行翻译；</br>

TeacherTransRepository 代码如下：

```java
public class TeacherTransRepository implements TransRepository<Long, TeacherDto> {

    @Override
    public Map<Long, TeacherDto> getTransValueMap(List<Long> transValues, Annotation transAnno) {
        return getTeachers().stream()
                .filter(x -> transValues.contains(x.getId()))
                .collect(Collectors.toMap(TeacherDto::getId, x -> x));
    }

    public List<TeacherDto> getTeachers() {
        List<TeacherDto> teachers = new ArrayList<>();
        teachers.add(new TeacherDto(1L, "老师1", 1L));
        teachers.add(new TeacherDto(2L, "老师2", 2L));
        teachers.add(new TeacherDto(3L, "老师3", 3L));
        teachers.add(new TeacherDto(4L, "老师4", 4L));
        return teachers;
    }

}


```

模拟根据id查询，获取指定id的数据。
SubjectTransRepository

```java
public class SubjectTransRepository implements TransRepository<Long, SubjectDto> {

    @Override
    public Map<Long, SubjectDto> getTransValueMap(List<Long> transValues, Annotation transAnno) {
        return getSubjects().stream()
                .filter(x -> transValues.contains(x.getId()))
                .collect(Collectors.toMap(SubjectDto::getId, x -> x));
    }

    public List<SubjectDto> getSubjects() {
        List<SubjectDto> subjects = new ArrayList<>();
        subjects.add(new SubjectDto(1L, "语文"));
        subjects.add(new SubjectDto(2L, "数学"));
        subjects.add(new SubjectDto(3L, "英语"));
        subjects.add(new SubjectDto(4L, "物理"));
        return subjects;
    }

}
```

注册翻译仓库：

```java
    @BeforeAll
public static void before() {
    TransRepositoryFactory.register(new TeacherTransRepository());
    TransRepositoryFactory.register(new TeacherTrans2Repository());
    TransRepositoryFactory.register(new SubjectTransRepository());
    TransRepositoryFactory.register(new DeptTransRepository());
    TransRepositoryFactory.register(new TeacherAndSubjectTransRepository());
    TransRepositoryFactory.register(new CityTransRepository());
    TransRepositoryFactory.register(new DictTransRepository(new DictLoader() {
@Override
public Map<String, String> loadDict(String dictGroup) {
        return dictMap().getOrDefault(dictGroup, new HashMap<>());
    }

private Map<String, Map<String, String>> dictMap() {
        Map<String, Map<String, String>> map = new HashMap<>();
        map.put("sexDict", new HashMap<>());
        map.put("jobDict", new HashMap<>());
        map.get("sexDict").put("1", "男");
        map.get("sexDict").put("2", "女");
        map.get("jobDict").put("1", "学习委员");
        map.get("jobDict").put("2", "生活委员");
        map.get("jobDict").put("3", "宣传委员");
        map.get("jobDict").put("4", "班长");
        map.get("jobDict").put("5", "团支书");
        map.get("jobDict").put("6", "团长");
        return map;
    }

    }));
    TransObjResolverFactory.register(new ResultResolver());
}

```

代码测试：

```java
@Test
void trans1(){
    UserDto userDto=new UserDto(1L,"张三",2L,"1","2","1");
    System.out.println("翻译前："+userDto);
    transService.trans(userDto);
    System.out.println("翻译后："+userDto);
    Assertions.assertEquals("男",userDto.getSexName());
    Assertions.assertEquals("生活委员",userDto.getJobName());
    Assertions.assertEquals("老师2",userDto.getTeacherName());
    Assertions.assertEquals("数学",userDto.getSubjectName());
    List<UserDto> userDtoList=new ArrayList<>();
    UserDto userDto2=new UserDto(2L,"李四",1L,"2","1","1");
    UserDto userDto3=new UserDto(3L,"王五",2L,"1","3","2");
    UserDto userDto4=new UserDto(4L,"赵六",3L,"2","4","2");
    userDtoList.add(userDto4);
    userDtoList.add(userDto3);
    userDtoList.add(userDto2);
    System.out.println("翻译前："+userDtoList);
    transService.trans(userDtoList);
    System.out.println("翻译后："+userDtoList);
}
```

结果输出

```java
翻译前：UserDto(id=1,name=张三,sex=1,sexName=null,job=2,jobName=null,teacherId=2,teacherName=null,subjectId=null,subjectName=null,deptCode=1,deptName=null)
翻译后：UserDto(id=1,name=张三,sex=1,sexName=男,job=2,jobName=生活委员,teacherId=2,teacherName=老师2,subjectId=2,subjectName=数学,deptCode=1,deptName=部门1)
翻译前：[UserDto(id=4,name=赵六,sex=2,sexName=null,job=4,jobName=null,teacherId=3,teacherName=null,subjectId=null,subjectName=null,deptCode=2,deptName=null),UserDto(id=3,name=王五,sex=1,sexName=null,job=3,jobName=null,teacherId=2,teacherName=null,subjectId=null,subjectName=null,deptCode=2,deptName=null),UserDto(id=2,name=李四,sex=2,sexName=null,job=1,jobName=null,teacherId=1,teacherName=null,subjectId=null,subjectName=null,deptCode=1,deptName=null)]
翻译后：[UserDto(id=4,name=赵六,sex=2,sexName=女,job=4,jobName=班长,teacherId=3,teacherName=老师3,subjectId=3,subjectName=英语,deptCode=2,deptName=部门2),UserDto(id=3,name=王五,sex=1,sexName=男,job=3,jobName=宣传委员,teacherId=2,teacherName=老师2,subjectId=2,subjectName=数学,deptCode=2,deptName=部门2),UserDto(id=2,name=李四,sex=2,sexName=女,job=1,jobName=学习委员,teacherId=1,teacherName=老师1,subjectId=1,subjectName=语文,deptCode=1,deptName=部门1)]
```

## 四、高级功能

### 1、自定义注解

使用@TransRepo注解标注在自定义注解上即可，比如框架自带的字典翻译@DictTransRepo
示例：

```java

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@TransRepo(using = TeacherTransRepository.class)
public @interface TeacherTransRepo {

    String name() default "";

}

```

```java

@Data
public class UserDto2 {

    private Long id;

    private String name;


    @TeacherTransRepo
    private List<Long> teacherIds;

    @DictTransRepo(group = "jobDict")
    private List<String> jobIds;

    @Trans(trans = "jobIds")
    private List<String> jobNames;

    @Trans(trans = "teacherIds", key = "name")
    private List<String> teacherName;

    @Trans(trans = "teacherIds", key = "subjectId")
    @TransRepo(using = SubjectTransRepository.class)
    private List<Long> subjectIds;

    @Trans(trans = "subjectIds", key = "name")
    private List<String> subjectNames;

    public UserDto2(Long id, String name, List<Long> teacherIds, List<String> jobIds) {
        this.id = id;
        this.name = name;
        this.teacherIds = teacherIds;
        this.jobIds = jobIds;
    }
}
```

测试

```java
@Test
void trans2() {
    List<Long> teacherIds=new ArrayList<>();
    teacherIds.add(1L);
    teacherIds.add(2L);
    List<String> jobIds=new ArrayList<>();
    jobIds.add("1");
    jobIds.add("2");
    UserDto2 userDto=new UserDto2(1L,"张三",teacherIds,jobIds);
    System.out.println("翻译前："+userDto);
    transService.trans(userDto);
    System.out.println("翻译后："+userDto);
    List<UserDto2> userDtoList=new ArrayList<>();
    UserDto2 userDto2=new UserDto2(2L,"李四",teacherIds,jobIds);
    List<Long> teacherIds2=new ArrayList<>();
    teacherIds2.add(3L);
    teacherIds2.add(4L);
    List<String> jobIds2=new ArrayList<>();
    jobIds2.add("3");
    jobIds2.add("4");
    UserDto2 userDto3=new UserDto2(3L,"王五",teacherIds2,jobIds2);
    UserDto2 userDto4=new UserDto2(4L,"赵六",teacherIds2,jobIds2);
    userDtoList.add(userDto4);
    userDtoList.add(userDto3);
    userDtoList.add(userDto2);
    System.out.println("翻译前："+userDtoList);
    transService.trans(userDtoList);
    System.out.println("翻译后："+userDtoList);
}

```

结果输出

```java
翻译前：UserDto2(id=1,name=张三,teacherIds=[1,2],jobIds=[1,2],jobNames=null,teacherName=null,subjectIds=null,subjectNames=null)
翻译后：UserDto2(id=1,name=张三,teacherIds=[1,2],jobIds=[1,2],jobNames=[学习委员,生活委员],teacherName=[老师1,老师2],subjectIds=[1,2],subjectNames=[语文,数学])
翻译前：[UserDto2(id=4,name=赵六,teacherIds=[3,4],jobIds=[3,4],jobNames=null,teacherName=null,subjectIds=null,subjectNames=null),UserDto2(id=3,name=王五,teacherIds=[3,4],jobIds=[3,4],jobNames=null,teacherName=null,subjectIds=null,subjectNames=null),UserDto2(id=2,name=李四,teacherIds=[1,2],jobIds=[1,2],jobNames=null,teacherName=null,subjectIds=null,subjectNames=null)]
翻译后：[UserDto2(id=4,name=赵六,teacherIds=[3,4],jobIds=[3,4],jobNames=[宣传委员,班长],teacherName=[老师3,老师4],subjectIds=[3,4],subjectNames=[英语,物理]),UserDto2(id=3,name=王五,teacherIds=[3,4],jobIds=[3,4],jobNames=[宣传委员,班长],teacherName=[老师3,老师4],subjectIds=[3,4],subjectNames=[英语,物理]),UserDto2(id=2,name=李四,teacherIds=[1,2],jobIds=[1,2],jobNames=[学习委员,生活委员],teacherName=[老师1,老师2],subjectIds=[1,2],subjectNames=[语文,数学])]
```

### 2、嵌套翻译

本框架支持多层嵌套翻译，比如翻译省市区，只要按翻译逻辑顺序定义好就行。框架会根据字段间的引用关系自动构建 `key → 子字段` 的翻译树，逐层回填。</br>
示例：

```java

@Data
public class CityDto {

    @TransRepo(using = CityTransRepository.class)
    private Long areaId;

    @Trans(trans = "areaId", key = "name", using = CityTransRepository.class)
    private String areaName;

    @Trans(trans = "areaId", key = "pid", using = CityTransRepository.class)
    @TransRepo(using = CityTransRepository.class)
    private Long cityId;

    @Trans(trans = "cityId", key = "name")
    private String cityName;

    @Trans(trans = "cityId", key = "pid")
//    @TransRepo(using = CityTransRepository.class)
    private Long provinceId;

    @Trans(trans = "provinceId", key = "name", using = CityTransRepository.class)
    private String provinceName;


    public CityDto(Long areaId) {
        this.areaId = areaId;
    }

}
```

```java
public class CityTransRepository implements TransRepository<Long, CityEntity> {

    @Override
    public Map<Long, CityEntity> getTransValueMap(List<Long> transValues, Annotation transAnno) {
        return data().stream()
                .filter(x -> transValues.contains(x.getId()))
                .collect(Collectors.toMap(CityEntity::getId, x -> x));
    }

    private List<CityEntity> data() {
        List<CityEntity> cityEntities = new ArrayList<>();
        cityEntities.add(new CityEntity(1L, "湖南省", 0L));
        cityEntities.add(new CityEntity(2L, "长沙市", 1L));
        cityEntities.add(new CityEntity(3L, "株洲市", 1L));
        cityEntities.add(new CityEntity(4L, "湘潭市", 1L));
        cityEntities.add(new CityEntity(5L, "雨花区", 2L));
        cityEntities.add(new CityEntity(6L, "岳麓区", 2L));
        cityEntities.add(new CityEntity(7L, "长沙县", 2L));
        cityEntities.add(new CityEntity(8L, "测试县", 10L));
        return cityEntities;
    }

}

@Data
@AllArgsConstructor
public class CityEntity {

    private Long id;

    private String name;

    private Long pid;

}

```

测试代码：

```java
@Test
void trans6(){
    CityDto cityDto=new CityDto(7L);
    System.out.println("翻译前："+cityDto);
    transService.trans(cityDto);
    System.out.println("翻译后："+cityDto);
    Assertions.assertEquals("长沙县",cityDto.getAreaName());
    Assertions.assertEquals("长沙市",cityDto.getCityName());
    Assertions.assertEquals("湖南省",cityDto.getProvinceName());
    List<CityDto> cityDtoList=new ArrayList<>();
    cityDtoList.add(new CityDto(7L));
    cityDtoList.add(new CityDto(8L));
    System.out.println("翻译前："+cityDtoList);
    transService.trans(cityDtoList);
    System.out.println("翻译后："+cityDtoList);
    Assertions.assertEquals("长沙县",cityDtoList.get(0).getAreaName());
    Assertions.assertEquals("长沙市",cityDtoList.get(0).getCityName());
    Assertions.assertEquals("湖南省",cityDtoList.get(0).getProvinceName());
    Assertions.assertEquals("测试县",cityDtoList.get(1).getAreaName());
    Assertions.assertNull(cityDtoList.get(1).getCityName());
    Assertions.assertNull(cityDtoList.get(1).getProvinceName());
}

```

翻译结果如下：

```java
翻译前：CityDto(areaId=7,areaName=null,cityId=null,cityName=null,provinceId=null,provinceName=null)
翻译后：CityDto(areaId=7,areaName=长沙县,cityId=2,cityName=长沙市,provinceId=1,provinceName=湖南省)
翻译前：[CityDto(areaId=7,areaName=null,cityId=null,cityName=null,provinceId=null,provinceName=null),CityDto(areaId=8,areaName=null,cityId=null,cityName=null,provinceId=null,provinceName=null)]
翻译后：[CityDto(areaId=7,areaName=长沙县,cityId=2,cityName=长沙市,provinceId=1,provinceName=湖南省),CityDto(areaId=8,areaName=测试县,cityId=10,cityName=null,provinceId=null,provinceName=null)]
```

> 注意：`areaId=8`（测试县）的 `pid=10` 在数据源中不存在，因此 `cityId`、`provinceId` 等后续层级无法继续翻译，对应字段为 `null`。这是**数据缺失**导致的预期行为，而非框架缺陷——只要链路上的每个 key 都能在仓库中找到对应记录，嵌套翻译即可正常完成。

### 3、包装类翻译

有些类是包装类，比如返回的结果，返回的分页对象等，需要翻译的数据一般都是里面的实际业务对象，这时候，需要我们去配置解析包装类的解析器。
示例：

```java

@Data
@AllArgsConstructor
public class Result<T> {

    private T data;

    private String message;

}

```

配置解析器，实现TransObjResolver接口即可

```java
public class ResultResolver implements TransObjResolver {

    @Override
    public boolean support(Object obj) {
        return obj instanceof Result;
    }

    @Override
    public Object resolveTransObj(Object obj) {
        return ((Result<?>) obj).getData();
    }

}

```

```java
TransObjResolverFactory.register(new ResultResolver());
```

测试：

```java
@Test
void trans3() {
    List<Long> teacherIds=new ArrayList<>();
    teacherIds.add(1L);
    teacherIds.add(2L);
    List<String> jobIds=new ArrayList<>();
    jobIds.add("1");
    jobIds.add("2");
    jobIds.add("3");
    UserDto2 userDto=new UserDto2(1L,"张三",teacherIds,jobIds);
    Result<UserDto2> result=new Result<>(userDto,"success");
    System.out.println("翻译前："+result);
    transService.trans(result);
    System.out.println("翻译后："+result);
    UserDto2 userDto2=new UserDto2(2L,"李四",teacherIds,jobIds);
    Result<UserDto2> result2=new Result<>(userDto2,"success");
    Result<Result<UserDto2>>result3=new Result<>(result2,"success");
    System.out.println("翻译前："+result3);
    transService.trans(result3);
    System.out.println("翻译后："+result3);
}

```

结果输出：

```java
翻译前：Result(data=UserDto2(id=1,name=张三,teacherIds=[1,2],jobIds=[1,2,3],jobNames=null,teacherName=null,subjectIds=null,subjectNames=null),message=success)
翻译后：Result(data=UserDto2(id=1,name=张三,teacherIds=[1,2],jobIds=[1,2,3],jobNames=[学习委员,生活委员,宣传委员],teacherName=[老师1,老师2],subjectIds=[1,2],subjectNames=[语文,数学]),message=success)
翻译前：Result(data=Result(data=UserDto2(id=2,name=李四,teacherIds=[1,2],jobIds=[1,2,3],jobNames=null,teacherName=null,subjectIds=null,subjectNames=null),message=success),message=success)
翻译后：Result(data=Result(data=UserDto2(id=2,name=李四,teacherIds=[1,2],jobIds=[1,2,3],jobNames=[学习委员,生活委员,宣传委员],teacherName=[老师1,老师2],subjectIds=[1,2],subjectNames=[语文,数学]),message=success),message=success)
```

> 包装类拆包由 `TransObjResolver` 负责，框架内置对常见包装类型的解析。若返回值是异步 / 响应式类型（如 `CompletableFuture`、`Mono`、`Flux`），Spring 集成下请使用 `TransUtil.transResult(result)`（详见[五、与 Spring Boot 集成](#五与-spring-boot-集成)），框架会在结果就绪后再执行翻译，而非对包装对象本身翻译（后者会静默失效）。

### 4、对象直接填充

有时候我们可能需要把完整的对象填充到需要翻译的字段中，只需要返回类型和需要翻译的字段的类型一致，框架会自动填充。

```java

@Data
public class UserDto3 {

    private Long id;

    private String name;

    @TeacherTransRepo
    @TransRepo(name = "teacherId1", using = TeacherTrans2Repository.class)
    private Long teacherId;

    @Trans(trans = "teacherId")
    private TeacherDto teacher;

    @Trans(trans = "teacherId1")
    private boolean isYuwenTeacher;

    @Trans(trans = "teacherId", using = TeacherTrans2Repository.class)
    private Boolean isYuwenTeacher2;

    public UserDto3(Long id, String name, Long teacherId) {
        this.id = id;
        this.name = name;
        this.teacherId = teacherId;
    }
}

```

```java
public class TeacherTrans2Repository implements TransRepository<Long, Boolean> {

    @Override
    public Map<Long, Boolean> getTransValueMap(List<Long> transValues, Annotation transAnno) {
        return getTeachers().stream()
                .filter(x -> transValues.contains(x.getId()))
                .collect(Collectors.toMap(TeacherDto::getId, x -> x.getSubjectId() == 1));
    }

    public List<TeacherDto> getTeachers() {
        List<TeacherDto> teachers = new ArrayList<>();
        teachers.add(new TeacherDto(1L, "老师1", 1L));
        teachers.add(new TeacherDto(2L, "老师2", 2L));
        teachers.add(new TeacherDto(3L, "老师3", 3L));
        teachers.add(new TeacherDto(4L, "老师4", 4L));
        return teachers;
    }

}
```

```java
@Test
void trans4(){
    UserDto3 userDto=new UserDto3(1L,"张三",1L);
    System.out.println("翻译前："+userDto);
    transService.trans(userDto);
    System.out.println("翻译后："+userDto);
    Assertions.assertNotNull(userDto.getTeacher());
    Assertions.assertEquals("老师1",userDto.getTeacher().getName());
    Assertions.assertTrue(userDto.isYuwenTeacher());
    Assertions.assertTrue(userDto.getIsYuwenTeacher2());
    List<UserDto3> userDtoList=new ArrayList<>();
    UserDto3 userDto2=new UserDto3(2L,"李四",1L);
    UserDto3 userDto3=new UserDto3(3L,"王五",2L);
    UserDto3 userDto4=new UserDto3(4L,"赵六",3L);
    userDtoList.add(userDto2);
    userDtoList.add(userDto3);
    userDtoList.add(userDto4);
    System.out.println("翻译前："+userDtoList);
    transService.trans(userDtoList);
    System.out.println("翻译后："+userDtoList);
    Assertions.assertNotNull(userDtoList.get(0).getTeacher());
    Assertions.assertEquals("老师1",userDtoList.get(0).getTeacher().getName());
    Assertions.assertTrue(userDtoList.get(0).isYuwenTeacher());
    Assertions.assertTrue(userDtoList.get(0).getIsYuwenTeacher2());
    Assertions.assertNotNull(userDtoList.get(1).getTeacher());
    Assertions.assertEquals("老师2",userDtoList.get(1).getTeacher().getName());
    Assertions.assertFalse(userDtoList.get(1).isYuwenTeacher());
    Assertions.assertFalse(userDtoList.get(1).getIsYuwenTeacher2());
    Assertions.assertNotNull(userDtoList.get(2).getTeacher());
    Assertions.assertEquals("老师3",userDtoList.get(2).getTeacher().getName());
    Assertions.assertFalse(userDtoList.get(2).isYuwenTeacher());
    Assertions.assertFalse(userDtoList.get(2).getIsYuwenTeacher2());
}
```

### 5、异常处理

翻译过程中若出现配置或运行问题，框架统一抛出 `TransException`（运行时异常），便于及时定位，而非像旧版本那样静默失败。常见场景：

- **未初始化**：未调用 `TransService.init()`（或非 Spring 环境下未注入已初始化的 `TransService`）即调用 `trans()`，抛出 `TransService has not been initialized`；
- **仓库未注册**：`@TransRepo` 指向的 `TransRepository` 未通过 `TransRepositoryFactory.register(...)`（或 Spring 下未标注 `@Component`）注册，翻译时抛出 `TransRepository is not registered`；
- **引用悬空**：`@Trans(trans = "xxx")` 指向的源字段 `xxx` 在类上既没有同名 `@TransRepo`、也没有通过 `@Trans(using = ...)` 显式指定仓库，抛出 `references translation repository 'xxx' which is not declared`；
- **字段不存在**：`@Trans(using = X, trans = "yyy")` 中 `yyy` 在类中不存在，抛出 `but no such field exists`；
- **循环引用**：嵌套翻译配置形成环路（如 A 依赖 B、B 又依赖 A），构建翻译树时抛出 `Circular translation reference detected`。

> 这些校验在类元数据解析阶段（首次翻译该类时）和翻译执行阶段都会进行，错误信息包含类名与字段名，便于快速定位。

## 五、与 Spring Boot 集成

maven 引入

```xml
<dependency>
    <groupId>io.github.orangewest</groupId>
    <artifactId>easy-trans-spring-start</artifactId>
    <version>2.0.0</version>
</dependency>
```

### 自动装配

引入 `easy-trans-spring-start` 后，框架通过 Spring Boot 自动配置（2.x 为 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，1.x 为 `spring.factories`）装配 `EasyTransAutoConfiguration`，无需任何额外配置即可使用：

- 自动创建并 `init()` 一个 `TransService`（单例，`@ConditionalOnMissingBean` 可覆盖）；
- 自动扫描容器中所有 `TransRepository`、`TransObjResolver`、`DictLoader` 实现类并注册（标注 `@Component` 即可，无需手动 `register`）；
- 自动注册 `@AutoTrans` 切面与 `TransUtil`。

### 在方法上自动翻译

只需在需要翻译的返回值方法上标注 `@AutoTrans`，框架会拦截返回值并完成翻译：

```java
@GetMapping("/query")
@AutoTrans
public Result<PageData<BizDTO>> page(Query query) {
    PageData<BizDTO> page = bizService.page(query);
    return new Result<PageData<BizDTO>>().ok(page);
}
```

- **同步返回**：直接对返回值（含 `Result`、`Page` 等包装对象，由 `TransObjResolver` 拆包）执行翻译；
- **异步 / 响应式返回**：若返回值是 `CompletableFuture`、`Mono`、`Flux` 等，切面会调用 `TransUtil.transResult`，在结果就绪后再翻译，而非对包装对象本身翻译。

### 手动翻译返回值

若需在非 `@AutoTrans` 方法中翻译返回值，可手动调用：

```java
Object result = bizService.page(query);
return TransUtil.transResult(result);   // 自动处理同步 / 异步 / 响应式
```

也可以直接调用 `TransUtil.trans(obj)` 对已经就绪的对象做同步翻译。

### 可选：Micrometer 监控

`micrometer-core` 在 `easy-trans-spring-start` 中是 **optional 依赖**。当 classpath 中存在 Micrometer 且容器中有 `MeterRegistry` 时，框架自动将翻译指标桥接到 Micrometer，无需任何代码；否则退化为无指标（零开销）。

需引入 Micrometer（以 Spring Boot Actuator 为例）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

桥接后暴露以下 Timer 指标：

| 指标名 | Tag | 含义 |
| --- | --- | --- |
| `easytrans.translate` | `success` | 单次 `trans()` 调用耗时 |
| `easytrans.repository` | `repo`、`success` | 单个翻译仓库耗时（`repo` 为 `@TransRepo` 字段名或 `@Trans` 源字段名） |

<!--links-->

[your-project-path]:orangewest/easy-trans

[contributors-shield]:https://img.shields.io/github/contributors/orangewest/easy-trans.svg?style=flat-square

[contributors-url]: https://github.com/orangewest/easy-trans/graphs/contributors

[forks-shield]: https://img.shields.io/github/forks/orangewest/easy-trans.svg?style=flat-square

[forks-url]: https://github.com/orangewest/easy-trans/network/members

[stars-shield]: https://img.shields.io/github/stars/orangewest/easy-trans.svg?style=flat-square

[stars-url]: https://github.com/orangewest/easy-trans/stargazers

[issues-shield]: https://img.shields.io/github/issues/orangewest/easy-trans.svg?style=flat-square

[issues-url]: https://img.shields.io/github/issues/orangewest/easy-trans.svg

[license-shield]: https://img.shields.io/github/license/orangewest/easy-trans.svg?style=flat-square

[license-url]: https://github.com/orangewest/easy-trans/blob/master/LICENSE.txt

