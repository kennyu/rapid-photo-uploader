# RapidPhotoUpload - Web Client

A modern React 18 web client built with TypeScript and Vite for high-volume photo upload management.

## 🚀 Features

- **React 18** with TypeScript for type-safe development
- **Vite** for blazing-fast development and optimized production builds
- **React Query (TanStack Query)** for efficient API state management
- **ESLint & Prettier** for code quality and consistency
- **Feature-based architecture** for scalable codebase organization
- **Proxy configuration** to connect with backend API

## 📁 Project Structure

```
web-client/
├── src/
│   ├── features/          # Feature-based modules
│   │   ├── auth/          # Authentication pages
│   │   │   ├── LoginPage.tsx
│   │   │   ├── RegisterPage.tsx
│   │   │   └── index.tsx
│   │   ├── gallery/       # Photo gallery
│   │   │   ├── GalleryPage.tsx
│   │   │   ├── usePhotos.ts  # React Query hook
│   │   │   └── index.tsx
│   │   └── upload/        # Photo upload
│   │       ├── UploadPage.tsx
│   │       └── index.tsx
│   ├── common/            # Shared components
│   │   └── Button.tsx
│   ├── api/               # API client
│   │   └── client.ts
│   ├── types/             # TypeScript types
│   │   └── api.ts
│   ├── App.tsx
│   ├── main.tsx
│   └── vite-env.d.ts
├── public/
├── .eslintrc.js
├── .prettierrc
├── tsconfig.json
├── vite.config.ts
└── package.json
```

## 🛠️ Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.3.1 | UI library |
| TypeScript | 5.7.2 | Type safety |
| Vite | 6.0.1 | Build tool & dev server |
| React Query | latest | API state management |
| ESLint | 9.15.0 | Linting |
| Prettier | 3.3.3 | Code formatting |

## 📋 Prerequisites

- Node.js 18+ and npm
- Backend API running on `http://localhost:8080` (optional for development)

## 🚀 Getting Started

### 1. Install Dependencies

```bash
npm install
```

### 2. Start Development Server

```bash
npm run dev
```

The app will be available at **http://localhost:3000**

### 3. Build for Production

```bash
npm run build
```

Output will be in the `dist/` directory.

### 4. Preview Production Build

```bash
npm run preview
```

## 📜 Available Scripts

| Script | Description |
|--------|-------------|
| `npm run dev` | Start development server (port 3000) |
| `npm run build` | Build for production |
| `npm run preview` | Preview production build locally |
| `npm run lint` | Run ESLint |
| `npm run format` | Format code with Prettier |

## 🔌 API Integration

The web client connects to the backend API at `http://localhost:8080`. This is configured in `vite.config.ts`:

```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
},
```

### Environment Variables

You can override the API URL using environment variables:

Create a `.env.local` file:

```bash
VITE_API_URL=http://your-api-url.com/api
```

## 📦 React Query Setup

React Query is configured in `src/main.tsx`:

```typescript
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
})
```

### Example Query Hook

See `src/features/gallery/usePhotos.ts` for an example of using React Query:

```typescript
export function usePhotos() {
  return useQuery({
    queryKey: ['photos'],
    queryFn: async () => {
      const response = await apiClient.get<PhotosResponse>('/v1/photos')
      return response
    },
  })
}
```

## 🎨 Code Style

This project uses:

- **ESLint** with TypeScript and React plugins
- **Prettier** for consistent formatting
- **Strict TypeScript** configuration

Run formatting before committing:

```bash
npm run format
npm run lint
```

## 🏗️ Architecture

### Feature-Based Organization

Each feature is self-contained with:
- Components
- Custom hooks (React Query)
- Types/interfaces
- Feature-specific logic

### Type Safety

All API types are defined in `src/types/api.ts` and shared across the application.

### API Client

The API client (`src/api/client.ts`) provides a simple interface for making requests:

```typescript
await apiClient.get<T>(endpoint)
await apiClient.post<T>(endpoint, data)
```

## 🐛 Development Tips

### React Query DevTools

React Query DevTools are enabled in development mode. Access them via the floating icon in the bottom-right corner of your browser.

### Hot Module Replacement (HMR)

Vite provides instant HMR. Your changes will be reflected immediately without full page reloads.

### TypeScript Strict Mode

This project uses `strict: true` in `tsconfig.json`. All components must have proper types.

## 🚧 Next Steps

- Implement actual photo upload functionality
- Add authentication flow
- Integrate with backend API endpoints
- Add photo gallery with filtering and tagging
- Implement responsive design for mobile

## 📚 Documentation

- [React Documentation](https://react.dev/)
- [Vite Documentation](https://vite.dev/)
- [React Query Documentation](https://tanstack.com/query/latest)
- [TypeScript Documentation](https://www.typescriptlang.org/docs/)

## ✅ Task Completion

**Task 9: Web Client Project Setup (React + TypeScript)** - ✅ Complete

All subtasks completed:
1. ✅ Scaffold React + TypeScript Project with Vite
2. ✅ Configure ESLint, Prettier, and Strict TypeScript
3. ✅ Organize Feature-Based Folder Structure
4. ✅ Integrate React Query for API State Management

---

**Built with ❤️ using React, TypeScript, and Vite**

