package io.github.orangewest.trans.service;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * 大列表查询自动分片：按全局 {@code repoBatchSize} 切分交给仓库的键、逐批查、合并结果，
 * 仓库实现对分片无感；默认 0 = 不分片（向后兼容）。
 */
class TransRepoBatchSizeTest {

    /** 记录每次 getTransValueMap 收到的批大小，用于验证切分。 */
    static final List<Integer> BATCH_SIZES = Collections.synchronizedList(new ArrayList<>());

    static class BatchRepo implements TransRepository<Long, String> {
        @Override
        public Map<Long, String> getTransValueMap(List<Long> transValues, TransContext context) {
            BATCH_SIZES.add(transValues.size());
            Map<Long, String> m = new HashMap<>();
            for (Long k : transValues) {
                m.put(k, "n" + k);
            }
            return m;
        }
    }

    static class Item {
        private Long id;
        @Trans(trans = "id", using = BatchRepo.class)
        private String name;

        Item(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }
    }

    @BeforeAll
    static void before() {
        TransRepositoryFactory.register(new BatchRepo());
    }

    @BeforeEach
    void clear() {
        BATCH_SIZES.clear();
    }

    private static List<Item> items(long n) {
        return LongStream.rangeClosed(1, n).mapToObj(Item::new).collect(Collectors.toList());
    }

    private static List<Integer> sortedSizes() {
        List<Integer> sizes = new ArrayList<>(BATCH_SIZES);
        Collections.sort(sizes);
        return sizes;
    }

    @Test
    void chunked_splitsKeysIntoBatches() {
        List<Item> items = items(25);
        TransService service = new TransService();
        service.setRepoBatchSize(10);

        service.trans(items);

        // 25 个键、batchSize=10 → 3 批：10 + 10 + 5（并行查询，顺序无关，按大小比较）
        Assertions.assertEquals(List.of(5, 10, 10), sortedSizes());
        // 结果仍完整正确
        Assertions.assertEquals("n1", items.get(0).getName());
        Assertions.assertEquals("n25", items.get(24).getName());
    }

    @Test
    void parallelDisabled_chunksQueriedSequentiallyInOrder() {
        List<Item> items = items(25);
        TransService service = new TransService();
        service.setRepoBatchSize(10);
        service.setParallelRepoGroups(false); // 关闭并行 → 分片串行、顺序确定

        service.trans(items);

        Assertions.assertEquals(List.of(10, 10, 5), BATCH_SIZES);
        Assertions.assertEquals("n25", items.get(24).getName());
    }

    @Test
    void underBatchSize_singleQuery() {
        List<Item> items = items(25);
        TransService service = new TransService(); // 默认 batchSize=500，25 < 500

        service.trans(items);

        // 键数不超过 batchSize：一次查询拿到全部
        Assertions.assertEquals(List.of(25), BATCH_SIZES);
        Assertions.assertEquals("n13", items.get(12).getName());
    }

    @Test
    void batchSizeZero_disablesSplitting() {
        List<Item> items = items(25);
        TransService service = new TransService();
        service.setRepoBatchSize(0); // 显式关闭分片

        service.trans(items);

        Assertions.assertEquals(List.of(25), BATCH_SIZES);
    }

    @Test
    void exactMultiple_noTrailingEmptyBatch() {
        List<Item> items = items(20);
        TransService service = new TransService();
        service.setRepoBatchSize(10);

        service.trans(items);

        // 整除：两批各 10，不产生尾部空批
        Assertions.assertEquals(List.of(10, 10), sortedSizes());
    }
}
