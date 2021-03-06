package cn.howardliu.gear.spring.boot.autoconfigure;

import com.alibaba.druid.pool.DruidDataSource;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static cn.howardliu.gear.spring.boot.autoconfigure.MybatisDruidProperties.MYBATIS_DRUID_PREFIX;

/**
 * <p>
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-Configuration} for Mybatis integrate with Druid.
 * <p>
 * Contributes:
 * <ul>
 * <li>{@link DruidDataSource}</li>
 * <li>{@link DataSourceTransactionManager}</li>
 * </ul>
 * <p>
 * If {@link org.mybatis.spring.annotation.MapperScan} is used, or a
 * configuration file is specified as a property, those will be considered,
 * otherwise this auto-configuration will attempt to register mappers based on
 * the interface definitions in or under the root auto-configuration package.
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass({DataSource.class, DruidDataSource.class, DataSourceTransactionManager.class})
@ConditionalOnProperty(prefix = MYBATIS_DRUID_PREFIX, name = {"jdbcUrl", "username", "password"})
@EnableConfigurationProperties(MybatisDruidProperties.class)
@AutoConfigureBefore({MybatisAutoConfiguration.class, DataSourceAutoConfiguration.class})
public class MyBatisDruidAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(MyBatisDruidAutoConfiguration.class);
    private final MybatisDruidProperties properties;

    public MyBatisDruidAutoConfiguration(MybatisDruidProperties properties) {
        this.properties = properties;
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    @ConditionalOnMissingBean(DataSource.class)
    public DruidDataSource dataSource() throws SQLException {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(this.properties.getDriverClassName());
        dataSource.setUrl(this.properties.getJdbcUrl());
        dataSource.setUsername(this.properties.getUsername());
        dataSource.setPassword(this.properties.getPassword());
        dataSource.setInitialSize(this.properties.getInitialPoolSize());
        dataSource.setMinIdle(this.properties.getMinPoolSize());
        dataSource.setMaxActive(this.properties.getMaxPoolWait());
        dataSource.setMaxWait(60000);
        dataSource.setTimeBetweenEvictionRunsMillis(6000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setValidationQuery("SELECT 'x'");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);
        dataSource.setFilters("stat");
        dataSource.setConnectionInitSqls(connectionInitSqls());
        return dataSource;
    }

    private Collection<String> connectionInitSqls() {
        Set<String> connectionInitSqls = new HashSet<>(1);
        if ("utf8mb4".equals(this.properties.getDefaultCharacter())) {
            connectionInitSqls.add("set names utf8mb4;");
        }
        return connectionInitSqls;
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) throws SQLException {
        return new DataSourceTransactionManager(dataSource);
    }

    @Configuration
    @ConditionalOnMissingClass("com.alibaba.druid.pool.DruidDataSource")
    public static class DruidDataSourceNotFoundConfiguration {
        @PostConstruct
        public void afterPropertiesSet() {
            logger.debug("No {} found.", DruidDataSource.class.getName());
        }
    }

    @Configuration
    @ConditionalOnMissingBean(DruidDataSource.class)
    public static class DruidDataSourceNotCreateConfiguration {
        @PostConstruct
        public void afterPropertiesSet() {
            logger.debug("No {} bean created.", DruidDataSource.class.getName());
        }
    }
}
