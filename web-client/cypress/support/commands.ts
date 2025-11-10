/// <reference types="cypress" />

// Custom command to register a user
Cypress.Commands.add('register', (email: string, password: string, fullName: string) => {
  cy.request({
    method: 'POST',
    url: `${Cypress.env('apiUrl')}/auth/register`,
    body: {
      email,
      password,
      fullName,
    },
  }).then((response) => {
    expect(response.status).to.eq(200);
    expect(response.body).to.have.property('token');
    
    // Store token in localStorage
    window.localStorage.setItem('authToken', response.body.token);
  });
});

// Custom command to login
Cypress.Commands.add('login', (email: string, password: string) => {
  cy.request({
    method: 'POST',
    url: `${Cypress.env('apiUrl')}/auth/login`,
    body: {
      email,
      password,
    },
  }).then((response) => {
    expect(response.status).to.eq(200);
    expect(response.body).to.have.property('token');
    
    // Store token in localStorage
    window.localStorage.setItem('authToken', response.body.token);
  });
});

// Custom command to get auth token
Cypress.Commands.add('getAuthToken', () => {
  return cy.window().then((win) => {
    return win.localStorage.getItem('authToken');
  });
});

