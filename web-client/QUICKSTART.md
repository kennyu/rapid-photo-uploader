# 🚀 RapidPhotoUpload Web Client - Quick Start

## ⚡ Start Development Server

### **Option 1: Using the Start Script (Easiest)**

```bash
cd web-client
.\start.bat
```

### **Option 2: Using npm**

```bash
cd web-client
npm run dev
```

---

## 🌐 Access the Application

Once started, open your browser and navigate to:

**http://localhost:3000**

You should see:
- **RapidPhotoUpload** header
- Navigation buttons: Upload, Gallery, Login, Register
- Feature pages that demonstrate the app structure

---

## 🎯 Features to Test

### **1. Navigation**
Click through the navigation buttons:
- **Upload** - Photo upload interface
- **Gallery** - Photo gallery (will try to connect to backend)
- **Login** - Login form
- **Register** - Registration form

### **2. React Query Devtools**
Look for a floating icon in the **bottom-right corner** of the browser:
- Click it to open React Query DevTools
- You'll see the query cache and network requests
- Try navigating to "Gallery" to see the `/v1/photos` query

### **3. Backend Connection (Optional)**
If the backend is running on `http://localhost:8080`:
- Gallery page will fetch photos from the API
- You'll see loading states and error handling
- Check the browser console (F12) for network requests

---

## ✅ Expected Output

When you run `npm run dev`, you should see:

```
  VITE v6.4.1  ready in XXX ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

---

## 🐛 Troubleshooting

### **Port 3000 already in use?**

```bash
# Kill the process using port 3000
netstat -ano | findstr :3000
taskkill /PID <PID_NUMBER> /F
```

### **Dependencies not installed?**

```bash
npm install
```

### **ESLint errors?**

```bash
npm run lint
```

### **Want to format code?**

```bash
npm run format
```

---

## 🔧 Development Commands

| Command | Description |
|---------|-------------|
| `npm run dev` | Start dev server (port 3000) |
| `npm run build` | Build for production |
| `npm run preview` | Preview production build |
| `npm run lint` | Run ESLint |
| `npm run format` | Format with Prettier |

---

## 📱 Features Demonstrated

### ✅ **Implemented:**
- React 18 with TypeScript
- Vite dev server with HMR
- Feature-based folder structure
- React Query for API state management
- ESLint + Prettier configuration
- Basic navigation between pages
- Sample auth forms (Login/Register)
- Gallery with API integration example

### 🚧 **To Be Implemented:**
- Actual photo upload functionality
- JWT authentication flow
- Real backend API integration
- Photo gallery with filtering/tagging
- File drag-and-drop
- Upload progress tracking
- Image preview before upload

---

## 🎨 UI/UX Features

- **Dark/Light mode** - Automatically detects system preference
- **Responsive design** - Basic responsive layout
- **Modern UI** - Clean, minimal interface
- **Fast HMR** - Instant updates during development

---

## 🧪 Testing React Query

1. **Start the dev server**
2. **Click "Gallery"** button
3. **Open React Query DevTools** (floating icon bottom-right)
4. **See the queries**:
   - `['photos']` - Will be in "error" state if backend isn't running
   - Click the query to see details

---

## 🔌 Backend Connection

The web client is configured to proxy API requests to the backend:

```typescript
// vite.config.ts
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

**To test with backend:**

1. Start backend: `cd ../backend && run.bat`
2. Start web client: `cd ../web-client && npm run dev`
3. Navigate to Gallery page
4. Photos should load from backend API

---

## 📊 What You're Running

- **React**: 18.3.1
- **TypeScript**: 5.7.2
- **Vite**: 6.0.1
- **React Query**: 5.90.7
- **Dev Server Port**: 3000
- **API Proxy Target**: http://localhost:8080

---

## 🚀 Next Steps

After verifying the web client works:

1. **Implement photo upload UI** (Task 11)
2. **Add authentication flow**
3. **Build photo gallery with filtering**
4. **Connect to backend API**

---

**Enjoy developing! 🎉**

If you see the RapidPhotoUpload interface in your browser, everything is working perfectly!

