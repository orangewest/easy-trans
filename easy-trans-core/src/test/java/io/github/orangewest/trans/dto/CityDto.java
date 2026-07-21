package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.repository.CityTransRepository;

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
    private Long provinceId;

    @Trans(trans = "provinceId", key = "name", using = CityTransRepository.class)
    private String provinceName;

    public CityDto(Long areaId) {
        this.areaId = areaId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public String getAreaName() {
        return areaName;
    }

    public Long getCityId() {
        return cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public Long getProvinceId() {
        return provinceId;
    }

    public String getProvinceName() {
        return provinceName;
    }
}
