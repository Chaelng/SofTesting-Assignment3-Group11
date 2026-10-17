Feature: Paying Bills from a Customer Account
  As a ParaBank customer
  I want to pay bills to a payee from my account
  So that I pay my bills online

  Background:
    Given I am logged in as a registered customer

  Scenario: Pay a bill to a valid payee
    When I pay a bill of $100 to a valid payee from my first account
    Then I should see that the bill payment was successful
    And my first account balance should decrease by $100

  Scenario Outline: Pay the minimum amount and the full available balance
    When I pay a bill of $<amount> to a valid payee from my first account
    Then I should see that the bill payment was successful
    And my first account balance should decrease by $<amount>

    Examples:
      | amount |
      | 0.01   |
      | 0.02   |
      | 515.49 |
      | 515.50 |

  @defect
  Scenario Outline: Reject a bill payment that exceeds the available balance
    When I pay a bill of $<amount> to a valid payee from my first account
    Then the bill payment should not go through
    And I should see an error message
    And my account balances should not change

    Examples:
      | amount  |
      | 515.51  |
      | 1000000 |

  @defect
  Scenario: Reject a bill payment of zero
    When I pay a bill of $0.00 to a valid payee from my first account
    Then the bill payment should not go through
    And I should see an error message
    And my account balances should not change

  @defect
  Scenario: Reject a negative bill payment amount
    When I pay a bill of $-100 to a valid payee from my first account
    Then the bill payment should not go through
    And I should see an error message
    And my account balances should not change

  @defect
  Scenario: Reject a bill payment amount with more than two decimal places
    When I pay a bill of $100.001 to a valid payee from my first account
    Then the bill payment should not go through
    And I should see an error message
    And my account balances should not change

  Scenario Outline: Reject a bill payment when a required payee field is empty
    When I pay a bill of $100 to a valid payee but leave the <field> empty
    Then I should see the message "<message>"
    And my account balances should not change

    Examples:
      | field                 | message                     |
      | payee name            | Payee name is required.     |
      | address               | Address is required.        |
      | city                  | City is required.           |
      | state                 | State is required.          |
      | zip code              | Zip Code is required.       |
      | phone number          | Phone number is required.   |
      | account number        | Account number is required. |
      | verify account number | Account number is required. |

  Scenario Outline: Reject a bill payment amount with an invalid input format
    When I try to pay a bill of "<amount>" to a valid payee from my first account
    Then I should see the message "<message>"
    And my account balances should not change

    Examples:
      | amount | message                      |
      |        | The amount cannot be empty.  |
      | abc    | Please enter a valid amount. |
      | $50    | Please enter a valid amount. |

  Scenario: Reject a bill payment amount written with a thousands separator
    When I try to pay a bill of "1,000" to a valid payee from my first account
    Then I should see an error message
    And my account balances should not change

  Scenario: Reject a bill payment when the account number confirmation does not match
    When I pay a bill of $100 to a payee but enter a different account number to verify
    Then I should see the message "The account numbers do not match."
    And my account balances should not change
    And no new transaction should appear in my account history

  Scenario: Reject a payee account number containing non-numeric characters
    When I pay a bill of $100 to a payee with the account number "1111a"
    Then I should see an error message
    And my account balances should not change
