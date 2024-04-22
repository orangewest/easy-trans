package io.github.orangewest.trans.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherDto {

    private Long id;

    private String name;

    private Long subjectId;


}
