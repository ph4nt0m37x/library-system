import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import keycloak from "../keycloak";

function MemberDetailsPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [member, setMember] = useState(null);
    const [loans, setLoans] = useState([]);
    const [fees, setFees] = useState([]);
    const [payments, setPayments] = useState([]);
    const [borrowingBan, setBorrowingBan] = useState(null);
    const [loanBookNames, setLoanBookNames] = useState({});
    const [libraries, setLibraries] = useState([]);
    const [books, setBooks] = useState([]);

    const [form, setForm] = useState({
        firstName: "",
        lastName: "",
        email: "",
        phoneNumber: "",
    });

    const [editing, setEditing] = useState(false);
    const [loading, setLoading] = useState(true);
    const [loansLoading, setLoansLoading] = useState(true);
    const [loansError, setLoansError] = useState("");
    const [feesLoading, setFeesLoading] = useState(true);
    const [feesError, setFeesError] = useState("");
    const [paymentsLoading, setPaymentsLoading] = useState(true);
    const [paymentsError, setPaymentsError] = useState("");
    const [banLoading, setBanLoading] = useState(true);
    const [banError, setBanError] = useState("");
    const [showPaymentQuoteModal, setShowPaymentQuoteModal] =
        useState(false);
    const [selectedFeeIds, setSelectedFeeIds] = useState([]);
    const [paymentQuote, setPaymentQuote] = useState(null);
    const [quotingPayment, setQuotingPayment] = useState(false);
    const [paymentQuoteError, setPaymentQuoteError] = useState("");
    const [showLoanModal, setShowLoanModal] = useState(false);
    const [loanOptionsLoading, setLoanOptionsLoading] = useState(false);
    const [creatingLoan, setCreatingLoan] = useState(false);
    const [loanError, setLoanError] = useState("");
    const [bookSearch, setBookSearch] = useState("");
    const [loanIdempotencyKey, setLoanIdempotencyKey] = useState("");
    const [loanForm, setLoanForm] = useState({
        libraryId: "",
        bookId: "",
    });
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");

    // Subscription modal
    const [showSubscriptionModal, setShowSubscriptionModal] =
        useState(false);

    const [subscriptionForm, setSubscriptionForm] = useState({
        tier: "THREE_MONTHS",
        startsAt: "",
    });

    const [startingSubscription, setStartingSubscription] =
        useState(false);
    const [subscriptionError, setSubscriptionError] =
        useState("");

    const isRenewal =
        (member?.subscriptionHistory?.length ?? 0) > 0;

    const fetchMember = async () => {
        const response = await fetch(
            `http://localhost:8000/api/members/${id}`,
{
    headers: {
        Authorization: `Bearer ${keycloak.token}`,
    },
}
);

if (!response.ok) {
    throw new Error("Failed to load member.");
}

const data = await response.json();

setMember(data);

setForm({
    firstName: data.firstName,
    lastName: data.lastName,
    email: data.email,
    phoneNumber: data.phoneNumber,
});

return data;
};

const fetchLoans = async () => {
    const response = await fetch(
        `http://localhost:8000/api/loans/member/${id}`,
        {
            headers: {
                Authorization: `Bearer ${keycloak.token}`,
            },
        }
    );

    if (!response.ok) {
        throw new Error("Failed to load member loans.");
    }

    const data = await response.json();
    setLoans(data);

    const bookNames = await Promise.all(
        data.map(async (loan) => {
            try {
                const bookResponse = await fetch(
                    `http://localhost:8000/api/books/${loan.bookId}`,
                    {
                        headers: {
                            Authorization: `Bearer ${keycloak.token}`,
                        },
                    }
                );

                if (!bookResponse.ok) {
                    return [loan.bookId, "Unknown book"];
                }

                const book = await bookResponse.json();
                return [loan.bookId, book.title];
            } catch {
                return [loan.bookId, "Unknown book"];
            }
        })
    );

    setLoanBookNames(Object.fromEntries(bookNames));
};

const fetchFees = async () => {
    const response = await fetch(
        `http://localhost:8000/api/fees/member/${id}`,
        {
            headers: {
                Authorization: `Bearer ${keycloak.token}`,
            },
        }
    );

    if (!response.ok) {
        throw new Error("Failed to load member fees.");
    }

    const data = await response.json();
    setFees(data);
    return data;
};

const fetchBorrowingBan = async () => {
    const response = await fetch(
        `http://localhost:8000/api/borrowing-bans/member/${id}`,
        {
            headers: {
                Authorization: `Bearer ${keycloak.token}`,
            },
        }
    );

    if (response.status === 404) {
        setBorrowingBan(null);
        return null;
    }

    if (!response.ok) {
        throw new Error("Failed to load borrowing-ban information.");
    }

    const data = await response.json();
    setBorrowingBan(data);
    return data;
};

const fetchPayments = async () => {
    const response = await fetch(
        `http://localhost:8000/api/payments/member/${id}`,
        {
            headers: {
                Authorization: `Bearer ${keycloak.token}`,
            },
        }
    );

    if (!response.ok) {
        throw new Error("Failed to load payment history.");
    }

    const data = await response.json();
    setPayments(data);
    return data;
};

const getResourceId = (resource) =>
    resource.id?.value ?? resource.id?.id ?? resource.id;

const loadLoanOptions = async () => {
    const headers = {
        Authorization: `Bearer ${keycloak.token}`,
    };

    const [librariesResponse, booksResponse] = await Promise.all([
        fetch("http://localhost:8000/api/libraries/available", { headers }),
        fetch("http://localhost:8000/api/books/available", { headers }),
    ]);

    if (!librariesResponse.ok || !booksResponse.ok) {
        throw new Error("Failed to load libraries and books.");
    }

    const [libraryData, bookData] = await Promise.all([
        librariesResponse.json(),
        booksResponse.json(),
    ]);

    setLibraries(libraryData);
    setBooks(bookData);
};

useEffect(() => {
    const loadMember = async () => {
        try {
            setLoading(true);
            setError("");

            await fetchMember();
        } catch (err) {
            console.error(err);

            setError(
                err.message || "Failed to load member."
            );
        } finally {
            setLoading(false);
        }
    };

    loadMember();

    const loadLoans = async () => {
        try {
            setLoansLoading(true);
            setLoansError("");
            await fetchLoans();
        } catch (err) {
            console.error(err);
            setLoansError(
                err.message || "Failed to load member loans."
            );
        } finally {
            setLoansLoading(false);
        }
    };

    loadLoans();

    const loadFees = async () => {
        try {
            setFeesLoading(true);
            setFeesError("");
            await fetchFees();
        } catch (err) {
            console.error(err);
            setFeesError(err.message || "Failed to load member fees.");
        } finally {
            setFeesLoading(false);
        }
    };

    loadFees();

    const loadBorrowingBan = async () => {
        try {
            setBanLoading(true);
            setBanError("");
            await fetchBorrowingBan();
        } catch (err) {
            console.error(err);
            setBanError(
                err.message || "Failed to load borrowing-ban information."
            );
        } finally {
            setBanLoading(false);
        }
    };

    loadBorrowingBan();

    const loadPayments = async () => {
        try {
            setPaymentsLoading(true);
            setPaymentsError("");
            await fetchPayments();
        } catch (err) {
            console.error(err);
            setPaymentsError(
                err.message || "Failed to load payment history."
            );
        } finally {
            setPaymentsLoading(false);
        }
    };

    loadPayments();
}, [id]);

// -----------------------------
// Member editing
// -----------------------------

const handleChange = (e) => {
    const { name, value } = e.target;

    setForm((previous) => ({
        ...previous,
        [name]: value,
    }));
};

const handleEdit = () => {
    setForm({
        firstName: member.firstName,
        lastName: member.lastName,
        email: member.email,
        phoneNumber: member.phoneNumber,
    });

    setError("");
    setEditing(true);
};

const handleCancel = () => {
    setForm({
        firstName: member.firstName,
        lastName: member.lastName,
        email: member.email,
        phoneNumber: member.phoneNumber,
    });

    setError("");
    setEditing(false);
};

const handleSave = async () => {
    try {
        setSaving(true);
        setError("");

        const nameChanged =
            form.firstName !== member.firstName ||
            form.lastName !== member.lastName;

        const contactChanged =
            form.email !== member.email ||
            form.phoneNumber !== member.phoneNumber;

        if (!nameChanged && !contactChanged) {
            setEditing(false);
            return;
        }

        if (nameChanged) {
            const nameResponse = await fetch(
                `http://localhost:8000/api/members/${id}/name`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${keycloak.token}`,
                    },
                    body: JSON.stringify({
                        firstName: form.firstName,
                        lastName: form.lastName,
                    }),
                }
            );

            if (!nameResponse.ok) {
                const message =
                    await nameResponse.text();

                throw new Error(
                    message ||
                    "Failed to update member name."
                );
            }
        }

        if (contactChanged) {
            const contactResponse = await fetch(
                `http://localhost:8000/api/members/${id}/contact-details`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${keycloak.token}`,
                    },
                    body: JSON.stringify({
                        email: form.email,
                        phoneNumber: form.phoneNumber,
                    }),
                }
            );

            if (!contactResponse.ok) {
                const message =
                    await contactResponse.text();

                throw new Error(
                    message ||
                    "Failed to update contact details."
                );
            }
        }

        // Axon read model may need a moment to update
        let updatedMember = null;

        for (let attempt = 0; attempt < 5; attempt++) {
            await new Promise((resolve) =>
                setTimeout(resolve, 300)
            );

            updatedMember = await fetchMember();

            const updated =
                updatedMember.firstName ===
                form.firstName &&
                updatedMember.lastName ===
                form.lastName &&
                updatedMember.email === form.email &&
                updatedMember.phoneNumber ===
                form.phoneNumber;

            if (updated) {
                break;
            }
        }

        setEditing(false);
    } catch (err) {
        console.error(err);

        setError(
            err.message || "Failed to update member."
        );
    } finally {
        setSaving(false);
    }
};

// -----------------------------
// Subscription
// -----------------------------

const handleSubscriptionChange = (e) => {
    const { name, value } = e.target;

    setSubscriptionForm((previous) => ({
        ...previous,
        [name]: value,
    }));
};

const openLoanModal = async () => {
    setShowLoanModal(true);
    setLoanError("");
    setBookSearch("");
    setLoanIdempotencyKey(crypto.randomUUID());
    setLoanForm({ libraryId: "", bookId: "" });

    try {
        setLoanOptionsLoading(true);
        await loadLoanOptions();
    } catch (err) {
        console.error(err);
        setLoanError(err.message || "Failed to load loan options.");
    } finally {
        setLoanOptionsLoading(false);
    }
};

const closeLoanModal = () => {
    if (creatingLoan) {
        return;
    }

    setShowLoanModal(false);
    setLoanError("");
};

const handleCreateLoan = async (event) => {
    event.preventDefault();

    if (!loanForm.libraryId || !loanForm.bookId) {
        setLoanError("Please select a library and book.");
        return;
    }

    try {
        setCreatingLoan(true);
        setLoanError("");

        const response = await fetch(
            "http://localhost:8000/api/loans/create",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${keycloak.token}`,
                },
                body: JSON.stringify({
                    memberId: member.memberId,
                    libraryId: loanForm.libraryId,
                    bookId: loanForm.bookId,
                    borrowedAt: new Date().toISOString(),
                    idempotencyKey: loanIdempotencyKey,
                }),
            }
        );

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || "Failed to create loan.");
        }

        setShowLoanModal(false);
        setLoansLoading(true);
        await fetchLoans();
    } catch (err) {
        console.error(err);
        setLoanError(err.message || "Failed to create loan.");
    } finally {
        setCreatingLoan(false);
        setLoansLoading(false);
    }
};

const openPaymentQuoteModal = () => {
    setSelectedFeeIds(activeFees.map((fee) => fee.feeId));
    setPaymentQuote(null);
    setPaymentQuoteError("");
    setShowPaymentQuoteModal(true);
};

const closePaymentQuoteModal = () => {
    if (quotingPayment) {
        return;
    }

    setShowPaymentQuoteModal(false);
    setPaymentQuoteError("");
};

const toggleFeeSelection = (feeId) => {
    setSelectedFeeIds((previous) =>
        previous.includes(feeId)
            ? previous.filter((id) => id !== feeId)
            : [...previous, feeId]
    );
};

const handleQuotePayment = async (event) => {
    event.preventDefault();

    if (selectedFeeIds.length === 0) {
        setPaymentQuoteError("Select at least one fee.");
        return;
    }

    try {
        setQuotingPayment(true);
        setPaymentQuoteError("");

        const quoteResponse = await fetch(
            "http://localhost:8000/api/payments/quote",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${keycloak.token}`,
                },
                body: JSON.stringify({
                    memberId: member.memberId,
                    currency: activeFees[0]?.currency || "MKD",
                    quotedAt: new Date().toISOString(),
                    feeIds: selectedFeeIds,
                }),
            }
        );

        if (!quoteResponse.ok) {
            const message = await quoteResponse.text();
            throw new Error(message || "Failed to prepare payment.");
        }

        const quote = await quoteResponse.json();
        const paymentResponse = await fetch(
            "http://localhost:8000/api/payments/record",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${keycloak.token}`,
                },
                body: JSON.stringify({
                    paymentId: quote.paymentId,
                    memberId: member.memberId,
                    amount: quote.amount,
                    currency: quote.currency,
                    paidAt: new Date().toISOString(),
                    feeIds: selectedFeeIds,
                }),
            }
        );

        if (!paymentResponse.ok) {
            const message = await paymentResponse.text();
            throw new Error(message || "Failed to record payment.");
        }

        setPaymentQuote(quote);

        try {
            for (let attempt = 0; attempt < 5; attempt++) {
                await new Promise((resolve) => setTimeout(resolve, 300));
                const updatedFees = await fetchFees();

                if (
                    selectedFeeIds.every((feeId) =>
                        updatedFees.some(
                            (fee) =>
                                fee.feeId === feeId && fee.status === "PAID"
                        )
                    )
                ) {
                    break;
                }
            }
        } catch (refreshError) {
            console.error("Failed to refresh fees after payment.", refreshError);
        }

        try {
            await fetchPayments();
        } catch (refreshError) {
            console.error("Failed to refresh payment history.", refreshError);
        }
    } catch (err) {
        console.error(err);
        setPaymentQuoteError(
            err.message || "Failed to record payment."
        );
    } finally {
        setQuotingPayment(false);
    }
};

const openSubscriptionModal = () => {
    setError("");
    setSubscriptionError("");

    setSubscriptionForm({
        tier: "THREE_MONTHS",
        startsAt: "",
    });

    setShowSubscriptionModal(true);
};

const closeSubscriptionModal = () => {
    if (startingSubscription) {
        return;
    }

    setShowSubscriptionModal(false);
    setSubscriptionError("");

    setSubscriptionForm({
        tier: "THREE_MONTHS",
        startsAt: "",
    });
};

const handleSubscriptionSubmit = async () => {
    if (
        !subscriptionForm.tier ||
        (!isRenewal && !subscriptionForm.startsAt)
    ) {
        setSubscriptionError(
            isRenewal
                ? "Please select a tier."
                : "Please select a tier and start date."
        );

        return;
    }

    try {
        setStartingSubscription(true);
        setSubscriptionError("");

        const requestBody = {
            tier: subscriptionForm.tier,
        };

        if (!isRenewal) {
            const startsAt = new Date(
                subscriptionForm.startsAt
            );

            if (Number.isNaN(startsAt.getTime())) {
                throw new Error(
                    "Please enter a valid start date."
                );
            }

            // datetime-local has no offset, while the start API expects
            // a ZonedDateTime.
            requestBody.startsAt = startsAt.toISOString();
        }

        const action = isRenewal ? "renew" : "start";
        const previousSubscriptionCount =
            member.subscriptionHistory?.length ?? 0;

        const response = await fetch(
            `http://localhost:8000/api/members/${member.memberId}/subscriptions/${action}`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${keycloak.token}`,
                },
                body: JSON.stringify(requestBody),
            }
        );

        if (!response.ok) {
            const responseBody = await response
                .json()
                .catch(() => null);

            throw new Error(
                responseBody?.message ||
                `Failed to ${action} subscription.`
            );
        }

        setShowSubscriptionModal(false);

        setSubscriptionForm({
            tier: "THREE_MONTHS",
            startsAt: "",
        });

        /*
         * The command succeeded, but the Axon read model
         * may need a moment to update.
         */
        for (let attempt = 0; attempt < 5; attempt++) {
            await new Promise((resolve) =>
                setTimeout(resolve, 300)
            );

            const updatedMember =
                await fetchMember();

            if (
                (updatedMember.subscriptionHistory?.length ?? 0) >
                previousSubscriptionCount
            ) {
                break;
            }
        }
    } catch (err) {
        console.error(err);

        setSubscriptionError(
            err.message ||
            `Failed to ${isRenewal ? "renew" : "start"} subscription.`
        );
    } finally {
        setStartingSubscription(false);
    }
};

// -----------------------------
// Loading / not found
// -----------------------------

if (loading) {
    return <p>Loading member...</p>;
}

if (!member) {
    return (
        <div>
            <p>{error || "Member not found."}</p>

            <button
                type="button"
                onClick={() => navigate("/members")}
            >
                Back to Members
            </button>
        </div>
    );
}

const filteredBooks = books.filter((book) => {
    const search = bookSearch.trim().toLowerCase();

    return (
        !search ||
        book.title?.toLowerCase().includes(search) ||
        book.author?.toLowerCase().includes(search) ||
        book.isbn?.toLowerCase().includes(search)
    );
});

const activeLoans = loans.filter(
    (loan) => loan.status === "ACTIVE"
);

const historicLoans = loans.filter(
    (loan) => loan.status !== "ACTIVE"
);

const activeLoansCount = activeLoans.length;

const activeFees = fees.filter((fee) => fee.status === "UNPAID");

const historicFees = fees.filter((fee) => fee.status === "PAID");

const historicBans = borrowingBan?.history?.filter(
    (ban) => ban.banId !== borrowingBan.currentBan?.banId
) || [];

return (
    <div>
        <button
            type="button"
            onClick={() => navigate("/members")}
        >
            ← Back to Members
        </button>

        <h1>Member Details</h1>

        {error && <p>{error}</p>}

        {/* -------------------------------- */}
        {/* Personal Information */}
        {/* -------------------------------- */}

        <section>
            <h2>Personal Information</h2>

            {editing ? (
                <>
                    <div>
                        <label htmlFor="firstName">
                            First name
                        </label>

                        <input
                            id="firstName"
                            name="firstName"
                            type="text"
                            value={form.firstName}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div>
                        <label htmlFor="lastName">
                            Last name
                        </label>

                        <input
                            id="lastName"
                            name="lastName"
                            type="text"
                            value={form.lastName}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div>
                        <label htmlFor="email">
                            Email
                        </label>

                        <input
                            id="email"
                            name="email"
                            type="email"
                            value={form.email}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div>
                        <label htmlFor="phoneNumber">
                            Phone number
                        </label>

                        <input
                            id="phoneNumber"
                            name="phoneNumber"
                            type="text"
                            value={form.phoneNumber}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <button
                        type="button"
                        onClick={handleCancel}
                        disabled={saving}
                    >
                        Cancel
                    </button>

                    <button
                        type="button"
                        onClick={handleSave}
                        disabled={saving}
                    >
                        {saving
                            ? "Saving..."
                            : "Save Changes"}
                    </button>
                </>
            ) : (
                <>
                    <p>
                        <strong>Name:</strong>{" "}
                        {member.firstName}{" "}
                        {member.lastName}
                    </p>

                    <p>
                        <strong>Email:</strong>{" "}
                        {member.email}
                    </p>

                    <p>
                        <strong>Phone:</strong>{" "}
                        {member.phoneNumber}
                    </p>

                    <button
                        type="button"
                        onClick={handleEdit}
                    >
                        Edit
                    </button>
                </>
            )}
        </section>

        {/* -------------------------------- */}
        {/* Membership Information */}
        {/* -------------------------------- */}

        <section>
            <h2>Membership Information</h2>

            <p>
                <strong>Membership number:</strong>{" "}
                {member.membershipNumber}
            </p>

            <p>
                <strong>Status:</strong>{" "}
                {member.active
                    ? "Active"
                    : "Inactive"}
            </p>

            <p>
                <strong>Registered:</strong>{" "}
                {new Date(
                    member.registeredAt
                ).toLocaleString()}
            </p>

            {member.changedAt && (
                <p>
                    <strong>Last changed:</strong>{" "}
                    {new Date(
                        member.changedAt
                    ).toLocaleString()}
                </p>
            )}
        </section>

        {/* -------------------------------- */}
        {/* Loans */}
        {/* -------------------------------- */}

        <section>
            <h2>
                Loans {" "}
                <span>(Active loans: {activeLoansCount})</span>
            </h2>

            <button
                type="button"
                onClick={openLoanModal}
            >
                Add Loan
            </button>

            {loansLoading ? (
                <p>Loading loans...</p>
            ) : loansError ? (
                <p role="alert">{loansError}</p>
            ) : activeLoans.length === 0 ? (
                <p>No active loans for this member.</p>
            ) : (
                activeLoans.map((loan) => (
                    <div key={loan.loanId}>
                        <p>
                            <strong>Book:</strong>{" "}
                            {loanBookNames[loan.bookId] || "Loading..."}
                        </p>

                        <button
                            type="button"
                            onClick={() => navigate(`/loans/${loan.loanId}`)}
                        >
                            Details
                        </button>

                        <hr />
                    </div>
                ))
            )}
        </section>

        <section>
            <h2>Loan History</h2>

            {loansLoading ? (
                <p>Loading loan history...</p>
            ) : loansError ? (
                <p role="alert">{loansError}</p>
            ) : historicLoans.length === 0 ? (
                <p>No historic loans for this member.</p>
            ) : (
                historicLoans.map((loan) => (
                    <div key={loan.loanId}>
                        <p>
                            <strong>Book:</strong>{" "}
                            {loanBookNames[loan.bookId] || "Loading..."}
                        </p>

                        <button
                            type="button"
                            onClick={() => navigate(`/loans/${loan.loanId}`)}
                        >
                            Details
                        </button>

                        <hr />
                    </div>
                ))
            )}
        </section>

        <section>
            <h2>
                Active Fees {" "}
                <span>(Active fees: {activeFees.length})</span>
            </h2>

            <button
                type="button"
                onClick={openPaymentQuoteModal}
                disabled={activeFees.length === 0}
            >
                Pay Active Fees
            </button>

            {feesLoading ? (
                <p>Loading fees...</p>
            ) : feesError ? (
                <p role="alert">{feesError}</p>
            ) : activeFees.length === 0 ? (
                <p>No active fees for this member.</p>
            ) : (
                activeFees.map((fee) => (
                    <div key={fee.feeId}>
                        <p>
                            <strong>Reason:</strong> {fee.reason}
                        </p>

                        <p>
                            <strong>Created:</strong>{" "}
                            {new Date(fee.createdAt).toLocaleString()}
                        </p>

                        {fee.dueAt && (
                            <p>
                                <strong>Due:</strong>{" "}
                                {new Date(fee.dueAt).toLocaleString()}
                            </p>
                        )}

                        <hr />
                    </div>
                ))
            )}
        </section>

        <section>
            <h2>Fee History</h2>

            {feesLoading ? (
                <p>Loading fee history...</p>
            ) : feesError ? (
                <p role="alert">{feesError}</p>
            ) : historicFees.length === 0 ? (
                <p>No historic fees for this member.</p>
            ) : (
                historicFees.map((fee) => (
                    <div key={fee.feeId}>
                        <p>
                            <strong>Reason:</strong> {fee.reason}
                        </p>

                        <p>
                            <strong>Created:</strong>{" "}
                            {new Date(fee.createdAt).toLocaleString()}
                        </p>

                        {fee.settledAt && (
                            <p>
                                <strong>Settled:</strong>{" "}
                                {new Date(fee.settledAt).toLocaleString()}
                            </p>
                        )}

                        <hr />
                    </div>
                ))
            )}
        </section>

        <section>
            <h2>Payment History</h2>

            {paymentsLoading ? (
                <p>Loading payment history...</p>
            ) : paymentsError ? (
                <p role="alert">{paymentsError}</p>
            ) : payments.length === 0 ? (
                <p>No payments found for this member.</p>
            ) : (
                payments.map((payment) => (
                    <div key={payment.paymentId}>
                        <p>
                            <strong>Total:</strong>{" "}
                            {payment.amount} {payment.currency}
                        </p>

                        <p>
                            <strong>Paid:</strong>{" "}
                            {new Date(payment.paidAt).toLocaleString()}
                        </p>

                        <p>
                            <strong>Fees paid:</strong>{" "}
                            {payment.allocations?.length || 0}
                        </p>

                        <hr />
                    </div>
                ))
            )}
        </section>

        <section>
            <h2>Active Borrowing Ban</h2>

            {banLoading ? (
                <p>Loading borrowing-ban information...</p>
            ) : banError ? (
                <p role="alert">{banError}</p>
            ) : borrowingBan?.active && borrowingBan.currentBan ? (
                <div>
                    <p>
                        <strong>Tier:</strong>{" "}
                        {borrowingBan.currentBan.tier}
                    </p>

                    <p>
                        <strong>Reason:</strong>{" "}
                        {borrowingBan.currentBan.reason}
                    </p>

                    <p>
                        <strong>Starts:</strong>{" "}
                        {new Date(
                            borrowingBan.currentBan.startsAt
                        ).toLocaleString()}
                    </p>

                    <p>
                        <strong>Ends:</strong>{" "}
                        {borrowingBan.currentBan.endsAt
                            ? new Date(
                                borrowingBan.currentBan.endsAt
                            ).toLocaleString()
                            : "Permanent"}
                    </p>
                </div>
            ) : (
                <p>No active borrowing ban.</p>
            )}
        </section>

        <section>
            <h2>Borrowing Ban History</h2>

            {banLoading ? (
                <p>Loading borrowing-ban history...</p>
            ) : banError ? (
                <p role="alert">{banError}</p>
            ) : historicBans.length === 0 ? (
                <p>No historic borrowing bans.</p>
            ) : (
                historicBans.map((ban) => (
                    <div key={ban.banId}>
                        <p>
                            <strong>Tier:</strong> {ban.tier}
                        </p>

                        <p>
                            <strong>Reason:</strong> {ban.reason}
                        </p>

                        <p>
                            <strong>Issued:</strong>{" "}
                            {new Date(ban.issuedAt).toLocaleString()}
                        </p>

                        <p>
                            <strong>Ended:</strong>{" "}
                            {ban.endsAt
                                ? new Date(ban.endsAt).toLocaleString()
                                : "Permanent"}
                        </p>

                        <hr />
                    </div>
                ))
            )}
        </section>

        {showPaymentQuoteModal && (
            <div
                style={{
                    position: "fixed",
                    inset: 0,
                    backgroundColor: "rgba(0, 0, 0, 0.5)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    zIndex: 1000,
                }}
                onClick={closePaymentQuoteModal}
            >
                <form
                    style={{
                        backgroundColor: "white",
                        padding: "24px",
                        borderRadius: "8px",
                        minWidth: "400px",
                        maxWidth: "90%",
                    }}
                    onClick={(event) => event.stopPropagation()}
                    onSubmit={handleQuotePayment}
                >
                    <h2>Pay Active Fees</h2>

                    <input
                        type="hidden"
                        name="memberId"
                        value={member.memberId}
                    />

                    {paymentQuoteError && (
                        <p role="alert">{paymentQuoteError}</p>
                    )}

                    {paymentQuote ? (
                        <div>
                            <p>Payment recorded successfully.</p>

                            <p>
                                <strong>Total:</strong>{" "}
                                {paymentQuote.amount} {paymentQuote.currency}
                            </p>

                            <p>
                                <strong>Payment ID:</strong>{" "}
                                {paymentQuote.paymentId}
                            </p>

                            <h3>Fee allocations</h3>

                            {paymentQuote.fees.map((fee) => (
                                <p key={fee.feeId}>
                                    {fee.amount} {paymentQuote.currency}
                                </p>
                            ))}
                        </div>
                    ) : (
                        <>
                            <p>Select the unpaid fees to pay.</p>

                            {activeFees.map((fee) => (
                                <div key={fee.feeId}>
                                    <label>
                                        <input
                                            type="checkbox"
                                            checked={selectedFeeIds.includes(fee.feeId)}
                                            onChange={() =>
                                                toggleFeeSelection(fee.feeId)
                                            }
                                            disabled={quotingPayment}
                                        />
                                        {" "}
                                        {fee.reason} — created {new Date(
                                            fee.createdAt
                                        ).toLocaleDateString()}
                                    </label>
                                </div>
                            ))}
                        </>
                    )}

                    <div>
                        <button
                            type="button"
                            onClick={closePaymentQuoteModal}
                            disabled={quotingPayment}
                        >
                            Close
                        </button>

                        {!paymentQuote && (
                            <button
                                type="submit"
                                disabled={quotingPayment}
                            >
                                {quotingPayment
                                    ? "Processing payment..."
                                    : "Pay Selected Fees"}
                            </button>
                        )}
                    </div>
                </form>
            </div>
        )}

        {showLoanModal && (
            <div
                style={{
                    position: "fixed",
                    inset: 0,
                    backgroundColor: "rgba(0, 0, 0, 0.5)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    zIndex: 1000,
                }}
                onClick={closeLoanModal}
            >
                <form
                    style={{
                        backgroundColor: "white",
                        padding: "24px",
                        borderRadius: "8px",
                        minWidth: "400px",
                        maxWidth: "90%",
                    }}
                    onClick={(event) => event.stopPropagation()}
                    onSubmit={handleCreateLoan}
                >
                    <h2>Add Loan</h2>

                    <input
                        type="hidden"
                        name="memberId"
                        value={member.memberId}
                    />

                    <input
                        type="hidden"
                        name="idempotencyKey"
                        value={loanIdempotencyKey}
                    />

                    {loanError && (
                        <p role="alert">{loanError}</p>
                    )}

                    {loanOptionsLoading ? (
                        <p>Loading libraries and books...</p>
                    ) : (
                        <>
                            <div>
                                <label htmlFor="loanLibrary">
                                    Library
                                </label>

                                <select
                                    id="loanLibrary"
                                    value={loanForm.libraryId}
                                    onChange={(event) =>
                                        setLoanForm((previous) => ({
                                            ...previous,
                                            libraryId: event.target.value,
                                        }))
                                    }
                                    disabled={creatingLoan}
                                    required
                                >
                                    <option value="">
                                        Select a library
                                    </option>

                                    {libraries.map((library) => (
                                        <option
                                            key={getResourceId(library)}
                                            value={getResourceId(library)}
                                        >
                                            {library.name}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label htmlFor="bookSearch">
                                    Search books
                                </label>

                                <input
                                    id="bookSearch"
                                    type="search"
                                    value={bookSearch}
                                    onChange={(event) =>
                                        setBookSearch(event.target.value)
                                    }
                                    placeholder="Title, author, or ISBN"
                                    disabled={creatingLoan}
                                />
                            </div>

                            <div>
                                <label htmlFor="loanBook">
                                    Book
                                </label>

                                <select
                                    id="loanBook"
                                    value={loanForm.bookId}
                                    onChange={(event) =>
                                        setLoanForm((previous) => ({
                                            ...previous,
                                            bookId: event.target.value,
                                        }))
                                    }
                                    disabled={creatingLoan}
                                    required
                                >
                                    <option value="">
                                        Select a book
                                    </option>

                                    {filteredBooks.map((book) => (
                                        <option
                                            key={getResourceId(book)}
                                            value={getResourceId(book)}
                                        >
                                            {book.title} — {book.author} ({book.isbn})
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </>
                    )}

                    <div>
                        <button
                            type="button"
                            onClick={closeLoanModal}
                            disabled={creatingLoan}
                        >
                            Cancel
                        </button>

                        <button
                            type="submit"
                            disabled={loanOptionsLoading || creatingLoan}
                        >
                            {creatingLoan ? "Adding..." : "Add Loan"}
                        </button>
                    </div>
                </form>
            </div>
        )}

        {/* -------------------------------- */}
        {/* Current Subscription */}
        {/* -------------------------------- */}

        <section>
            <h2>Current Subscription</h2>

            {member.currentSubscription ? (
                <div>
                    <p>
                        <strong>Tier:</strong>{" "}
                        {
                            member.currentSubscription
                                .tier
                        }
                    </p>

                    <p>
                        <strong>
                            Payment status:
                        </strong>{" "}
                        {
                            member.currentSubscription
                                .paymentStatus
                        }
                    </p>

                    <p>
                        <strong>Starts:</strong>{" "}
                        {new Date(
                            member.currentSubscription.startsAt
                        ).toLocaleString()}
                    </p>

                    <p>
                        <strong>Ends:</strong>{" "}
                        {new Date(
                            member.currentSubscription.endsAt
                        ).toLocaleString()}
                    </p>

                    <p>
                        <strong>Amount paid:</strong>{" "}
                        {
                            member.currentSubscription
                                .amountPaid
                        }{" "}
                        {
                            member.currentSubscription
                                .currency
                        }
                    </p>

                    <p>
                        <strong>Paid at:</strong>{" "}
                        {new Date(
                            member.currentSubscription.paidAt
                        ).toLocaleString()}
                    </p>

                    {member.currentSubscription
                        .paymentReference && (
                        <p>
                            <strong>
                                Payment reference:
                            </strong>{" "}
                            {
                                member
                                    .currentSubscription
                                    .paymentReference
                            }
                        </p>
                    )}
                </div>
            ) : (
                <p>No active subscription.</p>
            )}
        </section>

        {/* -------------------------------- */}
        {/* Subscription History */}
        {/* -------------------------------- */}

        <section>
            <h2>Subscription History</h2>

            {member.subscriptionHistory &&
            member.subscriptionHistory.length > 0 ? (
                member.subscriptionHistory.map(
                    (subscription) => (
                        <div
                            key={
                                subscription.subscriptionId
                            }
                        >
                            <p>
                                <strong>
                                    Tier:
                                </strong>{" "}
                                {subscription.tier}
                            </p>

                            <p>
                                <strong>
                                    Starts:
                                </strong>{" "}
                                {new Date(
                                    subscription.startsAt
                                ).toLocaleString()}
                            </p>

                            <p>
                                <strong>
                                    Ends:
                                </strong>{" "}
                                {new Date(
                                    subscription.endsAt
                                ).toLocaleString()}
                            </p>

                            <p>
                                <strong>
                                    Amount:
                                </strong>{" "}
                                {
                                    subscription.amountPaid
                                }{" "}
                                {
                                    subscription.currency
                                }
                            </p>

                            <hr />
                        </div>
                    )
                )
            ) : (
                <p>
                    No subscription history.
                </p>
            )}
        </section>

        {/* -------------------------------- */}
        {/* Subscription Actions */}
        {/* -------------------------------- */}

        <section>
            <h2>Subscription Actions</h2>

            <button
                type="button"
                onClick={openSubscriptionModal}
            >
                {isRenewal
                    ? "Renew Subscription"
                    : "Start Subscription"}
            </button>
        </section>

        {/* -------------------------------- */}
        {/* Subscription Modal */}
        {/* -------------------------------- */}

        {showSubscriptionModal && (
            <div
                style={{
                    position: "fixed",
                    inset: 0,
                    backgroundColor:
                        "rgba(0, 0, 0, 0.5)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    zIndex: 1000,
                }}
                onClick={closeSubscriptionModal}
            >
                <div
                    style={{
                        backgroundColor: "white",
                        padding: "24px",
                        borderRadius: "8px",
                        minWidth: "400px",
                        maxWidth: "90%",
                    }}
                    onClick={(e) =>
                        e.stopPropagation()
                    }
                >
                    <h2>
                        {isRenewal
                            ? "Renew Subscription"
                            : "Start Subscription"}
                    </h2>

                    {subscriptionError && (
                        <p role="alert">
                            {subscriptionError}
                        </p>
                    )}

                    {/* Tier */}

                    <div>
                        <label htmlFor="tier">
                            Tier
                        </label>

                        <select
                            id="tier"
                            name="tier"
                            value={
                                subscriptionForm.tier
                            }
                            onChange={
                                handleSubscriptionChange
                            }
                            disabled={
                                startingSubscription
                            }
                        >
                            <option value="THREE_MONTHS">
                                3 Months
                            </option>

                            <option value="SIX_MONTHS">
                                6 Months
                            </option>

                            <option value="TWELVE_MONTHS">
                                12 Months
                            </option>
                        </select>
                    </div>

                    {!isRenewal && (
                        <div>
                            <label htmlFor="startsAt">
                                Starts at
                            </label>

                            <input
                                id="startsAt"
                                name="startsAt"
                                type="datetime-local"
                                value={
                                    subscriptionForm.startsAt
                                }
                                onChange={
                                    handleSubscriptionChange
                                }
                                disabled={
                                    startingSubscription
                                }
                                required
                            />
                        </div>
                    )}

                    {/* Buttons */}

                    <div>
                        <button
                            type="button"
                            onClick={
                                closeSubscriptionModal
                            }
                            disabled={
                                startingSubscription
                            }
                        >
                            Cancel
                        </button>

                        <button
                            type="button"
                            onClick={
                                handleSubscriptionSubmit
                            }
                            disabled={
                                startingSubscription
                            }
                        >
                            {startingSubscription
                                ? isRenewal
                                    ? "Renewing..."
                                    : "Starting..."
                                : isRenewal
                                  ? "Renew Subscription"
                                  : "Start Subscription"}
                        </button>
                    </div>
                </div>
            </div>
        )}
    </div>
);
}

export default MemberDetailsPage;
