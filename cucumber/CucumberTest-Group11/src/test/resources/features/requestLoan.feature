Feature: Request Loan
  As a ParaBank customer
  I want to apply for a loan online
  So that I can borrow money when I need it

  Scenario: Loan is approved when I can afford the down payment
    Given I am logged in as a registered customer
    When I request a loan of $100 with a down payment of $50 from my first account
    Then the loan status should be "Approved"
    And I should be given a new loan account number

  Scenario Outline: Loan is denied when ___
    Given ___
    When ___ "<loanAmount>" ___ "<downPayment>" ___
    Then ___
    And ___

    Examples:
      | loanAmount | downPayment |
      | ___        | ___         |
      | ___        | ___         |
