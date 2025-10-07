# InvoiceFlow

InvoiceFlow is a comprehensive invoice management system that integrates with QuickBooks Online (QBO) API to synchronize and manage invoice data. The application consists of a Spring Boot backend and a React frontend.

## Project Structure

```
invoiceflow/
├── backend/                 # Spring Boot backend service
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/zzy/
│   │   │   │   ├── Application.java
│   │   │   │   ├── auth/           # OAuth2 authentication
│   │   │   │   ├── config/         # Configuration classes
│   │   │   │   ├── domain/         # Domain models and DTOs
│   │   │   │   ├── repository/     # Data access layer
│   │   │   │   ├── service/        # Business logic
│   │   │   │   └── web/            # REST controllers
│   │   │   └── resources/
│   │   │       └── application.yml # Main configuration
│   │   └── test/                   # Test classes
│   ├── build.gradle               # Gradle build file
│   └── gradlew                    # Gradle wrapper
├── frontend/               # React frontend application
│   ├── src/
│   │   ├── components/     # React components
│   │   ├── pages/          # Page components
│   │   ├── api.ts          # API service
│   │   └── App.tsx         # Main application component
│   ├── package.json        # Node.js dependencies
│   └── vite.config.ts      # Vite configuration
└── README.md              # Project documentation
```


## Prerequisites

- Java 21
- Node.js 16+
- Gradle 7+
- npm 8+

## Quick Start

### Backend Setup

1. Navigate to the backend directory:
```bash
cd backend
```


2. Configure environment variables:
```bash
export CLIENT_ID=your_qbo_client_id
export CLIENT_SECRET=your_qbo_client_secret
```


3. Build and run the application:
```bash
./gradlew bootRun
```


The backend will start on `http://localhost:8080`.

### Frontend Setup

1. Navigate to the frontend directory:
```bash
cd frontend
```


2. Install dependencies:
```bash
npm install
```


3. Run the development server:
```bash
npm run dev
```


The frontend will start on `http://localhost:5173`.

## Environment Variables Configuration

### Backend Environment Variables

| Variable Name | Description | Required |
|---------------|-------------|----------|
| `CLIENT_ID` | QuickBooks Online application client ID | ✅ |
| `CLIENT_SECRET` | QuickBooks Online application client secret | ✅ |

These variables are used to configure the OAuth2 integration with QuickBooks Online. You can obtain these credentials by creating an app in the [Intuit Developer Portal](https://developer.intuit.com/).

### Frontend Authentication

The frontend application stores authentication information in `localStorage`:
- `admin_basic`: Admin basic authentication token

## Key Features

- [x] QBO OAuth2 authentication integration
- [x] Invoice data synchronization (QBO → local database)
- [x] Invoice pagination and filtering
- [x] Invoice status statistics
- [x] Aging analysis report
- [x] Overdue invoice management
- [x] Real-time data visualization

## API Endpoints

### Authentication
- `GET /connect` - Link to connect page
- `GET /connected` - Connected

### Invoice Management
- `POST /api/admin/sync-qbo` - Sync invoices from QBO
- `GET /api/invoices` - Get invoice list with pagination

### Analytics
- `GET /api/invoices/aging/overdue` - Get status statistics
- `GET /api/summary` - Get summary dashboard data
- `GET /api/risk/kpi/overdue-by-due` - Overdue trend (daily or weekly)
- `GET /api/risk/customers` - Customer risk ranking

## Development

### Backend Development

The backend is built with Spring Boot and uses:
- Spring Data JPA for data persistence
- Spring Web for REST APIs
- H2 in-memory database for development
- OAuth2 for QuickBooks Online integration

To run tests:
```bash
./gradlew test
```


### Frontend Development

The frontend is built with:
- React with TypeScript
- Material-UI components
- Vite for build tooling

To build for production:
```bash
npm run build
```


## Database

The application uses an H2 in-memory database for development. The database is automatically created and populated when the application starts.

For production, you can configure a different database by modifying the `application.yml` file.

## Security

- OAuth2 integration with QuickBooks Online
- CSRF protection
- Secure session management

## Troubleshooting

### Common Issues

1. **OAuth2 Connection Issues**: Ensure your `CLIENT_ID` and `CLIENT_SECRET` are correctly configured
2. **CORS Errors**: The application includes CORS configuration for localhost development
3. **Database Connection Issues**: Check that the H2 database is properly configured in `application.yml`

### Logs

Application logs can be found in the console when running with `./gradlew bootRun`. For production deployments, configure logging in `application.yml`.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.