Feature: Transferring Money Within a Customer's Accounts
  As a ParaBank customer
  I want to transfer money between my own accounts
  So that I can manage my funds

  Background:
    Given I am logged in as a registered customer
    And I have a second account

  Scenario: Successful transfer between my own accounts
    When I transfer $100 from my first account to my second account
    Then I should see that the transfer is complete
    And my first account balance should decrease by $100
    And my second account balance should increase by $100

  Scenario Outline: Transfer the minimum amount and the full available balance
    When I transfer $<amount> from my first account to my second account
    Then I should see that the transfer is complete
    And my first account balance should decrease by $<amount>
    And my second account balance should increase by $<amount>

    Examples:
      | amount |
      | 0.01   |
      | 0.02   |
      | 415.49 |
      | 415.50 |

  @defect
  Scenario: Reject an amount with more than two decimal places
    When I transfer $100.001 from my first account to my second account
    Then the transfer should not go through
    And I should see an error message
    And my account balances should not change

  @defect
  Scenario Outline: Reject a transfer that exceeds the available balance
    When I transfer $<amount> from my first account to my second account
    Then the transfer should not go through
    And I should see an error message
    And my account balances should not change

    Examples:
      | amount  |
      | 415.51  |
      | 1000000 |

  @defect
  Scenario: Reject a transfer of zero
    When I transfer $0.00 from my first account to my second account
    Then the transfer should not go through
    And I should see an error message
    And my account balances should not change

  @defect
  Scenario: Reject a negative transfer amount
    When I transfer $-100 from my first account to my second account
    Then the transfer should not go through
    And I should see an error message
    And my account balances should not change

  Scenario Outline: Reject an amount with an invalid input format
    When I try to transfer "<amount>" from my first account to my second account
    Then I should see an error message
    And my account balances should not change
    And no new transaction should appear in my account history

    Examples:
      | amount |
      |        |
      | abc    |
      | $50    |
      | 1,000  |
