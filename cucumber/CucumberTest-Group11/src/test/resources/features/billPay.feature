Feature: Bill Pay
  As a ParaBank customer
  I want to pay bills to a payee from my account
  So that I can settle what I owe without visiting a branch

  Scenario: Successful bill payment
    Given I am logged in as a registered customer
    When I pay a bill of $250 to a valid payee from my first account
    Then I should see that the bill payment was successful
    And my first account balance should decrease by $250

  Scenario Outline: Bill payment is rejected when the payment details are invalid
    Given ___
    When ___ "<problem>"
    Then ___ "<message>"
    And ___

    Examples:
      | problem | message |
      | ___     | ___     |
      | ___     | ___     |
      | ___     | ___     |
