package com.genc.arfoms.booking.service;

import com.genc.arfoms.booking.model.Passenger;
import com.genc.arfoms.booking.repository.PassengerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PassengerService {

    @Autowired
    private PassengerRepository passengerRepository;

    public Passenger getPassenger() {
        List<Passenger> all = passengerRepository.findAll();
        return all.isEmpty() ? null : all.get(0);
    }
}

