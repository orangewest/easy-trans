package io.github.orangewest.trans.service;

import io.github.orangewest.trans.dto.NestAddressDto;
import io.github.orangewest.trans.dto.NestChildDto;
import io.github.orangewest.trans.dto.NestOrderDto;
import io.github.orangewest.trans.dto.NestParentDto;
import io.github.orangewest.trans.dto.NestUserDto;
import io.github.orangewest.trans.repository.dict.DictLoader;
import io.github.orangewest.trans.repository.dict.DictTransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TransNestTest {

    TransService transService;

    @BeforeAll
    static void before() {
        TransRepositoryFactory.register(new DictTransRepository(new DictLoader() {
            @Override
            public Map<String, String> loadDict(String dictGroup) {
                return dictMap().getOrDefault(dictGroup, new HashMap<>());
            }

            private Map<String, Map<String, String>> dictMap() {
                Map<String, Map<String, String>> map = new HashMap<>();
                map.put("sexDict", new HashMap<>());
                map.put("orderStatus", new HashMap<>());
                map.put("cityDict", new HashMap<>());
                map.get("sexDict").put("1", "Male");
                map.get("sexDict").put("2", "Female");
                map.get("orderStatus").put("P", "Paid");
                map.get("orderStatus").put("S", "Shipped");
                map.get("orderStatus").put("D", "Delivered");
                map.get("cityDict").put("BJ", "Beijing");
                map.get("cityDict").put("SH", "Shanghai");
                return map;
            }
        }));
    }

    @BeforeEach
    void init() {
        transService = new TransService();
    }

    @Test
    void transNestOrderList() {
        NestUserDto user = new NestUserDto(1L, "Alice", "1");
        NestOrderDto order1 = new NestOrderDto(101L, "P");
        NestOrderDto order2 = new NestOrderDto(102L, "S");
        List<NestOrderDto> orders = new ArrayList<>();
        orders.add(order1);
        orders.add(order2);
        user.setOrders(orders);

        transService.trans(user);

        Assertions.assertEquals("Male", user.getSexName());
        Assertions.assertEquals("Paid", user.getOrders().get(0).getStatusName());
        Assertions.assertEquals("Shipped", user.getOrders().get(1).getStatusName());
    }

    @Test
    void transNestSingleObject() {
        NestUserDto user = new NestUserDto(2L, "Bob", "2");
        NestAddressDto address = new NestAddressDto("SH");
        user.setAddress(address);

        transService.trans(user);

        Assertions.assertEquals("Female", user.getSexName());
        Assertions.assertEquals("Shanghai", user.getAddress().getCityName());
    }

    @Test
    void transNestCombined() {
        NestUserDto user = new NestUserDto(3L, "Charlie", "1");
        NestOrderDto order1 = new NestOrderDto(201L, "D");
        NestOrderDto order2 = new NestOrderDto(202L, "P");
        List<NestOrderDto> orders = new ArrayList<>();
        orders.add(order1);
        orders.add(order2);
        user.setOrders(orders);
        NestAddressDto address = new NestAddressDto("BJ");
        user.setAddress(address);

        transService.trans(user);

        Assertions.assertEquals("Male", user.getSexName());
        Assertions.assertEquals("Delivered", user.getOrders().get(0).getStatusName());
        Assertions.assertEquals("Paid", user.getOrders().get(1).getStatusName());
        Assertions.assertEquals("Beijing", user.getAddress().getCityName());
    }

    @Test
    void transNestCycleShouldNotLoop() {
        NestParentDto parent = new NestParentDto("Parent", "1");
        NestChildDto child = new NestChildDto("Child", "2");
        parent.setChild(child);
        child.setParent(parent);

        // Should complete without infinite loop or stack overflow
        Assertions.assertDoesNotThrow(() -> transService.trans(parent));

        // Parent's own translation should work
        Assertions.assertEquals("Male", parent.getSexName());
        // Child's translation should work too
        Assertions.assertEquals("Female", child.getTypeName());
    }

}
