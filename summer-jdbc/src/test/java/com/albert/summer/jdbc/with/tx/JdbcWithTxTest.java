package com.albert.summer.jdbc.with.tx;

import com.albert.summer.context.AnnotationConfigApplicationContext;
import com.albert.summer.context.ConfigurableApplicationContext;
import com.albert.summer.exception.TransactionException;
import com.albert.summer.jdbc.JdbcTemplate;
import com.albert.summer.jdbc.JdbcTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 有事务操作JDBC
 * @author yjw
 * @date 2024/7/23 21:37
 */
public class JdbcWithTxTest extends JdbcTestBase {


    @Test
    public void testJdbcWithTx() throws Exception {
        try (ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(JdbcWithTxApplication.class, createPropertyResolver())) {
            JdbcTemplate jdbcTemplate = ctx.getBean(JdbcTemplate.class);
            jdbcTemplate.update(CREATE_USER);
            jdbcTemplate.update(CREATE_ADDRESS);

            UserService userService = ctx.getBean(UserService.class);
            AddressService addressService = ctx.getBean(AddressService.class);
            // 开始事务后，是被AOP代理后的类
            assertNotSame(UserService.class, userService.getClass());
            assertNotSame(AddressService.class, addressService.getClass());
            // proxy object is not inject:
            //直接调用代理对象的方法，为空。因为注入的是原始对象
            assertNull(userService.addressService);
            assertNull(addressService.userService);

            // insert user:
            User bob = userService.createUser("Bob", 12);
            assertEquals(1, bob.id);

            // insert addresses:
            Address addr1 = new Address(bob.id, "Broadway, New York", 10012);
            Address addr2 = new Address(bob.id, "Fifth Avenue, New York", 10080);
            // NOTE user not exist for addr3:
            Address addr3 = new Address(bob.id + 1, "Ocean Drive, Miami, Florida", 33411);

            //add3没有对应的userId，所以会触发DataAccessException异常，然后触发回滚
            assertThrows(TransactionException.class, () -> {
                addressService.addAddress(addr1, addr2, addr3);
            });

            // ALL address should not inserted:
            assertTrue(addressService.getAddresses(bob.id).isEmpty());

            // insert addr1, addr2 for Bob only:
            addressService.addAddress(addr1, addr2);
            assertEquals(2, addressService.getAddresses(bob.id).size());

            // now delete bob will cause rollback:
            assertThrows(TransactionException.class, () -> {
                //内部抛出异常，触发回滚
                userService.deleteUser(bob);
            });

            // bob and his addresses still exist:
            assertEquals("Bob", userService.getUser(1).name);
            assertEquals(2, addressService.getAddresses(bob.id).size());
        }
        // re-open db and query:
        try (ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(JdbcWithTxApplication.class, createPropertyResolver())) {
            AddressService addressService = ctx.getBean(AddressService.class);
            List<Address> addressesOfBob = addressService.getAddresses(1);
            //两条数据
            assertEquals(2, addressesOfBob.size());
        }
    }
}

}
