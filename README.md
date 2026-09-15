# Library Management System

A service-oriented library management system for managing books, libraries, inventory, borrowing, and memberships across multiple library branches.

## Services

### Catalog Service

Manages the library's book catalog.

* Add and manage books
* Manage book categories
* Search books by title or author
* View book details and prices

### Inventory Service

Manages books available at each library branch.

* Manage libraries and their stock
* Track how many copies of a book are available
* Transfer books between libraries
* Update stock when books are borrowed or returned

### Borrowing Service

Handles the borrowing process.

* Create loans
* Track borrowed books
* Process returns
* Handle overdue loans
* Calculate borrowing-related information

### Membership Service

Manages library members and memberships.

* Create and manage members
* Manage membership subscriptions
* Track membership status and payment information
* Manage librarian and administrative users

### API Gateway

Provides a single entry point to the backend services.

* Routes requests to the appropriate service
* Handles authentication and authorization
* Allows the frontend to communicate with the microservices through one API

### MCP Service

Provides an MCP interface for interacting with the library system through supported tools and operations.

## How the System Is Used

The system is mainly operated by **librarians and library staff**.

A typical workflow is:

1. **Add books** to the catalog and provide their information and price.
2. **Manage libraries** and add available book stock to each branch.
3. **Register members** and manage their memberships.
4. When a member wants a book, the **librarian creates a loan** for them.
5. The system updates the available inventory automatically.
6. When the book is returned, the **loan is closed** and the inventory is updated.
7. If a library needs books from another branch, staff can create a **transfer request**.

## Architecture

The system consists of independent microservices with separate responsibilities and databases.

Communication is handled through:

* **REST** – synchronous communication and frontend requests
* **Apache Kafka** – asynchronous integration events
* **Consul** – service discovery
* **Keycloak** – authentication and authorization
* **Docker Compose** – running the system locally

## Running the Project

The project is intended to be run using Docker Compose.

Start the required services with:

```bash
docker compose up --build
```

The frontend communicates with the backend through the **API Gateway**.

After starting the system, log in through Keycloak and use the frontend according to the user's role and permissions.
