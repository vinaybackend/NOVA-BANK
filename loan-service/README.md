# Loan service

The loan service runs on port `9099` and is reachable through the gateway at
`/loan-service/api/v1`. It saves loan records in the shared `novabank` database
and sends the customer's access token when it asks Customer Service, KYC Service,
or Account Service for information or to move loan money. Approved disbursements
are credited to the customer's active savings account (or current account if
there is no savings account). EMI, prepayment, and foreclosure are deducted from
that same account. Each debit and credit is recorded in Account Service's
`loan_balance_operations` table to prevent a retried request from moving money
twice.

Set `DATABASE_PASSWORD` and `JWT_SECRET` before starting the service.
`JWT_SECRET` must be the same key used by Auth Service. The loan service does not
make another token: in Postman, use the access token you already got when you
logged in. The service passes that same token to Customer Service and KYC Service.
The upload directory can be changed with `LOAN_UPLOAD_DIR` (default:
`uploads/loan-documents`).

From the repository root, start it with:

```powershell
# Replace these two example values on your computer.
$env:DATABASE_PASSWORD = "<your database password>"
$env:JWT_SECRET = "<the Auth Service JWT secret>"
.\transaction-service\transaction-service\mvnw.cmd -f .\loan-service\loan-service\pom.xml spring-boot:run
```

Start Eureka, Account Service, Customer Service, and KYC Service before using the
loan workflow. Start the API Gateway too if you want to use the gateway URL below.

Create the `LOAN_OFFICER` role in Auth Service and assign it to authorized staff
before they use the approval and disbursement endpoints.

## First request examples

Open Postman and use the gateway URL: `http://localhost:9090/loan-service/api/v1`.
For signed-in endpoints, select **Authorization > Bearer Token** and paste your
existing Auth Service access token. Do not create a new token for loans.

An admin creates a product with `POST /loan-products`:

```json
{
  "name": "Personal loan",
  "description": "Personal expenses",
  "minimumAmount": 10000,
  "maximumAmount": 500000,
  "annualInterestRate": 12,
  "minimumTenureMonths": 6,
  "maximumTenureMonths": 60,
  "maximumFoirPercent": 40,
  "minimumCreditScore": 650
}
```

After signing in as a customer, view products with `GET /loan-products`. To
estimate a payment, send `POST /loan-products/1/estimate`:

```json
{
  "amount": 100000,
  "tenureMonths": 12,
  "monthlyIncome": 50000,
  "existingMonthlyEmi": 1000
}
```

To apply, send `POST /applications` with the product ID from the product list:

```json
{
  "productId": 1,
  "amount": 100000,
  "tenureMonths": 12
}
```

Use the application ID returned in the response for the next step in the loan
process. Endpoints that change application state must be called in the order
described below.

## Loan process

1. An admin or loan officer creates a product with `POST /loan-products`.
2. Customers view products with `GET /loan-products`, estimate with
   `POST /loan-products/{productId}/estimate`, then apply with `POST /applications`.
   The signed-in customer's profile and approved KYC status are checked for them.
3. The customer submits employment details with
   `POST /applications/{applicationId}/employment`. Salaried applicants provide
   `designation` and `workExperienceYears`; self-employed applicants provide
   `businessName`, `businessType`, `businessVintageYears`, `businessAddress`, and
   `annualTurnover`. Upload documents to
   `POST /applications/{applicationId}/documents` as multipart fields
   `documentType` and `file`. Credit assessment requires verified `AADHAAR`,
   `PAN`, and `BANK_STATEMENT`, plus `SALARY_SLIP` for salaried applicants or
   `BUSINESS_PROOF` for self-employed applicants.
4. Staff find applications with `GET /applications`; officers can view their
   assignments with `GET /applications/assigned`. Review files with
   `GET /applications/{applicationId}/documents`, preview them with
   `GET /documents/{documentId}/preview`, and verify each one with
   `PATCH /documents/{documentId}/verification` (`{"verified":true}`). Staff
   can request missing documents with
   `POST /applications/{applicationId}/additional-documents`; customers can
   then upload the requested files. Application actions are available from
   `GET /applications/{applicationId}/history`.
5. A loan officer assesses credit with
   `POST /applications/{applicationId}/credit-assessment` (`{"creditScore":720}`)
   and submits an `APPROVE` or `REJECT` recommendation with
   `POST /applications/{applicationId}/recommendation`. The recommendation
   includes remarks and the proposed amount, rate, and tenure. A branch manager
   makes the final decision with
   `POST /applications/{applicationId}/manager-decision`, using a mandatory
   reason and `APPROVE`, `REJECT`, or `RETURN`. An `APPROVE` decision may also
   include adjusted `amount`, `annualInterestRate`, or `tenureMonths`; the service
   checks the product limits, requested amount, minimum product rate, and
   repayment-capacity limit before creating the customer offer. Only a
   manager/admin can create the customer offer or authorize disbursement.
6. The customer accepts or rejects using `POST /applications/{applicationId}/offer-response`
   (`{"accepted":true}`). The response includes the generated agreement and KFS.
   The customer accepts the agreement with `POST /applications/{applicationId}/accept-agreement`.
7. An authorized branch manager/admin calls `POST /applications/{applicationId}/authorize-disbursement`,
   then `POST /applications/{applicationId}/disburse`. This creates the loan account
   and monthly repayment schedule.
8. Customers view accounts with `GET /accounts/me`, schedules with
   `GET /accounts/{accountId}/schedule` and payment history with
   `GET /accounts/{accountId}/payments`. Monthly payments, prepayments and
   foreclosure use `POST /accounts/{accountId}/payments`,
   `POST /accounts/{accountId}/prepayments`, and
   `POST /accounts/{accountId}/foreclosure` respectively. Get a foreclosure
   amount first with `GET /accounts/{accountId}/foreclosure-quote`. Payment
   requests include a unique `paymentId`, for example
   `{"paymentId":"pay-1","amount":8884.88}`. Reuse that ID if a request times out
   and you need to safely retry it.

Monthly repayments reduce the outstanding balance and are recorded as payment
history. This starter implementation uses a simple foreclosure quote based on
the current outstanding principal; production banking requires the institution's
approved interest, fee, repayment and legal-document calculations.

Staff dashboards use `GET /dashboard` for per-status application counts.
Branch managers/admins can assign or reassign work with
`POST /applications/{applicationId}/assignment` and an `officerId`. Application
records returned by the staff detail endpoint include the customer contact and
KYC snapshot, submitted employment/business information, assessment, recommendation,
offer, assignment, and decision reason. Staff can load the full Customer Service
profile plus application documents and history with
`GET /applications/{applicationId}/review-details`; Customer Service grants the
`LOAN_OFFICER` role access to the profile lookup endpoint for this workflow.
