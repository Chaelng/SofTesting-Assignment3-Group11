# Temporary: confirms Maven + Cucumber + ChromeDriver are wired up.
# Delete this file (and SetupCheckSteps.java) once your real features run.
Feature: Setup check
  Scenario: ParaBank home page opens in Chrome
    Given I open the ParaBank home page
    Then the page title should contain "ParaBank"
