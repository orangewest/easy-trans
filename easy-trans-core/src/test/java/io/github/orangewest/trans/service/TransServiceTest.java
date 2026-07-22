package io.github.orangewest.trans.service;

import io.github.orangewest.trans.dto.*;
import io.github.orangewest.trans.repository.*;
import io.github.orangewest.trans.repository.dict.DictLoader;
import io.github.orangewest.trans.repository.dict.DictTransRepository;
import io.github.orangewest.trans.resolver.ResultResolver;
import io.github.orangewest.trans.resolver.TransValueResolverFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TransServiceTest {

    TransService transService;

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
        TransValueResolverFactory.register(new ResultResolver());
    }

    @BeforeEach
    public void init() {
        transService = new TransService();
    }

    @Test
    void trans1() {
        UserDto userDto = new UserDto(1L, "张三", 2L, "1", "2", "1");
        System.out.println("翻译前：" + userDto);
        transService.trans(userDto);
        System.out.println("翻译后：" + userDto);
        Assertions.assertEquals("男", userDto.getSexName());
        Assertions.assertEquals("生活委员", userDto.getJobName());
        Assertions.assertEquals("老师2", userDto.getTeacherName());
        Assertions.assertEquals("数学", userDto.getSubjectName());
        List<UserDto> userDtoList = new ArrayList<>();
        UserDto userDto2 = new UserDto(2L, "李四", 1L, "2", "1", "1");
        UserDto userDto3 = new UserDto(3L, "王五", 2L, "1", "3", "2");
        UserDto userDto4 = new UserDto(4L, "赵六", 3L, "2", "4", "2");
        userDtoList.add(userDto4);
        userDtoList.add(userDto3);
        userDtoList.add(userDto2);
        System.out.println("翻译前：" + userDtoList);
        transService.trans(userDtoList);
        System.out.println("翻译后：" + userDtoList);

    }

    @Test
    void trans2() {
        List<Long> teacherIds = new ArrayList<>();
        teacherIds.add(1L);
        teacherIds.add(2L);
        List<String> jobIds = new ArrayList<>();
        jobIds.add("1");
        jobIds.add("2");
        UserDto2 userDto = new UserDto2(1L, "张三", teacherIds, jobIds);
        System.out.println("翻译前：" + userDto);
        transService.trans(userDto);
        System.out.println("翻译后：" + userDto);
        List<UserDto2> userDtoList = new ArrayList<>();
        UserDto2 userDto2 = new UserDto2(2L, "李四", teacherIds, jobIds);
        List<Long> teacherIds2 = new ArrayList<>();
        teacherIds2.add(3L);
        teacherIds2.add(4L);
        List<String> jobIds2 = new ArrayList<>();
        jobIds2.add("3");
        jobIds2.add("4");
        UserDto2 userDto3 = new UserDto2(3L, "王五", teacherIds2, jobIds2);
        UserDto2 userDto4 = new UserDto2(4L, "赵六", teacherIds2, jobIds2);
        userDtoList.add(userDto4);
        userDtoList.add(userDto3);
        userDtoList.add(userDto2);
        System.out.println("翻译前：" + userDtoList);
        transService.trans(userDtoList);
        System.out.println("翻译后：" + userDtoList);
    }

    @Test
    void trans3() {
        List<Long> teacherIds = new ArrayList<>();
        teacherIds.add(1L);
        teacherIds.add(2L);
        List<String> jobIds = new ArrayList<>();
        jobIds.add("1");
        jobIds.add("2");
        jobIds.add("3");
        UserDto2 userDto = new UserDto2(1L, "张三", teacherIds, jobIds);
        Result<UserDto2> result = new Result<>(userDto, "success");
        System.out.println("翻译前：" + result);
        transService.trans(result);
        System.out.println("翻译后：" + result);
        UserDto2 userDto2 = new UserDto2(2L, "李四", teacherIds, jobIds);
        Result<UserDto2> result2 = new Result<>(userDto2, "success");
        Result<Result<UserDto2>> result3 = new Result<>(result2, "success");
        System.out.println("翻译前：" + result3);
        transService.trans(result3);
        System.out.println("翻译后：" + result3);
    }

    @Test
    void trans4() {
        UserDto3 userDto = new UserDto3(1L, "张三", 1L);
        System.out.println("翻译前：" + userDto);
        transService.trans(userDto);
        System.out.println("翻译后：" + userDto);
        Assertions.assertNotNull(userDto.getTeacher());
        Assertions.assertEquals("老师1", userDto.getTeacher().getName());
        Assertions.assertTrue(userDto.isYuwenTeacher());
        Assertions.assertTrue(userDto.getIsYuwenTeacher2());
        List<UserDto3> userDtoList = new ArrayList<>();
        UserDto3 userDto2 = new UserDto3(2L, "李四", 1L);
        UserDto3 userDto3 = new UserDto3(3L, "王五", 2L);
        UserDto3 userDto4 = new UserDto3(4L, "赵六", 3L);
        userDtoList.add(userDto2);
        userDtoList.add(userDto3);
        userDtoList.add(userDto4);
        System.out.println("翻译前：" + userDtoList);
        transService.trans(userDtoList);
        System.out.println("翻译后：" + userDtoList);
        Assertions.assertNotNull(userDtoList.get(0).getTeacher());
        Assertions.assertEquals("老师1", userDtoList.get(0).getTeacher().getName());
        Assertions.assertTrue(userDtoList.get(0).isYuwenTeacher());
        Assertions.assertTrue(userDtoList.get(0).getIsYuwenTeacher2());
        Assertions.assertNotNull(userDtoList.get(1).getTeacher());
        Assertions.assertEquals("老师2", userDtoList.get(1).getTeacher().getName());
        Assertions.assertFalse(userDtoList.get(1).isYuwenTeacher());
        Assertions.assertFalse(userDtoList.get(1).getIsYuwenTeacher2());
        Assertions.assertNotNull(userDtoList.get(2).getTeacher());
        Assertions.assertEquals("老师3", userDtoList.get(2).getTeacher().getName());
        Assertions.assertFalse(userDtoList.get(2).isYuwenTeacher());
        Assertions.assertFalse(userDtoList.get(2).getIsYuwenTeacher2());
    }

    @Test
    void trans5() {
        UserDto4 userDto = new UserDto4(1L, "张三", 2L, 1L);
        System.out.println("翻译前：" + userDto);
        transService.trans(userDto);
        System.out.println("翻译后：" + userDto);
        Assertions.assertEquals("2#1", userDto.getTeacherAndSubject());
        List<UserDto4> userDtoList = new ArrayList<>();
        UserDto4 userDto2 = new UserDto4(2L, "李四", 1L, 2L);
        UserDto4 userDto3 = new UserDto4(3L, "王五", 2L, 3L);
        UserDto4 userDto4 = new UserDto4(4L, "赵六", 3L, 4L);
        userDtoList.add(userDto2);
        userDtoList.add(userDto3);
        userDtoList.add(userDto4);
        System.out.println("翻译前：" + userDtoList);
        transService.trans(userDtoList);
        System.out.println("翻译后：" + userDtoList);
        Assertions.assertEquals("1#2", userDtoList.get(0).getTeacherAndSubject());
        Assertions.assertEquals("2#3", userDtoList.get(1).getTeacherAndSubject());
        Assertions.assertEquals("3#4", userDtoList.get(2).getTeacherAndSubject());
    }

    @Test
    void trans6() {
        CityDto cityDto = new CityDto(7L);
        System.out.println("翻译前：" + cityDto);
        transService.trans(cityDto);
        System.out.println("翻译后：" + cityDto);
        Assertions.assertEquals("长沙县", cityDto.getAreaName());
        Assertions.assertEquals("长沙市", cityDto.getCityName());
        Assertions.assertEquals("湖南省", cityDto.getProvinceName());
        List<CityDto> cityDtoList = new ArrayList<>();
        cityDtoList.add(new CityDto(7L));
        cityDtoList.add(new CityDto(8L));
        System.out.println("翻译前：" + cityDtoList);
        transService.trans(cityDtoList);
        System.out.println("翻译后：" + cityDtoList);
        Assertions.assertEquals("长沙县", cityDtoList.get(0).getAreaName());
        Assertions.assertEquals("长沙市", cityDtoList.get(0).getCityName());
        Assertions.assertEquals("湖南省", cityDtoList.get(0).getProvinceName());
        Assertions.assertEquals("测试县", cityDtoList.get(1).getAreaName());
        Assertions.assertNull(cityDtoList.get(1).getCityName());
        Assertions.assertNull(cityDtoList.get(1).getProvinceName());
    }

}
