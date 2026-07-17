package io.github.orangewest.trans.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CityEntity {

    private Long id;

    private String name;

    private Long pid;

}
