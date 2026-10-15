Feature: Transfer Funds
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

  Scenario Outline: Transfer succeeds for the smallest amount and for my full available balance
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

  Scenario: Transfer amounts are recorded in whole cents
    When I transfer $10.555 from my first account to my second account
    Then I should see that the transfer is complete
    And the confirmation should show a transferred amount of "$10.55"

  @known-issue
  Scenario Outline: Transferring more than my available balance
    When I transfer $<amount> from my first account to my second account
    Then I should see that the transfer is complete
    And the confirmation should show a transferred amount of "<shown>"

    Examples:
      | amount  | shown       |
      | 415.51  | $415.51     |
      | 1000000 | $1000000.00 |

  @known-issue
  Scenario: Transferring a zero amount
    When I transfer $0.00 from my first account to my second account
    Then I should see that the transfer is complete
    And the confirmation should show a transferred amount of "$0.00"

  @known-issue
  Scenario: Transferring a negative amount
    When I transfer $-100 from my first account to my second account
    Then I should see that the transfer is complete
    And the confirmation should show a transferred amount of "-$100.00"

  Scenario Outline: Transfer does not go through when I enter an invalid amount
    When I try to transfer "<amount>" from my first account to my second account
    Then I should see an error message
    And my account balances should not change

    Examples:
      | amount |
      |        |
      | abc    |
      | $50    |
      | 1,000  |
