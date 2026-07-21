package io.github.orangewest.trans.dto;

public class CityEntity {

    private Long id;

    private String name;

    private Long pid;

    public CityEntity(Long id, String name, Long pid) {
        this.id = id;
        this.name = name;
        this.pid = pid;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getPid() {
        return pid;
    }
}
