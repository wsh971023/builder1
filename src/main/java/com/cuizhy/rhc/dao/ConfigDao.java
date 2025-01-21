package com.cuizhy.rhc.dao;

import com.cuizhy.rhc.model.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ConfigDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 获取配置
     */
    public Map<String,Object> getConfig(String type) {
        String sql = "select * from config where type=:type";
        Map<String,Object> params = new HashMap<>();
        params.put("type", type);
        List<Config> configs = jdbcTemplate.query(sql,params,new BeanPropertyRowMapper<>(Config.class));

        //返回 { key:value,key:value}
        return configs.stream().collect(HashMap::new,(m,v)->m.put(v.getKey(),v.getValue()),HashMap::putAll);
    }

    /**
     * 获取单个值
     */
    public String getValue(String key, String type) {
        String sql = "select value from config where type=:type and key=:key";
        return jdbcTemplate.queryForObject(sql, new HashMap<String, Object>() {{
            put("type", type);
            put("key", key);
        }}, String.class);
    }

}
