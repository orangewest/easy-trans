package io.github.orangewest.easytrans.demo.dto;

import io.github.orangewest.trans.annotation.EnumTrans;
import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.easytrans.demo.repository.SchoolRepository;

/**
 * R6 end-to-end scenario DTO:
 * <ul>
 *   <li>{@code schoolId} + {@code schoolName}: the repository result type is the plain
 *       {@code School} entity (not a {@code @Trans} DTO), so its {@code name} field is
 *       reflectively read only thanks to the R6 AOT hint.</li>
 *   <li>{@code nationCode} + {@code nationName}: {@code @EnumTrans(enumClass = NationEnum.class)}
 *       reads the enum constant's {@code label} field reflectively -- also covered by R6.</li>
 * </ul>
 */
public class R6Dto {

    @TransRepo(using = SchoolRepository.class)
    private Integer schoolId;

    @Trans(trans = "schoolId", key = "name")
    private String schoolName;

    private String nationCode;

    @EnumTrans(trans = "nationCode", enumClass = NationEnum.class, code = "code")
    private String nationName;

    public Integer getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Integer schoolId) {
        this.schoolId = schoolId;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getNationCode() {
        return nationCode;
    }

    public void setNationCode(String nationCode) {
        this.nationCode = nationCode;
    }

    public String getNationName() {
        return nationName;
    }

    public void setNationName(String nationName) {
        this.nationName = nationName;
    }

    @Override
    public String toString() {
        return "R6Dto(schoolId=" + schoolId + ", schoolName=" + schoolName
                + ", nationCode=" + nationCode                 + ", nationName=" + nationName + ")";
    }
}
