Feature: Transfer Funds
  As a ParaBank customer
  I want to transfer money between my own accounts
  So that I can manage my funds

  Scenario: Successful transfer between my own accounts
    Given I am logged in as a registered customer
    And I have a second account
    When I transfer $50 from my first account to my second account
    Then I should see that the transfer is complete
    And my first account balance should decrease by $50
    And my second account balance should increase by $50


  Scenario Outline: Transfer is not completed when the amount is invalid
    Given ___
    And ___
    When ___ "<amount>" ___
    Then ___
    And ___

    Examples:
      | amount |
      | ___    |
      | ___    |
