package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.annotation.DictTrans;

public class NestOrderDto {

    private Long id;
    private String status;

    @DictTrans(group = "orderStatus", trans = "status")
    private String statusName;

    public NestOrderDto() {
    }

    public NestOrderDto(Long id, String status) {
        this.id = id;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

}
