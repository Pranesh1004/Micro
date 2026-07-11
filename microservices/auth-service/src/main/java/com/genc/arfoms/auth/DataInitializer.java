package com.genc.arfoms.auth;

import com.genc.arfoms.auth.model.User;
import com.genc.arfoms.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing database with default users...");

        upsertUser("admin", "Admin@123", "Administrator", "Admin");
        upsertUser("scheduler", "Scheduler@123", "Flight Scheduler", "Flight Scheduler");
        upsertUser("agent", "Agent@123", "Reservation Agent", "Reservation Agent");
        upsertUser("crew", "Crew@123", "Crew Manager", "Crew Manager");
        upsertUser("loyalty", "Loyalty@123", "Loyalty Manager", "Loyalty Manager");
        upsertUser("groundstaff", "Ground@123", "Ground Staff", "Ground Staff");

        logger.info("Database initialization completed.");
    }

    private void upsertUser(String username, String password, String name, String role) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setPassword(password);
            user.setName(name);
            user.setRole(role);
            userRepository.save(user);
            logger.info("User '{}' updated successfully with role '{}'.", username, role);
            return;
        }

        User user = new User(username, password, name, role);
        userRepository.save(user);
        logger.info("User '{}' created successfully with role '{}'.", username, role);
    }
}
