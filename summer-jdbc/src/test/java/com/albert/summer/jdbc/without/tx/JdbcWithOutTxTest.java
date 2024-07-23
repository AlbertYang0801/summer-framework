package com.albert.summer.jdbc.without.tx;

import com.albert.summer.context.AnnotationConfigApplicationContext;
import com.albert.summer.context.ConfigurableApplicationContext;
import com.albert.summer.exception.DataAccessException;
import com.albert.summer.jdbc.JdbcTemplate;
import com.albert.summer.jdbc.JdbcTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 无事务操作JDBC
 * @author yjw
 * @date 2024/7/23 21:23
 */
public class JdbcWithOutTxTest extends JdbcTestBase {

    @Test
    public void testJdbcWithOutTx() throws Exception {
        try (ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(JdbcWithoutTxApplication.class, createPropertyResolver())) {
            JdbcTemplate jdbcTemplate = ctx.getBean(JdbcTemplate.class);
            jdbcTemplate.update(CREATE_USER);
            jdbcTemplate.update(CREATE_ADDRESS);

            int userId1 = jdbcTemplate.updateAndReturnGeneratedKey(INSERT_USER, "albert", "298").intValue();
            int userId2 = jdbcTemplate.updateAndReturnGeneratedKey(INSERT_USER, "syy", null).intValue();
            assertEquals(userId1, 1);
            assertEquals(userId2, 2);

            // query user:
            User albert = jdbcTemplate.queryForObject(SELECT_USER, User.class, userId1);
            User syy = jdbcTemplate.queryForObject(SELECT_USER, User.class, userId2);
            assertEquals(1, albert.id);
            assertEquals("albert", albert.name);
            assertEquals(298, albert.theAge);
            assertEquals(2, syy.id);
            assertEquals("syy", syy.name);
            assertNull(syy.theAge);

            //query name age
            String username1 = jdbcTemplate.queryForObject(SELECT_USER_NAME, String.class, userId1);
            String age1 = jdbcTemplate.queryForObject(SELECT_USER_AGE, String.class, userId1);
            assertEquals(username1, "albert");
            assertEquals(age1, "298");

            //update
            int id1 = jdbcTemplate.update(UPDATE_USER, "yyy", "300", "1");
            assertEquals(id1, 1);
            int id2 = jdbcTemplate.update(DELETE_USER, "2");
            assertEquals(id2, 2);
        }

        //reopen query id=2
        try (ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(JdbcWithoutTxApplication.class, createPropertyResolver())) {
            JdbcTemplate jdbcTemplate = ctx.getBean(JdbcTemplate.class);
            User bob = jdbcTemplate.queryForObject(SELECT_USER, User.class, 1);
            assertEquals("Bob Jones", bob.name);
            assertEquals(18, bob.theAge);
            //拦截DataAccessException异常
            assertThrows(DataAccessException.class, () -> {
                // 查询不到已经删除的结果，抛出DataAccessException
                jdbcTemplate.queryForObject(SELECT_USER, User.class, 2);
            });
        }
    }


}
