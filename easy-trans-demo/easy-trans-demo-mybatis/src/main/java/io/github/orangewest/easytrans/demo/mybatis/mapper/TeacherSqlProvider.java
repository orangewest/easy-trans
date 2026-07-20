package io.github.orangewest.easytrans.demo.mybatis.mapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 动态 SQL 提供器：根据传入的 id 列表拼出 {@code IN (...)} 查询。使用 {@code SELECT *} 以适配实体继承的字段。
 */
public class TeacherSqlProvider {

    public String selectBatchIds(Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Serializable> ids = (List<Serializable>) params.get("ids");
        StringBuilder sb = new StringBuilder("SELECT * FROM teacher WHERE id IN (");
        for (int i = 0; i < ids.size(); i++) {
            sb.append("#{ids[").append(i).append("]}");
            if (i < ids.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
