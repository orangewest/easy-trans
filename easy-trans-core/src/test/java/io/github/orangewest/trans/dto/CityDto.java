package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.repository.CityTransRepository;
import lombok.Data;

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
