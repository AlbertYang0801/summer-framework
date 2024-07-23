package com.albert.summer.jdbc.with.tx;

import com.albert.summer.annotation.Autowired;
import com.albert.summer.annotation.Component;
import com.albert.summer.annotation.Transactional;
import com.albert.summer.jdbc.JdbcTemplate;
import com.albert.summer.jdbc.JdbcTestBase;

import java.util.List;


@Component
@Transactional
public class AddressService {

    @Autowired
    UserService userService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    public void addAddress(Address... addresses) {
        for (Address address : addresses) {
            // check if userId is exist:
            User user = userService.getUser(address.userId);
            //if (user != null) {
            //    throw new RuntimeException("user already exists");
            //}
            jdbcTemplate.update(JdbcTestBase.INSERT_ADDRESS, address.userId, address.address, address.zip);
        }
    }

    public List<Address> getAddresses(int userId) {
        return jdbcTemplate.queryForList(JdbcTestBase.SELECT_ADDRESS_BY_USERID, Address.class, userId);
    }

    public void deleteAddress(int userId) {
        jdbcTemplate.update(JdbcTestBase.DELETE_ADDRESS_BY_USERID, userId);
        if (userId == 1) {
            throw new RuntimeException("Rollback delete for user id = 1");
        }
    }


}
