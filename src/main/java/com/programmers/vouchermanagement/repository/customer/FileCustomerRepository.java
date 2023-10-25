package com.programmers.vouchermanagement.repository.customer;

import com.programmers.vouchermanagement.common.ErrorMessage;
import com.programmers.vouchermanagement.domain.customer.Customer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("local")
public class FileCustomerRepository implements CustomerRepository {
    private static final String CSV_SEPARATOR = ",";

    private final String customerFilePath;
    private final ConcurrentHashMap<UUID, Customer> customers = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(FileCustomerRepository.class);

    public FileCustomerRepository(@Value("${csv.file.customer.path}") String customerFilePath) {
        this.customerFilePath = customerFilePath;
    }

    @PostConstruct
    public void init() {
        readCustomerFile();
    }

    @Override
    public List<Customer> findAll() {
        return customers.values().stream().toList();
    }

    @Override
    public Optional<Customer> findById(UUID customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }

    @Override
    public List<Customer> findByName(String name) {
        return customers.values().stream()
                .filter(customer -> customer.getName().equals(name))
                .toList();
    }

    @Override
    public List<Customer> findBannedCustomers() {
        return customers.values().stream()
                .filter(Customer::isBanned)
                .toList();
    }

    @Override
    public void save(Customer customer) {
        if (isCustomerPresent(customer.getId())) {
            throw new IllegalArgumentException("Customer already exists");
        }
        customers.put(customer.getId(), customer);
        updateFile();
    }

    @Override
    public void update(Customer customer) {
        if (!isCustomerPresent(customer.getId())) {
            throw new NoSuchElementException();
        }
        customers.put(customer.getId(), customer);
        updateFile();
    }

    @Override
    public void delete(UUID id) {
        if (!isCustomerPresent(id)) {
            throw new NoSuchElementException();
        }
        customers.remove(id);
        updateFile();
    }

    private void readCustomerFile() {
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(customerFilePath))) {
            while ((line = br.readLine()) != null) {
                String[] strings = line.split(CSV_SEPARATOR);
                Customer customer = new Customer(
                        UUID.fromString(strings[0]),
                        strings[1],
                        LocalDateTime.parse(strings[2]),
                        Boolean.parseBoolean(strings[3])
                );
                customers.put(customer.getId(), customer);
            }
        } catch (FileNotFoundException e) {
            logger.warn("{} : {}", ErrorMessage.FILE_NOT_FOUND_MESSAGE.getMessage(), customerFilePath);
        } catch (IOException e) {
            logger.error("Error occurred at FileReader: ", e);
        }
    }

    private void updateFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(customerFilePath))) {
            customers.values().stream()
                    .map(customer -> customer.joinInfo(CSV_SEPARATOR))
                    .forEach(line -> {
                        try {
                            bw.write(line);
                            bw.newLine();
                        } catch (IOException e) {
                            logger.error("Error occurred at FileWriter: ", e);
                        }
                    });
        } catch (IOException e) {
            logger.error("Error occurred af FileWriter: ", e);
        }
    }

    private boolean isCustomerPresent(UUID customerId) {
        return customers.containsKey(customerId);
    }
}
