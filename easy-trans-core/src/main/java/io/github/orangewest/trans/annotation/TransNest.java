package io.github.orangewest.trans.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field whose value (or its elements, for {@code Iterable}/arrays) should
 * be recursively translated via {@code trans()}. The framework collects all
 * same-type nested objects across the object graph, batch-translates them in one
 * pass (preserving the existing bulk/parallel advantage), and uses identity-based
 * cycle detection to prevent infinite recursion.
 *
 * <p>Typical usage:
 * <pre>{@code
 * class UserDto {
 *     @TransNest
 *     private List<OrderDto> orders;   // each OrderDto.statusName gets filled
 *     @TransNest
 *     private AddressDto address;      // AddressDto.cityName gets filled
 * }
 * }</pre>
 *
 * <p>{@code java.*} types, {@code null} values, and primitives are skipped
 * automatically.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TransNest {
}
