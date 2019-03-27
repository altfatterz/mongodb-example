package com.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScheduledTasks {

    private CustomerRepository customerRepository;

    public ScheduledTasks(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Scheduled(fixedRate = 200)
    public void reportCurrentTime() {
        customerRepository.save(new Customer("John", "Doe"));
    }
}
