import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import keycloak from "../keycloak";
import styles from "../styles/MemberDetailsPage.module.css";

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
        return (
            <div className={styles.page}>
                <div className={styles.loadingWrap}>
                    Loading member...
                </div>
            </div>
        );
    }

    if (!member) {
        return (
            <div className={styles.page}>
                <div className={styles.emptyState}>
                    {error || "Member not found."}
                </div>

                <div style={{ textAlign: "center", marginTop: 20 }}>
                    <button
                        type="button"
                        className={styles.primaryButton}
                        onClick={() => navigate("/members")}
                    >
                        Back to Members
                    </button>
                </div>
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

    const historicBans =
        borrowingBan?.history?.filter(
            (ban) => ban.banId !== borrowingBan.currentBan?.banId
        ) || [];

    return (
        <div className={styles.page}>
            <button
                type="button"
                className={styles.backButton}
                onClick={() => navigate("/members")}
            >
                ← Back to Members
            </button>

            <div className={styles.pageHeader}>
                <h1 className={styles.pageTitle}>
                    {member.firstName} {member.lastName}
                </h1>

                <p className={styles.pageSubtitle}>
                    Membership #{member.membershipNumber}
                </p>
            </div>

            {error && <div className={styles.error}>{error}</div>}

            {/* -------------------------------- */}
            {/* Personal Information */}
            {/* -------------------------------- */}

            <section className={styles.section}>
                <div className={styles.sectionHeader}>
                    <h2 className={styles.sectionTitle}>
                        Personal Information
                    </h2>

                    {!editing && (
                        <button
                            type="button"
                            className={styles.secondaryButton}
                            onClick={handleEdit}
                        >
                            Edit
                        </button>
                    )}
                </div>

                {editing ? (
                    <>
                        <div className={styles.formGrid}>
                            <div className={styles.field}>
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

                            <div className={styles.field}>
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

                            <div className={styles.field}>
                                <label htmlFor="email">Email</label>
                                <input
                                    id="email"
                                    name="email"
                                    type="email"
                                    value={form.email}
                                    onChange={handleChange}
                                    required
                                />
                            </div>

                            <div className={styles.field}>
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
                        </div>

                        <div className={styles.formActions}>
                            <button
                                type="button"
                                className={styles.secondaryButton}
                                onClick={handleCancel}
                                disabled={saving}
                            >
                                Cancel
                            </button>

                            <button
                                type="button"
                                className={styles.primaryButton}
                                onClick={handleSave}
                                disabled={saving}
                            >
                                {saving ? "Saving..." : "Save Changes"}
                            </button>
                        </div>
                    </>
                ) : (
                    <div className={styles.infoGrid}>
                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Name
                            </span>
                            <span className={styles.infoValue}>
                                {member.firstName} {member.lastName}
                            </span>
                        </div>

                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Email
                            </span>
                            <span className={styles.infoValue}>
                                {member.email}
                            </span>
                        </div>

                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Phone
                            </span>
                            <span className={styles.infoValue}>
                                {member.phoneNumber}
                            </span>
                        </div>
                    </div>
                )}
            </section>

            {/* -------------------------------- */}
            {/* Membership Information */}
            {/* -------------------------------- */}

            <section className={styles.section}>
                <div className={styles.sectionHeader}>
                    <h2 className={styles.sectionTitle}>
                        Membership Information
                    </h2>

                    <span
                        className={`${styles.status} ${
                            member.active
                                ? styles.statusActive
                                : styles.statusInactive
                        }`}
                    >
                        {member.active ? "Active" : "Inactive"}
                    </span>
                </div>

                <div className={styles.infoGrid}>
                    <div className={styles.infoItem}>
                        <span className={styles.infoLabel}>
                            Membership number
                        </span>
                        <span className={styles.infoValue}>
                            {member.membershipNumber}
                        </span>
                    </div>

                    <div className={styles.infoItem}>
                        <span className={styles.infoLabel}>
                            Registered
                        </span>
                        <span className={styles.infoValue}>
                            {new Date(member.registeredAt).toLocaleString()}
                        </span>
                    </div>

                    {member.changedAt && (
                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Last changed
                            </span>
                            <span className={styles.infoValue}>
                                {new Date(member.changedAt).toLocaleString()}
                            </span>
                        </div>
                    )}
                </div>
            </section>

            {/* -------------------------------- */}
            {/* Loans */}
            {/* -------------------------------- */}

            <section className={styles.section}>
                <div className={styles.sectionHeader}>
                    <h2 className={styles.sectionTitle}>
                        Loans
                        <span className={styles.sectionCount}>
                            ({activeLoansCount} active)
                        </span>
                    </h2>

                    <button
                        type="button"
                        className={styles.primaryButton}
                        onClick={openLoanModal}
                    >
                        + Add Loan
                    </button>
                </div>

                {loansLoading ? (
                    <p className={styles.mutedText}>Loading loans...</p>
                ) : loansError ? (
                    <div className={styles.error} role="alert">
                        {loansError}
                    </div>
                ) : activeLoans.length === 0 ? (
                    <div className={styles.emptyState}>
                        No active loans for this member.
                    </div>
                ) : (
                    <div className={styles.list}>
                        {activeLoans.map((loan) => (
                            <div key={loan.loanId} className={styles.listItem}>
                                <div className={styles.listItemHeader}>
                                    <div>
                                        <h3 className={styles.listItemTitle}>
                                            {loanBookNames[loan.bookId] ||
                                                "Loading..."}
                                        </h3>
                                    </div>

                                    <div className={styles.listItemActions}>
                                        <button
                                            type="button"
                                            className={styles.secondaryButton}
                                            onClick={() =>
                                                navigate(`/loans/${loan.loanId}`)
                                            }
                                        >
                                            Details
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </section>

            {/* -------------------------------- */}
            {/* Loan History */}
            {/* -------------------------------- */}

            <section className={styles.section}>
                <div className={styles.sectionHeader}>
                    <h2 className={styles.sectionTitle}>
                        Loan History
                    </h2>
                </div>

                {loansLoading ? (
                    <p className={styles.mutedText}>
                        Loading loan history...
                    </p>
                ) : loansError ? (
                    <div className={styles.error} role="alert">
                        {loansError}
                    </div>
                ) : historicLoans.length === 0 ? (
                    <div className={styles.emptyState}>
                        No historic loans for this member.
                    </div>
                ) : (
                    <div className={styles.list}>
                        {historicLoans.map((loan) => (
                            <div key={loan.loanId} className={styles.listItem}>
                                <div className={styles.listItemHeader}>
                                    <h3 className={styles.listItemTitle}>
                                        {loanBookNames[loan.bookId] ||
                                            "Loading..."}
                                    </h3>

                                    <div className={styles.listItemActions}>
                                        <button
                                            type="button"
                                            className={styles.secondaryButton}
                                            onClick={() =>
                                                navigate(`/loans/${loan.loanId}`)
                                            }
                                        >
                                            Details
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </section>

            {/* -------------------------------- */}
            {/* Active Fees */}
            {/* -------------------------------- */}

            <section className={styles.section}>
                <div className={styles.sectionHeader}>
                    <h2 className={styles.sectionTitle}>
                        Active Fees
                        <span className={styles.sectionCount}>
                            ({activeFees.length})
                        </span>
                    </h2>

                    <button
                        type="button"
                        className={styles.primaryButton}
                        onClick={openPaymentQuoteModal}
                        disabled={activeFees.length === 0}
                    >
                        Pay Active Fees
                    </button>
                </div>

                {feesLoading ? (
                    <p className={styles.mutedText}>Loading fees...</p>
                ) : feesError ? (
                    <div className={styles.error} role="alert">
                        {feesError}
                    </div>
                ) : activeFees.length === 0 ? (
                    <div className={styles.emptyState}>
                        No active fees for this member.
                    </div>
                ) : (
                    <div className={styles.list}>
                        {activeFees.map((fee) => (
                            <div key={fee.feeId} className={styles.listItem}>
                                <div className={styles.infoGrid}>
                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Reason
                                        </span>
                                        <span className={styles.infoValue}>
                                            {fee.reason}
                                        </span>
                                    </div>

                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Created
                                        </span>
                                        <span className={styles.infoValue}>
                                            {new Date(
                                                fee.createdAt
                                            ).toLocaleString()}
                                        </span>
                                    </div>

                                    {fee.dueAt && (
                                        <div className={styles.infoItem}>
                                            <span
                                                className={styles.infoLabel}
                                            >
                                                Due
                                            </span>
                                            <span
                                                className={styles.infoValue}
                                            >
                                                {new Date(
                                                    fee.dueAt
                                                ).toLocaleString()}
                                            </span>
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </section>

            {/* -------------------------------- */}
            {/* Fee History */}
            {/* -------------------------------- */}

            <section className={styles.section}>
                <div className={styles.sectionHeader}>
                    <h2 className={styles.sectionTitle}>
                        Fee History
                    </h2>
                </div>

                {feesLoading ? (
                    <p className={styles.mutedText}>
                        Loading fee history...
                    </p>
                ) : feesError ? (
                    <div className={styles.error} role="alert">
                        {feesError}
                    </div>
                ) : historicFees.length === 0 ? (
                    <div className={styles.emptyState}>
                        No historic fees for this member.
                    </div>
                ) : (
                    <div className={styles.list}>
                        {historicFees.map((fee) => (
                            <div key={fee.feeId} className={styles.listItem}>
                                <div className={styles.infoGrid}>
                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Reason
                                        </span>
                                        <span className={styles.infoValue}>
                                            {fee.reason}
                                        </span>
                                    </div>

                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Created
                                        </span>
                                        <span className={styles.infoValue}>
                                            {new Date(
                                                fee.createdAt
                                            ).toLocaleString()}
                                        </span>
                                    </div>

                                    {fee.settledAt && (
                                        <div className={styles.infoItem}>
                                            <span
                                                className={styles.infoLabel}
                                            >
                                                Settled
                                            </span>
                                            <span
                                                className={styles.infoValue}
                                            >
                                                {new Date(
                                                    fee.settledAt
                                                ).toLocaleString()}
                                            </span>
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </section>

            {/* -------------------------------- */}
            {/* Payment History */}
            {/* -------------------------------- */}

            <section className={styles.section}>
                <div className={styles.sectionHeader}>
                    <h2 className={styles.sectionTitle}>
                        Payment History
                    </h2>
                </div>

                {paymentsLoading ? (
                    <p className={styles.mutedText}>
                        Loading payment history...
                    </p>
                ) : paymentsError ? (
                    <div className={styles.error} role="alert">
                        {paymentsError}
                    </div>
                ) : payments.length === 0 ? (
                    <div className={styles.emptyState}>
                        No payments found for this member.
                    </div>
                ) : (
                    <div className={styles.list}>
                        {payments.map((payment) => (
                            <div
                                key={payment.paymentId}
                                className={styles.listItem}
                            >
                                <div className={styles.infoGrid}>
                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Total
                                        </span>
                                        <span className={styles.infoValue}>
                                            {payment.amount}{" "}
                                            {payment.currency}
                                        </span>
                                    </div>

                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Paid
                                        </span>
                                        <span className={styles.infoValue}>
                                            {new Date(
                                                payment.paidAt
                                            ).toLocaleString()}
                                        </span>
                                    </div>

                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Fees paid
                                        </span>
                                        <span className={styles.infoValue}>
                                            {payment.allocations?.length || 0}
                                        </span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </section>

            {/* -------------------------------- */}
            {/* Active Borrowing Ban */}
            {/* -------------------------------- */}

            <section className={styles.section}>
                <div className={styles.sectionHeader}>
                    <h2 className={styles.sectionTitle}>
                        Active Borrowing Ban
                    </h2>

                    {borrowingBan?.active &&
                        borrowingBan.currentBan && (
                            <span
                                className={`${styles.status} ${styles.statusWarn}`}
                            >
                                Banned
                            </span>
                        )}
                </div>

                {banLoading ? (
                    <p className={styles.mutedText}>
                        Loading borrowing-ban information...
                    </p>
                ) : banError ? (
                    <div className={styles.error} role="alert">
                        {banError}
                    </div>
                ) : borrowingBan?.active &&
                borrowingBan.currentBan ? (
                    <div className={styles.infoGrid}>
                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Tier
                            </span>
                            <span className={styles.infoValue}>
                                {borrowingBan.currentBan.tier}
                            </span>
                        </div>

                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Reason
                            </span>
                            <span className={styles.infoValue}>
                                {borrowingBan.currentBan.reason}
                            </span>
                        </div>

                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Starts
                            </span>
                            <span className={styles.infoValue}>
                                {new Date(
                                    borrowingBan.currentBan.startsAt
                                ).toLocaleString()}
                            </span>
                        </div>

                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Ends
                            </span>
                            <span className={styles.infoValue}>
                                {borrowingBan.currentBan.endsAt
                                    ? new Date(
                                        borrowingBan.currentBan
                                            .endsAt
                                    ).toLocaleString()
                                    : "Permanent"}
                            </span>
                        </div>
                    </div>
                ) : (
                    <div className={styles.emptyState}>
                        No active borrowing ban.
                    </div>
                )}
            </section>

            {/* -------------------------------- */}
            {/* Borrowing Ban History */}
            {/* -------------------------------- */}

            <section className={styles.section}>
                <div className={styles.sectionHeader}>
                    <h2 className={styles.sectionTitle}>
                        Borrowing Ban History
                    </h2>
                </div>

                {banLoading ? (
                    <p className={styles.mutedText}>
                        Loading borrowing-ban history...
                    </p>
                ) : banError ? (
                    <div className={styles.error} role="alert">
                        {banError}
                    </div>
                ) : historicBans.length === 0 ? (
                    <div className={styles.emptyState}>
                        No historic borrowing bans.
                    </div>
                ) : (
                    <div className={styles.list}>
                        {historicBans.map((ban) => (
                            <div key={ban.banId} className={styles.listItem}>
                                <div className={styles.infoGrid}>
                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Tier
                                        </span>
                                        <span className={styles.infoValue}>
                                            {ban.tier}
                                        </span>
                                    </div>

                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Reason
                                        </span>
                                        <span className={styles.infoValue}>
                                            {ban.reason}
                                        </span>
                                    </div>

                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Issued
                                        </span>
                                        <span className={styles.infoValue}>
                                            {new Date(
                                                ban.issuedAt
                                            ).toLocaleString()}
                                        </span>
                                    </div>

                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Ended
                                        </span>
                                        <span className={styles.infoValue}>
                                            {ban.endsAt
                                                ? new Date(
                                                    ban.endsAt
                                                ).toLocaleString()
                                                : "Permanent"}
                                        </span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </section>

            {/* -------------------------------- */}
            {/* Current Subscription */}
            {/* -------------------------------- */}

            <section className={styles.section}>
                <div className={styles.sectionHeader}>
                    <h2 className={styles.sectionTitle}>
                        Current Subscription
                    </h2>

                    {member.currentSubscription && (
                        <span
                            className={`${styles.status} ${styles.statusInfo}`}
                        >
                            {member.currentSubscription.paymentStatus ||
                                "Unknown"}
                        </span>
                    )}
                </div>

                {member.currentSubscription ? (
                    <div className={styles.infoGrid}>
                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Tier
                            </span>
                            <span className={styles.infoValue}>
                                {member.currentSubscription.tier}
                            </span>
                        </div>

                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Starts
                            </span>
                            <span className={styles.infoValue}>
                                {new Date(
                                    member.currentSubscription.startsAt
                                ).toLocaleString()}
                            </span>
                        </div>

                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Ends
                            </span>
                            <span className={styles.infoValue}>
                                {new Date(
                                    member.currentSubscription.endsAt
                                ).toLocaleString()}
                            </span>
                        </div>

                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Amount paid
                            </span>
                            <span className={styles.infoValue}>
                                {member.currentSubscription.amountPaid}{" "}
                                {member.currentSubscription.currency}
                            </span>
                        </div>

                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Paid at
                            </span>
                            <span className={styles.infoValue}>
                                {new Date(
                                    member.currentSubscription.paidAt
                                ).toLocaleString()}
                            </span>
                        </div>

                        {member.currentSubscription.paymentReference && (
                            <div className={styles.infoItem}>
                                <span className={styles.infoLabel}>
                                    Payment reference
                                </span>
                                <span
                                    className={`${styles.infoValue} ${styles.infoValueMono}`}
                                >
                                    {
                                        member.currentSubscription
                                            .paymentReference
                                    }
                                </span>
                            </div>
                        )}
                    </div>
                ) : (
                    <div className={styles.emptyState}>
                        No active subscription.
                    </div>
                )}
            </section>

            {/* -------------------------------- */}
            {/* Subscription History */}
            {/* -------------------------------- */}

            <section className={styles.section}>
                <div className={styles.sectionHeader}>
                    <h2 className={styles.sectionTitle}>
                        Subscription History
                    </h2>
                </div>

                {member.subscriptionHistory &&
                member.subscriptionHistory.length > 0 ? (
                    <div className={styles.list}>
                        {member.subscriptionHistory.map((subscription) => (
                            <div
                                key={subscription.subscriptionId}
                                className={styles.listItem}
                            >
                                <div className={styles.infoGrid}>
                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Tier
                                        </span>
                                        <span className={styles.infoValue}>
                                            {subscription.tier}
                                        </span>
                                    </div>

                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Starts
                                        </span>
                                        <span className={styles.infoValue}>
                                            {new Date(
                                                subscription.startsAt
                                            ).toLocaleString()}
                                        </span>
                                    </div>

                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Ends
                                        </span>
                                        <span className={styles.infoValue}>
                                            {new Date(
                                                subscription.endsAt
                                            ).toLocaleString()}
                                        </span>
                                    </div>

                                    <div className={styles.infoItem}>
                                        <span className={styles.infoLabel}>
                                            Amount
                                        </span>
                                        <span className={styles.infoValue}>
                                            {subscription.amountPaid}{" "}
                                            {subscription.currency}
                                        </span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className={styles.emptyState}>
                        No subscription history.
                    </div>
                )}
            </section>

            {/* -------------------------------- */}
            {/* Subscription Actions */}
            {/* -------------------------------- */}

            <section className={styles.section}>
                <div className={styles.sectionHeader}>
                    <h2 className={styles.sectionTitle}>
                        Subscription Actions
                    </h2>

                    <button
                        type="button"
                        className={styles.primaryButton}
                        onClick={openSubscriptionModal}
                    >
                        {isRenewal
                            ? "Renew Subscription"
                            : "Start Subscription"}
                    </button>
                </div>
            </section>

            {/* ============================= */}
            {/* PAYMENT QUOTE MODAL */}
            {/* ============================= */}

            {showPaymentQuoteModal && (
                <div
                    className={styles.modalOverlay}
                    onClick={closePaymentQuoteModal}
                >
                    <form
                        className={styles.modal}
                        onClick={(event) => event.stopPropagation()}
                        onSubmit={handleQuotePayment}
                    >
                        <h2 className={styles.modalTitle}>
                            Pay Active Fees
                        </h2>

                        {paymentQuoteError && (
                            <div className={styles.modalError} role="alert">
                                {paymentQuoteError}
                            </div>
                        )}

                        {paymentQuote ? (
                            <div className={styles.successBox}>
                                <p>
                                    <strong>
                                        Payment recorded successfully.
                                    </strong>
                                </p>
                                <p>
                                    Total: {paymentQuote.amount}{" "}
                                    {paymentQuote.currency}
                                </p>
                                <p>
                                    Payment ID: {paymentQuote.paymentId}
                                </p>
                            </div>
                        ) : (
                            <>
                                <p className={styles.mutedText}>
                                    Select the unpaid fees to pay.
                                </p>

                                <div className={styles.checkList}>
                                    {activeFees.map((fee) => (
                                        <label
                                            key={fee.feeId}
                                            className={styles.checkItem}
                                        >
                                            <input
                                                type="checkbox"
                                                checked={selectedFeeIds.includes(
                                                    fee.feeId
                                                )}
                                                onChange={() =>
                                                    toggleFeeSelection(
                                                        fee.feeId
                                                    )
                                                }
                                                disabled={quotingPayment}
                                            />
                                            <span>
                                                <strong>{fee.reason}</strong>
                                                <br />
                                                <span
                                                    className={
                                                        styles.infoValueMono
                                                    }
                                                >
                                                    Created{" "}
                                                    {new Date(
                                                        fee.createdAt
                                                    ).toLocaleDateString()}
                                                </span>
                                            </span>
                                        </label>
                                    ))}
                                </div>
                            </>
                        )}

                        <div className={styles.modalActions}>
                            <button
                                type="button"
                                className={styles.secondaryButton}
                                onClick={closePaymentQuoteModal}
                                disabled={quotingPayment}
                            >
                                Close
                            </button>

                            {!paymentQuote && (
                                <button
                                    type="submit"
                                    className={styles.primaryButton}
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

            {/* ============================= */}
            {/* LOAN MODAL */}
            {/* ============================= */}

            {showLoanModal && (
                <div
                    className={styles.modalOverlay}
                    onClick={closeLoanModal}
                >
                    <form
                        className={styles.modal}
                        onClick={(event) => event.stopPropagation()}
                        onSubmit={handleCreateLoan}
                    >
                        <h2 className={styles.modalTitle}>Add Loan</h2>

                        {loanError && (
                            <div className={styles.modalError} role="alert">
                                {loanError}
                            </div>
                        )}

                        {loanOptionsLoading ? (
                            <p className={styles.mutedText}>
                                Loading libraries and books...
                            </p>
                        ) : (
                            <>
                                <div className={styles.field}>
                                    <label htmlFor="loanLibrary">
                                        Library
                                    </label>
                                    <select
                                        id="loanLibrary"
                                        value={loanForm.libraryId}
                                        onChange={(event) =>
                                            setLoanForm((previous) => ({
                                                ...previous,
                                                libraryId:
                                                event.target.value,
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

                                <div className={styles.field}>
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

                                <div className={styles.field}>
                                    <label htmlFor="loanBook">Book</label>
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
                                                {book.title} — {book.author} (
                                                {book.isbn})
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            </>
                        )}

                        <div className={styles.modalActions}>
                            <button
                                type="button"
                                className={styles.secondaryButton}
                                onClick={closeLoanModal}
                                disabled={creatingLoan}
                            >
                                Cancel
                            </button>

                            <button
                                type="submit"
                                className={styles.primaryButton}
                                disabled={
                                    loanOptionsLoading || creatingLoan
                                }
                            >
                                {creatingLoan ? "Adding..." : "Add Loan"}
                            </button>
                        </div>
                    </form>
                </div>
            )}

            {/* ============================= */}
            {/* SUBSCRIPTION MODAL */}
            {/* ============================= */}

            {showSubscriptionModal && (
                <div
                    className={styles.modalOverlay}
                    onClick={closeSubscriptionModal}
                >
                    <div
                        className={styles.modal}
                        onClick={(e) => e.stopPropagation()}
                    >
                        <h2 className={styles.modalTitle}>
                            {isRenewal
                                ? "Renew Subscription"
                                : "Start Subscription"}
                        </h2>

                        {subscriptionError && (
                            <div
                                className={styles.modalError}
                                role="alert"
                            >
                                {subscriptionError}
                            </div>
                        )}

                        <div className={styles.field}>
                            <label htmlFor="tier">Tier</label>
                            <select
                                id="tier"
                                name="tier"
                                value={subscriptionForm.tier}
                                onChange={handleSubscriptionChange}
                                disabled={startingSubscription}
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
                            <div className={styles.field}>
                                <label htmlFor="startsAt">
                                    Starts at
                                </label>
                                <input
                                    id="startsAt"
                                    name="startsAt"
                                    type="datetime-local"
                                    value={subscriptionForm.startsAt}
                                    onChange={handleSubscriptionChange}
                                    disabled={startingSubscription}
                                    required
                                />
                            </div>
                        )}

                        <div className={styles.modalActions}>
                            <button
                                type="button"
                                className={styles.secondaryButton}
                                onClick={closeSubscriptionModal}
                                disabled={startingSubscription}
                            >
                                Cancel
                            </button>

                            <button
                                type="button"
                                className={styles.primaryButton}
                                onClick={handleSubscriptionSubmit}
                                disabled={startingSubscription}
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