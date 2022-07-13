package com.example.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "ACCOUNT")
@Data
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ACCOUNT_ID_SEQUENCE_GENERATOR")
    @SequenceGenerator(name = "ACCOUNT_ID_SEQUENCE_GENERATOR", sequenceName = "ACCOUNT_ID_SEQUENCE", allocationSize = 1)
    @Column(name = "ACCOUNT_ID")
    private Long id;

    @Column(name = "ACCOUNT_NAME")
    private String name;

    @Column(name = "ACCOUNT_BALANCE")
    private BigDecimal balance;
}
