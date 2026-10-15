Feature: Requesting a Loan Online
  As a ParaBank customer
  I want to apply for a loan online
  So that I can borrow money when I need it

  Background:
    Given I am logged in as a registered customer

  Scenario: Approve a loan when I can afford the down payment
    When I request a loan of $100 with a down payment of $50 from my first account
    Then the loan status should be "Approved"
    And I should see the message "Congratulations, your loan has been approved."
    And I should be given a new loan account number
    And my first account balance should decrease by $50

  Scenario Outline: Approve a loan when the down payment does not exceed my available balance
    When I request a loan of $100 with a down payment of $<downPayment> from my first account
    Then the loan status should be "Approved"
    And I should see the message "Congratulations, your loan has been approved."
    And I should be given a new loan account number
    And my first account balance should decrease by $<downPayment>

    Examples:
      | downPayment |
      | 0.00        |
      | 0.01        |
      | 250         |
      | 515.49      |
      | 515.50      |

  Scenario Outline: Approve loans of different sizes
    When I request a loan of $<loanAmount> with a down payment of $<downPayment> from my first account
    Then the loan status should be "Approved"
    And I should be given a new loan account number

    Examples:
      | loanAmount | downPayment |
      | 0.01       | 0.00        |
      | 1000       | 50          |
      | 2577.50    | 50          |

  Scenario Outline: Deny a loan amount above five times my available balance
    When I request a loan of $<loanAmount> with a down payment of $50 from my first account
    Then the loan status should be "Denied"
    And I should see the message "We cannot grant a loan in that amount with your available funds."
    And I should not be given a new loan account number
    And my account balances should not change

    Examples:
      | loanAmount |
      | 2600       |
      | 5000       |

  Scenario Outline: Deny a loan when I cannot afford the down payment
    When I request a loan of $100 with a down payment of $<downPayment> from my first account
    Then the loan status should be "Denied"
    And I should see the message "You do not have sufficient funds for the given down payment."
    And I should not be given a new loan account number
    And my account balances should not change
    And no new transaction should appear in my account history

    Examples:
      | downPayment |
      | 515.51      |
      | 1000        |

  Scenario: Deny a loan amount that is too large for my available funds
    When I request a loan of $100000 with a down payment of $50 from my first account
    Then the loan status should be "Denied"
    And I should see the message "We cannot grant a loan in that amount with your available funds."
    And I should not be given a new loan account number
    And my account balances should not change

  Scenario: Deny a loan when both the loan amount and the down payment are too large
    When I request a loan of $100000 with a down payment of $515.51 from my first account
    Then the loan status should be "Denied"
    And I should see the message "You do not have sufficient funds for the given down payment."
    And I should not be given a new loan account number
    And my account balances should not change

  Scenario: Deny a negative loan amount
    When I request a loan of $-100 with a down payment of $50 from my first account
    Then the loan status should be "Denied"
    And I should see the message "We cannot grant a loan in that amount with your available funds."
    And I should not be given a new loan account number
    And my account balances should not change

  Scenario: Reject a loan amount of zero
    When I request a loan of $0 with a down payment of $50 from my first account
    Then the loan should not be approved
    And I should not be given a new loan account number
    And my account balances should not change

  @defect
  Scenario: Reject a negative down payment
    When I request a loan of $100 with a down payment of $-0.01 from my first account
    Then the loan should not be approved
    And I should not be given a new loan account number
    And my account balances should not change

  @defect
  Scenario: Reject a loan amount with more than two decimal places
    When I request a loan of $100.001 with a down payment of $50 from my first account
    Then the loan should not be approved
    And I should not be given a new loan account number
    And my account balances should not change

  Scenario Outline: Reject a loan request with an invalid input format
    When I try to request a loan of "<loanAmount>" with a down payment of "<downPayment>" from my first account
    Then I should see an error message
    And I should not be given a new loan account number
    And my account balances should not change

    Examples:
      | loanAmount | downPayment |
      | abc        | 50          |
      | 100        | abc         |
      |            | 50          |
      | 100        |             |
