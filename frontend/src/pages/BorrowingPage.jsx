import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getAuthToken } from "../keycloak";
import styles from "../styles/BorrowingPage.module.css";

const API = "http://localhost:8000";

const getId = (value) => {
    if (!value) return null;

    if (typeof value === "string") {
        return value;
    }

    return value.value ?? value.id ?? null;
};

const formatDateTime = (value) => {
    if (!value) return "-";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat(undefined, {
        dateStyle: "medium",
        timeStyle: "short",
    }).format(date);
};

const isOverdue = (loan) => {
    return (
        loan.status === "ACTIVE" &&
        loan.dueAt &&
        new Date(loan.dueAt).getTime() < Date.now()
    );
};

function BorrowingPage() {
    const [loans, setLoans] = useState([]);
    const [members, setMembers] = useState({});
    const [books, setBooks] = useState({});
    const [libraries, setLibraries] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [statusFilter, setStatusFilter] = useState("ACTIVE");
    const [libraryFilter, setLibraryFilter] = useState("");
    const [memberFilter, setMemberFilter] = useState("");
    const [borrowedFrom, setBorrowedFrom] = useState("");
    const [borrowedTo, setBorrowedTo] = useState("");

    useEffect(() => {
        const loadLoans = async () => {
            try {
                const headers = {
                    Authorization: `Bearer ${getAuthToken()}`,
                };

                const [
                    loansResponse,
                    membersResponse,
                    booksResponse,
                    librariesResponse,
                ] = await Promise.all([
                    fetch(`${API}/api/loans/all`, { headers }),
                    fetch(`${API}/api/members/all`, { headers }),
                    fetch(`${API}/api/books/available`, { headers }),
                    fetch(`${API}/api/libraries/available`, { headers }),
                ]);

                if (!loansResponse.ok) {
                    throw new Error(`Failed to fetch loans: ${loansResponse.status}`);
                }

                if (!membersResponse.ok) {
                    throw new Error(`Failed to fetch members: ${membersResponse.status}`);
                }

                if (!booksResponse.ok) {
                    throw new Error(`Failed to fetch books: ${booksResponse.status}`);
                }

                if (!librariesResponse.ok) {
                    throw new Error(
                        `Failed to fetch libraries: ${librariesResponse.status}`
                    );
                }

                const [
                    loansData,
                    membersData,
                    booksData,
                    librariesData,
                ] = await Promise.all([
                    loansResponse.json(),
                    membersResponse.json(),
                    booksResponse.json(),
                    librariesResponse.json(),
                ]);

                setLoans(loansData);
                setMembers(
                    Object.fromEntries(
                        membersData.map((member) => [member.memberId, member])
                    )
                );
                setBooks(
                    Object.fromEntries(
                        booksData
                            .map((book) => [getId(book.id), book])
                            .filter(([id]) => id)
                    )
                );
                setLibraries(
                    Object.fromEntries(
                        librariesData
                            .map((library) => [getId(library.id), library])
                            .filter(([id]) => id)
                    )
                );
            } catch (error) {
                console.error(error);
                setError("Could not load borrowing information.");
            } finally {
                setLoading(false);
            }
        };

        loadLoans();
    }, []);

    const memberLabel = (memberId) => {
        const member = members[memberId];

        if (!member) {
            return memberId;
        }

        const name = [member.firstName, member.lastName].filter(Boolean).join(" ");
        const number = member.membershipNumber ? `#${member.membershipNumber}` : "";

        return [name, number].filter(Boolean).join(" ");
    };

    const statuses = useMemo(() => {
        return ["OVERDUE", ...Array.from(new Set(loans.map((loan) => loan.status).filter(Boolean)))]
            .sort();
    }, [loans]);

    const libraryOptions = useMemo(() => {
        return Object.entries(libraries)
            .map(([id, library]) => ({
                id,
                name: library.name ?? id,
            }))
            .sort((a, b) => a.name.localeCompare(b.name));
    }, [libraries]);

    const filteredLoans = useMemo(() => {
        const memberSearch = memberFilter.trim().toLowerCase();
        const fromTime = borrowedFrom
            ? new Date(`${borrowedFrom}T00:00:00`).getTime()
            : null;
        const toTime = borrowedTo
            ? new Date(`${borrowedTo}T23:59:59.999`).getTime()
            : null;

        return loans
            .filter((loan) => {
                const borrowedTime = loan.borrowedAt
                    ? new Date(loan.borrowedAt).getTime()
                    : null;

                return (
                    (!statusFilter ||
                        (statusFilter === "OVERDUE"
                            ? isOverdue(loan)
                            : loan.status === statusFilter)) &&
                    (!libraryFilter || loan.libraryId === libraryFilter) &&
                    (!memberSearch || (() => {
                        const member = members[loan.memberId];
                        const searchable = [
                            member?.firstName,
                            member?.lastName,
                            member?.membershipNumber,
                        ]
                            .filter(Boolean)
                            .join(" ")
                            .toLowerCase();

                        return searchable.includes(memberSearch);
                    })()) &&
                    (!fromTime || (borrowedTime && borrowedTime >= fromTime)) &&
                    (!toTime || (borrowedTime && borrowedTime <= toTime))
                );
            })
            .sort((a, b) => {
                return (
                    new Date(b.borrowedAt).getTime() -
                    new Date(a.borrowedAt).getTime()
                );
            });
    }, [
        borrowedFrom,
        borrowedTo,
        libraryFilter,
        loans,
        members,
        memberFilter,
        statusFilter,
    ]);

    const activeCount = loans.filter((loan) => loan.status === "ACTIVE").length;
    const overdueCount = loans.filter(isOverdue).length;

    const clearFilters = () => {
        setStatusFilter("");
        setLibraryFilter("");
        setMemberFilter("");
        setBorrowedFrom("");
        setBorrowedTo("");
    };

    if (loading) {
        return (
            <div className={styles.page}>
                <div className={styles.emptyState}>Loading borrowing information...</div>
            </div>
        );
    }

    return (
        <div className={styles.page}>
            <div className={styles.header}>
                <div>
                    <h1>Loans</h1>
                    <p>Review loans by status, library, member, and borrowed date.</p>
                </div>

                <div className={styles.summary}>
                    <span>{loans.length} total</span>
                    <span>{activeCount} active</span>
                    <span>{overdueCount} overdue</span>
                </div>
            </div>

            {error && <div className={styles.error}>{error}</div>}

            <section className={styles.filters}>
                <label className={styles.filterField}>
                    <span>Status</span>
                    <select
                        value={statusFilter}
                        onChange={(event) => setStatusFilter(event.target.value)}
                    >
                        <option value="">All statuses</option>
                        {statuses.map((status) => (
                            <option key={status} value={status}>
                                {status}
                            </option>
                        ))}
                    </select>
                </label>

                <label className={styles.filterField}>
                    <span>Library</span>
                    <select
                        value={libraryFilter}
                        onChange={(event) => setLibraryFilter(event.target.value)}
                    >
                        <option value="">All libraries</option>
                        {libraryOptions.map((library) => (
                            <option key={library.id} value={library.id}>
                                {library.name}
                            </option>
                        ))}
                    </select>
                </label>

                <label className={styles.filterField}>
                    <span>Member</span>
                    <input
                        type="search"
                        value={memberFilter}
                        onChange={(event) => setMemberFilter(event.target.value)}
                        placeholder="Search member name or number"
                    />
                </label>

                <label className={styles.filterField}>
                    <span>Borrowed From</span>
                    <input
                        type="date"
                        value={borrowedFrom}
                        onChange={(event) => setBorrowedFrom(event.target.value)}
                        max={borrowedTo || undefined}
                    />
                </label>

                <label className={styles.filterField}>
                    <span>Borrowed To</span>
                    <input
                        type="date"
                        value={borrowedTo}
                        onChange={(event) => setBorrowedTo(event.target.value)}
                        min={borrowedFrom || undefined}
                    />
                </label>

                <button
                    type="button"
                    className={styles.secondaryButton}
                    onClick={clearFilters}
                >
                    Clear Filters
                </button>
            </section>

            {filteredLoans.length === 0 ? (
                <div className={styles.emptyState}>No loans match the current filters.</div>
            ) : (
                <div className={styles.loanList}>
                    {filteredLoans.map((loan) => {
                        const book = books[loan.bookId];
                        const library = libraries[loan.libraryId];

                        return (
                            <article key={loan.loanId} className={styles.loanCard}>
                            <div className={styles.loanHeader}>
                                <div>
                                    <Link
                                        to={`/loans/${loan.loanId}`}
                                        className={styles.loanTitle}
                                    >
                                        Loan
                                    </Link>
                                    <p>{memberLabel(loan.memberId)}</p>
                                </div>

                                <span
                                    className={`${styles.status} ${
                                        isOverdue(loan)
                                            ? styles.statusOverdue
                                            : styles[`status${loan.status}`] ??
                                              styles.statusDefault
                                    }`}
                                >
                                    {isOverdue(loan) ? "OVERDUE" : loan.status}
                                </span>
                            </div>

                            <div className={styles.details}>
                                <div>
                                    <span>Book</span>
                                    <Link
                                        to={`/books/${loan.bookId}`}
                                        className={styles.detailLink}
                                    >
                                        {book?.title ?? loan.bookId}
                                    </Link>
                                </div>
                                <div>
                                    <span>Library</span>
                                    {loan.libraryId ? (
                                        <Link
                                            to={`/stock?libraryId=${encodeURIComponent(
                                                loan.libraryId
                                            )}`}
                                            className={styles.detailLink}
                                        >
                                            {library?.name ?? loan.libraryId}
                                        </Link>
                                    ) : (
                                        <strong>-</strong>
                                    )}
                                </div>
                                <div>
                                    <span>Borrowed</span>
                                    <strong>{formatDateTime(loan.borrowedAt)}</strong>
                                </div>
                                <div>
                                    <span>Due</span>
                                    <strong>{formatDateTime(loan.dueAt)}</strong>
                                </div>
                                <div>
                                    <span>Returned</span>
                                    <strong>{formatDateTime(loan.returnedAt)}</strong>
                                </div>
                            </div>
                        </article>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

export default BorrowingPage;
