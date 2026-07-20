package io.github.orangewest.trans.repository;

import io.github.orangewest.trans.dto.CityEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CityTransRepository implements TransRepository<Long, CityEntity> {

    @Override
    public Map<Long, CityEntity> getTransValueMap(List<Long> transValues, TransContext context) {
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
