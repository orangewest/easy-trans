package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.annotation.DictTrans;

public class NestAddressDto {

    private String cityCode;

    @DictTrans(group = "cityDict", trans = "cityCode")
    private String cityName;

    public NestAddressDto() {
    }

    public NestAddressDto(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCityCode() {
        return cityCode;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

}
