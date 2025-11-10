// ***********************************************************
// This file is processed and loaded automatically before test files
// https://on.cypress.io/configuration
// ***********************************************************

import './commands';

// Prevent TypeScript errors for custom commands
declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Custom command to register a new user
       * @example cy.register('test@example.com', 'password123', 'Test User')
       */
      register(email: string, password: string, fullName: string): Chainable<void>;
      
      /**
       * Custom command to login
       * @example cy.login('test@example.com', 'password123')
       */
      login(email: string, password: string): Chainable<void>;
      
      /**
       * Custom command to get auth token from localStorage
       * @example cy.getAuthToken().then((token) => {...})
       */
      getAuthToken(): Chainable<string | null>;
    }
  }
}

