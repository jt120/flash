package com.jt.flash.proxy.dao;

import com.jt.flash.proxy.bean.Config;
import com.jt.flash.proxy.util.C;
import com.jt.flash.proxy.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * since 2016/6/25.
 */
@Repository
public class ConfigDao {

    private static final Logger log = LoggerFactory.getLogger(ConfigDao.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    public int version() {
        int version = 0;
        try {
            version = jdbcTemplate.queryForObject("select max(conf_version) mc from t_conf",
                    new RowMapper<Integer>() {
                         
                        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return rs.getInt("mc");
                        }
                    });
        } catch (DataAccessException e) {
            log.warn("query fail", e);
        }
        return version;
    }

    public void insertConfig(Config config, int version) {
        jdbcTemplate.update(
                "insert into t_conf (conf_name, conf_value, conf_version) values (?, ?, ?)",
                C.app_name,
                JsonUtil.writeValueAsString(config),
                version);
    }

    public Config loadLastest() {
        try {
            Config conf = jdbcTemplate.queryForObject("select (conf_value) from t_conf where conf_version=(select max(conf_version) from t_conf)",
                    new RowMapper<Config>() {
                         
                        public Config mapRow(ResultSet rs, int rowNum) throws SQLException {
                            Config config = JsonUtil.readValue(rs.getString("conf_value"), Config.class);
                            return config;
                        }
                    });
            return conf;
        } catch (DataAccessException e) {
            log.warn("query fail", e);
        }
        return null;
    }
}
