# 🔧 Quick Fix - PostgreSQL Required!

## ❌ Current Error

```
Connection to localhost:5432 refused
```

**Root Cause**: PostgreSQL database is not installed or not running.

---

## ✅ Solution: Install & Start PostgreSQL

### **Option 1: Install PostgreSQL (Chocolatey - Fastest)**

```bash
# Install PostgreSQL
choco install postgresql -y

# PostgreSQL will auto-start as a Windows service
```

### **Option 2: Install PostgreSQL (Winget)**

```bash
winget install PostgreSQL.PostgreSQL
```

### **Option 3: Manual Install**

1. Download from: https://www.postgresql.org/download/windows/
2. Run installer (remember the password you set for `postgres` user)
3. PostgreSQL will start automatically as a Windows service

---

## 📋 Quick Setup Steps

### **1. Verify PostgreSQL is Running**

```bash
# Check if PostgreSQL service is running
sc query postgresql-x64-16

# If not running, start it:
net start postgresql-x64-16
```

### **2. Create Database**

```bash
# Using psql (PostgreSQL command line)
# Password: the password you set during installation

# Find psql.exe (usually in):
# C:\Program Files\PostgreSQL\16\bin\psql.exe

"C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -c "CREATE DATABASE rapidphoto;"
```

**Or use PowerShell**:
```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -c "CREATE DATABASE rapidphoto;"
```

### **3. Configure Application**

The default configuration in `application.properties` should work:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/rapidphoto
spring.datasource.username=postgres
spring.datasource.password=postgres
```

**If you used a different password**, create `application-local.properties`:

```properties
spring.datasource.password=YOUR_ACTUAL_PASSWORD
```

---

## 🚀 Run Again

After PostgreSQL is installed and database is created:

```bash
run.bat
```

---

## 🎯 Alternative: Run Without PostgreSQL (Testing Mode)

If you want to test the build without PostgreSQL, you can temporarily disable the database:

1. Edit `application.properties`
2. Add these lines at the top:

```properties
# Disable database for testing
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
  org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
```

3. Run again: `run.bat`

**Note**: Most endpoints won't work without a database, but the health check will!

---

## 📊 Full Setup Checklist

For complete backend functionality, you need:

- [x] **Java 21** - ✅ Already installed!
- [x] **Gradle** - ✅ Already configured!
- [x] **Build Success** - ✅ Completed!
- [ ] **PostgreSQL** - ⚠️ Install now
- [ ] **Database Created** - Run `CREATE DATABASE rapidphoto;`
- [ ] **AWS S3** - Optional for now (see SETUP_GUIDE.md)

---

## 🔍 Verification

After installing PostgreSQL, verify it's working:

```bash
# Check PostgreSQL version
"C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres --version

# List databases
"C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -l
```

You should see `rapidphoto` in the database list!

---

## 📚 Need More Help?

- **Complete Setup**: [SETUP_GUIDE.md](SETUP_GUIDE.md)
- **API Documentation**: [API_REFERENCE.md](API_REFERENCE.md)
- **Architecture**: [ARCHITECTURE.md](ARCHITECTURE.md)

---

**Once PostgreSQL is running, the backend will start perfectly!** 🚀

