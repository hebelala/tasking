/**
 * Copyright © 2019 hebelala (hebelala@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.hebelala.tasking.demo.tasks;

import com.github.hebelala.tasking.api.Request;
import com.github.hebelala.tasking.api.Response;
import com.github.hebelala.tasking.api.Task;
import com.github.hebelala.tasking.demo.entity.Customer;
import com.github.hebelala.tasking.demo.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author hebelala
 */
@Component
public class DemoTask implements Task {

    private final Logger logger = LoggerFactory.getLogger(getClass());

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
        logger.info("request is {} ", request);

        customerRepository
                .findAll()
                .forEach((val) -> logger.info("id:name is ", val.getId(), val.getName()));
        return new Response();
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
