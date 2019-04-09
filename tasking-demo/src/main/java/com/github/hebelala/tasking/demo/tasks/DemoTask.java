package com.github.hebelala.tasking.demo.tasks;

import com.github.hebelala.tasking.api.Request;
import com.github.hebelala.tasking.api.Response;
import com.github.hebelala.tasking.api.Task;
import com.github.hebelala.tasking.api.trigger.Trigger;
import com.github.hebelala.tasking.demo.entity.Customer;
import com.github.hebelala.tasking.demo.repository.CustomerRepository;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author hebelala
 */
@Component
public class DemoTask implements Task {

  private Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private CustomerRepository customerRepository;

  @PostConstruct
  public void init() {
    Customer customer = new Customer();
    customer.setName("hebe");
    Customer save = customerRepository.save(customer);
    logger.info("Saved: {}:{}", save.getId(), save.getName());
  }

  @Override
  public Response run(Request request) {
    Trigger trigger = request.getTrigger();
    logger.info("Trigger class is {} ", trigger.getClass().getName());

    customerRepository.findAll()
        .forEach((val) -> logger.info("id:name is ", val.getId(), val.getName()));
    return new Response();
  }

}
