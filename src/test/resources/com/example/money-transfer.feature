Feature: Money transfer

  Background:
    Given Account "Main" exists and has balance "0.00"
    And Account "Secondary" exists and has balance "0.00"

  Scenario: Transferring money
    Given The "Main" account balance is "1,000.00"
    And The "Secondary" account balance is "2,000.00"
    When I transfer "500.00" from "Main" account to "Secondary" account
    Then The "Main" account balance should be "500.00"
    And The "Secondary" account balance should be "2,500.00"

  Scenario: Failing to transfer due to insufficient balance
    Given The "Main" account balance is "500.00"
    When I try to transfer "750.00" from "Main" account to "Secondary" account
    Then The transfer is cancelled with error "Insufficient balance in source account"
