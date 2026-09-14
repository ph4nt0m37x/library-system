import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import keycloak from "../keycloak";

function LoanDetailsPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [loan, setLoan] = useState(null);
    const [bookName, setBookName] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [loanAction, setLoanAction] = useState("");
    const [actionError, setActionError] = useState("");

    useEffect(() => {
        const loadLoan = async () => {
            try {
                setLoading(true);
                setError("");

                const loanResponse = await fetch(
                    `http://localhost:8000/api/loans/${id}`,
                    {
                        headers: {
                            Authorization: `Bearer ${keycloak.token}`,
                        },
                    }
                );

                if (!loanResponse.ok) {
                    throw new Error("Loan not found.");
                }

                const loanData = await loanResponse.json();
                setLoan(loanData);

                const bookResponse = await fetch(
                    `http://localhost:8000/api/books/${loanData.bookId}`,
                    {
                        headers: {
                            Authorization: `Bearer ${keycloak.token}`,
                        },
                    }
                );

                if (bookResponse.ok) {
                    const bookData = await bookResponse.json();
                    setBookName(bookData.title);
                } else {
                    setBookName("Unknown book");
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

    if (loading) {
        return <p>Loading loan...</p>;
    }

    if (!loan) {
        return (
            <div>
                <p>{error || "Loan not found."}</p>
                <button type="button" onClick={() => navigate(-1)}>
                    Back
                </button>
            </div>
        );
    }

    const handleReturn = async () => {
        try {
            setLoanAction("return");
            setActionError("");

            const response = await fetch(
                `http://localhost:8000/api/loans/${id}/return`,
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
                await new Promise((resolve) => setTimeout(resolve, 300));

                const loanResponse = await fetch(
                    `http://localhost:8000/api/loans/${id}`,
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
            setActionError(err.message || "Failed to return book.");
        } finally {
            setLoanAction("");
        }
    };

    const handleIncident = async (action, expectedStatus) => {
        try {
            setLoanAction(action);
            setActionError("");

            const response = await fetch(
                `http://localhost:8000/api/loans/${id}/${action}`,
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
                await new Promise((resolve) => setTimeout(resolve, 300));

                const loanResponse = await fetch(
                    `http://localhost:8000/api/loans/${id}`,
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
                `http://localhost:8000/api/loans/${id}/extend`,
                {
                    method: "POST",
                    headers: {
                        Authorization: `Bearer ${keycloak.token}`,
                    },
                }
            );

            if (!response.ok) {
                const message = await response.text();
                throw new Error(message || "Failed to extend loan.");
            }

            for (let attempt = 0; attempt < 5; attempt++) {
                await new Promise((resolve) => setTimeout(resolve, 300));

                const loanResponse = await fetch(
                    `http://localhost:8000/api/loans/${id}`,
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
            setActionError(err.message || "Failed to extend loan.");
        } finally {
            setLoanAction("");
        }
    };

    return (
        <div>
            <button type="button" onClick={() => navigate(-1)}>
                Back
            </button>

            <h1>Loan Details</h1>

            <p>
                <strong>Book:</strong> {bookName || "Loading..."}
            </p>

            <p>
                <strong>Status:</strong> {loan.status}
            </p>

            {loan.status === "ACTIVE" && (
                <div>
                    {actionError && <p role="alert">{actionError}</p>}

                    <button
                        type="button"
                        onClick={handleReturn}
                        disabled={Boolean(loanAction)}
                    >
                        {loanAction === "return"
                            ? "Returning..."
                            : "Return Book"}
                    </button>

                    <button
                        type="button"
                        onClick={handleExtend}
                        disabled={Boolean(loanAction)}
                    >
                        {loanAction === "extend"
                            ? "Extending..."
                            : "Extend Loan"}
                    </button>

                    <button
                        type="button"
                        onClick={() => handleIncident("lost", "LOST")}
                        disabled={Boolean(loanAction)}
                    >
                        {loanAction === "lost"
                            ? "Recording..."
                            : "Mark as Lost"}
                    </button>

                    <button
                        type="button"
                        onClick={() => handleIncident("damage", "DAMAGED")}
                        disabled={Boolean(loanAction)}
                    >
                        {loanAction === "damage"
                            ? "Recording..."
                            : "Record Damage"}
                    </button>
                </div>
            )}

            <p>
                <strong>Library ID:</strong> {loan.libraryId || "Not specified"}
            </p>

            <p>
                <strong>Borrowed:</strong>{" "}
                {new Date(loan.borrowedAt).toLocaleString()}
            </p>

            <p>
                <strong>Due:</strong> {new Date(loan.dueAt).toLocaleString()}
            </p>

            <p>
                <strong>Extended:</strong>{" "}
                {loan.extendedAt
                    ? new Date(loan.extendedAt).toLocaleString()
                    : "Not extended"}
            </p>

            <p>
                <strong>Returned:</strong>{" "}
                {loan.returnedAt
                    ? new Date(loan.returnedAt).toLocaleString()
                    : "Not returned"}
            </p>

            {loan.incidentDeclaredAt && (
                <p>
                    <strong>Incident declared:</strong>{" "}
                    {new Date(loan.incidentDeclaredAt).toLocaleString()}
                </p>
            )}
        </div>
    );
}

export default LoanDetailsPage;
