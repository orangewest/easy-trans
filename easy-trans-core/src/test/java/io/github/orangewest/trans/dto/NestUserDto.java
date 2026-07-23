package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.annotation.DictTrans;
import io.github.orangewest.trans.annotation.TransNest;

import java.util.ArrayList;
import java.util.List;

public class NestUserDto {

    private Long id;
    private String name;

    private String sex;

    @DictTrans(group = "sexDict", trans = "sex")
    private String sexName;

    @TransNest
    private List<NestOrderDto> orders;

    @TransNest
    private NestAddressDto address;

    public NestUserDto() {
    }

    public NestUserDto(Long id, String name, String sex) {
        this.id = id;
        this.name = name;
        this.sex = sex;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSex() {
        return sex;
    }

    public String getSexName() {
        return sexName;
    }

    public void setSexName(String sexName) {
        this.sexName = sexName;
    }

    public List<NestOrderDto> getOrders() {
        return orders;
    }

    public void setOrders(List<NestOrderDto> orders) {
        this.orders = orders;
    }

    public NestAddressDto getAddress() {
        return address;
    }

    public void setAddress(NestAddressDto address) {
        this.address = address;
    }

}
