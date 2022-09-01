package com.db.awmd.challenge.domain;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class Payment {

    @NotBlank
    private String from;

    @NotBlank
    private String to;

    @NotNull
    @Min(value = 1, message = "Amount must be positive")
    private BigDecimal amount;

}
