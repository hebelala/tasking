package com.github.hebelala.tasking.demo.repository;

import com.github.hebelala.tasking.demo.entity.Customer;
import org.springframework.data.repository.CrudRepository;

/**
 * @author hebelala
 */
public interface CustomerRepository extends CrudRepository<Customer, Long> {

}
