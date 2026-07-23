package io.github.orangewest.easytrans.demo.dto;

/**
 * Enum used as a translation source via {@code @EnumTrans(enumClass = NationEnum.class)}.
 *
 * <p>Its {@code code} and {@code label} fields are read reflectively by
 * {@code EnumTransRepository}/{@code ReflectUtils.readValueByKey} at runtime
 * (code to match the source value, label as the displayed value). Under GraalVM
 * native image these field reads only succeed if {@code EasyTransRuntimeHints} (R6)
 * registered the {@code ACCESS_DECLARED_FIELDS} hint for this enum class -- which it
 * does by capturing the {@code enumClass()} value of the {@code @EnumTrans} annotation.
 */
public enum NationEnum {

    CN("cn", "China"),
    US("us", "America");

    public final String code;
    public final String label;

    NationEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
