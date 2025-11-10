describe('Photo Gallery', () => {
  const email = `gallery-test-${Date.now()}@example.com`;
  const password = 'TestPassword123!';

  beforeEach(() => {
    // Create and login user
    cy.register(email, password, 'Gallery Test User');
  });

  describe('Gallery Display', () => {
    it('should display empty gallery message when no photos', () => {
      cy.visit('/gallery');

      cy.contains(/no photos|empty|upload some photos/i).should('be.visible');
    });

    it('should display photos in gallery after upload', () => {
      // Mock photo data
      cy.intercept('GET', '**/photos*', {
        statusCode: 200,
        body: {
          content: [
            {
              id: 'photo-1',
              filename: 'test-photo-1.jpg',
              thumbnailUrl: 'https://via.placeholder.com/150',
              downloadUrl: 'https://via.placeholder.com/800',
              status: 'COMPLETE',
              fileSize: 1024000,
              tags: ['vacation', 'beach'],
              createdAt: new Date().toISOString(),
            },
            {
              id: 'photo-2',
              filename: 'test-photo-2.jpg',
              thumbnailUrl: 'https://via.placeholder.com/150',
              downloadUrl: 'https://via.placeholder.com/800',
              status: 'COMPLETE',
              fileSize: 2048000,
              tags: ['family'],
              createdAt: new Date().toISOString(),
            },
          ],
          page: 0,
          size: 20,
          totalElements: 2,
          totalPages: 1,
        },
      }).as('getPhotos');

      cy.visit('/gallery');
      cy.wait('@getPhotos');

      // Should display both photos
      cy.contains('test-photo-1.jpg').should('be.visible');
      cy.contains('test-photo-2.jpg').should('be.visible');
    });

    it('should display photo thumbnails', () => {
      cy.intercept('GET', '**/photos*', {
        body: {
          content: [
            {
              id: 'photo-1',
              filename: 'thumbnail-test.jpg',
              thumbnailUrl: 'https://via.placeholder.com/150',
              downloadUrl: 'https://via.placeholder.com/800',
              status: 'COMPLETE',
              fileSize: 1024000,
              tags: [],
              createdAt: new Date().toISOString(),
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        },
      });

      cy.visit('/gallery');

      // Should display image
      cy.get('img[alt*="thumbnail-test"]').should('be.visible');
    });
  });

  describe('Pagination', () => {
    it('should paginate when there are many photos', () => {
      // Create 25 photos (more than default page size of 20)
      const photos = Array.from({ length: 25 }, (_, i) => ({
        id: `photo-${i}`,
        filename: `photo-${i}.jpg`,
        thumbnailUrl: 'https://via.placeholder.com/150',
        downloadUrl: 'https://via.placeholder.com/800',
        status: 'COMPLETE',
        fileSize: 1024000,
        tags: [],
        createdAt: new Date().toISOString(),
      }));

      cy.intercept('GET', '**/photos?page=0*', {
        body: {
          content: photos.slice(0, 20),
          page: 0,
          size: 20,
          totalElements: 25,
          totalPages: 2,
        },
      });

      cy.visit('/gallery');

      // Should show pagination controls
      cy.contains(/next|page 2|>/i).should('be.visible');
    });
  });

  describe('Tag Management', () => {
    const mockPhoto = {
      id: 'tag-test-photo',
      filename: 'tag-test.jpg',
      thumbnailUrl: 'https://via.placeholder.com/150',
      downloadUrl: 'https://via.placeholder.com/800',
      status: 'COMPLETE',
      fileSize: 1024000,
      tags: ['vacation'],
      createdAt: new Date().toISOString(),
    };

    beforeEach(() => {
      cy.intercept('GET', '**/photos*', {
        body: {
          content: [mockPhoto],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        },
      }).as('getPhotos');

      cy.visit('/gallery');
      cy.wait('@getPhotos');
    });

    it('should display existing tags', () => {
      cy.contains('vacation').should('be.visible');
    });

    it('should open tag edit modal when clicking photo or edit button', () => {
      // Click on photo or edit button
      cy.contains('tag-test.jpg').click()
        .or(() => {
          cy.contains('button', /edit|tags/i).click();
        });

      // Modal should open
      cy.get('[role="dialog"]')
        .or(() => {
          cy.get('.modal');
        })
        .should('be.visible');
    });

    it('should add new tag', () => {
      cy.intercept('POST', '**/photos/*/tags/*', {
        statusCode: 200,
      }).as('addTag');

      // Open modal
      cy.contains('tag-test.jpg').click();

      // Add new tag
      cy.get('input[placeholder*="tag"]').type('beach');
      cy.contains('button', /add|save/i).click();

      cy.wait('@addTag');

      // Should show success message or new tag
      cy.contains('beach').should('be.visible');
    });

    it('should remove tag', () => {
      cy.intercept('DELETE', '**/photos/*/tags/*', {
        statusCode: 200,
      }).as('removeTag');

      // Open modal
      cy.contains('tag-test.jpg').click();

      // Remove existing tag
      cy.contains('vacation').parent().contains('button', /remove|x|delete/i).click();

      cy.wait('@removeTag');

      // Tag should be removed
      cy.contains('vacation').should('not.exist');
    });
  });

  describe('Tag Filtering', () => {
    beforeEach(() => {
      const photos = [
        {
          id: 'photo-1',
          filename: 'vacation-1.jpg',
          thumbnailUrl: 'https://via.placeholder.com/150',
          status: 'COMPLETE',
          tags: ['vacation', 'beach'],
          createdAt: new Date().toISOString(),
        },
        {
          id: 'photo-2',
          filename: 'work-1.jpg',
          thumbnailUrl: 'https://via.placeholder.com/150',
          status: 'COMPLETE',
          tags: ['work'],
          createdAt: new Date().toISOString(),
        },
      ];

      cy.intercept('GET', '**/photos?*', (req) => {
        const url = new URL(req.url);
        const tag = url.searchParams.get('tag');

        if (tag === 'vacation') {
          req.reply({
            body: {
              content: photos.filter((p) => p.tags.includes('vacation')),
              page: 0,
              size: 20,
              totalElements: 1,
            },
          });
        } else {
          req.reply({
            body: {
              content: photos,
              page: 0,
              size: 20,
              totalElements: 2,
            },
          });
        }
      });
    });

    it('should filter photos by tag', () => {
      cy.visit('/gallery');

      // Should show all photos initially
      cy.contains('vacation-1.jpg').should('be.visible');
      cy.contains('work-1.jpg').should('be.visible');

      // Click vacation tag filter
      cy.contains('button', 'vacation').click();

      // Should only show vacation photos
      cy.contains('vacation-1.jpg').should('be.visible');
      cy.contains('work-1.jpg').should('not.exist');
    });

    it('should clear tag filter', () => {
      cy.visit('/gallery?tag=vacation');

      // Should show only vacation photos
      cy.contains('vacation-1.jpg').should('be.visible');

      // Click "All" or clear filter
      cy.contains('button', /all|clear|show all/i).click();

      // Should show all photos
      cy.contains('vacation-1.jpg').should('be.visible');
      cy.contains('work-1.jpg').should('be.visible');
    });
  });

  describe('Photo Download', () => {
    it('should download photo when clicking download button', () => {
      cy.intercept('GET', '**/photos*', {
        body: {
          content: [
            {
              id: 'download-test',
              filename: 'download-test.jpg',
              thumbnailUrl: 'https://via.placeholder.com/150',
              downloadUrl: 'https://via.placeholder.com/800',
              status: 'COMPLETE',
              fileSize: 1024000,
              tags: [],
              createdAt: new Date().toISOString(),
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
      });

      cy.visit('/gallery');

      // Click download button
      cy.contains('download-test.jpg')
        .parent()
        .contains('button', /download/i)
        .click();

      // Download should be triggered (checking for link with download attribute)
      cy.get('a[download]').should('exist');
    });
  });

  describe('Navigation', () => {
    it('should navigate between gallery and upload', () => {
      cy.visit('/gallery');

      // Click upload link
      cy.contains('a', /upload/i).click();
      cy.url().should('include', '/upload');

      // Navigate back to gallery
      cy.contains('a', /gallery/i).click();
      cy.url().should('include', '/gallery');
    });
  });
});

