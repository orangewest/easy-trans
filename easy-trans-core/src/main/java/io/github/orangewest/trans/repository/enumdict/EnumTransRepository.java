package io.github.orangewest.trans.repository.enumdict;

import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.util.ReflectUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 枚举即字典仓库：把一个 enum 当作翻译源。
 * <p>
 * 由 {@link io.github.orangewest.trans.annotation.EnumTrans} 驱动。对每个源值匹配到对应枚举常量并作为结果对象返回，
 * 再由目标字段的 {@code @Trans(key=label)} 从枚举常量中取出展示字段（public 字段）。
 * <p>
 * 无外部依赖、无状态，可作为单例注册一次。
 */
public class EnumTransRepository implements TransRepository<Object, Object> {

    @Override
    public Map<Object, Object> getTransValueMap(List<Object> transValues, TransContext context) {
        Class<?> enumClass = resolveEnumClass(context);
        if (enumClass == null || !enumClass.isEnum()) {
            return Collections.emptyMap();
        }
        String codeField = context.get("code", String.class);
        Object[] constants = enumClass.getEnumConstants();
        Map<Object, Object> result = new HashMap<>();
        for (Object v : transValues) {
            if (v == null) {
                continue;
            }
            Enum<?> matched = match(constants, v, codeField);
            if (matched != null) {
                result.put(v, matched);
            }
        }
        return result;
    }

    private Class<?> resolveEnumClass(TransContext context) {
        Class<?> ec = context.get("enumClass", Class.class);
        if (ec != null && ec != Void.class) {
            return ec;
        }
        return context.sourceType();
    }

    private static Enum<?> match(Object[] constants, Object v, String codeField) {
        // 恒等：源值本身就是该枚举的常量
        for (Object c : constants) {
            if (c == v) {
                return (Enum<?>) c;
            }
        }
        // 按枚举的 code 字段匹配
        if (codeField != null && !codeField.isEmpty()) {
            for (Object c : constants) {
                if (Objects.equals(ReflectUtils.readValueByKey(c, codeField), v)) {
                    return (Enum<?>) c;
                }
            }
            return null;
        }
        // 无 code：按 name（String 源）/ ordinal（int 源）匹配
        for (Object c : constants) {
            Enum<?> e = (Enum<?>) c;
            if (v instanceof String s && e.name().equals(s)) {
                return e;
            }
            if (v instanceof Number n && e.ordinal() == n.intValue()) {
                return e;
            }
        }
        return null;
    }
}
