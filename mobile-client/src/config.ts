// App configuration
export const config = {
  // API Configuration
  apiUrl: __DEV__ 
    ? 'http://localhost:8080/api/v1' // Development - change to your local IP if testing on device
    : 'https://:ec2-54-152-45-144.compute-1.amazonaws.com:8080/api/v1', // Production

  // App Configuration
  maxPhotosPerUpload: 100,
  uploadTimeout: 30000, // 30 seconds per photo
  
  // Feature Flags
  features: {
    multipleUpload: true,
    photoTags: true,
    offlineMode: false, // Future feature
  },
};

// Helper to get device-accessible API URL
// When testing on a real device, you'll need to use your computer's IP
// Example: 'http://192.168.1.100:8080/api'
export const getApiUrl = (): string => {
  return config.apiUrl;
};

