package com.genc.arfoms.booking.dto;

import lombok.Data;


@Data
public class PassengerDTO {

    private Long id;
    private String fullName;
    private Integer age;
    private String gender;
    private String email;
    private String phone;
}

