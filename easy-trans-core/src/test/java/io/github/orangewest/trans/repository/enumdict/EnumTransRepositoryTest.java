package io.github.orangewest.trans.repository.enumdict;

import io.github.orangewest.trans.repository.DefaultTransContext;
import io.github.orangewest.trans.repository.TransContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EnumTransRepositoryTest {

    enum Sex {
        MALE("男"), FEMALE("女");
        public final String label;

        Sex(String label) {
            this.label = label;
        }
    }

    enum Status {
        ON(1, "启用"), OFF(0, "禁用");
        public final int code;
        public final String label;

        Status(int code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    @Test
    void scenarioA_sourceIsEnum_inferredFromFieldType() {
        EnumTransRepository repo = new EnumTransRepository();
        TransContext ctx = new DefaultTransContext("sex",
                Map.of("enumClass", Void.class, "code", "", "label", "label"), Sex.class);
        Map<Object, Object> map = repo.getTransValueMap(List.of(Sex.MALE, Sex.FEMALE), ctx);
        assertEquals(2, map.size());
        assertSame(Sex.MALE, map.get(Sex.MALE));
        assertSame(Sex.FEMALE, map.get(Sex.FEMALE));
    }

    @Test
    void scenarioB_sourceIsCode_explicitEnumClass() {
        EnumTransRepository repo = new EnumTransRepository();
        TransContext ctx = new DefaultTransContext("status",
                Map.of("enumClass", Status.class, "code", "code", "label", "label"), null);
        Map<Object, Object> map = repo.getTransValueMap(List.of(1, 0), ctx);
        assertEquals(2, map.size());
        assertSame(Status.ON, map.get(1));
        assertSame(Status.OFF, map.get(0));
    }

    @Test
    void scenarioB_byName() {
        EnumTransRepository repo = new EnumTransRepository();
        TransContext ctx = new DefaultTransContext("sex",
                Map.of("enumClass", Sex.class, "code", "", "label", "label"), null);
        Map<Object, Object> map = repo.getTransValueMap(List.of("MALE"), ctx);
        assertEquals(1, map.size());
        assertSame(Sex.MALE, map.get("MALE"));
    }

    @Test
    void unknownValue_notMapped() {
        EnumTransRepository repo = new EnumTransRepository();
        TransContext ctx = new DefaultTransContext("sex",
                Map.of("enumClass", Sex.class, "code", "", "label", "label"), null);
        Map<Object, Object> map = repo.getTransValueMap(List.of("MALE", "XXX"), ctx);
        assertEquals(1, map.size());
        assertSame(Sex.MALE, map.get("MALE"));
    }
}
