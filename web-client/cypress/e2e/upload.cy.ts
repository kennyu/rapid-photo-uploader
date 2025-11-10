describe('Photo Upload', () => {
  const email = `upload-test-${Date.now()}@example.com`;
  const password = 'TestPassword123!';

  beforeEach(() => {
    // Create and login user
    cy.register(email, password, 'Upload Test User');
    cy.visit('/upload');
  });

  it('should display upload page correctly', () => {
    cy.contains('Upload Photos').should('be.visible');
    cy.contains('Select Photos').should('be.visible');
  });

  it('should allow file selection via button', () => {
    // Create a fake file
    const fileName = 'test-photo.jpg';
    const fileContent = 'fake-image-content';
    
    cy.get('input[type="file"]').should('exist');
    
    // Simulate file selection
    cy.get('input[type="file"]').selectFile(
      {
        contents: Cypress.Buffer.from(fileContent),
        fileName: fileName,
        mimeType: 'image/jpeg',
      },
      { force: true }
    );

    // Should show file in the list (or trigger upload)
    cy.contains(fileName, { timeout: 5000 }).should('be.visible');
  });

  it('should upload multiple files', () => {
    const files = [
      {
        contents: Cypress.Buffer.from('photo1'),
        fileName: 'photo1.jpg',
        mimeType: 'image/jpeg',
      },
      {
        contents: Cypress.Buffer.from('photo2'),
        fileName: 'photo2.jpg',
        mimeType: 'image/jpeg',
      },
      {
        contents: Cypress.Buffer.from('photo3'),
        fileName: 'photo3.jpg',
        mimeType: 'image/jpeg',
      },
    ];

    cy.get('input[type="file"]').selectFile(files, { force: true });

    // Should show all files
    files.forEach((file) => {
      cy.contains(file.fileName, { timeout: 5000 }).should('be.visible');
    });
  });

  it('should show upload progress', () => {
    const fileName = 'progress-test.jpg';
    
    cy.intercept('POST', '**/photos/upload/initiate', (req) => {
      req.reply({
        statusCode: 200,
        body: {
          photoId: 'test-photo-id',
          uploadJobId: 'test-job-id',
          preSignedUrl: 'https://example.com/upload',
        },
        delay: 500, // Simulate network delay
      });
    }).as('initiateUpload');

    cy.get('input[type="file"]').selectFile(
      {
        contents: Cypress.Buffer.from('test-content'),
        fileName: fileName,
        mimeType: 'image/jpeg',
      },
      { force: true }
    );

    // Wait for upload initiation
    cy.wait('@initiateUpload');

    // Should show some kind of progress indicator
    // This could be a progress bar, percentage, or status text
    cy.get('[data-testid="upload-progress"]', { timeout: 10000 })
      .should('exist')
      .or(() => {
        cy.contains(/uploading|progress|%/i).should('be.visible');
      });
  });

  it('should handle upload errors gracefully', () => {
    cy.intercept('POST', '**/photos/upload/initiate', {
      statusCode: 500,
      body: { error: 'Server error' },
    }).as('failedUpload');

    cy.get('input[type="file"]').selectFile(
      {
        contents: Cypress.Buffer.from('test'),
        fileName: 'error-test.jpg',
        mimeType: 'image/jpeg',
      },
      { force: true }
    );

    cy.wait('@failedUpload');

    // Should show error message
    cy.contains(/error|failed/i, { timeout: 5000 }).should('be.visible');
  });

  it('should validate file types', () => {
    // Try to upload a non-image file
    cy.get('input[type="file"]').selectFile(
      {
        contents: Cypress.Buffer.from('not an image'),
        fileName: 'document.pdf',
        mimeType: 'application/pdf',
      },
      { force: true }
    );

    // Should either reject the file or show an error
    cy.contains(/invalid|not supported|image only/i, { timeout: 5000 })
      .should('be.visible')
      .or(() => {
        // Or the file input might simply not accept it
        cy.contains('document.pdf').should('not.exist');
      });
  });

  it('should clear completed uploads', () => {
    const fileName = 'clear-test.jpg';

    // Mock successful upload
    cy.intercept('POST', '**/photos/upload/initiate', {
      statusCode: 200,
      body: {
        photoId: 'test-id',
        uploadJobId: 'test-job',
        preSignedUrl: 'https://example.com/upload',
      },
    });

    cy.intercept('PUT', 'https://example.com/upload', {
      statusCode: 200,
    });

    cy.get('input[type="file"]').selectFile(
      {
        contents: Cypress.Buffer.from('test'),
        fileName: fileName,
        mimeType: 'image/jpeg',
      },
      { force: true }
    );

    // Wait for upload to complete
    cy.contains('complete', { timeout: 10000, matchCase: false });

    // Look for a clear/remove button
    cy.contains('button', /clear|remove|done/i).click();

    // File should be removed from list
    cy.contains(fileName).should('not.exist');
  });
});

