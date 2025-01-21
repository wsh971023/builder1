package com.cuizhy.rhc.dao;

import com.cuizhy.rhc.model.Info;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InfoDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 获取所有配置
     */
    public List<Info> getInfo() {
        String sql = "select * from info";
        return jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(Info.class));
    }

    /**
     * 根据name和env 获取配置
     */
    public Info getInfo(String name, String env) {
        String sql = "select * from info where name=:name and env=:env";
        return jdbcTemplate.queryForObject(sql, new java.util.HashMap<String, Object>() {{
            put("name", name);
            put("env", env);
        }}, new BeanPropertyRowMapper<>(Info.class));
    }

    public List<Info> getInfo(String env) {
        String sql = "select * from info where env=:env";
        return jdbcTemplate.query(sql, new java.util.HashMap<String, Object>() {{
            put("env", env);
        }}, new BeanPropertyRowMapper<>(Info.class));
    }


}
