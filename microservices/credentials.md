# ARFOMS Microservices Credentials

The following hardcoded credentials are used by the `auth-service` to generate JWT tokens for different roles in the system.

| Role / Module | Username | Password |
| :--- | :--- | :--- |
| **Admin** | `admin` | `Admin@123` |
| **Flight Scheduler** | `scheduler` | `Scheduler@123` |
| **Reservation Agent** | `agent` | `Agent@123` |
| **Crew Manager** | `crew` | `Crew@123` |
| **Loyalty Manager** | `loyalty` | `Loyalty@123` |
| **Ground Staff (Check-in)** | `groundstaff` | `Ground@123` |

> **Note:** These credentials must be used exactly as shown (case-sensitive) on the frontend login page to successfully authenticate and receive a JWT token.
