import { useEffect, useState } from "react";
import { useNavigate, useParams, Link } from "react-router-dom";
import styles from "../styles/LoanDetailsPage.module.css";
import keycloak from "../keycloak";

function LoanDetailsPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [loan, setLoan] = useState(null);
    const [book, setBook] = useState(null);
    const [library, setLibrary] = useState(null);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [loanAction, setLoanAction] = useState("");
    const [actionError, setActionError] = useState("");

    const API = "http://localhost:8000";

    useEffect(() => {
        const loadLoan = async () => {
            try {
                setLoading(true);
                setError("");

                const headers = {
                    Authorization: `Bearer ${keycloak.token}`,
                };

                // Get loan
                const loanResponse = await fetch(
                    `${API}/api/loans/${id}`,
                    { headers }
                );

                if (!loanResponse.ok) {
                    throw new Error("Loan not found.");
                }

                const loanData = await loanResponse.json();
                setLoan(loanData);

                // Get book and library at the same time
                const [bookResponse, libraryResponse] = await Promise.all([
                    fetch(
                        `${API}/api/books/${loanData.bookId}`,
                        { headers }
                    ),
                    fetch(
                        `${API}/api/libraries/${loanData.libraryId}`,
                        { headers }
                    ),
                ]);

                if (bookResponse.ok) {
                    const bookData = await bookResponse.json();
                    setBook(bookData);
                }

                if (libraryResponse.ok) {
                    const libraryData = await libraryResponse.json();
                    setLibrary(libraryData);
                }
            } catch (err) {
                console.error(err);
                setError(err.message || "Failed to load loan.");
            } finally {
                setLoading(false);
            }
        };

        loadLoan();
    }, [id]);

    const handleReturn = async () => {
        try {
            setLoanAction("return");
            setActionError("");

            const response = await fetch(
                `${API}/api/loans/${id}/return`,
                {
                    method: "POST",
                    headers: {
                        Authorization: `Bearer ${keycloak.token}`,
                    },
                }
            );

            if (!response.ok) {
                const message = await response.text();
                throw new Error(message || "Failed to return book.");
            }

            for (let attempt = 0; attempt < 5; attempt++) {
                await new Promise((resolve) =>
                    setTimeout(resolve, 300)
                );

                const loanResponse = await fetch(
                    `${API}/api/loans/${id}`,
                    {
                        headers: {
                            Authorization: `Bearer ${keycloak.token}`,
                        },
                    }
                );

                if (!loanResponse.ok) {
                    continue;
                }

                const updatedLoan = await loanResponse.json();
                setLoan(updatedLoan);

                if (updatedLoan.status === "RETURNED") {
                    break;
                }
            }
        } catch (err) {
            console.error(err);
            setActionError(
                err.message || "Failed to return book."
            );
        } finally {
            setLoanAction("");
        }
    };

    const handleIncident = async (action, expectedStatus) => {
        try {
            setLoanAction(action);
            setActionError("");

            const response = await fetch(
                `${API}/api/loans/${id}/${action}`,
                {
                    method: "POST",
                    headers: {
                        Authorization: `Bearer ${keycloak.token}`,
                    },
                }
            );

            if (!response.ok) {
                const message = await response.text();
                throw new Error(
                    message || `Failed to record ${action}.`
                );
            }

            for (let attempt = 0; attempt < 5; attempt++) {
                await new Promise((resolve) =>
                    setTimeout(resolve, 300)
                );

                const loanResponse = await fetch(
                    `${API}/api/loans/${id}`,
                    {
                        headers: {
                            Authorization: `Bearer ${keycloak.token}`,
                        },
                    }
                );

                if (!loanResponse.ok) {
                    continue;
                }

                const updatedLoan = await loanResponse.json();
                setLoan(updatedLoan);

                if (updatedLoan.status === expectedStatus) {
                    break;
                }
            }
        } catch (err) {
            console.error(err);
            setActionError(
                err.message || `Failed to record ${action}.`
            );
        } finally {
            setLoanAction("");
        }
    };

    const handleExtend = async () => {
        try {
            setLoanAction("extend");
            setActionError("");

            const previousExtendedAt = loan.extendedAt;

            const response = await fetch(
                `${API}/api/loans/${id}/extend`,
                {
                    method: "POST",
                    headers: {
                        Authorization: `Bearer ${keycloak.token}`,
                    },
                }
            );

            if (!response.ok) {
                const message = await response.text();
                throw new Error(
                    message || "Failed to extend loan."
                );
            }

            for (let attempt = 0; attempt < 5; attempt++) {
                await new Promise((resolve) =>
                    setTimeout(resolve, 300)
                );

                const loanResponse = await fetch(
                    `${API}/api/loans/${id}`,
                    {
                        headers: {
                            Authorization: `Bearer ${keycloak.token}`,
                        },
                    }
                );

                if (!loanResponse.ok) {
                    continue;
                }

                const updatedLoan = await loanResponse.json();
                setLoan(updatedLoan);

                if (updatedLoan.extendedAt !== previousExtendedAt) {
                    break;
                }
            }
        } catch (err) {
            console.error(err);
            setActionError(
                err.message || "Failed to extend loan."
            );
        } finally {
            setLoanAction("");
        }
    };

    if (loading) {
        return (
            <div className={styles.page}>
                <div className={styles.loadingWrap}>
                    Loading loan...
                </div>
            </div>
        );
    }

    if (!loan) {
        return (
            <div className={styles.page}>
                <div className={styles.emptyState}>
                    {error || "Loan not found."}
                </div>

                <div style={{ textAlign: "center" }}>
                    <button
                        type="button"
                        className={styles.primaryButton}
                        onClick={() => navigate(-1)}
                    >
                        ← Back
                    </button>
                </div>
            </div>
        );
    }

    const getStatusClass = (status) => {
        switch ((status ?? "").toUpperCase()) {
            case "ACTIVE":
                return styles.statusActive;
            case "RETURNED":
                return styles.statusReturned;
            case "LOST":
                return styles.statusLost;
            case "DAMAGED":
                return styles.statusDamaged;
            default:
                return styles.statusNeutral;
        }
    };

    const prettyStatus = (status) => {
        if (!status) return "Unknown";

        return String(status)
            .toLowerCase()
            .split("_")
            .map(
                (w) =>
                    w.charAt(0).toUpperCase() + w.slice(1)
            )
            .join(" ");
    };

    return (
        <div className={styles.page}>
            <div className={styles.headerRow}>
                <button
                    type="button"
                    className={styles.backButton}
                    onClick={() => navigate(-1)}
                >
                    ← Back
                </button>

                <div className={styles.pageHeader}>
                    <div className={styles.pageTitleRow}>
                        <div>
                            <h1 className={styles.pageTitle}>
                                Loan Details
                            </h1>

                            <p className={styles.pageSubtitle}>
                                {book?.title || "Unknown book"}
                            </p>
                        </div>

                        <span
                            className={`${styles.status} ${getStatusClass(
                                loan.status
                            )}`}
                        >
                {prettyStatus(loan.status)}
            </span>
                    </div>
                </div>
            </div>

            {loan.status === "ACTIVE" && (
                <section className={styles.card}>
                    <div className={styles.cardHeader}>
                        <h2 className={styles.cardTitle}>
                            Actions
                        </h2>
                    </div>

                    {actionError && (
                        <div
                            className={styles.error}
                            role="alert"
                        >
                            {actionError}
                        </div>
                    )}

                    <div className={styles.actionsGrid}>
                        <button
                            type="button"
                            className={styles.primaryButton}
                            onClick={handleReturn}
                            disabled={Boolean(loanAction)}
                        >
                            {loanAction === "return"
                                ? "Returning..."
                                : "Return Book"}
                        </button>

                        <button
                            type="button"
                            className={styles.secondaryButton}
                            onClick={handleExtend}
                            disabled={Boolean(loanAction)}
                        >
                            {loanAction === "extend"
                                ? "Extending..."
                                : "Extend Loan"}
                        </button>

                        <button
                            type="button"
                            className={styles.dangerButton}
                            onClick={() =>
                                handleIncident(
                                    "lost",
                                    "LOST"
                                )
                            }
                            disabled={Boolean(loanAction)}
                        >
                            {loanAction === "lost"
                                ? "Recording..."
                                : "Mark as Lost"}
                        </button>

                        <button
                            type="button"
                            className={styles.warnButton}
                            onClick={() =>
                                handleIncident(
                                    "damage",
                                    "DAMAGED"
                                )
                            }
                            disabled={Boolean(loanAction)}
                        >
                            {loanAction === "damage"
                                ? "Recording..."
                                : "Record Damage"}
                        </button>
                    </div>
                </section>
            )}

            <section className={styles.card}>
                <div className={styles.cardHeader}>
                    <h2 className={styles.cardTitle}>
                        Loan Information
                    </h2>
                </div>

                <div className={styles.infoGrid}>
                    <div className={styles.infoItem}>
                        <span className={styles.infoLabel}>
                            Book
                        </span>

                        {book ? (
                            <Link
                                to={`/books/${loan.bookId}`}
                                className={styles.bookLink}
                            >
                                {book.title}
                            </Link>
                        ) : (
                            <span className={styles.infoValue}>
                                Unknown book
                            </span>
                        )}
                    </div>

                    <div className={styles.infoItem}>
                        <span className={styles.infoLabel}>
                            Library
                        </span>

                        <span className={styles.infoValue}>
                            {library?.name || "Unknown library"}
                        </span>
                    </div>

                    <div className={styles.infoItem}>
                        <span className={styles.infoLabel}>
                            Borrowed
                        </span>

                        <span className={styles.infoValue}>
                            {new Date(
                                loan.borrowedAt
                            ).toLocaleString()}
                        </span>
                    </div>

                    <div className={styles.infoItem}>
                        <span className={styles.infoLabel}>
                            Due
                        </span>

                        <span className={styles.infoValue}>
                            {new Date(
                                loan.dueAt
                            ).toLocaleString()}
                        </span>
                    </div>

                    <div className={styles.infoItem}>
                        <span className={styles.infoLabel}>
                            Extended
                        </span>

                        <span
                            className={
                                loan.extendedAt
                                    ? styles.infoValue
                                    : `${styles.infoValue} ${styles.infoValueMuted}`
                            }
                        >
                            {loan.extendedAt
                                ? new Date(
                                    loan.extendedAt
                                ).toLocaleString()
                                : "Not extended"}
                        </span>
                    </div>

                    <div className={styles.infoItem}>
                        <span className={styles.infoLabel}>
                            Returned
                        </span>

                        <span
                            className={
                                loan.returnedAt
                                    ? styles.infoValue
                                    : `${styles.infoValue} ${styles.infoValueMuted}`
                            }
                        >
                            {loan.returnedAt
                                ? new Date(
                                    loan.returnedAt
                                ).toLocaleString()
                                : "Not returned"}
                        </span>
                    </div>

                    {loan.incidentDeclaredAt && (
                        <div className={styles.infoItem}>
                            <span className={styles.infoLabel}>
                                Incident declared
                            </span>

                            <span className={styles.infoValue}>
                                {new Date(
                                    loan.incidentDeclaredAt
                                ).toLocaleString()}
                            </span>
                        </div>
                    )}
                </div>
            </section>
        </div>
    );
}

export default LoanDetailsPage;