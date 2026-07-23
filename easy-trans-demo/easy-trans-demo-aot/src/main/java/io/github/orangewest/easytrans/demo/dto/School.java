package io.github.orangewest.easytrans.demo.dto;

/**
 * Plain result entity returned by {@code SchoolRepository}.
 *
 * <p>Deliberately NOT annotated with {@code @Trans}/{@code @TransRepo}: it exists only as a
 * repository result type. Its {@code name} field is read reflectively at runtime by
 * {@code ReflectUtils.readValueByKey} (via {@code TransModel.fillValue}). Under GraalVM native
 * image this field access only succeeds if {@code EasyTransRuntimeHints} (R6) registered the
 * {@code ACCESS_DECLARED_FIELDS} hint for this class -- which it does by resolving the
 * generic {@code R} of {@code SchoolRepository} via {@code ResolvableType}.
 */
public class School {

    private Integer id;
    private String name;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "School(id=" + id + ", name=" + name + ")";
    }
}
