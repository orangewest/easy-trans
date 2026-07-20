package io.github.orangewest.easytrans.demo;

import io.github.orangewest.trans.repository.dict.DictLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DemoConfig {

    /**
     * 字典加载器：供 {@code @DictTransRepo} 使用。
     */
    @Bean
    public DictLoader dictLoader() {
        return group -> {
            Map<String, String> dict = new HashMap<>();
            if ("sex".equals(group)) {
                dict.put("1", "男");
                dict.put("2", "女");
            }
            return Collections.unmodifiableMap(dict);
        };
    }
}
